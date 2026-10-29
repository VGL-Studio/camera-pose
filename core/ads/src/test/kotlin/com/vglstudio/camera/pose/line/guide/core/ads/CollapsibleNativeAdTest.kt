package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.AndroidComposeTestRule
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** A fake collapsible ad: headline, advertiser, "Install", a fake icon and media (plan T-012, section 4). */
private fun collapsibleTestAd(headline: String = "Headline of the ad") = FakeLoadedNativeAd(
    NativeAdAssets(
        headline = headline,
        advertiser = "Advertiser",
        body = null,
        callToAction = "Install",
        icon = ImageBitmap(40, 40),
        hasMedia = true,
    ),
)

/**
 * The collapsible layer of the bottom bar (KB-4, D-03, CL-01 to CL-04; plan T-030): opens 250 dp high with a 32 dp
 * collapse button in the top end corner, hides on a tap, and opens again `time_expand_collap_ad` seconds after the
 * tap, counted from the tap whatever the refresh does. Sizes from design/context/, 1 dp tolerance. Timing tests stop
 * the clock of Compose and read the monotonic clock from it, so both move together.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class CollapsibleNativeAdTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val feed = FakeNativeFeed()
    private var collapsedReports = 0
    private var reexpandReports = 0
    private var attempt by mutableIntStateOf(0)
    private lateinit var state: CollapsibleAdState

    private val context: Context get() = ApplicationProvider.getApplicationContext()
    private val badge: String get() = context.getString(R.string.ads_native_badge)

    private val clock = MonotonicClock { composeRule.mainClock.currentTime }

    @Composable
    private fun Host(expandAfterSeconds: Long, renderAd: (@Composable (LoadedNativeAd, () -> Unit) -> Unit)? = null) {
        AppTheme(darkTheme = false) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
                key(attempt) {
                    state = rememberCollapsibleAdState()
                    val states = remember { feed.states() }
                    val modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = Spacing.md, end = Spacing.md, bottom = Spacing.md)
                        .testTag(HOST)
                    if (renderAd == null) {
                        CollapsibleNativeAdLayerHost(
                            states = states,
                            expandAfterSeconds = expandAfterSeconds,
                            clock = clock,
                            state = state,
                            onCollapsed = { collapsedReports++ },
                            onReexpanded = { reexpandReports++ },
                            modifier = modifier,
                        )
                    } else {
                        CollapsibleNativeAdLayerHost(
                            states = states,
                            expandAfterSeconds = expandAfterSeconds,
                            clock = clock,
                            state = state,
                            onCollapsed = { collapsedReports++ },
                            onReexpanded = { reexpandReports++ },
                            modifier = modifier,
                            renderAd = renderAd,
                        )
                    }
                }
            }
        }
    }

    /** The host with an ad shown; [expandAfterSeconds] 0 never opens it again, so no timer runs. */
    private fun setHost(expandAfterSeconds: Long = 0, ad: FakeLoadedNativeAd? = collapsibleTestAd()) {
        composeRule.setContent { Host(expandAfterSeconds) }
        composeRule.waitForIdle()
        if (ad != null) update { feed.load(ad) }
    }

    /** Same, with the clock of Compose stopped: the test moves it ([advanceTo], [frame]). */
    private fun setTimedHost(expandAfterSeconds: Long, ad: FakeLoadedNativeAd = collapsibleTestAd()) {
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent { Host(expandAfterSeconds) }
        frame()
        feed.load(ad)
        frame()
    }

    private fun frame() {
        composeRule.mainClock.advanceTimeByFrame()
        composeRule.waitForIdle()
    }

    private fun advanceTo(timeMillis: Long) {
        val step = timeMillis - composeRule.mainClock.currentTime
        if (step > 0) composeRule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
        composeRule.waitForIdle()
    }

    private fun update(change: () -> Unit) {
        composeRule.runOnIdle(change)
        composeRule.waitForIdle()
    }

    private fun node(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true)

    private fun bounds(tag: String) = node(tag).getUnclippedBoundsInRoot()

    private fun assertAbsent(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)

    private fun assertShown() = composeRule.onAllNodesWithTag(AD, useUnmergedTree = true).assertCountEquals(1)

    private fun assertHeight(tag: String, expected: Float) {
        val b = bounds(tag)
        assertEquals("height of $tag", expected, (b.bottom - b.top).value, TOLERANCE)
    }

    private fun collapse() {
        node(COLLAPSE).performClick()
        composeRule.waitForIdle()
    }

    /** Collapses with the clock stopped; returns the mark the state took. */
    private fun collapseTimed(): Long {
        node(COLLAPSE).performClick()
        // One frame delivers the tap, the next one draws the collapsed state.
        frame()
        frame()
        return checkNotNull(state.collapsedAtMillis) { "no mark after collapsing" }
    }

    // Size and parts of the open layer (KB-4, design/context/AdGalleryScreen_collapsible-expanded_light.md).

    @Test
    fun firstShown_isExpanded250dp() {
        setHost()
        node(MEDIA).assertExists()
        node(COLLAPSE).assertExists()
        assertHeight(AD, 250f)
        composeRule.onNodeWithText(badge, useUnmergedTree = true).assertExists()
        composeRule.onNodeWithContentDescription("Collapse ad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun collapseButton_drawn32dp_inTopEndCorner4dp_touchAtLeast48dp() {
        setHost()
        val ad = bounds(AD)
        val button = bounds(COLLAPSE)
        assertEquals("button width", 32f, (button.right - button.left).value, TOLERANCE)
        assertEquals("button height", 32f, (button.bottom - button.top).value, TOLERANCE)
        assertEquals("gap to the top", 4f, (button.top - ad.top).value, TOLERANCE)
        assertEquals("gap to the end", 4f, (ad.right - button.right).value, TOLERANCE)
        val centerY = (button.top + button.bottom) / 2
        val centerX = (button.left + button.right) / 2

        // The touch area is widened to 48 dp around the 32 dp button: 5 dp outside still collapses.
        tapAt(button.left - 5.dp, centerY)
        assertEquals("5 dp left of the button", 1, collapsedReports)

        update { attempt++ }
        update { feed.load(collapsibleTestAd()) }
        tapAt(centerX, button.bottom + 5.dp)
        assertEquals("5 dp below the button", 2, collapsedReports)

        // Control: 10 dp left of the button is outside the 48 dp area.
        update { attempt++ }
        update { feed.load(collapsibleTestAd()) }
        tapAt(button.left - 10.dp, centerY)
        assertEquals("10 dp left of the button", 2, collapsedReports)
        assertShown()
    }

    @Test
    fun layer_leavesAdChoicesTopStartCornerFree() {
        setHost()
        val ad = bounds(AD)
        // The SDK pins AdChoices in the top start corner of this layer (ADCHOICES_TOP_LEFT): nothing to tap there.
        val corner = with(composeRule.density) { DpRect(ad.left, ad.top, ad.left + 24.dp, ad.top + 24.dp).toRect() }
        val clickable = composeRule
            .onAllNodes(SemanticsMatcher.keyIsDefined(SemanticsActions.OnClick), useUnmergedTree = true)
            .fetchSemanticsNodes()
        assertTrue("the collapse button is clickable", clickable.isNotEmpty())
        clickable.forEach { clickableNode ->
            assertFalse(
                "a clickable node ${clickableNode.boundsInRoot} overlaps the AdChoices corner $corner",
                clickableNode.boundsInRoot.overlaps(corner),
            )
        }
    }

    @Test
    fun layer_iconIs32dp_headline13sp_ctaIs36dp() {
        setHost()
        val icon = bounds(NativeAdTestTags.ICON)
        assertEquals(32f, (icon.right - icon.left).value, TOLERANCE)
        assertEquals(32f, (icon.bottom - icon.top).value, TOLERANCE)
        assertHeight(NativeAdTestTags.CTA, 36f)
        val cta = bounds(NativeAdTestTags.CTA)
        val ad = bounds(AD)
        val ctaWidth = (cta.right - cta.left).value
        assertEquals("CTA as wide as the layer, 12 dp in", (ad.right - ad.left).value - 24f, ctaWidth, TOLERANCE)

        val results = mutableListOf<TextLayoutResult>()
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true)
            .fetchSemanticsNode().config[SemanticsActions.GetTextLayoutResult].action?.invoke(results)
        assertEquals(13f, results.single().layoutInput.style.fontSize.value, 0.01f)
    }

    @Test
    fun media_fillsRemainingHeight() {
        setHost()
        val media = bounds(MEDIA)
        assertTrue("media is ${media.right - media.left} wide", (media.right - media.left).value >= 120f)
        // 250 - 2 x (1 + 12) margins - 38 header - 36 call to action - 2 x 8 gaps = 134 dp, as in the design.
        assertHeight(MEDIA, 134f)
    }

    @Test
    fun collapseButton_colorsBlack40AndWhite() {
        var scheme: ColorScheme? = null
        composeRule.setContent {
            AppTheme(darkTheme = false) { scheme = MaterialTheme.colorScheme }
            Host(expandAfterSeconds = 0)
        }
        composeRule.waitForIdle()
        update { feed.load(collapsibleTestAd()) }
        val colors = checkNotNull(scheme)
        val bitmap = windowBitmap()
        val button = windowBounds(COLLAPSE)
        // In the circle, away from the 20 dp icon: the 40% scrim over the media.
        val background = bitmap.getPixel((button.center.x - px(11.dp)).toInt(), button.center.y.toInt())
        val expected = colors.scrim.copy(alpha = 0.4f).compositeOver(colors.surfaceContainer)
        assertColor("button background", expected, background)
        // The chevron's stroke: the pixel of the icon most unlike the background is white.
        val icon = Rect(
            button.center.x - px(10.dp),
            button.center.y - px(10.dp),
            button.center.x + px(10.dp),
            button.center.y + px(10.dp),
        )
        assertColor("chevron", Color.White, farthestPixel(bitmap, icon, background))
    }

    // Collapse (KB-4, criterion 2).

    @Test
    fun tapCollapse_hidesLayer_keepsAd() {
        val ad = collapsibleTestAd()
        setHost(ad = ad)
        // Control: the layer is shown first.
        assertShown()

        collapse()

        assertAbsent(AD)
        assertAbsent(MEDIA)
        assertAbsent(COLLAPSE)
        assertEquals("the ad is kept to open again", 0, ad.destroyCount)
        assertTrue(state.collapsed)
    }

    @Test
    fun tapCollapse_reportsOnce() {
        setHost()
        collapse()
        assertEquals(1, collapsedReports)
    }

    @Test
    fun stateHolder_collapseSetsMark_expandClearsIt() {
        val holder = CollapsibleAdState(collapsed = false)
        assertFalse(holder.collapsed)
        assertNull(holder.collapsedAtMillis)
        holder.collapse(1_000)
        holder.collapse(5_000)
        assertTrue(holder.collapsed)
        assertEquals("the first tap sets the mark", 1_000L, holder.collapsedAtMillis)

        holder.expand()
        assertFalse(holder.collapsed)
        assertNull(holder.collapsedAtMillis)
    }

    // First ad, loading, no ad (CL-01, D-04).

    @Test
    fun loading_drawsNothing_slotVisible() {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                BottomAdBarLayout(
                    reservation = BottomAdBarDefaults.nativeReservation,
                    slot = { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, it.testTag(SLOT)) },
                    layer = {
                        CollapsibleNativeAdLayerHost(
                            states = remember { feed.states() },
                            expandAfterSeconds = 30,
                            clock = clock,
                            state = rememberCollapsibleAdState(),
                            onCollapsed = {},
                            onReexpanded = {},
                            modifier = it,
                        )
                    },
                ) {}
            }
        }
        composeRule.waitForIdle()
        assertAbsent(AD)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        node(SLOT).assertExists()

        // Control: the ad arrives and the layer opens over the slot.
        update { feed.load(collapsibleTestAd()) }
        assertShown()
        node(SLOT).assertExists()
    }

    @Test
    fun unavailable_drawsNothing() {
        setHost()
        // Control: the host has the layer's height while an ad is shown.
        assertHeight(HOST, 250f)

        update { feed.state.value = NativeAdState.Unavailable }
        assertAbsent(AD)
        assertHeight(HOST, 0f)
    }

    @Test
    fun firstAdAfterCollapse_staysCollapsed() {
        setHost()
        collapse()
        // The layer reloads (no ad for a while), then a first ad of the new load comes.
        update { feed.state.value = NativeAdState.Loading }
        assertAbsent(AD)
        update { feed.load(collapsibleTestAd("Second ad")) }
        assertAbsent(AD)
        assertTrue(state.collapsed)
    }

    @Test
    fun collapsibleFormat_loadsAsMedium() {
        assertEquals(NativeAdSize.MEDIUM, AdFormat.NATIVE_COLLAPSIBLE.nativeLoadSize())
    }

    @Test
    fun leavingComposition_destroysHeldAd_evenWhenCollapsed() {
        val ad = collapsibleTestAd()
        setHost(ad = ad)
        collapse()
        assertEquals(0, ad.destroyCount)

        update { attempt++ }

        assertEquals("the held ad is destroyed with the layer (L-061)", 1, ad.destroyCount)
    }

    // Refresh does not change the state (CL-02, CL-04, L-A12).

    @Test
    fun collapsed_refreshSwap_staysCollapsed() {
        setHost()
        collapse()
        update { feed.load(collapsibleTestAd("Second ad")) }
        assertAbsent(AD)
        assertTrue(state.collapsed)
    }

    @Test
    fun expanded_refreshSwap_staysExpanded_rebinds() {
        setHost()
        update { feed.load(collapsibleTestAd("Second ad")) }
        assertShown()
        composeRule.onNodeWithText("Second ad", useUnmergedTree = true).assertExists()
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertFalse(state.collapsed)
    }

    // Opens again (CL-03).

    @Test
    fun collapsed_reexpandsAfterRemoteSeconds() {
        setTimedHost(expandAfterSeconds = 30)
        val mark = collapseTimed()
        assertAbsent(AD)

        advanceTo(mark + 29_999)
        assertTrue("still collapsed 1 ms before 30 s", state.collapsed)
        assertEquals(0, reexpandReports)

        advanceTo(mark + 30_000)
        assertFalse("open at 30 s", state.collapsed)
        assertEquals(1, reexpandReports)
        frame()
        assertShown()
    }

    @Test
    fun collapsed_zeroSeconds_neverReexpands() {
        setTimedHost(expandAfterSeconds = 0)
        val mark = collapseTimed()

        advanceTo(mark + 10 * 60_000)
        frame()

        assertAbsent(AD)
        assertTrue(state.collapsed)
        assertEquals(0, reexpandReports)
    }

    @Test
    fun reexpandDue_withoutAd_waitsForAd() {
        setTimedHost(expandAfterSeconds = 30)
        val mark = collapseTimed()
        feed.state.value = NativeAdState.Loading
        frame()

        advanceTo(mark + 40_000)
        frame()
        assertAbsent(AD)
        assertTrue("still collapsed while there is no ad", state.collapsed)

        feed.load(collapsibleTestAd("Late ad"))
        frame()
        frame()
        assertShown()
        composeRule.onNodeWithText("Late ad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun refreshSwap_doesNotResetReexpandTimer() {
        setTimedHost(expandAfterSeconds = 30)
        val mark = collapseTimed()

        advanceTo(mark + 20_000)
        feed.load(collapsibleTestAd("Second ad"))
        frame()
        assertAbsent(AD)

        advanceTo(mark + 30_000)
        assertFalse("open 30 s after the tap", state.collapsed)
        frame()
        assertShown()
        composeRule.onNodeWithText("Second ad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun collapsed_survivesStateRestoration_timerKeepsMark() {
        val restoration = StateRestorationTester(composeRule)
        composeRule.mainClock.autoAdvance = false
        restoration.setContent { Host(expandAfterSeconds = 30) }
        frame()
        feed.load(collapsibleTestAd())
        frame()
        val mark = collapseTimed()

        advanceTo(mark + 10_000)
        restoration.emulateSavedInstanceStateRestore()
        frame()
        feed.load(collapsibleTestAd())
        frame()
        assertTrue("collapsed after restoring", state.collapsed)
        assertEquals("the mark is kept", mark, state.collapsedAtMillis)
        assertAbsent(AD)

        advanceTo(mark + 29_999)
        assertTrue("still collapsed before 30 s from the tap", state.collapsed)
        advanceTo(mark + 30_000)
        assertFalse("open 30 s from the tap, not from the restoration", state.collapsed)
        frame()
        assertShown()
    }

    @Test
    fun reexpand_rebindsHeldAdOnce() {
        val ad = collapsibleTestAd()
        val bindsPerView = mutableMapOf<NativeAdView, Int>()
        composeRule.setContent {
            Host(expandAfterSeconds = 0) { held, onCollapse ->
                NativeAdViewHost(bind = { bindsPerView[it] = (bindsPerView[it] ?: 0) + 1 }) { binder ->
                    CollapsibleNativeAdLayout(held.assets, onCollapse, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.waitForIdle()
        update { feed.load(ad) }
        val first = bindsPerView.keys.single()
        collapse()

        update { state.expand() }

        val views = bindsPerView.keys.toList()
        assertEquals("a new NativeAdView after opening again", 2, views.size)
        val second = views.last()
        assertNotSame(first, second)
        assertEquals("the new view is bound once", 1, bindsPerView[second])
        assertTrue(second.mediaView is MediaView)
        assertNotNull(second.headlineView)
        assertNotNull(second.iconView)
        assertNotNull(second.advertiserView)
        assertNotNull(second.callToActionView)
        assertEquals("the held ad is shown again, not destroyed", 0, ad.destroyCount)
        assertEquals("no new load", 1, feed.collections)
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun layer_smallScreen_fitsOnScreen() {
        setHost()
        assertHeight(AD, 250f)
        val screen = Rect(0f, 0f, 360f, 640f)
        listOf(COLLAPSE, NativeAdTestTags.CTA).forEach { tag ->
            val b = bounds(tag)
            val inside = b.left.value >= screen.left - TOLERANCE &&
                b.right.value <= screen.right + TOLERANCE &&
                b.top.value >= screen.top - TOLERANCE &&
                b.bottom.value <= screen.bottom + TOLERANCE
            assertTrue("$tag at $b is not inside the 360 x 640 dp screen", inside)
        }
    }

    private fun tapAt(x: Dp, y: Dp) {
        val point = with(composeRule.density) { Offset(x.toPx(), y.toPx()) }
        composeRule.onRoot().performTouchInput { click(point) }
        composeRule.waitForIdle()
    }

    private fun windowBitmap(): Bitmap {
        val view = (composeRule as AndroidComposeTestRule<*, *>).activity.window.decorView
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        view.draw(Canvas(bitmap))
        return bitmap
    }

    private fun windowBounds(tag: String): Rect = node(tag).fetchSemanticsNode().boundsInWindow

    private fun px(dp: Dp): Float = with(composeRule.density) { dp.toPx() }

    /** The pixel of [area] most unlike [background]: a pixel inside the icon's stroke. */
    private fun farthestPixel(bitmap: Bitmap, area: Rect, background: Int): Int {
        var best = background
        var bestDistance = -1
        for (y in area.top.toInt() until area.bottom.toInt()) {
            for (x in area.left.toInt() until area.right.toInt()) {
                val pixel = bitmap.getPixel(x, y)
                val distance = channels(pixel).zip(channels(background)).sumOf { (a, b) -> abs(a - b) }
                if (distance > bestDistance) {
                    bestDistance = distance
                    best = pixel
                }
            }
        }
        return best
    }

    private fun assertColor(what: String, expected: Color, actual: Int) {
        val want = channels(expected.toArgb())
        val got = channels(actual)
        assertTrue(
            "$what: expected ${want.joinToString()} got ${got.joinToString()}",
            want.zip(got).all { (a, b) -> abs(a - b) <= COLOR_TOLERANCE },
        )
    }

    private fun channels(argb: Int): List<Int> = listOf((argb shr 16) and 0xFF, (argb shr 8) and 0xFF, argb and 0xFF)

    private companion object {
        const val AD = "collapsible_ad"
        const val MEDIA = "collapsible_ad_media"
        const val COLLAPSE = "collapsible_ad_collapse"
        const val HOST = "host"
        const val SLOT = "slot"
        const val TOLERANCE = 1f
        const val COLOR_TOLERANCE = 8
    }
}
