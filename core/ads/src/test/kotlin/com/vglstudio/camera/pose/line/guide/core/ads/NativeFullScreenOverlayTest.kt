package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.activity.ComponentDialog
import androidx.activity.compose.setContent
import androidx.compose.runtime.Composable
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.hasAnyChild
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import org.robolectric.shadows.ShadowDialog

/**
 * The overlay of the native full-screen turn (plan T-026, K-02 to K-08): the 36 dp countdown in the top left corner,
 * which becomes the close button, the placeholder while the ad loads, the back button, and a recreated activity that
 * keeps the deadline. The drawing is checked without the dialog ([NativeFullScreenOverlayContent]); the back button
 * and the recreated activity go through [NativeFullScreenOverlayHost] with its dialog and a real
 * [NativeFullScreenTurns] in virtual time.
 */
@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFullScreenOverlayTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private var closes = 0

    private fun setOverlay(view: NativeFullScreenView) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                NativeFullScreenOverlayContent(view, onClose = { closes++ }, animated = false)
            }
        }
        composeRule.waitForIdle()
    }

    private fun adView(secondsLeft: Int, assets: NativeAdAssets = FakeNativeAdAssets.Design) =
        NativeFullScreenView(ad = FakeLoadedNativeAd(assets), secondsLeft = secondsLeft, loadFailed = false)

    private fun boundsOf(tag: String): DpRect = composeRule.onNodeWithTag(tag).getUnclippedBoundsInRoot()

    private fun assertRect(expected: DpRect, actual: DpRect) {
        listOf(
            expected.left to actual.left,
            expected.top to actual.top,
            expected.right to actual.right,
            expected.bottom to actual.bottom,
        ).forEach { (e, a) -> assertEquals("$actual", e.value, a.value, TOLERANCE) }
    }

    @Test
    fun countdown_isCircle36dp_atTopLeft() {
        setOverlay(adView(secondsLeft = 3))
        assertRect(DpRect(16.dp, 24.dp, 52.dp, 60.dp), boundsOf(COUNTDOWN))
        val layouts = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("3").fetchSemanticsNode()
            .config[SemanticsActions.GetTextLayoutResult].action!!(layouts)
        val style = layouts.single().layoutInput.style
        assertEquals(16.sp, style.fontSize)
        assertEquals(FontWeight.SemiBold, style.fontWeight)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
    }

    @Test
    fun countdown_tapDoesNothing() {
        setOverlay(adView(secondsLeft = 2))
        composeRule.onNodeWithTag(COUNTDOWN).assert(SemanticsMatcher.keyNotDefined(SemanticsActions.OnClick))
        composeRule.onNodeWithTag(COUNTDOWN).performClick()
        composeRule.waitForIdle()
        assertEquals(0, closes)
    }

    @Test
    fun closeEnabled_showsCloseButton_sameBounds_tapClosesOnce() {
        setOverlay(adView(secondsLeft = 0))
        assertRect(DpRect(16.dp, 24.dp, 52.dp, 60.dp), boundsOf(CLOSE))
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(0)
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, closes)
    }

    @Test
    fun failed_showsCloseButtonAtOnce_overPlaceholder() {
        setOverlay(NativeFullScreenView(ad = null, secondsLeft = 2, loadFailed = true))
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(1)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(0)
        composeRule.onAllNodesWithTag(PLACEHOLDER).assertCountEquals(1)
        composeRule.onNodeWithTag(CLOSE).performClick()
        composeRule.waitForIdle()
        assertEquals(1, closes)
    }

    @Test
    fun loading_showsPlaceholderWithCountdown() {
        setOverlay(NativeFullScreenView(ad = null, secondsLeft = 2, loadFailed = false))
        composeRule.onAllNodesWithTag(PLACEHOLDER).assertCountEquals(1)
        composeRule.onNodeWithTag(COUNTDOWN).assert(hasAnyChild(hasText("2")))
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
        assertRect(DpRect(16.dp, 24.dp, 52.dp, 60.dp), boundsOf(COUNTDOWN))
    }

    @Test
    fun countdown_leavesTopRightFree() {
        setOverlay(adView(secondsLeft = 3))
        val countdown = boundsOf(COUNTDOWN)
        // 48 x 48 dp in the top right corner, inside the 16 dp margin: where the SDK draws AdChoices.
        val topRight = DpRect(411.dp - 16.dp - 48.dp, 24.dp, 411.dp - 16.dp, 72.dp)
        val overlapsX = countdown.left < topRight.right && topRight.left < countdown.right
        val overlapsY = countdown.top < topRight.bottom && topRight.top < countdown.bottom
        val overlaps = overlapsX && overlapsY
        assertFalse("$countdown overlaps $topRight", overlaps)
        // Control: the countdown was found and is on the top row.
        assertEquals(24f, countdown.top.value, TOLERANCE)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun smallScreen_countdownAndCtaInside() {
        setOverlay(adView(secondsLeft = 3, assets = FakeNativeAdAssets.Longest))
        listOf(COUNTDOWN, CTA).forEach { tag ->
            composeRule.onNodeWithTag(tag).assertIsDisplayed()
            val bounds = boundsOf(tag)
            assertTrue("$tag outside the screen: $bounds", bounds.left.value >= 0f && bounds.top.value >= 0f)
            assertTrue(
                "$tag outside the screen: $bounds",
                bounds.right.value <= 360f + TOLERANCE && bounds.bottom.value <= 640f + TOLERANCE,
            )
        }
    }

    // The host with its dialog and a real coordinator in virtual time.

    private val testScope = TestScope(StandardTestDispatcher())
    private val port = FakeNativeFullScreenPort { testScope.testScheduler.currentTime }
    private val turns = NativeFullScreenTurns(
        port = port,
        clock = MonotonicClock { testScope.testScheduler.currentTime },
        scope = testScope,
        logger = RecordingAppLogger(),
    )
    private var finishes = 0

    @Composable
    private fun Host() {
        AppTheme(darkTheme = false) {
            NativeFullScreenOverlayHost(turns.view, onClose = turns::close, onBack = turns::onBack)
        }
    }

    private fun advanceTo(time: Long) {
        testScope.advanceTimeBy(time - testScope.testScheduler.currentTime)
        testScope.runCurrent()
        composeRule.waitForIdle()
    }

    private fun openCombo() {
        turns.afterInterstitial(AdPlacement.GALLERY_INTERSTITIAL) { finishes++ }
        testScope.runCurrent()
        assertNotNull(turns.view.value)
    }

    /** Back is sent to the dialog's own dispatcher: Robolectric does not route the activity's Back into it. */
    private fun pressBackOnDialog() {
        val dialog = ShadowDialog.getLatestDialog() as ComponentDialog
        composeRule.runOnUiThread { dialog.onBackPressedDispatcher.onBackPressed() }
        composeRule.waitForIdle()
    }

    @Test
    fun backPress_duringCountdown_isIgnored_afterCountdown_closes() {
        port.slot = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        composeRule.setContent { Host() }
        openCombo()
        composeRule.waitForIdle()
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)
        pressBackOnDialog()
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)

        advanceTo(3_000)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(1)
        pressBackOnDialog()
        assertNull(turns.view.value)
        assertEquals(1, finishes)
        composeRule.onAllNodesWithTag(CLOSE).assertCountEquals(0)
    }

    @Test
    fun backPress_whenFailed_closesAtOnce() {
        port.ensureLoadingResult = false
        composeRule.setContent { Host() }
        openCombo()
        composeRule.waitForIdle()
        assertTrue(turns.view.value!!.loadFailed)
        pressBackOnDialog()
        assertNull(turns.view.value)
        assertEquals(1, finishes)
    }

    @Test
    fun recreate_keepsDeadline_notRestarted() {
        port.slot = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        composeRule.setContent { Host() }
        openCombo()
        advanceTo(1_500)
        composeRule.onNodeWithTag(COUNTDOWN).assert(hasAnyChild(hasText("2")))

        composeRule.activityRule.scenario.recreate()
        // The new activity sets its content again, like MainActivity does in onCreate.
        composeRule.activityRule.scenario.onActivity { it.setContent { Host() } }
        advanceTo(2_100)
        composeRule.onAllNodesWithTag(COUNTDOWN).assertCountEquals(1)
        composeRule.onNodeWithTag(COUNTDOWN).assert(hasAnyChild(hasText("1")))
        composeRule.onAllNodesWithText(FakeNativeAdAssets.Design.headline).assertCountEquals(1)
        assertEquals(0, finishes)
    }

    private companion object {
        const val CLOSE = NativeFullScreenAdTestTags.CLOSE
        const val COUNTDOWN = NativeFullScreenAdTestTags.COUNTDOWN
        const val PLACEHOLDER = NativeFullScreenAdTestTags.PLACEHOLDER
        const val CTA = NativeFullScreenAdTestTags.CTA
        const val TOLERANCE = 0.5f
    }
}
