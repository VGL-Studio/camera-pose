package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.testTag
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
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDimens
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarScaffold
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import kotlinx.coroutines.runBlocking
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
        // Only the options scroll; the button stays in view under them.
        composeRule.onNode(hasText("Hủy") and hasAnyAncestorTag(ChoiceDialogTestTags.CARD)).assertIsDisplayed()
        val screen = composeRule.onNodeWithTag(SettingsTestTags.SCREEN).getUnclippedBoundsInRoot()
        val card = composeRule.onNodeWithTag(ChoiceDialogTestTags.CARD).getUnclippedBoundsInRoot()
        assertTrue("card $card in screen $screen", card.top >= screen.top && card.bottom <= screen.bottom)
        assertTrue("card $card in screen $screen", card.left >= screen.left && card.right <= screen.right)
    }

    // A large screen in landscape follows the device (spec camera, KB-11; plan T-056). The second frame is the first
    // minus the system bars.
    @Test
    @Config(qualifiers = "vi-w960dp-h600dp-land-xxhdpi")
    fun landscape_versionRowReachable_w960h600() = assertVersionRowReachable()

    @Test
    @Config(qualifiers = "vi-w960dp-h520dp-land-xxhdpi")
    fun landscape_versionRowReachable_w960h520() = assertVersionRowReachable()

    private fun assertVersionRowReachable() {
        setScreen()
        val versionRow = composeRule.onNode(hasText("Phiên bản") and hasText("1.0.0"))
        versionRow.performScrollTo().assertIsDisplayed()
        val screen = composeRule.onNodeWithTag(SettingsTestTags.SCREEN).getUnclippedBoundsInRoot()
        val row = versionRow.getUnclippedBoundsInRoot()
        assertTrue("version row bottom ${row.bottom} inside the screen ${screen.bottom}", row.bottom <= screen.bottom)
    }

    // The bottom ad bar (spec settings, KB-1 criteria 2 to 5; spec ads, question 19; plan T-093, section 4). The
    // short frame: without padding the list is 540 in 576 under the top bar, so it does not scroll.

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun small_nativeBottomBar_lastRowScrollsAboveSlot() {
        val listState = ScrollState(0)
        var padded by mutableStateOf(true)
        setBarScreen(listState) { screen ->
            FakeBar(BottomAdBarDefaults.nativeReservation, slotHeight = 80.dp) { padding ->
                screen(if (padded) padding else PaddingValues())
            }
        }
        scrollToEnd(listState)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(FAKE_SLOT).getUnclippedBoundsInRoot()
        assertEquals("slot height", 80f, (slot.bottom - slot.top).value, 0.5f)
        assertEquals("slot margin from the bottom", 16f, (root.bottom - slot.bottom).value, 0.5f)
        val row = versionRowBounds()
        assertTrue("version row ${row.bottom} above slot ${slot.top}", row.bottom.value <= slot.top.value + 0.5f)
        // Control: without the bottom padding the last row ends under the slot.
        padded = false
        composeRule.waitForIdle()
        scrollToEnd(listState)
        val unpadded = versionRowBounds()
        assertTrue("control: version row ${unpadded.bottom} under slot ${slot.top}", unpadded.bottom > slot.top)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun small_bannerBottomBar_lastRowScrollsAboveSlot() {
        val listState = ScrollState(0)
        var padded by mutableStateOf(true)
        setBarScreen(listState) { screen ->
            FakeBar(reservation = 50.dp, slotHeight = 50.dp, slotMargin = 0.dp) { padding ->
                screen(if (padded) padding else PaddingValues())
            }
        }
        scrollToEnd(listState)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        val slot = composeRule.onNodeWithTag(FAKE_SLOT).getUnclippedBoundsInRoot()
        assertEquals("banner slot at the bottom", root.bottom.value, slot.bottom.value, 0.5f)
        val row = versionRowBounds()
        assertTrue("version row ${row.bottom} above slot ${slot.top}", row.bottom.value <= slot.top.value + 0.5f)
        // The padding is the one the bar gives, nothing added.
        assertTrue("version row ${row.bottom} right above slot ${slot.top}", (slot.top - row.bottom).value < 1f)
        // Control: without the bottom padding the last row ends under the slot.
        padded = false
        composeRule.waitForIdle()
        scrollToEnd(listState)
        val unpadded = versionRowBounds()
        assertTrue("control: version row ${unpadded.bottom} under slot ${slot.top}", unpadded.bottom > slot.top)
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun small_realBottomBar_withoutAds_takesNoSpace() {
        val listState = ScrollState(0)
        var mode by mutableStateOf(BarMode.BARE)
        setBarScreen(listState) { screen ->
            when (mode) {
                BarMode.BARE -> screen(PaddingValues())
                BarMode.REAL -> BottomAdBarScaffold(AdPlacement.SETTINGS_BOTTOM) { screen(it) }
                BarMode.FAKE -> FakeBar(BottomAdBarDefaults.nativeReservation, slotHeight = 80.dp) { screen(it) }
            }
        }
        scrollToEnd(listState)
        val bareRow = versionRowBounds()
        // The real bar of the framework without an ads controller (tests, no consent, switch off).
        mode = BarMode.REAL
        composeRule.waitForIdle()
        scrollToEnd(listState)
        val root = composeRule.onRoot().getUnclippedBoundsInRoot()
        composeRule.onNodeWithTag(REAL_SLOT).assertDoesNotExist()
        val row = versionRowBounds()
        assertEquals("version row top kept", bareRow.top.value, row.top.value, 0.5f)
        assertEquals("version row bottom kept", bareRow.bottom.value, row.bottom.value, 0.5f)
        val list = composeRule.onNodeWithTag(SettingsTestTags.LIST).getUnclippedBoundsInRoot()
        assertEquals("list reaches the bottom", root.bottom.value, list.bottom.value, 0.5f)
        // Control: the same screen in a bar with a native slot keeps 96 under the last row.
        mode = BarMode.FAKE
        composeRule.waitForIdle()
        scrollToEnd(listState)
        val withSlot = versionRowBounds()
        assertEquals("control: with a slot", 96f, (root.bottom - withSlot.bottom).value, 0.5f)
    }

    @Test
    fun saveFailedSnackbar_aboveSlot() {
        composeRule.mainClock.autoAdvance = false
        setBarScreen(ScrollState(0), ready(saveFailed = true)) { screen ->
            FakeBar(BottomAdBarDefaults.nativeReservation, slotHeight = 80.dp) { screen(it) }
        }
        composeRule.mainClock.advanceTimeByFrame()
        val slot = composeRule.onNodeWithTag(FAKE_SLOT).getUnclippedBoundsInRoot()
        val message = composeRule.onNodeWithText("Không lưu được lựa chọn. Hãy thử lại.").getUnclippedBoundsInRoot()
        assertTrue("message ${message.bottom} above slot ${slot.top}", message.bottom <= slot.top)
    }

    private fun setBarScreen(
        listState: ScrollState,
        state: SettingsUiState = ready(),
        bar: @Composable (screen: @Composable (PaddingValues) -> Unit) -> Unit,
    ) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                bar { padding ->
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
                        contentPadding = padding,
                        listScrollState = listState,
                        privacyOptionsEntry = withPrivacyOptions,
                        dialogInWindow = false,
                    )
                }
            }
        }
    }

    private fun scrollToEnd(listState: ScrollState) {
        composeRule.runOnIdle { runBlocking { listState.scrollTo(listState.maxValue) } }
        composeRule.waitForIdle()
    }

    private fun versionRowBounds() =
        composeRule.onNode(hasText("Phiên bản") and hasText("1.0.0")).getUnclippedBoundsInRoot()

    private enum class BarMode { BARE, REAL, FAKE }

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
        const val FAKE_SLOT = "fake_ad_slot"

        // The tag of the real slot of the framework (BottomAdBarTestTags, internal to :core:ads).
        const val REAL_SLOT = "bottom_ad_bar_slot"

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

/**
 * The bottom bar of the framework with a fake slot [slotHeight] high (plan T-093, section 4): the slot takes the
 * modifier the bar gives, [slotMargin] from the edges.
 */
@Composable
private fun FakeBar(
    reservation: Dp,
    slotHeight: Dp,
    slotMargin: Dp = BottomAdBarDimens.SlotMargin,
    content: @Composable (PaddingValues) -> Unit,
) {
    BottomAdBarLayout(
        reservation = reservation,
        slot = { Box(it.height(slotHeight).testTag("fake_ad_slot")) },
        layer = null,
        slotMargin = slotMargin,
        content = content,
    )
}
