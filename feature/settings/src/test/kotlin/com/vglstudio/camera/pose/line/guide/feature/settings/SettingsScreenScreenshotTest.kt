package com.vglstudio.camera.pose.line.guide.feature.settings

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdAssets
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdCard
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdSize
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Tests in the standard frame are named like the design images in
 * docs/features/settings/design/screens/ and compared by scripts/design-compare.sh (ADR-0020). The screen sits under
 * a Spacing.lg spacer standing in for the status bar of the design frame (plan T-025, question 8); the privacy
 * options row is shown and pickers are drawn inside the screen. The _small tests use the short phone frame (L-028).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class SettingsScreenScreenshotTest {
    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/SettingsScreen_default_light.png") { Screen(DEFAULT, dark = false) }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/SettingsScreen_default_dark.png") { Screen(DEFAULT, dark = true) }
    }

    @Test
    fun themePicker_light() {
        captureRoboImage("src/test/screenshots/SettingsScreen_theme-picker_light.png") {
            Screen(THEME_PICKER, dark = false)
        }
    }

    @Test
    fun themePicker_dark() {
        captureRoboImage("src/test/screenshots/SettingsScreen_theme-picker_dark.png") {
            Screen(THEME_PICKER, dark = true)
        }
    }

    @Test
    fun languagePicker_light() {
        captureRoboImage("src/test/screenshots/SettingsScreen_language-picker_light.png") {
            Screen(LANGUAGE_PICKER, dark = false)
        }
    }

    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun default_small() {
        captureRoboImage("src/test/screenshots/SettingsScreen_default_small.png") { Screen(DEFAULT, dark = false) }
    }

    // The most content: the language picker with eight options.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun languagePicker_small() {
        captureRoboImage("src/test/screenshots/SettingsScreen_language-picker_small.png") {
            Screen(LANGUAGE_PICKER, dark = false)
        }
    }

    // Design `ad` (spec settings, KB-1; spec ads, question 19; plan T-093): the native bottom bar of the framework
    // with a fixed ad, as the real one is never drawn in tests.
    @Test
    fun ad_light() {
        captureRoboImage("src/test/screenshots/SettingsScreen_ad_light.png") {
            Screen(DEFAULT, dark = false, ad = true)
        }
    }

    @Test
    fun ad_dark() {
        captureRoboImage("src/test/screenshots/SettingsScreen_ad_dark.png") { Screen(DEFAULT, dark = true, ad = true) }
    }

    // With the bar, the list scrolled to its end: the version row ends right above the slot.
    @Test
    @Config(qualifiers = "vi-w360dp-h640dp-xxhdpi")
    fun ad_small() {
        captureRoboImage("src/test/screenshots/SettingsScreen_ad_small.png") {
            // ScrollState moves a value past the end back to maxValue when the list is measured.
            Screen(DEFAULT, dark = false, ad = true, listScrollState = remember { ScrollState(Int.MAX_VALUE) })
        }
    }

    @Composable
    private fun Screen(
        state: SettingsUiState,
        dark: Boolean,
        ad: Boolean = false,
        listScrollState: ScrollState = rememberScrollState(),
    ) {
        AppTheme(darkTheme = dark) {
            val screen = @Composable { contentPadding: PaddingValues ->
                Column(Modifier.background(MaterialTheme.colorScheme.background)) {
                    Spacer(Modifier.height(Spacing.lg))
                    SettingsScreen(
                        uiState = state,
                        onBack = {},
                        onThemeClick = {},
                        onThemeSelect = {},
                        onShutterSoundChange = {},
                        onLanguageClick = {},
                        onLanguageSelect = {},
                        onPrivacyOptionsClick = {},
                        onPrivacyPolicyClick = {},
                        onDialogDismiss = {},
                        onSaveErrorShown = {},
                        contentPadding = contentPadding,
                        listScrollState = listScrollState,
                        privacyOptionsEntry = { row -> row {} },
                        dialogInWindow = false,
                    )
                }
            }
            if (ad) {
                // Around the status bar spacer too, so the slot is 16 from the bottom of the frame, as the design.
                BottomAdBarLayout(
                    reservation = BottomAdBarDefaults.nativeReservation,
                    slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) },
                    layer = null,
                    content = screen,
                )
            } else {
                screen(PaddingValues())
            }
        }
    }

    private companion object {
        val DEFAULT = SettingsUiState.Ready(
            themeMode = ThemeMode.SYSTEM,
            shutterSoundEnabled = true,
            languageTag = "",
            version = "1.0.0",
        )
        val THEME_PICKER = DEFAULT.copy(dialog = SettingsDialog.THEME)
        val LANGUAGE_PICKER = DEFAULT.copy(dialog = SettingsDialog.LANGUAGE)
    }
}

/** The fixed ad of the design (context/SettingsScreen_ad_light.md). */
private val SCREENSHOT_AD = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = null,
    callToAction = "Install",
    icon = null,
    hasMedia = false,
)
