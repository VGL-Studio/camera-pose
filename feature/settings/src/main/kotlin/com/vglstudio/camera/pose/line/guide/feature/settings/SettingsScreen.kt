package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.navigation.compose.hiltViewModel
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode

object SettingsTestTags {
    const val SCREEN = "settings_screen"
    const val LIST = "settings_list"
}

/** Skeleton for the tests written first (plan T-025, section 4). */
@Composable
fun SettingsRoute(onBack: () -> Unit, viewModel: SettingsViewModel = hiltViewModel()) {
    SettingsScreen(
        uiState = SettingsUiState.Loading,
        onBack = onBack,
        onThemeClick = viewModel::onThemeClick,
        onThemeSelect = viewModel::onThemeSelect,
        onShutterSoundChange = viewModel::onShutterSoundChange,
        onLanguageClick = viewModel::onLanguageClick,
        onLanguageSelect = viewModel::onLanguageSelect,
        onPrivacyOptionsClick = viewModel::onPrivacyOptionsClick,
        onPrivacyPolicyClick = viewModel::onPrivacyPolicyClick,
        onDialogDismiss = viewModel::onDialogDismiss,
        onSaveErrorShown = viewModel::onSaveErrorShown,
    )
}

/** Skeleton for the tests written first (plan T-025, section 4). */
@Suppress("UNUSED_PARAMETER")
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onBack: () -> Unit,
    onThemeClick: () -> Unit,
    onThemeSelect: (ThemeMode) -> Unit,
    onShutterSoundChange: (Boolean) -> Unit,
    onLanguageClick: () -> Unit,
    onLanguageSelect: (String) -> Unit,
    onPrivacyOptionsClick: () -> Unit,
    onPrivacyPolicyClick: () -> Unit,
    onDialogDismiss: () -> Unit,
    onSaveErrorShown: () -> Unit,
    modifier: Modifier = Modifier,
    privacyOptionsEntry: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit = {},
    dialogInWindow: Boolean = true,
) {
    Box(modifier.testTag(SettingsTestTags.SCREEN))
}
