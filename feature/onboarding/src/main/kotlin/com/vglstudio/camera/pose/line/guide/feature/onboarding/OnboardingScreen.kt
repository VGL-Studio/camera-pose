package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel

/** Skeleton (plan T-012, step 3). */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun OnboardingRoute(
    onStarted: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    OnboardingScreen(uiState = OnboardingUiState.Ready, onEnableCamera = {})
}

/** Skeleton (plan T-012, step 3): only the screen tag. */
@Composable
@Suppress("UNUSED_PARAMETER")
internal fun OnboardingScreen(uiState: OnboardingUiState, onEnableCamera: () -> Unit, modifier: Modifier = Modifier) {
    Box(modifier = modifier.fillMaxSize().testTag(OnboardingTestTags.SCREEN))
}

internal object OnboardingTestTags {
    const val SCREEN = "onboarding_screen"
    const val CONTENT = "onboarding_content"
    const val ILLUSTRATION = "onboarding_illustration"
    const val TITLE = "onboarding_title"
    const val BODY = "onboarding_body"
    const val FEATURES = "onboarding_features"
    const val FEATURE_POSES = "onboarding_feature_poses"
    const val FEATURE_ADJUST = "onboarding_feature_adjust"
    const val FEATURE_SAVED = "onboarding_feature_saved"
    const val ENABLE_CAMERA = "onboarding_enable_camera"
    const val NOTE = "onboarding_note"
}
