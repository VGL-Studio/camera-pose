package com.vglstudio.camera.pose.line.guide.feature.loading

import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.loading.LoadingUiState.Failed
import com.vglstudio.camera.pose.line.guide.feature.loading.LoadingUiState.Finished
import com.vglstudio.camera.pose.line.guide.feature.loading.LoadingUiState.InProgress
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationRepository
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationTask
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LoadingViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val repository = FakePreparationRepository()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()

    private fun TestScope.viewModel(flagOn: Boolean = true) = LoadingViewModel(
        repository = repository,
        analytics = analytics,
        logger = logger,
        featureFlags = FakeFeatureFlags(flagOn),
        clock = { testScheduler.currentTime },
    )

    private fun completedEvent(durationMs: Long, retryCount: Int) = AnalyticsEvent(
        name = LoadingViewModel.EVENT_COMPLETED,
        params = mapOf("duration_ms" to durationMs.toString(), "retry_count" to retryCount.toString()),
    )

    @Test
    fun flagOn_startsInProgressAtZero() = runTest {
        val viewModel = viewModel()
        assertEquals(InProgress(0f), viewModel.uiState.value)
        assertEquals(PreparationTask.entries.toList(), repository.calls)
    }

    @Test
    fun eachTaskDone_addsAQuarter_andNeverDecreases() = runTest {
        val viewModel = viewModel()
        val seen = mutableListOf<LoadingUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { seen += it } }

        repository.complete(PreparationTask.HELLO_FILE, true)
        assertEquals(InProgress(0.25f), viewModel.uiState.value)
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        assertEquals(InProgress(0.5f), viewModel.uiState.value)
        repository.complete(PreparationTask.GOODBYE_FILE, true)
        assertEquals(InProgress(0.75f), viewModel.uiState.value)
        repository.complete(PreparationTask.CONTENT_FILE, true)
        assertEquals(InProgress(1f), viewModel.uiState.value)

        val progress = seen.map { (it as InProgress).progress }
        assertEquals(listOf(0f, 0.25f, 0.5f, 0.75f, 1f), progress)
        assertEquals(progress.sorted(), progress)
    }

    @Test
    fun allTasksDoneAfterTwoSeconds_finishesAtOnce() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(2_500)
        runCurrent()
        repository.completeAll(true)
        assertEquals(Finished, viewModel.uiState.value)
    }

    @Test
    fun allTasksDoneBeforeTwoSeconds_staysAtFullUntilTwoSeconds() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(500)
        runCurrent()
        repository.completeAll(true)
        assertEquals(InProgress(1f), viewModel.uiState.value)
        advanceTimeBy(1_499)
        assertEquals(InProgress(1f), viewModel.uiState.value)
        advanceTimeBy(1)
        runCurrent()
        assertEquals(Finished, viewModel.uiState.value)
    }

    @Test
    fun finished_logsLoadingCompletedOnce_withDurationAndRetryCount() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(500)
        runCurrent()
        repository.completeAll(true)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
        advanceTimeBy(1_500)
        runCurrent()
        assertEquals(Finished, viewModel.uiState.value)
        advanceTimeBy(10_000)
        runCurrent()
        assertEquals(listOf(completedEvent(durationMs = 2_000, retryCount = 0)), analytics.events)
    }

    @Test
    fun logsStart_eachTaskResult_retry_andFinish() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.fail(PreparationTask.HELLO_FILE, IllegalStateException("unexpected"))
        repository.complete(PreparationTask.CONTENT_FILE, true)
        repository.complete(PreparationTask.GOODBYE_FILE, false)
        assertTrue(viewModel.uiState.value is Failed)
        viewModel.onRetry()
        repository.completeAll(true)
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(Finished, viewModel.uiState.value)

        val entries = logger.entries
        assertEquals(
            listOf(
                LogLevel.INFO, // started
                LogLevel.INFO, // REMOTE_CONFIG done
                LogLevel.WARN, // HELLO_FILE failed with an error
                LogLevel.INFO, // CONTENT_FILE done
                LogLevel.WARN, // GOODBYE_FILE failed
                LogLevel.WARN, // waiting for retry
                LogLevel.INFO, // retry 1
                LogLevel.INFO, // HELLO_FILE done
                LogLevel.INFO, // GOODBYE_FILE done
                LogLevel.INFO, // finished
            ),
            entries.map { it.level },
        )
        assertTrue(entries.all { it.tag == LoadingViewModel.LOG_TAG })
        assertTrue(entries[2].error is IllegalStateException)
        // Task names only: never a file path or content (ADR-0019).
        assertFalse(entries.any { "test/v1" in it.message || ".txt" in it.message })
    }

    @Test
    fun taskFails_showsFailedWithProgress_andDoesNotFinish() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, false)
        // Others are still running: no error yet (plan T-001, question 7).
        assertEquals(InProgress(0f), viewModel.uiState.value)
        repository.complete(PreparationTask.HELLO_FILE, true)
        repository.complete(PreparationTask.CONTENT_FILE, true)
        repository.complete(PreparationTask.GOODBYE_FILE, true)
        assertEquals(Failed(0.75f), viewModel.uiState.value)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(Failed(0.75f), viewModel.uiState.value)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
    }

    @Test
    fun taskThrows_isTreatedAsFailure_andShowsFailed() = runTest {
        val viewModel = viewModel()
        repository.fail(PreparationTask.REMOTE_CONFIG, IllegalStateException("unexpected"))
        repository.complete(PreparationTask.HELLO_FILE, true)
        repository.complete(PreparationTask.CONTENT_FILE, true)
        repository.complete(PreparationTask.GOODBYE_FILE, true)
        assertEquals(Failed(0.75f), viewModel.uiState.value)
    }

    @Test
    fun retry_runsOnlyUnfinishedTasks_andProgressContinues() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.complete(PreparationTask.HELLO_FILE, false)
        repository.complete(PreparationTask.CONTENT_FILE, true)
        repository.complete(PreparationTask.GOODBYE_FILE, false)
        assertEquals(Failed(0.5f), viewModel.uiState.value)

        repository.calls.clear()
        viewModel.onRetry()
        assertEquals(InProgress(0.5f), viewModel.uiState.value)
        assertEquals(listOf(PreparationTask.HELLO_FILE, PreparationTask.GOODBYE_FILE), repository.calls)
    }

    @Test
    fun retry_whileInProgress_doesNothing() = runTest {
        val viewModel = viewModel()
        repository.calls.clear()
        viewModel.onRetry()
        assertTrue(repository.calls.isEmpty())
        assertEquals(InProgress(0f), viewModel.uiState.value)
    }

    @Test
    fun retrySucceeds_finishes_andCountsOneRetry() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.complete(PreparationTask.HELLO_FILE, false)
        repository.complete(PreparationTask.CONTENT_FILE, true)
        repository.complete(PreparationTask.GOODBYE_FILE, true)
        assertEquals(Failed(0.75f), viewModel.uiState.value)

        advanceTimeBy(3_000)
        runCurrent()
        viewModel.onRetry()
        repository.complete(PreparationTask.HELLO_FILE, true)
        assertEquals(Finished, viewModel.uiState.value)
        assertEquals(listOf(completedEvent(durationMs = 3_000, retryCount = 1)), analytics.events)
    }

    @Test
    fun flagOff_finishesAtOnce_withoutRunningTasksOrEvent() = runTest {
        val viewModel = viewModel(flagOn = false)
        assertEquals(Finished, viewModel.uiState.value)
        assertEquals(emptyList<PreparationTask>(), repository.calls)
        advanceTimeBy(5_000)
        runCurrent()
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
        assertEquals(listOf(LogLevel.INFO), logger.entries.map { it.level })
    }
}

/** Each [run] call waits until the test calls [complete] for that task. */
private class FakePreparationRepository : PreparationRepository {
    val calls = mutableListOf<PreparationTask>()
    private val pending = mutableMapOf<PreparationTask, CompletableDeferred<Boolean>>()

    override suspend fun run(task: PreparationTask): Boolean {
        calls += task
        val result = CompletableDeferred<Boolean>()
        pending[task] = result
        return result.await()
    }

    fun complete(task: PreparationTask, success: Boolean) {
        checkNotNull(pending.remove(task)) { "$task is not running" }.complete(success)
    }

    fun fail(task: PreparationTask, error: Throwable) {
        checkNotNull(pending.remove(task)) { "$task is not running" }.completeExceptionally(error)
    }

    fun completeAll(success: Boolean) = pending.keys.toList().forEach { complete(it, success) }
}

private class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

private class FakeFeatureFlags(private val on: Boolean) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(on)
}
