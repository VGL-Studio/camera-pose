package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlinx.coroutines.flow.flowOf
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshots of one step of a flow with a native ad (plan T-032): the 250 dp card outside the pager, above the Next
 * button, and the full page with no controls in the area of the step, Next below it. The step texts are an example
 * set in this test, not texts of an app. The small frame takes the longest texts (L-028).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFlowStepScreenshotTest {
    @Composable
    private fun Step(darkTheme: Boolean, content: @Composable ColumnScope.() -> Unit) {
        AppTheme(darkTheme = darkTheme) {
            Surface(color = MaterialTheme.colorScheme.background) {
                Column(modifier = Modifier.fillMaxSize()) {
                    content()
                    PrimaryButton(text = NEXT, onClick = {}, modifier = Modifier.padding(Spacing.md).fillMaxWidth())
                }
            }
        }
    }

    @Composable
    private fun ColumnScope.StepContent(body: String) {
        Column(
            modifier = Modifier.weight(1f).fillMaxWidth().padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = TITLE, style = MaterialTheme.typography.headlineSmall)
            Text(text = body, style = MaterialTheme.typography.bodyLarge)
        }
    }

    @Composable
    private fun CardStep(darkTheme: Boolean, assets: NativeAdAssets, body: String) {
        Step(darkTheme) {
            StepContent(body)
            NativeAdCard(assets, NativeAdSize.STEP, Modifier.padding(horizontal = Spacing.md))
        }
    }

    @Composable
    private fun FullPageStep(darkTheme: Boolean, assets: NativeAdAssets) {
        Step(darkTheme) {
            NativeFullScreenStepContent(
                assets = assets,
                modifier = Modifier.weight(1f),
                media = { FakeMedia() },
                adChoices = { FakeAdChoices() },
            )
        }
    }

    @Test
    fun card_light() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_card_light.png") {
            CardStep(darkTheme = false, FakeNativeAdAssets.Design, BODY)
        }
    }

    @Test
    fun card_dark() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_card_dark.png") {
            CardStep(darkTheme = true, FakeNativeAdAssets.Design, BODY)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun card_small() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_card_small.png") {
            CardStep(darkTheme = false, FakeNativeAdAssets.Longest, LONG_BODY)
        }
    }

    @Test
    fun fullPage_light() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_full-page_light.png") {
            FullPageStep(darkTheme = false, FakeNativeAdAssets.Design)
        }
    }

    @Test
    fun fullPage_dark() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_full-page_dark.png") {
            FullPageStep(darkTheme = true, FakeNativeAdAssets.Design)
        }
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun fullPage_small() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_full-page_small.png") {
            FullPageStep(darkTheme = false, FakeNativeAdAssets.Longest)
        }
    }

    @Test
    @Config(qualifiers = "w640dp-h360dp-xxhdpi")
    fun fullPage_compact_light() {
        captureRoboImage("src/test/screenshots/NativeFlowStep_full-page-compact_light.png") {
            FullPageStep(darkTheme = false, FakeNativeAdAssets.Design)
        }
    }

    @Test
    fun fullPage_placeholder_light() {
        // The placeholder is static (L-048 does not apply): the page waits for an ad that has not come.
        captureRoboImage("src/test/screenshots/NativeFlowStep_full-page-placeholder_light.png") {
            Step(darkTheme = false) {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Unavailable),
                    modifier = Modifier.weight(1f),
                )
            }
        }
    }

    private companion object {
        const val NEXT = "Next"
        const val TITLE = "Step 2 of 3"
        const val BODY = "Example text of one step of a flow. The ad sits outside the pager, above the Next button."
        const val LONG_BODY = "Example text of one step of a flow that runs much longer than usual, so the card and " +
            "the Next button are checked on a short screen with the most text a step may hold."
    }
}
