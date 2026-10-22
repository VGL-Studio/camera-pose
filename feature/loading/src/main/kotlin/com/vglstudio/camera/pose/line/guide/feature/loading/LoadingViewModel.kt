package com.vglstudio.camera.pose.line.guide.feature.loading

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationRepository
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationTask
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/** Returns a monotonic time in milliseconds. A fun interface so tests can use virtual time. */
fun interface LoadingClock {
    fun elapsedRealtimeMillis(): Long
}

/**
 * Runs the three preparation tasks one after the other: Remote Config 10%, the pose catalog 10%, the starter set
 * 80%, growing with each file (spec: loading). The catalog needs the path just fetched with Remote Config, and the
 * starter set needs the catalog. Progress never goes down, also across retries.
 * The screen stays at least [MIN_DURATION_MILLIS] and, if a task fails, waits for the user to retry.
 * The flag is read once when the screen opens: it only decides whether the app shows loading at start-up,
 * so watching it afterwards would mean nothing (plan T-001, question 3).
 */
@HiltViewModel
class LoadingViewModel @Inject constructor(
    private val repository: PreparationRepository,
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val featureFlags: FeatureFlags,
    private val clock: LoadingClock,
) : ViewModel() {
    private val state = MutableStateFlow<LoadingUiState>(LoadingUiState.InProgress(0f))
    val uiState: StateFlow<LoadingUiState> = state.asStateFlow()

    // Read by progress() from the threads that load files, so a concurrent set.
    private val doneTasks: MutableSet<PreparationTask> = ConcurrentHashMap.newKeySet()

    /** Thousandths of the bar reached by the starter set; only grows. Written from the threads that load files. */
    private val starterShare = AtomicInteger(0)
    private var startedAt = 0L
    private var retryCount = 0

    init {
        viewModelScope.launch {
            if (featureFlags.enabled(FeatureFlag.LOADING).first()) {
                startedAt = clock.elapsedRealtimeMillis()
                logger.info(LOG_TAG, "Loading started")
                runPending()
            } else {
                logger.info(LOG_TAG, "Loading is off, going to the start screen")
                state.value = LoadingUiState.Finished
            }
        }
    }

    fun onRetry() {
        if (state.value !is LoadingUiState.Failed) return
        retryCount++
        logger.info(LOG_TAG, "Retry $retryCount")
        state.value = LoadingUiState.InProgress(progress())
        viewModelScope.launch { runPending() }
    }

    private suspend fun runPending() {
        for (task in PreparationTask.entries.filter { it !in doneTasks }) {
            if (!runSafely(task)) {
                logger.warn(LOG_TAG, "Task $task not done, waiting for retry")
                state.value = LoadingUiState.Failed(progress())
                return
            }
            doneTasks += task
            logger.info(LOG_TAG, "Task $task done")
            raiseProgress()
        }
        val remaining = MIN_DURATION_MILLIS - (clock.elapsedRealtimeMillis() - startedAt)
        if (remaining > 0) delay(remaining)
        val durationMillis = clock.elapsedRealtimeMillis() - startedAt
        analytics.log(
            AnalyticsEvent(
                name = EVENT_COMPLETED,
                params = mapOf(
                    "duration_ms" to durationMillis.toString(),
                    "retry_count" to retryCount.toString(),
                ),
            ),
        )
        logger.info(LOG_TAG, "Loading finished in $durationMillis ms after $retryCount retries")
        state.value = LoadingUiState.Finished
    }

    /** An unexpected error in one task is a failed task, not a crash of the whole app. */
    private suspend fun runSafely(task: PreparationTask): Boolean {
        val done = try {
            repository.run(task, ::onStarterSetProgress)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            logger.warn(LOG_TAG, "Task $task failed", e)
            return false
        }
        if (!done) logger.warn(LOG_TAG, "Task $task failed")
        return done
    }

    /** May be called from any thread; never lowers the progress, never replaces Failed or Finished. */
    private fun onStarterSetProgress(done: Int, total: Int) {
        if (total <= 0) return
        val share = PreparationTask.STARTER_SET.share * done.coerceIn(0, total) / total
        starterShare.accumulateAndGet(share, ::maxOf)
        raiseProgress()
    }

    private fun raiseProgress() {
        state.update { current ->
            if (current is LoadingUiState.InProgress) {
                LoadingUiState.InProgress(maxOf(current.progress, progress()))
            } else {
                current
            }
        }
    }

    private fun progress(): Float {
        val done = doneTasks.filter { it != PreparationTask.STARTER_SET }.sumOf { it.share }
        val starterDone = PreparationTask.STARTER_SET in doneTasks
        val starter = if (starterDone) PreparationTask.STARTER_SET.share else starterShare.get()
        return (done + starter) / PreparationTask.TOTAL_SHARE.toFloat()
    }

    internal companion object {
        const val EVENT_COMPLETED = "loading_completed"
        const val MIN_DURATION_MILLIS = 2_000L
        const val LOG_TAG = "Loading"
    }
}
