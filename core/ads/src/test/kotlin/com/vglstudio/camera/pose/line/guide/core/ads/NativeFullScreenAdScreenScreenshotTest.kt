package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the native full-screen ad with a fake ad (Roborazzi). The two default images have the names of the
 * design images (ADR-0020). Nothing here animates (no countdown, no loading state), so the screen settles (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenAdScreenScreenshotTest {
    @Composable
    private fun Screen(darkTheme: Boolean, assets: NativeAdAssets = FakeNativeAdAssets.Design) {
        AppTheme(darkTheme = darkTheme) {
            NativeFullScreenAdScreen(
                assets = assets,
                onClose = {},
                media = { FakeMedia() },
                adChoices = { FakeAdChoices() },
            )
        }
    }

    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/NativeFullScreenAdScreen_default_light.png") {
            Screen(darkTheme = false)
        }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/NativeFullScreenAdScreen_default_dark.png") {
            Screen(darkTheme = true)
        }
    }

    /** At font scale 2.0 (plan T-045): the "Ad" label keeps the height of its text, the media gives way (O-08). */
    @Test
    fun fontScale2_light() {
        captureRoboImage("src/test/screenshots/NativeFullScreenAdScreen_font-scale-2_light.png") {
            WithFontScale(2f) { Screen(darkTheme = false) }
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun maxContent_small() {
        captureRoboImage("src/test/screenshots/NativeFullScreenAdScreen_max-content_small.png") {
            Screen(darkTheme = false, assets = FakeNativeAdAssets.Longest)
        }
    }
}
