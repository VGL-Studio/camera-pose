package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the native ad components with a fake ad (Roborazzi), on the screen background with the 16 dp
 * screen margin, so the card's background and border show against the screen (KB-1). The placeholder is captured
 * with its animation off: an endless animation never lets the screen settle (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdCardScreenshotTest {
    @Composable
    private fun OnScreen(darkTheme: Boolean, content: @Composable () -> Unit) {
        AppTheme(darkTheme = darkTheme) {
            Surface {
                Box(modifier = Modifier.fillMaxWidth().padding(Spacing.md)) { content() }
            }
        }
    }

    @Test
    fun small_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_small_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL) }
        }
    }

    @Test
    fun small_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_small_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL) }
        }
    }

    @Test
    fun medium_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_medium_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM) }
        }
    }

    @Test
    fun medium_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_medium_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM) }
        }
    }

    @Test
    fun full_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_full_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.FULL) }
        }
    }

    @Test
    fun full_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_full_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.FULL) }
        }
    }

    @Test
    fun placeholder_small_light() {
        captureRoboImage("src/test/screenshots/NativeAdPlaceholder_small_light.png") {
            OnScreen(darkTheme = false) { NativeAdPlaceholder(NativeAdSize.SMALL, animated = false) }
        }
    }

    @Test
    fun placeholder_medium_light() {
        captureRoboImage("src/test/screenshots/NativeAdPlaceholder_medium_light.png") {
            OnScreen(darkTheme = false) { NativeAdPlaceholder(NativeAdSize.MEDIUM, animated = false) }
        }
    }

    @Test
    fun medium_ctaLarge_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_medium-cta-large_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM, ctaLarge = true) }
        }
    }

    @Test
    fun step_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_step_light.png") {
            OnScreen(darkTheme = false) { NativeAdCard(FakeNativeAdAssets.Design, NativeAdSize.STEP) }
        }
    }

    @Test
    fun step_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_step_dark.png") {
            OnScreen(darkTheme = true) { NativeAdCard(FakeNativeAdAssets.Design, NativeAdSize.STEP) }
        }
    }

    @Test
    fun placeholder_step_light() {
        captureRoboImage("src/test/screenshots/NativeAdPlaceholder_step_light.png") {
            OnScreen(darkTheme = false) { NativeAdPlaceholder(NativeAdSize.STEP, animated = false) }
        }
    }

    @Test
    fun card_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_card_light.png") {
            OnScreen(darkTheme = false) {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(CARD_WIDTH, CARD_HEIGHT))
            }
        }
    }

    @Test
    fun card_dark() {
        captureRoboImage("src/test/screenshots/NativeAdCard_card_dark.png") {
            OnScreen(darkTheme = true) {
                NativeAdCard(sampleNativeAdAssets, NativeAdSize.CARD, Modifier.size(CARD_WIDTH, CARD_HEIGHT))
            }
        }
    }

    @Test
    fun placeholder_card_light() {
        captureRoboImage("src/test/screenshots/NativeAdPlaceholder_card_light.png") {
            OnScreen(darkTheme = false) {
                NativeAdPlaceholder(NativeAdSize.CARD, Modifier.size(CARD_WIDTH, CARD_HEIGHT), animated = false)
            }
        }
    }

    /** Small, medium and the step card at font scale 2.0 (plan T-045): call-to-action text whole, media 120 dp. */
    @Test
    fun fontScale2_light() {
        captureRoboImage("src/test/screenshots/NativeAdCard_font-scale-2_light.png") {
            WithFontScale(2f) {
                OnScreen(darkTheme = false) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
                        NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL)
                        NativeAdCard(sampleNativeAdAssets, NativeAdSize.MEDIUM)
                        NativeAdCard(sampleNativeAdAssets, NativeAdSize.STEP)
                    }
                }
            }
        }
    }

    private companion object {
        /** The card of the README example, 180 x 300 dp (ads-business-rules.md 5.7). */
        val CARD_WIDTH = 180.dp
        val CARD_HEIGHT = 300.dp
    }
}
