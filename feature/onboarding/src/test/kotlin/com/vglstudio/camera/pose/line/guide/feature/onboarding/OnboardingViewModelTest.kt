package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Onboarding logic (spec onboarding, KB-1 and KB-2; plan T-012, section 4). */
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) =
        OnboardingViewModel(analytics = analytics, logger = logger, savedStateHandle = savedState)

    private fun onboardingLog(level: LogLevel, message: String) = LogEntry(level, "Onboarding", message)

    @Test
    fun shown_logsShown_withoutEventOrAnalytics() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            expectNoEvents()
        }
        assertEquals(OnboardingUiState.Ready, viewModel.uiState.value)
        assertTrue(analytics.events.isEmpty())
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Onboarding shown")))
    }

    @Test
    fun enableCamera_withoutPermission_logsComplete_thenStarted_thenRequestPermission() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.RequestingPermission, viewModel.uiState.value)
    }

    @Test
    fun enableCamera_tappedTwice_logsOnce_andRequestsOnce() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(COMPLETE), analytics.events)
    }

    @Test
    fun permissionGranted_logsResultWithOnboardingSource_andFinishes() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = true)
            assertEquals(OnboardingEvent.Finished, awaitItem())
        }
        assertEquals(2, analytics.events.size)
        assertEquals(resultEvent("granted"), analytics.events[1])
        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Camera permission granted")))
    }

    @Test
    fun twoTaps_reachFinished_withNothingInBetween() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            viewModel.onPermissionResult(granted = true)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
    }

    @Test
    fun enableCamera_permissionAlreadyGranted_finishesWithoutPromptOrResultEvent() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = true)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
    }

    @Test
    fun permissionDeniedWithDialog_logsDenied_andStillFinishes() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = false, dialogShown = true)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(COMPLETE, resultEvent("denied")), analytics.events)
        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
    }

    @Test
    fun permissionDeniedWithoutDialog_logsNoResult_andStillFinishes() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = false, dialogShown = false)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Camera permission denied without dialog")))
    }

    @Test
    fun permissionResult_whenNotRequesting_isIgnored() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onPermissionResult(granted = true)
            expectNoEvents()
            assertTrue(analytics.events.isEmpty())
            assertEquals(OnboardingUiState.Ready, viewModel.uiState.value)
            // The stray result changed nothing: the next tap still asks for the permission (so the test cannot
            // pass on a view model that ignores every call).
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
        }
        assertEquals(listOf(COMPLETE), analytics.events)
    }

    @Test
    fun restoredWhileRequesting_acceptsResult_withoutSecondComplete() = runTest {
        val viewModel = viewModel(
            SavedStateHandle(mapOf("requesting_permission" to true, "completion_logged" to true)),
        )
        assertEquals(OnboardingUiState.RequestingPermission, viewModel.uiState.value)
        viewModel.events.test {
            viewModel.onPermissionResult(granted = false)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(OnboardingUiState.Finished, viewModel.uiState.value)
        assertTrue(analytics.events.none { it.name == "onboarding_complete" })
    }

    @Test
    fun logs_useOnboardingTag() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            viewModel.onPermissionResult(granted = true)
            skipItems(3)
        }
        assertTrue(logger.entries.isNotEmpty())
        assertTrue(logger.entries.all { it.tag == "Onboarding" })
        assertEquals(
            listOf(
                "Onboarding shown",
                "Enable camera tapped",
                "Asking for the camera permission",
                "Camera permission granted",
            ),
            logger.entries.map { it.message },
        )
    }

    private fun resultEvent(result: String) = AnalyticsEvent(
        name = "camera_permission_result",
        params = mapOf("result" to result, "source" to "onboarding"),
    )

    private companion object {
        val COMPLETE = AnalyticsEvent("onboarding_complete")
    }
}

private class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}
