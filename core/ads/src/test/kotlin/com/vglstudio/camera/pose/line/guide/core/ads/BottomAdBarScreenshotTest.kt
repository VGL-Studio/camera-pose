package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the bottom bar with fake ads (Roborazzi, plan T-030): the native slot (106 dp, plan T-109) 16 dp
 * from the edges, the collapsible layer (299 dp) floating over it, and the content keeping the slot and its margin
 * (122 dp; drawn as a tinted block, so the kept space shows). Nothing here animates, so the screen settles (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomAdBarScreenshotTest {
    @Composable
    private fun Bar(layerShown: Boolean) {
        AdsTestTheme(darkTheme = false) {
            Surface {
                BottomAdBarLayout(
                    reservation = BottomAdBarDefaults.nativeReservation(),
                    slot = { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, it) },
                    layer = if (layerShown) {
                        { CollapsibleNativeAdCard(screenshotAd, onCollapse = {}, modifier = it) }
                    } else {
                        null
                    },
                ) { padding ->
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(padding)
                            .background(MaterialTheme.colorScheme.surfaceContainer)
                            .padding(Spacing.md),
                    ) {
                        Text("Screen content", style = MaterialTheme.typography.bodyLarge)
                    }
                }
            }
        }
    }

    @Test
    fun expanded_light() {
        captureRoboImage("src/test/screenshots/BottomAdBar_expanded_light.png") { Bar(layerShown = true) }
    }

    @Test
    fun collapsed_light() {
        captureRoboImage("src/test/screenshots/BottomAdBar_collapsed_light.png") { Bar(layerShown = false) }
    }

    /** A 891 x 411 dp window (KB-4 added 2026-11-16): the 106 dp slot only, no layer, the content keeping 122 dp. */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun landscape_light() {
        captureRoboImage("src/test/screenshots/BottomAdBar_landscape_light.png") { Bar(layerShown = true) }
    }

    /** A 411 x 445 dp window (split screen): the 106 dp slot only, no layer. */
    @Test
    @Config(qualifiers = "w411dp-h445dp-xxhdpi")
    fun split_light() {
        captureRoboImage("src/test/screenshots/BottomAdBar_split_light.png") { Bar(layerShown = true) }
    }

    /** A 411 x 600 dp window: the layer 284 dp (MediaView 166 dp), its top at half the window (300 dp). */
    @Test
    @Config(qualifiers = "w411dp-h600dp-xxhdpi")
    fun shortWindow_light() {
        captureRoboImage("src/test/screenshots/BottomAdBar_short-window_light.png") { Bar(layerShown = true) }
    }

    private companion object {
        /** The illustration texts of the collapsible frames (design/context/). */
        val screenshotAd = NativeAdAssets(
            headline = "Headline of the ad",
            advertiser = "Advertiser",
            body = null,
            callToAction = "Install",
            icon = null,
            hasMedia = true,
        )
    }
}
