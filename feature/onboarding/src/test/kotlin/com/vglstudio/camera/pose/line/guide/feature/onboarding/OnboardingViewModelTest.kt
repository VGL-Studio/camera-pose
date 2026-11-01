package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.LogEntry
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.CAMERA
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.INTRO
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.POSES
import com.vglstudio.camera.pose.line.guide.feature.onboarding.OnboardingPage.PREMIUM
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** Onboarding logic (spec onboarding, KB-1, KB-2 and KB-5; plan T-012, section 4; plan T-082, section 4). */
class OnboardingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()
    private val flags = FakeFeatureFlags()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = OnboardingViewModel(
        analytics = analytics,
        logger = logger,
        featureFlags = flags,
        savedStateHandle = savedState,
    )

    private fun onboardingLog(level: LogLevel, message: String) = LogEntry(level, "Onboarding", message)

    /** "Next" up to the last page (premium flag off: two taps). */
    private fun toLastPage(viewModel: OnboardingViewModel) {
        repeat(2) { viewModel.onNextClick() }
        assertTrue("on the last page", viewModel.uiState.value.isLastPage)
    }

    @Test
    fun shown_logsShownAndStepOne_withoutEvent() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            expectNoEvents()
        }
        assertEquals(OnboardingUiState.Browsing(PAGES_3, 0), viewModel.uiState.value)
        assertEquals(listOf(step(1)), analytics.events)
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Onboarding shown")))
    }

    @Test
    fun enableCamera_withoutPermission_logsComplete_thenStarted_thenRequestPermission() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(step(1), step(2), step(3), COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.RequestingPermission(PAGES_3), viewModel.uiState.value)
    }

    @Test
    fun enableCamera_tappedTwice_logsOnce_andRequestsOnce() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(step(1), step(2), step(3), COMPLETE), analytics.events)
    }

    @Test
    fun permissionGranted_logsResultWithOnboardingSource_andFinishes() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = true)
            assertEquals(OnboardingEvent.Finished, awaitItem())
        }
        assertEquals(5, analytics.events.size)
        assertEquals(resultEvent("granted"), analytics.events[4])
        assertEquals(OnboardingUiState.Finished(PAGES_3), viewModel.uiState.value)
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Camera permission granted")))
    }

    @Test
    fun twoTaps_reachFinished_withNothingInBetween() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
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
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = true)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(step(1), step(2), step(3), COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.Finished(PAGES_3), viewModel.uiState.value)
    }

    @Test
    fun permissionDeniedWithDialog_logsDenied_andStillFinishes() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = false, dialogShown = true)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(step(1), step(2), step(3), COMPLETE, resultEvent("denied")), analytics.events)
        assertEquals(OnboardingUiState.Finished(PAGES_3), viewModel.uiState.value)
    }

    @Test
    fun permissionDeniedWithoutDialog_logsNoResult_andStillFinishes() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            skipItems(2)
            viewModel.onPermissionResult(granted = false, dialogShown = false)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(listOf(step(1), step(2), step(3), COMPLETE), analytics.events)
        assertEquals(OnboardingUiState.Finished(PAGES_3), viewModel.uiState.value)
        assertTrue(logger.entries.contains(onboardingLog(LogLevel.INFO, "Camera permission denied without dialog")))
    }

    @Test
    fun permissionResult_whenNotRequesting_isIgnored() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onPermissionResult(granted = true)
            expectNoEvents()
            assertEquals(listOf(step(1)), analytics.events)
            assertEquals(OnboardingUiState.Browsing(PAGES_3, 0), viewModel.uiState.value)
            // The stray result changed nothing: the next tap still asks for the permission (so the test cannot
            // pass on a view model that ignores every call).
            toLastPage(viewModel)
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
        }
        assertEquals(listOf(COMPLETE), analytics.events.filter { it.name != STEP })
    }

    @Test
    fun restoredWhileRequesting_acceptsResult_withoutSecondComplete() = runTest {
        // Control: without the viewed steps, the restored last page counts as shown for the first time.
        viewModel(
            SavedStateHandle(
                mapOf(
                    "requesting_permission" to true,
                    "completion_logged" to true,
                    "page_index" to 2,
                    "premium_page" to false,
                ),
            ),
        )
        assertEquals(listOf(step(3)), analytics.events)
        analytics.events.clear()
        val viewModel = viewModel(
            SavedStateHandle(
                mapOf(
                    "requesting_permission" to true,
                    "completion_logged" to true,
                    "page_index" to 2,
                    "premium_page" to false,
                    "viewed_steps" to intArrayOf(1, 2, 3),
                ),
            ),
        )
        assertEquals(OnboardingUiState.RequestingPermission(PAGES_3), viewModel.uiState.value)
        // Restoring logs no step again.
        assertTrue(analytics.events.isEmpty())
        viewModel.events.test {
            viewModel.onPermissionResult(granted = false)
            assertEquals(OnboardingEvent.Finished, awaitItem())
            expectNoEvents()
        }
        assertEquals(OnboardingUiState.Finished(PAGES_3), viewModel.uiState.value)
        assertTrue(analytics.events.none { it.name == "onboarding_complete" })
        assertEquals(listOf(resultEvent("denied")), analytics.events)
    }

    @Test
    fun restoredOnLastPage_withCompletionLogged_tapDoesNotLogAgain() = runTest {
        val viewModel = viewModel(
            SavedStateHandle(
                mapOf(
                    "completion_logged" to true,
                    "page_index" to 2,
                    "premium_page" to false,
                    "viewed_steps" to intArrayOf(1, 2, 3),
                ),
            ),
        )
        // Control: restored on the last page, where "Turn on camera" is.
        assertEquals(OnboardingUiState.Browsing(PAGES_3, 2), viewModel.uiState.value)
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            assertEquals(OnboardingEvent.Started, awaitItem())
            assertEquals(OnboardingEvent.RequestPermission, awaitItem())
        }
        assertTrue(analytics.events.none { it.name == "onboarding_complete" })
    }

    @Test
    fun logs_useOnboardingTag() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
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
                "Page shown: 1 intro",
                "Page shown: 2 poses",
                "Page shown: 3 camera",
                "Enable camera tapped",
                "Asking for the camera permission",
                "Camera permission granted",
            ),
            logger.entries.map { it.message },
        )
    }

    // ---- Multi-page flow (plan T-082, section 4) ----

    @Test
    fun premiumOff_threePages_withoutPremium() = runTest {
        val viewModel = viewModel()
        assertEquals(PAGES_3, viewModel.uiState.value.pages)
        viewModel.onNextClick()
        viewModel.onNextClick()
        assertEquals(CAMERA, viewModel.uiState.value.page)
        assertTrue(viewModel.uiState.value.isLastPage)
    }

    @Test
    fun premiumOn_fourPages_thirdIsPremium() = runTest {
        flags.premium.value = true
        val viewModel = viewModel()
        assertEquals(PAGES_4, viewModel.uiState.value.pages)
        viewModel.onNextClick()
        viewModel.onNextClick()
        assertEquals(PREMIUM, viewModel.uiState.value.page)
        viewModel.onNextClick()
        assertEquals(CAMERA, viewModel.uiState.value.page)
        assertTrue(viewModel.uiState.value.isLastPage)
    }

    @Test
    fun premiumTurnsOn_onFirstOrSecondPage_listFollows() = runTest {
        val viewModel = viewModel()
        viewModel.onNextClick()
        assertEquals(PAGES_3, viewModel.uiState.value.pages)
        flags.premium.value = true
        assertEquals(PAGES_4, viewModel.uiState.value.pages)
        assertEquals(1, viewModel.uiState.value.index)
        // Control: back off before the third page, the list follows again.
        flags.premium.value = false
        assertEquals(PAGES_3, viewModel.uiState.value.pages)
        assertEquals(1, viewModel.uiState.value.index)
    }

    @Test
    fun premiumChanges_afterThirdPage_listStays() = runTest {
        flags.premium.value = true
        val viewModel = viewModel()
        viewModel.onNextClick()
        viewModel.onNextClick()
        assertEquals(PREMIUM, viewModel.uiState.value.page)
        flags.premium.value = false
        assertEquals(PAGES_4, viewModel.uiState.value.pages)
        assertEquals(PREMIUM, viewModel.uiState.value.page)
        viewModel.onBackClick()
        viewModel.onBackClick()
        assertEquals(0, viewModel.uiState.value.index)
        flags.premium.value = true
        flags.premium.value = false
        assertEquals(PAGES_4, viewModel.uiState.value.pages)
    }

    @Test
    fun next_movesOnePage_untilLast() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            assertEquals(0, viewModel.uiState.value.index)
            viewModel.onNextClick()
            assertEquals(1, viewModel.uiState.value.index)
            viewModel.onNextClick()
            assertEquals(2, viewModel.uiState.value.index)
            val last = viewModel.uiState.value
            val eventsBefore = analytics.events.toList()
            viewModel.onNextClick()
            assertEquals(last, viewModel.uiState.value)
            assertEquals(eventsBefore, analytics.events)
            expectNoEvents()
        }
    }

    @Test
    fun back_movesToPreviousPage_notBeforeFirst() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        assertEquals(2, viewModel.uiState.value.index)
        viewModel.onBackClick()
        assertEquals(1, viewModel.uiState.value.index)
        viewModel.onBackClick()
        assertEquals(0, viewModel.uiState.value.index)
        val backLogs = logger.entries.count { it.message.startsWith("Back to page") }
        viewModel.onBackClick()
        assertEquals(0, viewModel.uiState.value.index)
        assertEquals(backLogs, logger.entries.count { it.message.startsWith("Back to page") })
    }

    @Test
    fun back_whileRequestingPermission_isIgnored() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.onEnableCameraClick(permissionGranted = false)
        viewModel.onBackClick()
        assertEquals(OnboardingUiState.RequestingPermission(PAGES_3), viewModel.uiState.value)
    }

    @Test
    fun stepViewed_loggedOncePerStep() = runTest {
        val viewModel = viewModel()
        toLastPage(viewModel)
        viewModel.onBackClick()
        viewModel.onBackClick()
        toLastPage(viewModel)
        assertEquals(listOf(step(1), step(2), step(3)), analytics.events)
        // Control: with four pages the fourth step is logged as well.
        analytics.events.clear()
        flags.premium.value = true
        val fourPages = viewModel()
        repeat(3) { fourPages.onNextClick() }
        assertEquals(listOf(step(1), step(2), step(3), step(4)), analytics.events)
    }

    @Test
    fun restoredOnSecondPage_keepsPage_doesNotLogAgain() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("page_index" to 1, "viewed_steps" to intArrayOf(1, 2))))
        assertEquals(1, viewModel.uiState.value.index)
        assertTrue(analytics.events.isEmpty())
        viewModel.onNextClick()
        assertEquals(listOf(step(3)), analytics.events)
    }

    @Test
    fun restoredOnPremiumPage_keepsFourPages_evenIfFlagOff() = runTest {
        val viewModel = viewModel(SavedStateHandle(mapOf("page_index" to 2, "premium_page" to true)))
        assertEquals(PAGES_4, viewModel.uiState.value.pages)
        assertEquals(PREMIUM, viewModel.uiState.value.page)
    }

    @Test
    fun next_doesNotSendStarted() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            toLastPage(viewModel)
            expectNoEvents()
        }
    }

    @Test
    fun newViewModel_startsOnFirstPage() = runTest {
        // Control: a saved page is restored, so the empty handle below really starts over.
        assertEquals(1, viewModel(SavedStateHandle(mapOf("page_index" to 1))).uiState.value.index)
        assertEquals(0, viewModel(SavedStateHandle()).uiState.value.index)
    }

    @Test
    fun enableCamera_beforeLastPage_isIgnored() = runTest {
        val viewModel = viewModel()
        viewModel.events.test {
            viewModel.onEnableCameraClick(permissionGranted = false)
            viewModel.onNextClick()
            assertEquals(1, viewModel.uiState.value.index)
            viewModel.onEnableCameraClick(permissionGranted = false)
            expectNoEvents()
        }
        assertTrue(analytics.events.none { it.name == "onboarding_complete" })
        assertEquals(OnboardingUiState.Browsing(PAGES_3, 1), viewModel.uiState.value)
    }

    @Test
    fun taps_equalPagesPlusOne() = runTest {
        listOf(false, true).forEach { premium ->
            flags.premium.value = premium
            val viewModel = viewModel()
            var taps = 0
            while (!viewModel.uiState.value.isLastPage && taps < MAX_TAPS) {
                viewModel.onNextClick()
                taps++
            }
            viewModel.onEnableCameraClick(permissionGranted = false)
            taps++
            viewModel.onPermissionResult(granted = true)
            taps++
            val state = viewModel.uiState.value
            assertTrue("premium $premium: finished", state is OnboardingUiState.Finished)
            assertEquals("premium $premium", if (premium) 4 else 3, state.pages.size)
            assertEquals("premium $premium", state.pages.size + 1, taps)
        }
    }

    @Test
    fun logs_pageAndBack() = runTest {
        val viewModel = viewModel()
        viewModel.onNextClick()
        viewModel.onBackClick()
        flags.premium.value = true
        val messages = logger.entries.map { it.message }
        assertTrue(messages.toString(), messages.contains("Page shown: 2 poses"))
        assertTrue(messages.toString(), messages.contains("Back to page 1"))
        assertTrue(messages.toString(), messages.contains("Premium page on"))
        assertFalse(messages.toString(), messages.contains("Premium page off"))
    }

    private fun resultEvent(result: String) = AnalyticsEvent(
        name = "camera_permission_result",
        params = mapOf("result" to result, "source" to "onboarding"),
    )

    @Test
    fun introAdWait_logsStartAndEnd() {
        val viewModel = viewModel()
        // Control: nothing about the hold before it is reported.
        assertFalse(logger.entries.any { it.message.startsWith("Intro ad wait") })
        viewModel.onIntroAdWaitStarted()
        viewModel.onIntroAdWaitEnded(IntroAdWaitEnd.TIMEOUT)
        val hold = logger.entries.filter { it.message.startsWith("Intro ad wait") }
        assertEquals(
            listOf(
                onboardingLog(LogLevel.INFO, "Intro ad wait started"),
                onboardingLog(LogLevel.INFO, "Intro ad wait ended: timeout"),
            ),
            hold,
        )
    }

    private companion object {
        const val STEP = "onboarding_step_viewed"
        const val MAX_TAPS = 10
        val COMPLETE = AnalyticsEvent("onboarding_complete")
        val PAGES_3 = listOf(INTRO, POSES, CAMERA)
        val PAGES_4 = listOf(INTRO, POSES, PREMIUM, CAMERA)

        fun step(n: Int) = AnalyticsEvent(STEP, mapOf("step" to "$n"))
    }
}

private class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

/** [premium] answers FeatureFlag.PREMIUM_POSES; every other flag is off. */
private class FakeFeatureFlags(val premium: MutableStateFlow<Boolean> = MutableStateFlow(false)) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> =
        if (flag == FeatureFlag.PREMIUM_POSES) premium else MutableStateFlow(false)
}
