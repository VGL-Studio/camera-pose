package com.vglstudio.camera.pose.line.guide.feature.settings

import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode

/** Everything the settings screen can show (spec settings; plan T-025, section 3.1). */
sealed interface SettingsUiState {
    /** The stored choices are being read: only the top bar shows. */
    data object Loading : SettingsUiState

    data class Ready(
        val themeMode: ThemeMode,
        val shutterSoundEnabled: Boolean,
        /** Saved language below Android 13 ("" = system); unused from Android 13. */
        val languageTag: String,
        val version: String,
        val dialog: SettingsDialog? = null,
        /** A choice could not be saved: the screen tells the user once, then calls onSaveErrorShown. */
        val saveFailed: Boolean = false,
    ) : SettingsUiState
}

/** The picker open over the screen. */
enum class SettingsDialog { THEME, LANGUAGE }

/** One-time actions the screen performs outside itself. */
sealed interface SettingsEvent {
    /** Opens the app language page of the device settings (Android 13 and higher, KB-4). */
    data object OpenSystemLanguageSettings : SettingsEvent

    /** Opens [url] in the browser (KB-5). */
    data class OpenUrl(val url: String) : SettingsEvent

    /** Recreates the activity so the language just saved is applied (below Android 13, KB-4). */
    data object ApplyLanguage : SettingsEvent
}
