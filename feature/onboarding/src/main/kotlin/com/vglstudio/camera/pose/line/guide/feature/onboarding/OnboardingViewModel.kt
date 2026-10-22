package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow

/** Skeleton (plan T-012, step 3): every function exists and does nothing. */
@HiltViewModel
class OnboardingViewModel @Inject constructor(
    @Suppress("unused") private val analytics: Analytics,
    @Suppress("unused") private val logger: AppLogger,
    @Suppress("unused") private val savedStateHandle: SavedStateHandle,
) : ViewModel() {
    private val state = MutableStateFlow<OnboardingUiState>(OnboardingUiState.Ready)
    val uiState: StateFlow<OnboardingUiState> = state.asStateFlow()

    private val eventChannel = Channel<OnboardingEvent>(Channel.BUFFERED)
    val events: Flow<OnboardingEvent> = eventChannel.receiveAsFlow()

    @Suppress("UNUSED_PARAMETER")
    fun onEnableCameraClick(permissionGranted: Boolean) = Unit

    @Suppress("UNUSED_PARAMETER")
    fun onPermissionResult(granted: Boolean, dialogShown: Boolean = true) = Unit
}
