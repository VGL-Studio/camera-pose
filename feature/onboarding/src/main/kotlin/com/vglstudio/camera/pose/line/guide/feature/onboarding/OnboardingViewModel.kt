package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

/**
 * The onboarding flow (spec onboarding, KB-1, KB-2 and KB-5): "Next" goes through the pages, Back goes to the page
 * before; on the last page one tap on "Turn on camera" marks onboarding done (through :app), asks for the camera
 * permission when it is missing, then always goes on to the camera. Which screen follows loading is decided in :app.
 * The page list follows feature_premium_poses_enabled while the user is on page 1 or 2, then stays as it is to the end
 * of the flow (plan T-082, question 3). The saved-state keys survive an activity recreated while the system prompt is
 * open, and a process stopped by the system; closing the app starts the flow over.
 */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val analytics: Analytics,
    private val logger: AppLogger,
    featureFlags: FeatureFlags,
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private var premium = savedStateHandle.get<Boolean>(KEY_PREMIUM) ?: false

    // The page list stops following the flag once the user reaches the third page (plan T-082, question 3).
    private var frozen = savedStateHandle.contains(KEY_PREMIUM)
    private val viewedSteps = savedStateHandle.get<IntArray>(KEY_VIEWED_STEPS)?.toMutableSet() ?: mutableSetOf()

    private val state = MutableStateFlow(initialState())
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    private val eventChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = eventChannel.receiveAsFlow()

    init {
        logger.info(LOG_TAG, "Onboarding shown")
        // Page 1 is the same with or without the premium page, so its step does not wait for the flag.
        onPageShown()
        viewModelScope.launch {
            featureFlags.enabled(FeatureFlag.PREMIUM_POSES).distinctUntilChanged().collect { on ->
                if (frozen || on == premium) return@collect
                val current = state.value as? OnboardingUiState.Browsing ?: return@collect
                premium = on
                state.value = current.copy(pages = onboardingPages(on))
                logger.info(LOG_TAG, "Premium page ${if (on) "on" else "off"}")
            }
        }
    }

    private fun initialState(): OnboardingUiState {
        val pages = onboardingPages(premium)
        if (savedStateHandle.get<Boolean>(KEY_REQUESTING_PERMISSION) == true) {
            return OnboardingUiState.RequestingPermission(pages)
        }
        val index = (savedStateHandle.get<Int>(KEY_PAGE_INDEX) ?: 0).coerceIn(0, pages.lastIndex)
        return OnboardingUiState.Browsing(pages, index)
    }

    fun onNextClick() {
        val current = state.value as? OnboardingUiState.Browsing ?: return
        if (current.isLastPage) return
        val next = current.index + 1
        if (next >= FREEZE_INDEX && !frozen) {
            frozen = true
            savedStateHandle[KEY_PREMIUM] = premium
        }
        savedStateHandle[KEY_PAGE_INDEX] = next
        state.value = OnboardingUiState.Browsing(onboardingPages(premium), next)
        onPageShown()
    }

    fun onBackClick() {
        val current = state.value as? OnboardingUiState.Browsing ?: return
        if (current.index == 0) return
        savedStateHandle[KEY_PAGE_INDEX] = current.index - 1
        state.value = current.copy(index = current.index - 1)
        logger.info(LOG_TAG, "Back to page ${current.index}")
    }

    fun onEnableCameraClick(permissionGranted: Boolean) {
        val current = state.value
        if (current !is OnboardingUiState.Browsing || !current.isLastPage) return
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
            state.value = OnboardingUiState.RequestingPermission(current.pages)
            logger.info(LOG_TAG, "Asking for the camera permission")
            eventChannel.trySend(OnboardingEvent.RequestPermission)
        }
    }

    /**
     * [dialogShown] is false when the permission was already permanently denied and the system answered without
     * asking: there is no user answer to record (plan T-012, version 2; same rule as the camera, T-026).
     */
    fun onPermissionResult(granted: Boolean, dialogShown: Boolean = true) {
        if (state.value !is OnboardingUiState.RequestingPermission) return
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
        state.value = OnboardingUiState.Finished(state.value.pages)
        eventChannel.trySend(OnboardingEvent.Finished)
    }

    /** Logs onboarding_step_viewed the first time a step is shown in this flow (spec onboarding, KB-5, question 7). */
    private fun onPageShown() {
        val current = state.value
        val step = current.index + 1
        logger.info(LOG_TAG, "Page shown: $step ${current.page.logName}")
        if (viewedSteps.add(step)) {
            analytics.log(AnalyticsEvent(EVENT_STEP_VIEWED, mapOf(PARAM_STEP to step.toString())))
            savedStateHandle[KEY_VIEWED_STEPS] = viewedSteps.toIntArray()
        }
    }

    internal companion object {
        const val LOG_TAG = "Onboarding"
        const val EVENT_COMPLETE = "onboarding_complete"
        const val EVENT_PERMISSION_RESULT = "camera_permission_result"
        const val EVENT_STEP_VIEWED = "onboarding_step_viewed"
        const val PARAM_STEP = "step"
        const val SOURCE = "onboarding"
        const val KEY_REQUESTING_PERMISSION = "requesting_permission"
        const val KEY_COMPLETION_LOGGED = "completion_logged"
        const val KEY_PAGE_INDEX = "page_index"
        const val KEY_PREMIUM = "premium_page"
        const val KEY_VIEWED_STEPS = "viewed_steps"

        /** Index of the third page: from there on the page list no longer follows the flag. */
        const val FREEZE_INDEX = 2
    }
}
