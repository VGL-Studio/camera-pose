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
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of the bottom bar with fake ads (Roborazzi, plan T-030): the 80 dp native slot 16 dp from the edges,
 * the 250 dp collapsible layer floating over it, and the content keeping the slot's 96 dp (drawn as a tinted block,
 * so the kept space shows). Nothing here animates, so the screen settles (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomAdBarScreenshotTest {
    @Composable
    private fun Bar(layerShown: Boolean) {
        AppTheme(darkTheme = false) {
            Surface {
                BottomAdBarLayout(
                    reservation = BottomAdBarDefaults.nativeReservation,
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
