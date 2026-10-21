package com.vglstudio.camera.pose.line.guide.feature.sample

import app.cash.turbine.ReceiveTurbine
import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.sample.data.CounterRepository
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

class SampleViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakeCounterRepository()
    private val analytics = FakeAnalytics()
    private val featureFlags = FakeFeatureFlags(enabled = true)
    private val logger = RecordingAppLogger()

    private fun viewModel() = SampleViewModel(
        repository = repository,
        analytics = analytics,
        logger = logger,
        featureFlags = featureFlags,
    )

    @Test
    fun whenStarted_thenShowsStoredCount() = runTest {
        repository.counter.value = 4
        viewModel().uiState.test {
            assertEquals(SampleUiState.Ready(4), awaitItemAfterLoading())
        }
    }

    @Test
    fun whenIncrement_thenCountGoesUpAndEventIsLogged() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SampleUiState.Ready(0), awaitItemAfterLoading())
            viewModel.onIncrement()
            assertEquals(SampleUiState.Ready(1), awaitItem())
        }
        assertEquals(listOf(AnalyticsEvent(SampleViewModel.EVENT_INCREMENT)), analytics.events)
    }

    @Test
    fun whenSaveFails_thenShowsErrorWithoutCrashingOrLogging() = runTest {
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SampleUiState.Ready(0), awaitItemAfterLoading())
            repository.failNextWrite = true
            viewModel.onIncrement()
            assertEquals(SampleUiState.Ready(0, saveFailed = true), awaitItem())

            // A new attempt clears the error and shows the saved value. The intermediate state
            // Ready(0, saveFailed = false) may be conflated away, so it is optional here.
            viewModel.onIncrement()
            var next = awaitItem()
            if (next == SampleUiState.Ready(0, saveFailed = false)) next = awaitItem()
            assertEquals(SampleUiState.Ready(1, saveFailed = false), next)
        }
        assertEquals(listOf(AnalyticsEvent(SampleViewModel.EVENT_INCREMENT)), analytics.events)
        assertEquals(listOf(LogLevel.WARN, LogLevel.INFO), logger.entries.map { it.level })
    }

    @Test
    fun whenFlagIsOff_thenScreenIsDisabledAndIncrementDoesNothing() = runTest {
        featureFlags.enabled.value = false
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SampleUiState.Disabled, awaitItemAfterLoading())
        }
        viewModel.onIncrement()
        assertEquals(0, repository.counter.value)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
    }

    @Test
    fun whenFlagTurnsOnLater_thenScreenBecomesReady() = runTest {
        featureFlags.enabled.value = false
        val viewModel = viewModel()
        viewModel.uiState.test {
            assertEquals(SampleUiState.Disabled, awaitItemAfterLoading())
            featureFlags.enabled.value = true
            assertEquals(SampleUiState.Ready(0), awaitItem())
        }
    }
}

private suspend fun ReceiveTurbine<SampleUiState>.awaitItemAfterLoading(): SampleUiState {
    val first = awaitItem()
    return if (first == SampleUiState.Loading) awaitItem() else first
}

private class FakeCounterRepository : CounterRepository {
    val counter = MutableStateFlow(0)
    var failNextWrite = false
    override val count: StateFlow<Int> = counter

    override suspend fun increment() {
        if (failNextWrite) {
            failNextWrite = false
            throw IOException("disk full")
        }
        counter.value += 1
    }
}

private class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

private class FakeFeatureFlags(enabled: Boolean) : FeatureFlags {
    val enabled = MutableStateFlow(enabled)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> = enabled
}
