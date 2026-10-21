package com.vglstudio.camera.pose.line.guide.feature.sample

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.sample.data.CounterRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filterNotNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Reference implementation of a feature ViewModel. New features follow this shape:
 * - state exposed as a [StateFlow] of a sealed UI state, user actions as plain functions;
 * - the feature flag is observed as a flow, so a remote value fetched later takes effect without a restart;
 * - storage errors are caught and shown in the UI state, never left to crash the app;
 * - what happens (actions, caught errors) is logged through [AppLogger], without personal data (ADR-0019).
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SampleViewModel @Inject constructor(
    private val repository: CounterRepository,
    private val analytics: Analytics,
    private val logger: AppLogger,
    featureFlags: FeatureFlags,
) : ViewModel() {
    // null until the first flag value arrives, so the screen shows Loading instead of flashing "disabled".
    private val enabled: StateFlow<Boolean?> = featureFlags.enabled(FeatureFlag.SAMPLE)
        .stateIn(viewModelScope, SharingStarted.Eagerly, initialValue = null)

    private val saveFailed = MutableStateFlow(false)

    val uiState: StateFlow<SampleUiState> = enabled
        .filterNotNull()
        .flatMapLatest { isEnabled ->
            if (isEnabled) {
                combine(repository.count, saveFailed) { count, failed -> SampleUiState.Ready(count, failed) }
            } else {
                flowOf(SampleUiState.Disabled)
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS),
            initialValue = SampleUiState.Loading,
        )

    fun onIncrement() {
        if (enabled.value != true) return
        viewModelScope.launch {
            saveFailed.value = false
            try {
                repository.increment()
            } catch (error: IOException) {
                logger.warn(LOG_TAG, "Saving the counter failed", error)
                saveFailed.value = true
                return@launch
            }
            logger.info(LOG_TAG, "Counter incremented")
            analytics.log(AnalyticsEvent(name = EVENT_INCREMENT))
        }
    }

    internal companion object {
        const val STOP_TIMEOUT_MILLIS = 5_000L
        const val EVENT_INCREMENT = "sample_increment"
        const val LOG_TAG = "Sample"
    }
}
