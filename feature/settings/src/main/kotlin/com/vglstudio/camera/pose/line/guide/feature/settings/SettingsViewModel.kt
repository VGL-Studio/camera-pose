package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.settings.data.AppVersionProvider
import com.vglstudio.camera.pose.line.guide.feature.settings.data.LanguageSettingsMode
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

/** Skeleton for the tests written first (plan T-025, section 4): always Loading, does nothing. */
@Suppress("unused", "UNUSED_PARAMETER")
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val appLinks: AppLinks,
    private val appVersionProvider: AppVersionProvider,
    private val languageSettingsMode: LanguageSettingsMode,
    private val logger: AppLogger,
) : ViewModel() {
    val uiState: StateFlow<SettingsUiState> = MutableStateFlow<SettingsUiState>(SettingsUiState.Loading).asStateFlow()

    val events: Flow<SettingsEvent> = emptyFlow()

    fun onThemeClick() = Unit

    fun onThemeSelect(mode: ThemeMode) = Unit

    fun onDialogDismiss() = Unit

    fun onShutterSoundChange(enabled: Boolean) = Unit

    fun onLanguageClick() = Unit

    fun onLanguageSelect(tag: String) = Unit

    fun onPrivacyOptionsClick() = Unit

    fun onPrivacyPolicyClick() = Unit

    fun onExternalPageUnavailable(page: String) = Unit

    fun onSaveErrorShown() = Unit
}
