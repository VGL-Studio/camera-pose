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

    /** Remote Config and the catalog done: the starter set is running at 20%. */
    private fun configAndCatalogDone() {
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.complete(PreparationTask.CATALOG, true)
    }

    @Test
    fun flagOn_startsAtZero_andRunsRemoteConfigOnly() = runTest {
        val viewModel = viewModel()
        assertEquals(InProgress(0f), viewModel.uiState.value)
        assertEquals(listOf(PreparationTask.REMOTE_CONFIG), repository.calls)
    }

    @Test
    fun tasksRunInOrder_configThenCatalogThenStarterSet() = runTest {
        viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        assertEquals(listOf(PreparationTask.REMOTE_CONFIG, PreparationTask.CATALOG), repository.calls)
        repository.complete(PreparationTask.CATALOG, true)
        assertEquals(
            listOf(PreparationTask.REMOTE_CONFIG, PreparationTask.CATALOG, PreparationTask.STARTER_SET),
            repository.calls,
        )
    }

    @Test
    fun progress_isTenTenEighty_starterSetPerFile_neverDecreasing() = runTest {
        val viewModel = viewModel()
        val seen = mutableListOf<LoadingUiState>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.uiState.collect { seen += it } }

        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        assertEquals(InProgress(0.1f), viewModel.uiState.value)
        repository.complete(PreparationTask.CATALOG, true)
        assertEquals(InProgress(0.2f), viewModel.uiState.value)
        repository.progress(0, 8)
        assertEquals(InProgress(0.2f), viewModel.uiState.value)
        repository.progress(1, 8)
        assertEquals(InProgress(0.3f), viewModel.uiState.value)
        repository.progress(4, 8)
        assertEquals(InProgress(0.6f), viewModel.uiState.value)
        repository.progress(8, 8)
        assertEquals(InProgress(1f), viewModel.uiState.value)
        repository.complete(PreparationTask.STARTER_SET, true)
        assertEquals(InProgress(1f), viewModel.uiState.value)

        val progress = seen.map { (it as InProgress).progress }
        assertEquals(progress.sorted(), progress)
    }

    @Test
    fun starterSetProgressOutOfOrder_neverGoesBack() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        repository.progress(3, 8)
        repository.progress(2, 8)
        assertEquals(InProgress(0.5f), viewModel.uiState.value)
    }

    @Test
    fun starterSetOfSixtyFiles_roundsDownPerFile() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        // 800 * 1 / 60 = 13 thousandths.
        repository.progress(1, 60)
        assertEquals(InProgress(0.213f), viewModel.uiState.value)
        repository.progress(59, 60)
        assertEquals(InProgress(0.986f), viewModel.uiState.value)
    }

    @Test
    fun starterSetWithoutFiles_reachesFullWhenDone() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        repository.progress(0, 0)
        assertEquals(InProgress(0.2f), viewModel.uiState.value)
        repository.complete(PreparationTask.STARTER_SET, true)
        assertEquals(InProgress(1f), viewModel.uiState.value)
    }

    @Test
    fun taskShares_addUpToWholeBar() {
        assertEquals(PreparationTask.TOTAL_SHARE, PreparationTask.entries.sumOf { it.share })
        assertEquals(1000, PreparationTask.TOTAL_SHARE)
        assertEquals(
            listOf(PreparationTask.REMOTE_CONFIG, PreparationTask.CATALOG, PreparationTask.STARTER_SET),
            PreparationTask.entries.toList(),
        )
    }

    @Test
    fun allTasksDoneAfterTwoSeconds_finishesAtOnce() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(2_500)
        runCurrent()
        repository.completeInOrder()
        assertEquals(Finished, viewModel.uiState.value)
    }

    @Test
    fun allTasksDoneBeforeTwoSeconds_staysAtFullUntilTwoSeconds() = runTest {
        val viewModel = viewModel()
        advanceTimeBy(500)
        runCurrent()
        repository.completeInOrder()
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
        repository.completeInOrder()
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
        repository.complete(PreparationTask.CATALOG, false)
        assertTrue(viewModel.uiState.value is Failed)
        viewModel.onRetry()
        repository.completeInOrder()
        advanceTimeBy(2_000)
        runCurrent()
        assertEquals(Finished, viewModel.uiState.value)

        val entries = logger.entries
        assertEquals(
            listOf(
                LogLevel.INFO, // started
                LogLevel.INFO, // REMOTE_CONFIG done
                LogLevel.WARN, // CATALOG failed
                LogLevel.WARN, // waiting for retry
                LogLevel.INFO, // retry 1
                LogLevel.INFO, // CATALOG done
                LogLevel.INFO, // STARTER_SET done
                LogLevel.INFO, // finished
            ),
            entries.map { it.level },
        )
        assertTrue(entries.all { it.tag == LoadingViewModel.LOG_TAG })
        // Task names only: never a path or a pose id (ADR-0019).
        assertFalse(
            entries.any { entry ->
                listOf("poses/", ".json", ".webp", "test/v1").any { it in entry.message }
            },
        )
    }

    @Test
    fun remoteConfigFails_showsFailedAtZero_andSkipsCatalogAndStarterSet() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, false)
        assertEquals(Failed(0f), viewModel.uiState.value)
        assertEquals(listOf(PreparationTask.REMOTE_CONFIG), repository.calls)
        advanceTimeBy(20_000)
        runCurrent()
        assertEquals(Failed(0f), viewModel.uiState.value)
        assertEquals(emptyList<AnalyticsEvent>(), analytics.events)
    }

    @Test
    fun catalogFails_showsFailedAtTenPercent_andSkipsStarterSet() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.complete(PreparationTask.CATALOG, false)
        assertEquals(Failed(0.1f), viewModel.uiState.value)
        assertFalse(PreparationTask.STARTER_SET in repository.calls)
    }

    @Test
    fun starterSetFails_showsFailedAtReachedProgress() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        repository.progress(5, 8)
        repository.complete(PreparationTask.STARTER_SET, false)
        assertEquals(Failed(0.7f), viewModel.uiState.value)
    }

    @Test
    fun taskThrows_isTreatedAsFailure_andShowsFailed() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        repository.fail(PreparationTask.CATALOG, IllegalStateException("unexpected"))
        assertEquals(Failed(0.1f), viewModel.uiState.value)
        assertTrue(logger.entries.any { it.error is IllegalStateException })
    }

    @Test
    fun retry_afterStarterSetFailed_runsOnlyStarterSet_andProgressContinues() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        repository.progress(5, 8)
        repository.complete(PreparationTask.STARTER_SET, false)
        assertEquals(Failed(0.7f), viewModel.uiState.value)

        repository.calls.clear()
        viewModel.onRetry()
        assertEquals(InProgress(0.7f), viewModel.uiState.value)
        assertEquals(listOf(PreparationTask.STARTER_SET), repository.calls)
        repository.progress(5, 8)
        assertEquals(InProgress(0.7f), viewModel.uiState.value)
        repository.progress(6, 8)
        assertEquals(InProgress(0.8f), viewModel.uiState.value)
    }

    @Test
    fun retry_afterRemoteConfigFailed_runsAllThreeInOrder() = runTest {
        val viewModel = viewModel()
        repository.complete(PreparationTask.REMOTE_CONFIG, false)
        assertEquals(Failed(0f), viewModel.uiState.value)

        repository.calls.clear()
        viewModel.onRetry()
        assertEquals(listOf(PreparationTask.REMOTE_CONFIG), repository.calls)
        repository.complete(PreparationTask.REMOTE_CONFIG, true)
        assertEquals(listOf(PreparationTask.REMOTE_CONFIG, PreparationTask.CATALOG), repository.calls)
        repository.complete(PreparationTask.CATALOG, true)
        assertEquals(
            listOf(PreparationTask.REMOTE_CONFIG, PreparationTask.CATALOG, PreparationTask.STARTER_SET),
            repository.calls,
        )
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
        repository.complete(PreparationTask.CATALOG, false)
        assertEquals(Failed(0.1f), viewModel.uiState.value)

        advanceTimeBy(3_000)
        runCurrent()
        viewModel.onRetry()
        repository.completeInOrder()
        assertEquals(Finished, viewModel.uiState.value)
        assertEquals(listOf(completedEvent(durationMs = 3_000, retryCount = 1)), analytics.events)
    }

    @Test
    fun progressAfterFailed_keepsFailed() = runTest {
        val viewModel = viewModel()
        configAndCatalogDone()
        repository.progress(5, 8)
        repository.complete(PreparationTask.STARTER_SET, false)
        assertEquals(Failed(0.7f), viewModel.uiState.value)

        // A late report from the run that just failed.
        repository.progress(8, 8)
        assertEquals(Failed(0.7f), viewModel.uiState.value)
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

/**
 * Each [run] call waits until the test calls [complete] or [fail] for that task. The progress callback of the last
 * starter set run is kept, so [progress] reports files for it, also after that run ended.
 */
private class FakePreparationRepository : PreparationRepository {
    val calls = mutableListOf<PreparationTask>()
    private val pending = mutableMapOf<PreparationTask, CompletableDeferred<Boolean>>()
    private var starterSetProgress: ((Int, Int) -> Unit)? = null

    override suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean {
        calls += task
        if (task == PreparationTask.STARTER_SET) starterSetProgress = onProgress
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

    fun progress(done: Int, total: Int) {
        checkNotNull(starterSetProgress) { "The starter set has not started" }(done, total)
    }

    /** Completes, one after the other, every task that runs, until none is left. */
    fun completeInOrder() {
        while (pending.isNotEmpty()) {
            PreparationTask.entries.filter { it in pending }.forEach { complete(it, true) }
        }
    }
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
