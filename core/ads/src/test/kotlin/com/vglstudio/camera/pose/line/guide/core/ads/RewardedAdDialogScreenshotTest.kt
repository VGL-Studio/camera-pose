package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
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
 * Screenshots of the card of the rewarded dialog (R-01; plan T-031, question 6: no design yet, so these images are
 * the reference). Nothing animates, so the screen settles (L-048). No compose rule in this class.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class RewardedAdDialogScreenshotTest {
    @Composable
    private fun Card(darkTheme: Boolean, lines: List<String> = listOf(SHORT_LINE)) {
        AppTheme(darkTheme = darkTheme) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.colorScheme.background)
                    .padding(Spacing.md),
                contentAlignment = Alignment.Center,
            ) {
                RewardedAdDialogCard(title = "Unlock export", onWatch = {}, onNoThanks = {}) {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                        lines.forEach {
                            Text(
                                text = it,
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                    }
                }
            }
        }
    }

    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/RewardedAdDialog_default_light.png") { Card(darkTheme = false) }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/RewardedAdDialog_default_dark.png") { Card(darkTheme = true) }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun longContent_small() {
        captureRoboImage("src/test/screenshots/RewardedAdDialog_content_small.png") {
            Card(darkTheme = false, lines = (1..20).map { "Line $it of what this export includes" })
        }
    }

    private companion object {
        const val SHORT_LINE = "Watch a short ad to export this file."
    }
}
