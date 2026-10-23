package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.assertHasClickAction
import androidx.compose.ui.test.assertHasNoClickAction
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsNotSelected
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertIsSelected
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The settings screen (spec settings, KB-1 to KB-6; plan T-025, section 4). The screen language is Vietnamese, as the
 * text table of the design. Pickers are drawn inside the screen (dialogInWindow = false), as in the screenshot tests.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class SettingsScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val calls = mutableListOf<String>()
    private val themes = mutableListOf<ThemeMode>()
    private val sounds = mutableListOf<Boolean>()
    private val languages = mutableListOf<String>()

    private val withPrivacyOptions: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit =
        { row -> row { calls += "ads" } }

    private fun setScreen(
        state: SettingsUiState = ready(),
        privacyOptionsEntry: @Composable (row: @Composable (onClick: () -> Unit) -> Unit) -> Unit = withPrivacyOptions,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                SettingsScreen(
                    uiState = state,
                    onBack = { calls += "back" },
                    onThemeClick = { calls += "theme" },
                    onThemeSelect = { themes += it },
                    onShutterSoundChange = { sounds += it },
                    onLanguageClick = { calls += "language" },
                    onLanguageSelect = { languages += it },
                    onPrivacyOptionsClick = { calls += "privacyOptions" },
                    onPrivacyPolicyClick = { calls += "privacyPolicy" },
                    onDialogDismiss = { calls += "dismiss" },
                    onSaveErrorShown = { calls += "saveErrorShown" },
                    privacyOptionsEntry = privacyOptionsEntry,
                    dialogInWindow = false,
                )
            }
        }
    }

    private fun radio(label: String): SemanticsNodeInteraction =
        composeRule.onNode(hasText(label) and SemanticsMatcher.expectValue(SemanticsProperties.Role, Role.RadioButton))

    private fun row(title: String): SemanticsNodeInteraction = composeRule.onNode(hasText(title) and hasClickOrToggle())

    private fun hasClickOrToggle() = SemanticsMatcher("is a row") {
        it.config.contains(SemanticsActions.OnClick) ||
            it.config.contains(SemanticsProperties.ToggleableState)
    }

    // KB-1

    @Test
    fun ready_showsAllRowsInOrder() {
        setScreen()
        val texts = listOf(
            "Cài đặt", "Chung", "Giao diện", "Theo hệ thống", "Âm khi chụp", "Ngôn ngữ", "Tiếng Việt",
            "Quyền riêng tư", "Tùy chọn quyền riêng tư", "Chính sách quyền riêng tư", "Thông tin", "Phiên bản", "1.0.0",
        )
        val tops = texts.map { text ->
            composeRule.onNodeWithText(text, useUnmergedTree = true).getUnclippedBoundsInRoot().top
        }
        tops.zipWithNext().forEachIndexed { index, (above, below) ->
            assertTrue("${texts[index]} must be above ${texts[index + 1]}", above < below)
        }
    }

    @Test
    fun back_callsOnBack() {
        setScreen()
        composeRule.onNode(hasContentDescription("Quay lại")).performClick()
        assertEquals(listOf("back"), calls)
    }

    // KB-2

    @Test
    fun themeDialog_marksCurrent_andSelects() {
        setScreen(ready(themeMode = ThemeMode.DARK, dialog = SettingsDialog.THEME))
        composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).assertExists()
        composeRule.onAllNodesWithText("Giao diện").fetchSemanticsNodes().let { assertTrue(it.size >= 2) }
        radio("Tối").assertIsSelected()
        radio("Theo hệ thống").assertIsNotSelected()
        radio("Sáng").assertIsNotSelected()

        radio("Sáng").performClick()
        assertEquals(listOf(ThemeMode.LIGHT), themes)
        composeRule.onNodeWithText("Hủy").performClick()
        assertEquals(listOf("dismiss"), calls)
    }

    @Test
    fun themeRow_showsCurrentChoice() {
        setScreen(ready(themeMode = ThemeMode.LIGHT))
        composeRule.onNodeWithText("Sáng", useUnmergedTree = true).assertExists()
        row("Giao diện").assertHasClickAction().performClick()
        assertEquals(listOf("theme"), calls)
    }

    // KB-3

    @Test
    fun shutterSwitch_reflectsAndToggles() {
        setScreen(ready(shutterSoundEnabled = true))
        row("Âm khi chụp").assertIsOn().performClick()
        assertEquals(listOf(false), sounds)
    }

    // KB-4

    @Test
    fun languageRow_showsLanguageInUse_vi() {
        setScreen()
        composeRule.onNodeWithText("Tiếng Việt", useUnmergedTree = true).assertExists()
        row("Ngôn ngữ").performClick()
        assertEquals(listOf("language"), calls)
    }

    @Test
    @Config(qualifiers = "hi-w411dp-h891dp-xxhdpi")
    fun languageRow_showsLanguageInUse_hi() {
        setScreen()
        composeRule.onNodeWithText("हिन्दी", useUnmergedTree = true).assertExists()
        row("भाषा").performClick()
        assertEquals(listOf("language"), calls)
    }

    @Test
    @Config(qualifiers = "fr-w411dp-h891dp-xxhdpi")
    fun languageRow_unsupportedLocale_showsEnglish() {
        setScreen()
        composeRule.onNodeWithText("English", useUnmergedTree = true).assertExists()
        row("Language").performClick()
        assertEquals(listOf("language"), calls)
    }

    @Test
    fun languageDialog_listsSystemAndSevenLanguages() {
        setScreen(ready(languageTag = "th", dialog = SettingsDialog.LANGUAGE))
        composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).assertExists()
        (listOf("Theo hệ thống") + LANGUAGE_NAMES).forEach { radio(it).assertExists() }
        radio("ไทย").assertIsSelected()
        radio("Theo hệ thống").assertIsNotSelected()

        radio("Español (Latinoamérica)").performScrollTo().performClick()
        radio("Theo hệ thống").performScrollTo().performClick()
        assertEquals(listOf("es-419", ""), languages)
    }

    // KB-5

    @Test
    fun privacyOptions_shownWhenEntryProvided_andClicks() {
        setScreen()
        val optionsRow = row("Tùy chọn quyền riêng tư")
        optionsRow.assertExists()
        val bounds = optionsRow.getUnclippedBoundsInRoot()
        val link = composeRule.onAllNodesWithTag(SettingsRowTestTags.LINK, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertTrue(link.any { it.center.y in bounds.top.value * density()..bounds.bottom.value * density() })

        optionsRow.performClick()
        assertEquals(listOf("privacyOptions", "ads"), calls)
    }

    @Test
    fun privacyOptions_hiddenWithoutEntry() {
        setScreen(privacyOptionsEntry = {})
        composeRule.onNodeWithText("Tùy chọn quyền riêng tư", useUnmergedTree = true).assertDoesNotExist()
        composeRule.onNodeWithText("Chính sách quyền riêng tư", useUnmergedTree = true).assertExists()
    }

    @Test
    fun privacyPolicy_clicks() {
        setScreen()
        val policyRow = row("Chính sách quyền riêng tư")
        val bounds = policyRow.getUnclippedBoundsInRoot()
        val link = composeRule.onAllNodesWithTag(SettingsRowTestTags.LINK, useUnmergedTree = true)
            .fetchSemanticsNodes()
            .map { it.boundsInRoot }
        assertTrue(link.any { it.center.y in bounds.top.value * density()..bounds.bottom.value * density() })
        policyRow.performClick()
        assertEquals(listOf("privacyPolicy"), calls)
    }

    // KB-6

    @Test
    fun versionRow_showsVersion_notClickable_noChevron() {
        setScreen()
        composeRule.onNodeWithText("1.0.0", useUnmergedTree = true).assertExists()
        val versionRow = composeRule.onNode(hasText("Phiên bản") and hasText("1.0.0"))
        versionRow.assertHasNoClickAction()
        val bounds = versionRow.getUnclippedBoundsInRoot()
        val range = bounds.top.value * density()..bounds.bottom.value * density()
        listOf(SettingsRowTestTags.CHEVRON, SettingsRowTestTags.LINK, SettingsRowTestTags.SWITCH).forEach { tag ->
            val inRow = composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes()
                .filter { it.boundsInRoot.center.y in range }
            assertTrue("$tag in the version row", inRow.isEmpty())
        }
    }

    // A failed save is told to the user.

    @Test
    fun saveFailed_showsSnackbar_thenReports() {
        composeRule.mainClock.autoAdvance = false
        setScreen(ready(saveFailed = true))
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.onNodeWithText("Không lưu được lựa chọn. Hãy thử lại.").assertExists()
        assertEquals(0, calls.count { it == "saveErrorShown" })
        composeRule.mainClock.advanceTimeBy(5_000)
        assertEquals(1, calls.count { it == "saveErrorShown" })
    }

    // Short screens (L-028)

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun small_versionRowReachable() {
        setScreen()
        val versionRow = composeRule.onNode(hasText("Phiên bản") and hasText("1.0.0"))
        versionRow.performScrollTo().assertIsDisplayed()
        val screen = composeRule.onNodeWithTag(SettingsTestTags.SCREEN).getUnclippedBoundsInRoot()
        assertTrue(versionRow.getUnclippedBoundsInRoot().bottom <= screen.bottom)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun small_languageDialog_allOptionsReachable() {
        setScreen(ready(dialog = SettingsDialog.LANGUAGE))
        radio("Tiếng Việt").performScrollTo().assertIsDisplayed()
        composeRule.onNode(hasText("Hủy") and hasAnyAncestorTag(ChoiceDialogTestTags.CARD))
            .performScrollTo()
            .assertIsDisplayed()
        val screen = composeRule.onNodeWithTag(SettingsTestTags.SCREEN).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertTrue("card $card in screen $screen", card.top >= screen.top && card.bottom <= screen.bottom)
        assertTrue("card $card in screen $screen", card.left >= screen.left && card.right <= screen.right)
    }

    private fun hasAnyAncestorTag(tag: String) = hasAnyAncestor(hasTestTag(tag))

    private fun density(): Float = composeRule.density.density

    private fun ready(
        themeMode: ThemeMode = ThemeMode.SYSTEM,
        shutterSoundEnabled: Boolean = true,
        languageTag: String = "",
        dialog: SettingsDialog? = null,
        saveFailed: Boolean = false,
    ) = SettingsUiState.Ready(themeMode, shutterSoundEnabled, languageTag, "1.0.0", dialog, saveFailed)

    private companion object {
        val LANGUAGE_NAMES = listOf(
            "English",
            "हिन्दी",
            "Português (Brasil)",
            "Español (Latinoamérica)",
            "Bahasa Indonesia",
            "ไทย",
            "Tiếng Việt",
        )
    }
}
