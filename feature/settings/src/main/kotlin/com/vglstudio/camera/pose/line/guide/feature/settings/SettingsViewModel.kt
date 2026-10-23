package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.settings.data.AppVersionProvider
import com.vglstudio.camera.pose.line.guide.feature.settings.data.LanguageSettingsMode
import dagger.hilt.android.lifecycle.HiltViewModel
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * The settings screen (spec settings): shows the stored choices and saves new ones. The look and the shutter sound are
 * applied by :app and the camera, which follow [UserPreferences]; the language is applied by the system from
 * Android 13, below that by recreating the activity. Analytics and feature flags are not used: the spec has no
 * event, and the flag only decides in :app whether the gear opens this screen.
 */
@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val userPreferences: UserPreferences,
    private val appLinks: AppLinks,
    private val appVersionProvider: AppVersionProvider,
    private val languageSettingsMode: LanguageSettingsMode,
    private val logger: AppLogger,
) : ViewModel() {
    /** What the screen adds to the stored choices: the open picker and a save error to tell. */
    private data class Extras(val dialog: SettingsDialog? = null, val saveFailed: Boolean = false)

    private val extras = MutableStateFlow(Extras())
    private val version: String by lazy { appVersionProvider.versionName() }

    val uiState: StateFlow<SettingsUiState> = combine(
        userPreferences.themeMode,
        userPreferences.shutterSoundEnabled,
        userPreferences.appLanguage,
        extras,
    ) { themeMode, shutterSound, language, extras ->
        SettingsUiState.Ready(
            themeMode = themeMode,
            shutterSoundEnabled = shutterSound,
            languageTag = language,
            version = version,
            dialog = extras.dialog,
            saveFailed = extras.saveFailed,
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(STOP_TIMEOUT_MILLIS), SettingsUiState.Loading)

    private val eventChannel = Channel<SettingsEvent>(Channel.BUFFERED)
    val events: Flow<SettingsEvent> = eventChannel.receiveAsFlow()

    fun onThemeClick() {
        extras.update { it.copy(dialog = SettingsDialog.THEME) }
    }

    fun onThemeSelect(mode: ThemeMode) {
        closeDialog()
        save("Theme") {
            userPreferences.setThemeMode(mode)
            logger.info(LOG_TAG, "Theme set to ${mode.name.lowercase()}")
        }
    }

    fun onDialogDismiss() = closeDialog()

    fun onShutterSoundChange(enabled: Boolean) {
        save("Shutter sound") {
            userPreferences.setShutterSoundEnabled(enabled)
            logger.info(LOG_TAG, "Shutter sound ${if (enabled) "on" else "off"}")
        }
    }

    fun onLanguageClick() {
        if (languageSettingsMode.usesSystemPage()) {
            logger.info(LOG_TAG, "Opening the system language page")
            eventChannel.trySend(SettingsEvent.OpenSystemLanguageSettings)
        } else {
            extras.update { it.copy(dialog = SettingsDialog.LANGUAGE) }
        }
    }

    fun onLanguageSelect(tag: String) {
        closeDialog()
        viewModelScope.launch {
            if (tag == userPreferences.appLanguage.first()) return@launch
            save("Language") {
                userPreferences.setAppLanguage(tag)
                // A language tag is not personal data.
                logger.info(LOG_TAG, "Language set to ${tag.ifEmpty { "system" }}")
                eventChannel.trySend(SettingsEvent.ApplyLanguage)
            }
        }
    }

    /** The consent form itself is shown by :core:ads (PrivacyOptionsButton). */
    fun onPrivacyOptionsClick() {
        logger.info(LOG_TAG, "Opening privacy options")
    }

    fun onPrivacyPolicyClick() {
        viewModelScope.launch {
            // The address in use when tapped; not logged.
            val url = appLinks.privacyPolicyUrl.first()
            logger.info(LOG_TAG, "Opening the privacy policy")
            eventChannel.trySend(SettingsEvent.OpenUrl(url))
        }
    }

    /** The device has no app for [page] (no browser, or no app language page on a modified Android). */
    fun onExternalPageUnavailable(page: String) {
        logger.warn(LOG_TAG, "No app to open $page")
    }

    fun onSaveErrorShown() {
        extras.update { it.copy(saveFailed = false) }
    }

    private fun closeDialog() {
        extras.update { it.copy(dialog = null) }
    }

    private fun save(what: String, write: suspend () -> Unit) {
        viewModelScope.launch {
            try {
                write()
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "$what could not be saved", e)
                extras.update { it.copy(saveFailed = true) }
            }
        }
    }

    internal companion object {
        const val LOG_TAG = "Settings"
        const val STOP_TIMEOUT_MILLIS = 5_000L
    }
}
