package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/**
 * The onboarding screen (spec onboarding, KB-1 and KB-2): one tap on "Turn on camera" marks onboarding done (through
 * :app), asks for the camera permission when it is missing, then always goes on to the camera. Which screen follows
 * loading is decided in :app, so this view model reads no feature flag (as the camera, T-013).
 * The two saved-state keys survive an activity recreated while the system prompt is open.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val analytics: Analytics,
    private val logger: AppLogger,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<OnboardingUiState>(
        if (savedStateHandle.get<Boolean>(KEY_REQUESTING_PERMISSION) == true) {
            OnboardingUiState.RequestingPermission
        } else {
            OnboardingUiState.Ready
        },
    )
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    private val eventChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = eventChannel.receiveAsFlow()

    init {
        logger.info(LOG_TAG, "Onboarding shown")
    }

    fun onEnableCameraClick(permissionGranted: Boolean) {
        if (state.value != OnboardingUiState.Ready) return
        if (savedStateHandle.get<Boolean>(KEY_COMPLETION_LOGGED) != true) {
            analytics.log(AnalyticsEvent(EVENT_COMPLETE))
            savedStateHandle[KEY_COMPLETION_LOGGED] = true
        }
        logger.info(LOG_TAG, "Enable camera tapped")
        eventChannel.trySend(OnboardingEvent.Started)
        if (permissionGranted) {
            logger.info(LOG_TAG, "Camera permission already granted")
            finish()
        } else {
            savedStateHandle[KEY_REQUESTING_PERMISSION] = true
            state.value = OnboardingUiState.RequestingPermission
            logger.info(LOG_TAG, "Asking for the camera permission")
            eventChannel.trySend(OnboardingEvent.RequestPermission)
        }
    }

    /**
     * [dialogShown] is false when the permission was already permanently denied and the system answered without
     * asking: there is no user answer to record (plan T-012, version 2; same rule as the camera, T-026).
     */
    fun onPermissionResult(granted: Boolean, dialogShown: Boolean = true) {
        if (state.value != OnboardingUiState.RequestingPermission) return
        if (dialogShown) {
            val result = if (granted) "granted" else "denied"
            analytics.log(AnalyticsEvent(EVENT_PERMISSION_RESULT, mapOf("result" to result, "source" to SOURCE)))
            logger.info(LOG_TAG, "Camera permission $result")
        } else {
            logger.info(LOG_TAG, "Camera permission denied without dialog")
        }
        savedStateHandle.remove<Boolean>(KEY_REQUESTING_PERMISSION)
        finish()
    }

    private fun finish() {
        state.value = OnboardingUiState.Finished
        eventChannel.trySend(OnboardingEvent.Finished)
    }

    internal companion object {
        const val LOG_TAG = "Onboarding"
        const val EVENT_COMPLETE = "onboarding_complete"
        const val EVENT_PERMISSION_RESULT = "camera_permission_result"
        const val SOURCE = "onboarding"
        const val KEY_REQUESTING_PERMISSION = "requesting_permission"
        const val KEY_COMPLETION_LOGGED = "completion_logged"
    }
}
