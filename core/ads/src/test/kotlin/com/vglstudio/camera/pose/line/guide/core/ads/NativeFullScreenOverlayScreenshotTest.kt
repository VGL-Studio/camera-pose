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
 * Screenshots of the overlay of the native full-screen turn (plan T-026), drawn without its dialog: a dialog window
 * makes Roborazzi capture the whole screen. Each state is a fixed [NativeFullScreenView] with the placeholder pulse
 * off, so nothing animates and the screen settles (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenOverlayScreenshotTest {
    @Composable
    private fun Overlay(view: NativeFullScreenView, darkTheme: Boolean = false) {
        AppTheme(darkTheme = darkTheme) {
            NativeFullScreenOverlayContent(view, onClose = {}, animated = false)
        }
    }

    private val countdown = NativeFullScreenView(
        ad = FakeLoadedNativeAd(FakeNativeAdAssets.Design),
        secondsLeft = 3,
        loadFailed = false,
    )

    @Test
    fun countdown_light() {
        captureRoboImage("src/test/screenshots/NativeFullScreenOverlay_countdown_light.png") {
            Overlay(countdown)
        }
    }

    @Test
    fun countdown_dark() {
        captureRoboImage("src/test/screenshots/NativeFullScreenOverlay_countdown_dark.png") {
            Overlay(countdown, darkTheme = true)
        }
    }

    @Test
    fun loading_light() {
        captureRoboImage("src/test/screenshots/NativeFullScreenOverlay_loading_light.png") {
            Overlay(NativeFullScreenView(ad = null, secondsLeft = 2, loadFailed = false))
        }
    }

    @Test
    fun failed_light() {
        captureRoboImage("src/test/screenshots/NativeFullScreenOverlay_failed_light.png") {
            Overlay(NativeFullScreenView(ad = null, secondsLeft = 2, loadFailed = true))
        }
    }
}

/**
 * The overlay on a short screen (L-028), with the longest texts of an ad. A class of its own with no compose rule, so
 * the capture is not mixed with a rule's window (CLAUDE.md, step 10).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class NativeFullScreenOverlaySmallScreenshotTest {
    @Test
    fun countdown_small() {
        captureRoboImage("src/test/screenshots/NativeFullScreenOverlay_countdown_small.png") {
            AppTheme(darkTheme = false) {
                NativeFullScreenOverlayContent(
                    NativeFullScreenView(
                        ad = FakeLoadedNativeAd(FakeNativeAdAssets.Longest),
                        secondsLeft = 3,
                        loadFailed = false,
                    ),
                    onClose = {},
                    animated = false,
                )
            }
        }
    }
}
