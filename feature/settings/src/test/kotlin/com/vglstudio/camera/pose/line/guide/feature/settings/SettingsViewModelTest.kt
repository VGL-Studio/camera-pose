package com.vglstudio.camera.pose.line.guide.feature.settings

import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.settings.data.AppVersionProvider
import com.vglstudio.camera.pose.line.guide.feature.settings.data.LanguageSettingsMode
import java.io.IOException
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The settings screen logic (spec settings, KB-1 to KB-6; plan T-025, section 4). */
@OptIn(ExperimentalCoroutinesApi::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val preferences = FakeUserPreferences()
    private val links = FakeAppLinks()
    private val logger = RecordingAppLogger()
    private val events = mutableListOf<SettingsEvent>()

    private fun TestScope.viewModel(systemPage: Boolean = false): SettingsViewModel {
        val viewModel = SettingsViewModel(
            userPreferences = preferences,
            appLinks = links,
            appVersionProvider = AppVersionProvider { "1.0.0" },
            languageSettingsMode = LanguageSettingsMode { systemPage },
            logger = logger,
        )
        val dispatcher = UnconfinedTestDispatcher(testScheduler)
        backgroundScope.launch(dispatcher) { viewModel.uiState.collect {} }
        backgroundScope.launch(dispatcher) { viewModel.events.collect { events += it } }
        return viewModel
    }

    private fun SettingsViewModel.ready(): SettingsUiState.Ready {
        val state = uiState.value
        assertTrue("state is $state", state is SettingsUiState.Ready)
        return state as SettingsUiState.Ready
    }

    // KB-1, KB-6

    @Test
    fun ready_reflectsStoredChoices_andVersion() = runTest {
        preferences.themeMode.value = ThemeMode.DARK
        preferences.shutterSoundEnabled.value = false
        preferences.appLanguage.value = ""
        val viewModel = viewModel()
        runCurrent()
        assertEquals(SettingsUiState.Ready(ThemeMode.DARK, false, "", "1.0.0"), viewModel.uiState.value)
    }

    // KB-2

    @Test
    fun themeClick_opensDialog_selectSavesAndCloses() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onThemeClick()
        runCurrent()
        assertEquals(SettingsDialog.THEME, viewModel.ready().dialog)

        viewModel.onThemeSelect(ThemeMode.LIGHT)
        runCurrent()
        assertNull(viewModel.ready().dialog)
        assertEquals(listOf(ThemeMode.LIGHT), preferences.savedThemes)
        assertEquals(ThemeMode.LIGHT, viewModel.ready().themeMode)
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.tag == "Settings" })
    }

    @Test
    fun themeDialog_dismiss_savesNothing() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onThemeClick()
        runCurrent()
        assertEquals(SettingsDialog.THEME, viewModel.ready().dialog)

        viewModel.onDialogDismiss()
        runCurrent()
        assertNull(viewModel.ready().dialog)
        assertTrue(preferences.savedThemes.isEmpty())
        assertEquals(ThemeMode.SYSTEM, viewModel.ready().themeMode)
    }

    // KB-3

    @Test
    fun shutterSoundChange_isSaved() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onShutterSoundChange(false)
        runCurrent()
        assertEquals(listOf(false), preferences.savedSounds)
        assertFalse(viewModel.ready().shutterSoundEnabled)
    }

    // KB-4

    @Test
    fun languageClick_withSystemPage_opensSystemSettings() = runTest {
        val viewModel = viewModel(systemPage = true)
        runCurrent()
        viewModel.onLanguageClick()
        runCurrent()
        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenSystemLanguageSettings), events)
        assertNull(viewModel.ready().dialog)
    }

    @Test
    fun languageClick_withoutSystemPage_opensDialog() = runTest {
        val viewModel = viewModel(systemPage = false)
        runCurrent()
        viewModel.onLanguageClick()
        runCurrent()
        assertEquals(SettingsDialog.LANGUAGE, viewModel.ready().dialog)
        assertTrue(events.isEmpty())
    }

    @Test
    fun languageSelect_savesAndApplies() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onLanguageClick()
        runCurrent()
        viewModel.onLanguageSelect("hi")
        runCurrent()
        assertEquals(listOf("hi"), preferences.savedLanguages)
        assertEquals(listOf<SettingsEvent>(SettingsEvent.ApplyLanguage), events)
        assertNull(viewModel.ready().dialog)
    }

    @Test
    fun languageSelect_sameAsSaved_doesNothing() = runTest {
        preferences.appLanguage.value = "hi"
        val viewModel = viewModel()
        runCurrent()
        viewModel.onLanguageClick()
        runCurrent()
        viewModel.onLanguageSelect("hi")
        runCurrent()
        assertTrue(preferences.savedLanguages.isEmpty())
        assertTrue(events.isEmpty())
        assertNull(viewModel.ready().dialog)

        // Control: another language is saved and applied.
        viewModel.onLanguageSelect("th")
        runCurrent()
        assertEquals(listOf("th"), preferences.savedLanguages)
        assertEquals(listOf<SettingsEvent>(SettingsEvent.ApplyLanguage), events)
    }

    // KB-5

    @Test
    fun privacyPolicyClick_opensCurrentUrl() = runTest {
        val viewModel = viewModel()
        runCurrent()
        links.privacyPolicyUrl.value = "https://example.com/p"
        viewModel.onPrivacyPolicyClick()
        runCurrent()
        assertEquals(listOf<SettingsEvent>(SettingsEvent.OpenUrl("https://example.com/p")), events)
    }

    @Test
    fun externalPageUnavailable_logsWarning() = runTest {
        val viewModel = viewModel()
        runCurrent()
        viewModel.onExternalPageUnavailable("privacy_policy")
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Settings" })
    }

    // A failed save never stops the app; the user is told.

    @Test
    fun saveFails_setsSaveFailed_andLogsWarning() = runTest {
        preferences.failWrites = true
        val viewModel = viewModel()
        runCurrent()
        viewModel.onThemeSelect(ThemeMode.DARK)
        runCurrent()
        assertTrue(viewModel.ready().saveFailed)
        assertEquals(ThemeMode.SYSTEM, viewModel.ready().themeMode)
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Settings", warning.tag)
        assertTrue(warning.error is IOException)

        viewModel.onSaveErrorShown()
        runCurrent()
        assertFalse(viewModel.ready().saveFailed)
    }

    @Test
    fun languageSaveFails_doesNotApply() = runTest {
        preferences.failWrites = true
        val viewModel = viewModel()
        runCurrent()
        viewModel.onLanguageSelect("vi")
        runCurrent()
        assertTrue(viewModel.ready().saveFailed)
        assertTrue(events.isEmpty())
    }
}
