package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.view.View
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.assertContentDescriptionEquals
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsEnabled
import androidx.compose.ui.test.assertWidthIsEqualTo
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.Insets
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The native full-screen ad screen (KB-5): a 36 dp close button visible from the first frame, in the top left corner,
 * away from AdChoices and the call to action; sizes from design/context/NativeFullScreenAdScreen_default_light.md.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenAdScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var closes = 0

    private val context: Context get() = ApplicationProvider.getApplicationContext()

    private fun setScreen(assets: NativeAdAssets = FakeNativeAdAssets.Design) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                NativeFullScreenAdScreen(
                    assets = assets,
                    onClose = { closes++ },
                    media = { FakeMedia() },
                    adChoices = { FakeAdChoices() },
                )
            }
        }
    }

    @Test
    fun closeButton_isDisplayedAtFirstFrame() {
        composeRule.mainClock.autoAdvance = false
        setScreen()
        composeRule.onNodeWithTag(CLOSE)
            .assertIsDisplayed()
            .assertIsEnabled()
            .assertContentDescriptionEquals(context.getString(R.string.ads_close_full_screen))
        assertEquals("Close ad", context.getString(R.string.ads_close_full_screen))
    }

    @Test
    fun closeButton_drawn36dp_touchAtLeast48dp() {
        setScreen()
        composeRule.onNodeWithTag(CLOSE)
            .assertWidthIsEqualTo(36.dp)
            .assertHeightIsEqualTo(36.dp)
        val close = composeRule.onNodeWithTag(CLOSE).getUnclippedBoundsInRoot()
        val centerX = (close.left + close.right) / 2
        val centerY = (close.top + close.bottom) / 2

        // Compose widens the touch area of a small button to 48 dp (6 dp on each side) without changing the layout.
        tapAt(close.right + 5.dp, centerY)
        assertEquals("tap 5 dp right of the button", 1, closes)
        tapAt(centerX, close.bottom + 5.dp)
        assertEquals("tap 5 dp below the button", 2, closes)

        // Control: 8 dp away is outside the 48 dp area, so the taps above hit the widened area, not anything.
        tapAt(close.right + 8.dp, centerY)
        assertEquals("tap 8 dp right of the button", 2, closes)
    }

    private fun tapAt(x: Dp, y: Dp) {
        val point = with(composeRule.density) { Offset(x.toPx(), y.toPx()) }
        composeRule.onRoot().performTouchInput { click(point) }
        composeRule.waitForIdle()
    }

    @Test
    fun media_startsAt76dp() {
        setScreen()
        // Design: top margin 24, close button bar 36, gap 16.
        val media = composeRule.onNodeWithTag(MEDIA).getUnclippedBoundsInRoot()
        assertEquals(76f, media.top.value, TOLERANCE)
    }

    @Test
    fun closeButton_isTopLeft_awayFromAdChoices() {
        setScreen()
        val close = composeRule.onNodeWithTag(CLOSE).getUnclippedBoundsInRoot()
        val adChoices = composeRule.onNodeWithTag(FAKE_AD_CHOICES_TAG).getUnclippedBoundsInRoot()
        val cta = composeRule.onNodeWithTag(CTA).getUnclippedBoundsInRoot()
        assertEquals(16f, close.left.value, TOLERANCE)
        assertEquals(24f, close.top.value, TOLERANCE)
        assertTrue("AdChoices too close: $close, $adChoices", adChoices.left.value - close.right.value >= 200f)
        assertTrue("CTA too close: $close, $cta", cta.top.value - close.bottom.value > 600f)
    }

    @Test
    fun tapClose_callsOnCloseOnce() {
        setScreen()
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, closes)
    }

    @Test
    fun texts_followDesign() {
        setScreen()
        val design = FakeNativeAdAssets.Design
        composeRule.onNodeWithText(design.headline).assertIsDisplayed()
        composeRule.onNodeWithText(design.advertiser!!).assertIsDisplayed()
        composeRule.onNodeWithText(design.body!!).assertIsDisplayed()
        composeRule.onNodeWithText(design.callToAction!!).assertIsDisplayed()
        composeRule.onNodeWithText(context.getString(R.string.ads_native_badge)).assertIsDisplayed()
        composeRule.onNodeWithTag(CTA).assertHeightIsEqualTo(52.dp)
        composeRule.onNodeWithTag(ICON, useUnmergedTree = true)
            .assertWidthIsEqualTo(48.dp)
            .assertHeightIsEqualTo(48.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(design.headline).fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        assertEquals(16.sp, layouts.single().layoutInput.style.fontSize)
        composeRule.onNodeWithTag(MEDIA).assertHeightIsAtLeast(120.dp)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallScreen_closeAndCtaInside() {
        setScreen(FakeNativeAdAssets.Longest)
        listOf(CLOSE, CTA).forEach { tag ->
            val bounds = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()
            assertTrue("$tag outside the screen: $bounds", bounds.left.value >= 0f && bounds.top.value >= 0f)
            assertTrue(
                "$tag outside the screen: $bounds",
                bounds.right.value <= 360f + TOLERANCE && bounds.bottom.value <= 640f + TOLERANCE,
            )
        }
        composeRule.onNodeWithTag(MEDIA).assertHeightIsAtLeast(120.dp)
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText(FakeNativeAdAssets.Longest.body!!)
            .fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult]
            .action!!
            .invoke(layouts)
        assertTrue("body has ${layouts.single().lineCount} lines", layouts.single().lineCount <= 3)
    }

    // The page of a flow step (plan T-032): no controls, in the area the app gives it.

    @Test
    fun flowStep_hasNoCloseButton_noCountdown() {
        var flowStep by mutableStateOf(true)
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                NativeFullScreenAdLayout(
                    assets = FakeNativeAdAssets.Design,
                    onClose = if (flowStep) null else ({ closes++ }),
                    binder = PreviewNativeAdBinder,
                    media = { FakeMedia() },
                    adChoices = { FakeAdChoices() },
                    secondsLeft = if (flowStep) 3 else null,
                )
            }
        }
        assertNoNode(CLOSE)
        assertNoNode(COUNTDOWN)
        composeRule.onNodeWithTag(FAKE_AD_CHOICES_TAG).assertExists()
        composeRule.onNodeWithTag(CTA).assertExists()

        // Control: the same layout with onClose has its close button.
        flowStep = false
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(CLOSE).assertExists()
    }

    @Test
    fun flowStep_publicContent_hasNoCloseButton() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                NativeFullScreenStepContent(
                    assets = FakeNativeAdAssets.Design,
                    media = { FakeMedia() },
                    adChoices = { FakeAdChoices() },
                )
            }
        }
        assertNoNode(CLOSE)
        assertNoNode(COUNTDOWN)
        composeRule.onNodeWithText(FakeNativeAdAssets.Design.headline).assertExists()
        composeRule.onNodeWithTag(CTA).assertHeightIsEqualTo(52.dp)
    }

    @Test
    fun flowStep_mediaStartsLikeAfterInter() {
        var flowStep by mutableStateOf(true)
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                if (flowStep) {
                    NativeFullScreenStepContent(FakeNativeAdAssets.Design, media = { FakeMedia() })
                } else {
                    NativeFullScreenAdScreen(FakeNativeAdAssets.Design, onClose = {}, media = { FakeMedia() })
                }
            }
        }
        val stepMedia = composeRule.onNodeWithTag(MEDIA).getUnclippedBoundsInRoot()
        assertEquals(76f, stepMedia.top.value, TOLERANCE)

        flowStep = false
        composeRule.waitForIdle()
        val afterInterMedia = composeRule.onNodeWithTag(MEDIA).getUnclippedBoundsInRoot()
        assertEquals(afterInterMedia.top.value, stepMedia.top.value, TOLERANCE)
        assertEquals(afterInterMedia.left.value, stepMedia.left.value, TOLERANCE)
    }

    @Test
    fun flowStep_noWindowInsetPadding() {
        var flowStep by mutableStateOf(true)
        var view: View? = null
        composeRule.setContent {
            view = LocalView.current
            AppTheme(darkTheme = false) {
                Box(Modifier.size(360.dp, 556.dp).testTag(AREA)) {
                    if (flowStep) {
                        NativeFullScreenStepContent(FakeNativeAdAssets.Design, media = { FakeMedia() })
                    } else {
                        NativeFullScreenAdScreen(FakeNativeAdAssets.Design, onClose = {}, media = { FakeMedia() })
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // A status bar of 40 dp: the page of a flow step sits in the frame of the app, which handles the safe area.
        val topInset = with(composeRule.density) { STATUS_BAR.roundToPx() }
        val dispatchStatusBar = {
            composeRule.runOnUiThread {
                ViewCompat.dispatchApplyWindowInsets(
                    view!!,
                    WindowInsetsCompat.Builder()
                        .setInsets(WindowInsetsCompat.Type.statusBars(), Insets.of(0, topInset, 0, 0))
                        .build(),
                )
            }
            composeRule.waitForIdle()
        }
        dispatchStatusBar()
        val area = composeRule.onNodeWithTag(AREA).getUnclippedBoundsInRoot()
        val media = composeRule.onNodeWithTag(MEDIA).getUnclippedBoundsInRoot()
        val cta = composeRule.onNodeWithTag(CTA).getUnclippedBoundsInRoot()
        assertEquals("starts at the 16 dp margin", 16f, (media.left - area.left).value, TOLERANCE)
        assertEquals("no safe area added", 76f, (media.top - area.top).value, TOLERANCE)
        assertTrue("CTA inside the area: $cta", cta.bottom <= area.bottom)

        // Control: the page after an interstitial covers the window, so it adds the status bar.
        flowStep = false
        composeRule.waitForIdle()
        // Compose reads the insets of the window only once a composable asks for them: sent again for this page.
        dispatchStatusBar()
        val afterInter = composeRule.onNodeWithTag(MEDIA).getUnclippedBoundsInRoot()
        assertEquals(76f + STATUS_BAR.value, (afterInter.top - area.top).value, TOLERANCE)
    }

    private fun assertNoNode(tag: String) {
        assertTrue(
            "$tag should not be drawn",
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    private companion object {
        const val CLOSE = "nativeFullScreenClose"
        const val CTA = "nativeFullScreenCta"
        const val MEDIA = "nativeFullScreenMedia"
        const val ICON = "nativeFullScreenIcon"
        const val COUNTDOWN = "nativeFullScreenCountdown"
        const val AREA = "stepArea"
        val STATUS_BAR = 40.dp
        const val TOLERANCE = 0.5f
    }
}
