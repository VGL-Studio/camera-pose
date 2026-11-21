package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.StateRestorationTester
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
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

/**
 * A native feed of one slot or layer for tests: each collection starts at Loading (like a slot built anew), shows
 * what the test puts in [state], and destroys the ad it shows when the collection is cancelled (L-061).
 * [activeCollectors] counts the collections running now, [cancellations] the collections cancelled (plan T-090).
 */
internal class FakeNativeFeed {
    val state = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
    var collections = 0
        private set
    var activeCollectors = 0
        private set
    var cancellations = 0
        private set

    fun states(): Flow<NativeAdState> = flow {
        collections++
        activeCollectors++
        state.value = NativeAdState.Loading
        try {
            emitAll(state)
        } catch (e: CancellationException) {
            cancellations++
            throw e
        } finally {
            activeCollectors--
            (state.value as? NativeAdState.Loaded)?.ad?.destroy()
        }
    }

    fun load(ad: LoadedNativeAd) {
        state.value = NativeAdState.Loaded(ad)
    }
}

/**
 * The bottom bar (plan T-030; D-01 to D-05, CL-01, L-061, L-068): which layers it builds, the space the content keeps,
 * and what happens when switches change while it shows. Built with [BottomAdBarHost] and fake feeds, no controller.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomAdBarTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var canRequest by mutableStateOf(true)
    private var enabled by mutableStateOf(true)
    private var flags by mutableStateOf(barFlags(nativeBottom = true, collap = true))
    private var slotAllowed by mutableStateOf(true)
    private var layerUnitIds by mutableStateOf<AdUnitIds>(AllUnits)
    private var collapsibleLayer by mutableStateOf(true)

    private val slotFeed = FakeNativeFeed()
    private val layerFeed = FakeNativeFeed()
    private val banners = FakeBannerAdViews()

    /** Whether the screen of the banner slot shows: false holds its first send (B-05). */
    private val bannerVisible = MutableStateFlow(true)
    private val recordedDelays = mutableListOf<Long>()
    private val reservations = mutableListOf<Dp>()
    private val layerChanges = mutableListOf<BottomAdBarLayers?>()
    private var contentTaps = 0

    private val placement = AdPlacement.POSES_BOTTOM

    /**
     * The bar of the tests. With [enabledFlow] (a real switch of the controller, plan T-063), the switch of the layer
     * choice, the banner and the native slot all come from that flow, through the gate functions of the real slots;
     * without it, from [enabled] and [slotAllowed] as before.
     */
    @Composable
    private fun Bar(
        enabledFlow: Flow<Boolean>? = null,
        content: @Composable (PaddingValues) -> Unit = { DefaultContent(it) },
    ) {
        AdsTestTheme(darkTheme = false) {
            val enabledFromFlow = enabledFlow?.let { remember(it) { it }.collectAsState(initial = false).value }
            val barEnabled = enabledFromFlow ?: enabled
            val nativeAllowed = enabledFromFlow?.let { nativeSlotAllowed(canRequest, it, "unit") } ?: slotAllowed
            val choice = rememberBottomAdBarLayers(canRequest, barEnabled, flags, collapsibleLayer = collapsibleLayer)
            BottomAdBarHost(
                layers = choice.layers,
                nativeSlot = { delay, onTakesSpace, modifier, placeholderWhileLoading ->
                    SideEffect { recordedDelays += delay }
                    NativeAdSlotGate(
                        allowed = nativeAllowed,
                        states = remember { slotFeed.states() },
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = false,
                        onAdAvailabilityChange = {},
                        modifier = modifier,
                        onTakesSpaceChange = onTakesSpace,
                        placeholderWhileLoading = placeholderWhileLoading,
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL, Modifier.testTag(SLOT_AD)) },
                    )
                },
                bannerSlot = { onAvailability, onHeight, modifier ->
                    BannerSlotHost(
                        gateOpen = canRequest,
                        enabled = barEnabled,
                        unitId = layerUnitIds.of(placement, AdFormat.BANNER),
                        heightDp = BANNER_HEIGHT_DP,
                        visible = bannerVisible,
                        covered = MutableStateFlow(false),
                        mayRequestNow = { canRequest },
                        retryResets = MutableStateFlow(0),
                        reloadMillis = MutableStateFlow(0L),
                        logger = RecordingAppLogger(),
                        placementKey = placement.key,
                        newView = banners::newView,
                        modifier = modifier,
                        onAdAvailabilityChange = onAvailability,
                        inBottomBar = true,
                        onSlotHeight = onHeight,
                    )
                },
                layer = { modifier, onShowingChange ->
                    CollapsibleNativeAdLayerHost(
                        states = remember { layerFeed.states() },
                        expandAfterSeconds = 30,
                        clock = { composeRule.mainClock.currentTime },
                        state = rememberCollapsibleAdState(),
                        onCollapsed = {},
                        onReexpanded = {},
                        modifier = modifier,
                        allowed = layerUnitIds.of(placement, AdFormat.NATIVE_COLLAPSIBLE) != null,
                        onShowingChange = onShowingChange,
                    )
                },
                onLayersChange = { layerChanges += it },
                onSlotHasAdChange = choice.onSlotHasAdChange,
                content = content,
            )
        }
    }

    @Composable
    private fun DefaultContent(padding: PaddingValues) {
        val bottom = padding.calculateBottomPadding()
        SideEffect { if (reservations.lastOrNull() != bottom) reservations += bottom }
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .testTag(CONTENT)
                .clickable { contentTaps++ },
        )
    }

    private fun setBar() {
        composeRule.setContent { Bar() }
        composeRule.waitForIdle()
    }

    private fun update(change: () -> Unit) {
        composeRule.runOnIdle(change)
        composeRule.waitForIdle()
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertAbsent(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(0)

    private fun assertPresent(tag: String) =
        composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).assertCountEquals(1)

    private fun contentBottom(): Float = bounds(CONTENT).bottom.value

    private fun assertContentBottom(message: String, expected: Float) =
        assertEquals(message, expected, contentBottom(), TOLERANCE)

    /**
     * An ad for each layer, both kept (D-07, plan T-090): the layer gets its ad and is collapsed when it opened (no
     * collapse button on a window with no room for it), then the slot gets its ad. Returns the slot ad, the layer ad.
     */
    private fun loadBoth(): Pair<FakeLoadedNativeAd, FakeLoadedNativeAd> {
        val slotAd = FakeLoadedNativeAd()
        val layerAd = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))
        update { layerFeed.load(layerAd) }
        if (composeRule.onAllNodesWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true)
                .fetchSemanticsNodes().isNotEmpty()
        ) {
            collapseLayer()
        }
        update { slotFeed.load(slotAd) }
        return slotAd to layerAd
    }

    /** The slot gets its ad, then the layer gets its ad and opens over it (D-07: the slot gives way). */
    private fun loadLayerOpen(): Pair<FakeLoadedNativeAd, FakeLoadedNativeAd> {
        val slotAd = FakeLoadedNativeAd()
        val layerAd = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))
        update { slotFeed.load(slotAd) }
        update { layerFeed.load(layerAd) }
        return slotAd to layerAd
    }

    private fun collapseLayer() {
        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()
    }

    private fun slotHeight(): Float = bounds(BottomAdBarTestTags.SLOT).let { (it.bottom - it.top).value }

    // Layers of the bar (D-01, D-04).

    @Test
    fun layers_nativeBottomOn_collapOn_nativeSlotAndLayer() {
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true),
            bottomAdBarLayers(allowed = true, flags = barFlags(nativeBottom = true, collap = true)),
        )
    }

    @Test
    fun layers_notAllowed_null() {
        val on = barFlags(nativeBottom = true, collap = true)
        assertNull(bottomAdBarLayers(allowed = false, flags = on))
        // Control: the same switches with the bar allowed give layers.
        assertNotNull(bottomAdBarLayers(allowed = true, flags = on))
    }

    @Test
    fun layers_collapOff_keepsNativeSlot_noLayer() {
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = false),
            bottomAdBarLayers(allowed = true, flags = barFlags(nativeBottom = true, collap = false)),
        )
    }

    @Test
    fun layers_nativeBottomOff_bannerSlot_noLayer() {
        // is_show_native_collap on alone: showNativeCollap is already false without is_show_native_bottom (T-019).
        val off = barFlags(nativeBottom = false, collap = false)
        assertEquals(BottomAdBarLayers(BottomSlotKind.BANNER, collapsible = false), bottomAdBarLayers(true, off))
    }

    @Test
    fun allowed_consentAndPlacement_bannerWithAdKeptWhenConsentWithdrawn() {
        assertTrue(bottomAdBarAllowed(canRequestAds = true, enabled = true))
        assertFalse(bottomAdBarAllowed(canRequestAds = false, enabled = true))
        assertFalse(bottomAdBarAllowed(canRequestAds = true, enabled = false))
        // B-06: a banner that had an ad keeps its frame when consent is withdrawn, not when the placement is off.
        assertTrue(bottomAdBarAllowed(canRequestAds = false, enabled = true, bannerHadAd = true))
        assertFalse(bottomAdBarAllowed(canRequestAds = true, enabled = false, bannerHadAd = true))
    }

    @Test
    fun layers_bannerKeptWithoutConsent_neverNativeOrLayer() {
        val nativeOn = barFlags(nativeBottom = true, collap = true)
        // B-06 keeps the banner that had an ad; with no consent nothing else may load, even if the native slot is on.
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.BANNER, collapsible = false),
            bottomAdBarLayers(
                canRequestAds = false,
                enabled = true,
                flags = nativeOn,
                bannerHadAd = true,
                collapsibleLayer = true,
            ),
        )
        // Control: with consent the same switches give the native slot and the layer.
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true),
            bottomAdBarLayers(
                canRequestAds = true,
                enabled = true,
                flags = nativeOn,
                bannerHadAd = true,
                collapsibleLayer = true,
            ),
        )
        assertNull(
            bottomAdBarLayers(
                canRequestAds = false,
                enabled = true,
                flags = nativeOn,
                bannerHadAd = false,
                collapsibleLayer = true,
            ),
        )
        assertNull(
            bottomAdBarLayers(
                canRequestAds = false,
                enabled = false,
                flags = nativeOn,
                bannerHadAd = true,
                collapsibleLayer = true,
            ),
        )
    }

    @Test
    fun layers_collapsibleLayerOff_noLayerWithCollapOn() {
        // A placement declared without the layer (D-01).
        val collapOn = barFlags(nativeBottom = true, collap = true)
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = false),
            bottomAdBarLayers(true, true, collapOn, false, collapsibleLayer = false),
        )
        // Control: the same switches for a placement with the layer give it.
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true),
            bottomAdBarLayers(true, true, collapOn, false, collapsibleLayer = true),
        )
        // is_show_native_bottom off still gives the banner slot.
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.BANNER, collapsible = false),
            bottomAdBarLayers(
                true,
                true,
                barFlags(nativeBottom = false, collap = true),
                false,
                collapsibleLayer = false,
            ),
        )
    }

    @Test
    fun host_collapsibleLayerOff_collapSwitchedOn_noLayer() {
        collapsibleLayer = false
        flags = barFlags(nativeBottom = true, collap = false)
        setBar()
        assertPresent(BottomAdBarTestTags.SLOT)
        assertAbsent(BottomAdBarTestTags.LAYER)

        // is_show_native_collap switched on while the screen shows.
        update { flags = barFlags(nativeBottom = true, collap = true) }

        assertAbsent(BottomAdBarTestTags.LAYER)
        assertPresent(BottomAdBarTestTags.SLOT)

        // Control: the same bar for a placement with the layer gets it.
        update { collapsibleLayer = true }
        assertPresent(BottomAdBarTestTags.LAYER)
        assertPresent(BottomAdBarTestTags.SLOT)
    }

    // Space kept by the content (D-04, AR-02).

    @Test
    fun reservation_nativeWithAd_isSlotPlusMargin() {
        assertEquals(
            122.dp,
            bottomBarReservation(
                BottomSlotKind.NATIVE,
                nativeTakesSpace = true,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        var reservation = 0.dp
        composeRule.setContent { reservation = BottomAdBarDefaults.nativeReservation() }
        composeRule.waitForIdle()
        assertEquals(122.dp, reservation)
        assertEquals(
            bottomBarReservation(
                BottomSlotKind.NATIVE,
                nativeTakesSpace = true,
                bannerHeight = 0.dp,
                nativeSlotHeight = 106.dp,
            ),
            reservation,
        )
    }

    @Test
    fun reservation_bannerWithAd_isBannerHeight() {
        assertEquals(
            62.dp,
            bottomBarReservation(
                BottomSlotKind.BANNER,
                nativeTakesSpace = false,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
    }

    @Test
    fun reservation_noAd_isZero() {
        assertEquals(
            0.dp,
            bottomBarReservation(
                BottomSlotKind.NATIVE,
                nativeTakesSpace = false,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        // D-04: the banner slot keeps the height its frame takes, ad or not: 0 once the frame takes none (a failure
        // before the first ad); control: its frame height while it loads (issue #527).
        assertEquals(
            0.dp,
            bottomBarReservation(
                BottomSlotKind.BANNER,
                nativeTakesSpace = false,
                bannerHeight = 0.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        assertEquals(
            62.dp,
            bottomBarReservation(
                BottomSlotKind.BANNER,
                nativeTakesSpace = false,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        // Control: the same slot with an ad keeps space (L-055): the 106 dp slot + 16 dp.
        assertEquals(
            106.dp + 16.dp,
            bottomBarReservation(
                BottomSlotKind.NATIVE,
                nativeTakesSpace = true,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        assertEquals(
            122.dp,
            bottomBarReservation(
                BottomSlotKind.NATIVE,
                nativeTakesSpace = true,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
    }

    @Test
    fun reservation_notAllowed_isZero() {
        assertEquals(
            0.dp,
            bottomBarReservation(null, nativeTakesSpace = true, bannerHeight = 62.dp, nativeSlotHeight = 106.dp),
        )
    }

    @Test
    fun reservedSlot_equalsDrawnSlotHeight() {
        val reserved = bottomBarReservation(
            BottomSlotKind.NATIVE,
            nativeTakesSpace = true,
            bannerHeight = 0.dp,
            nativeSlotHeight = 106.dp,
        ) - BottomAdBarDimens.SlotMargin
        assertEquals(106.dp, reserved)
        setBar()
        val (_, layerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, layerAd.destroyCount)
        val slot = bounds(SLOT_AD)
        assertEquals(reserved.value, (slot.bottom - slot.top).value, TOLERANCE)
    }

    /**
     * KB-17 criterion 5, D-04, KB-32 (plan T-109): the space kept for the native slot is the same on every window at
     * font scale 1.0, the slot drawn (106 dp) and its 16 dp margin: 122 dp; a banner still keeps its own height.
     */
    @Test
    fun nativeReservation_sameEveryWindow() = assertNativeReservation(expected = 122.dp, expectedSlot = 106.dp)

    /** The same on a 360 dp screen. */
    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun nativeReservation_sameEveryWindow_360() = assertNativeReservation(expected = 122.dp, expectedSlot = 106.dp)

    /** The same on a 891 x 411 dp window. */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun nativeReservation_sameEveryWindow_landscape() =
        assertNativeReservation(expected = 122.dp, expectedSlot = 106.dp)

    /**
     * KB-17 criterion 5, KB-32 (question 66, plan T-109): with larger text the slot grows and the space kept with it:
     * the slot drawn and its 16 dp margin, more than 122 dp at font scale 2.0; control at 1.0 in the same test, 122 dp.
     */
    @Test
    fun nativeReservation_followsFontScale() {
        var fontScale by mutableStateOf(1f)
        var reservation = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    reservation = BottomAdBarDefaults.nativeReservation()
                    BottomAdBarLayout(
                        reservation = reservation,
                        slot = { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, it.testTag(SLOT_AD)) },
                        layer = null,
                    ) {}
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals("kept space at 1.0", 122f, reservation.value, 0.5f)
        update { fontScale = 2f }
        val slot = bounds(SLOT_AD)
        assertEquals("slot drawn plus 16 dp at 2.0", (slot.bottom - slot.top).value + 16f, reservation.value, 0.5f)
        assertTrue("kept space at 2.0 is $reservation", reservation > 122.dp)
    }

    private fun assertNativeReservation(expected: Dp, expectedSlot: Dp? = null) {
        var reservation = 0.dp
        var slotHeight = 0.dp
        composeRule.setContent {
            AdsTestTheme {
                reservation = BottomAdBarDefaults.nativeReservation()
                slotHeight = BottomAdBarDefaults.nativeSlotHeight()
                BottomAdBarLayout(
                    reservation = reservation,
                    slot = { NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, it.testTag(SLOT_AD)) },
                    layer = null,
                ) {}
            }
        }
        composeRule.waitForIdle()
        assertEquals("kept space", expected.value, reservation.value, 0.5f)
        val slot = bounds(SLOT_AD)
        assertEquals("slot drawn plus 16 dp", (slot.bottom - slot.top).value + 16f, reservation.value, 0.5f)
        assertEquals("slot height", (slot.bottom - slot.top).value, slotHeight.value, 0.5f)
        // Control: a banner keeps its own frame, whatever the screen width.
        assertEquals(
            62.dp,
            bottomBarReservation(
                BottomSlotKind.BANNER,
                nativeTakesSpace = false,
                bannerHeight = 62.dp,
                nativeSlotHeight = 106.dp,
            ),
        )
        if (expectedSlot != null) assertEquals("slot height", expectedSlot.value, slotHeight.value, 0.5f)
    }

    // Landscape, split screen and low windows (KB-4 added 2026-11-16, KB-17, KB-32 criterion 3).

    /**
     * KB-4 added 2026-11-16, KB-17: on a 891 x 411 dp window the layer has no room (not even with its media at 120
     * dp): only the 106 dp slot, 16 dp above the bottom, the content kept above it, the layer's ads never collected.
     */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun landscape_slotOnly_noLayer() {
        setBar()
        loadBoth()
        val slot = bounds(SLOT_AD)
        assertEquals("slot height", 106f, (slot.bottom - slot.top).value, TOLERANCE)
        assertEquals("slot top", 289f, slot.top.value, TOLERANCE)
        assertEquals("slot bottom", 395f, slot.bottom.value, TOLERANCE)
        assertContentBottom("content ends at the slot top", 289f)
        assertEquals(listOf(0.dp, 122.dp), reservations)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals("layer collections", 0, layerFeed.collections)
    }

    /** KB-4 added 2026-11-16: a 411 x 445 dp window (split screen) has no room for the layer either. */
    @Test
    @Config(qualifiers = "w411dp-h445dp-xxhdpi")
    fun split_slotOnly_noLayer() {
        setBar()
        loadBoth()
        val slot = bounds(SLOT_AD)
        assertEquals("slot height", 106f, (slot.bottom - slot.top).value, TOLERANCE)
        assertEquals("slot top", 323f, slot.top.value, TOLERANCE)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals("layer collections", 0, layerFeed.collections)
    }

    /**
     * KB-4 added 2026-11-16, KB-32 criterion 3: on a 411 x 600 dp window the layer with its 16 dp margin reaches half
     * the window, its media shrinking first: 284 dp (media area 178 dp, MediaView 166 dp, content 106 dp), top 300 dp.
     */
    @Test
    @Config(qualifiers = "w411dp-h600dp-xxhdpi")
    fun shortWindow_layerMediaShrinks() {
        var layerHeight: Dp? = 0.dp
        composeRule.setContent {
            layerHeight = BottomAdBarDefaults.collapsibleLayerHeight()
            Bar()
        }
        composeRule.waitForIdle()
        loadLayerOpen()
        val layer = bounds(CollapsibleNativeAdTestTags.AD)
        assertEquals("layer height", 284f, (layer.bottom - layer.top).value, TOLERANCE)
        assertEquals("layer top at half the window", 300f, layer.top.value, TOLERANCE)
        val media = bounds(CollapsibleNativeAdTestTags.MEDIA)
        assertEquals("MediaView height", 166f, (media.bottom - media.top).value, TOLERANCE)
        val content = bounds(CollapsibleNativeAdTestTags.CONTENT)
        assertEquals("content height", 106f, (content.bottom - content.top).value, TOLERANCE)
        assertEquals("collapsibleLayerHeight()", 284f, layerHeight?.value ?: -1f, 0.5f)
    }

    /** The layer height for screens drawing a fake bar: 299 dp on the 411 x 891 dp frame of this class. */
    @Test
    fun collapsibleLayerHeight_nullWhenNoRoom() {
        assertEquals(299f, collapsibleLayerHeight()?.value ?: -1f, 0.5f)
    }

    /** No room on a 891 x 411 dp window: no height, so a fake bar draws no layer. */
    @Test
    @Config(qualifiers = "w891dp-h411dp-xxhdpi")
    fun collapsibleLayerHeight_nullWhenNoRoom_landscape() {
        assertNull(collapsibleLayerHeight())
    }

    /** No room on a 411 x 445 dp window (split screen). */
    @Test
    @Config(qualifiers = "w411dp-h445dp-xxhdpi")
    fun collapsibleLayerHeight_nullWhenNoRoom_split() {
        assertNull(collapsibleLayerHeight())
    }

    private fun collapsibleLayerHeight(): Dp? {
        var height: Dp? = (-1).dp
        composeRule.setContent { height = BottomAdBarDefaults.collapsibleLayerHeight() }
        composeRule.waitForIdle()
        return height
    }

    /**
     * KB-4 criterion 4, CL-05 (plan T-109): on a 411 x 520 dp window the layer has room at font scale 1.0 (244 dp: the
     * media area 138 dp, floor(260 - 122)); at 2.0 the small native is taller than 112 dp, so there is no room left.
     */
    @Test
    @Config(qualifiers = "w411dp-h520dp-xxhdpi")
    fun collapsibleLayerHeight_largeTextLeavesNoRoom() {
        var fontScale by mutableStateOf(1f)
        var height: Dp? = (-1).dp
        var slotHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme {
                    height = BottomAdBarDefaults.collapsibleLayerHeight()
                    slotHeight = BottomAdBarDefaults.nativeSlotHeight()
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals("layer at 1.0", 244f, height?.value ?: -1f, 0.5f)
        update { fontScale = 2f }
        // Control: the small native at 2.0 leaves less than the 132 dp media area in half the window.
        assertTrue("small native at 2.0 is $slotHeight", slotHeight > 112.dp)
        assertNull("layer at 2.0", height)
    }

    @Test
    fun contentBottom_meetsSlotTop_whenSlotHasAd() {
        flags = barFlags(nativeBottom = true, collap = false)
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        val slot = bounds(SLOT_AD)
        assertEquals("slot top", SLOT_TOP_DP, slot.top.value, TOLERANCE)
        assertEquals("slot bottom", 875f, slot.bottom.value, TOLERANCE)
        assertContentBottom("content ends at the slot top", slot.top.value)

        // Control: no ad, the content goes down to the bottom of the screen.
        update { slotFeed.state.value = NativeAdState.Unavailable }
        assertContentBottom("content with no ad", SCREEN_HEIGHT)
    }

    @Test
    fun slotLoading_reservesPlaceholderSpace() {
        setBar()
        // D-02: the 106 dp placeholder is shown; D-04: the space is kept for it, the content ends at its top.
        val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
        assertEquals(106f, (placeholder.bottom - placeholder.top).value, TOLERANCE)
        assertContentBottom("content while loading", placeholder.top.value)
        assertContentBottom("content while loading", SLOT_TOP_DP)

        // The ad arrives in the same space.
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("content with an ad", SLOT_TOP_DP)

        // Control: a slot that draws nothing keeps nothing.
        update { slotFeed.state.value = NativeAdState.Unavailable }
        assertContentBottom("content with no ad", SCREEN_HEIGHT)
    }

    /**
     * KB-17 criterion 5, KB-32 (plan T-109): the real bar at font scale 2.0, the slot loading: the placeholder is as
     * high as the slot ([BottomAdBarDefaults.nativeSlotHeight] at 2.0, taller than 106 dp) and the content ends at its
     * top.
     */
    @Test
    fun slotLoading_largeText_reservesPlaceholderSpace() {
        var fontScale by mutableStateOf(1f)
        var slotHeight = 0.dp
        composeRule.setContent {
            WithFontScale(fontScale) {
                AdsTestTheme { slotHeight = BottomAdBarDefaults.nativeSlotHeight() }
                Bar()
            }
        }
        composeRule.waitForIdle()
        // Control: the placeholder at 1.0, so the one at 2.0 is seen to grow with the text.
        val atDefault = bounds(NativeAdTestTags.PLACEHOLDER).let { (it.bottom - it.top).value }
        update { fontScale = 2f }
        val placeholder = bounds(NativeAdTestTags.PLACEHOLDER)
        assertTrue(
            "placeholder at 2.0 ${placeholder.bottom - placeholder.top}, at 1.0 $atDefault",
            (placeholder.bottom - placeholder.top).value > atDefault + TOLERANCE,
        )
        assertTrue("slot at 2.0 is $slotHeight", slotHeight > 106.dp)
        assertEquals(
            "placeholder as high as the slot",
            slotHeight.value,
            placeholder.bottom.value - placeholder.top.value,
            TOLERANCE,
        )
        assertContentBottom("content while loading", placeholder.top.value)
        assertEquals("placeholder 16 dp above the bottom", SCREEN_HEIGHT - 16f, placeholder.bottom.value, TOLERANCE)
    }

    // The native slot of the bar (D-02, D-04): the space kept always equals the space the slot takes (issue #527).

    @Test
    fun nativeSlot_reservationEqualsSlot_loadingLayerOpenAd() {
        setBar()
        // D-02: the placeholder shows while the first load is in flight; D-04: the space is kept for it.
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertNativeSlotEqualsReservation("first load in flight", NATIVE_RESERVATION_DP)

        // The layer opens over the loading slot (D-07, plan T-090): the slot gives way, no placeholder, its load
        // stopped; the content keeps the space it had (the layer keeps none, D-04), the slot box draws nothing.
        update { layerFeed.load(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))) }
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertEquals("slot collections cancelled", 1, slotFeed.cancellations)
        assertEquals(NATIVE_RESERVATION_DP.dp, reservations.last())
        assertEquals("slot box while it gives way", 0f, slotHeight(), TOLERANCE)

        // Control: with an ad both are the slot and its margin, not 0, so the equality is not met by two zeros.
        collapseLayer()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertPresent(SLOT_AD)
        assertNativeSlotEqualsReservation("ad shown", NATIVE_RESERVATION_DP)
    }

    @Test
    fun nativeSlot_reservationEqualsSlot_failedBeforeAd_layerOpen_adAgain() {
        setBar()
        // Control: loading keeps the slot and its margin.
        assertNativeSlotEqualsReservation("first load in flight", NATIVE_RESERVATION_DP)

        update { slotFeed.state.value = NativeAdState.Unavailable }
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertNativeSlotEqualsReservation("failed before the first ad", 0)

        // The flow of the slot retries without showing the placeholder again (it never emits Loading again):
        // nothing is kept, also while the layer opens over the empty slot (D-04); the slot gives way, its retries
        // stop (D-07).
        update { layerFeed.load(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))) }
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertNativeSlotEqualsReservation("retrying, layer open over the empty slot", 0)
        assertEquals("no slot collection while it gives way", 0, slotFeed.activeCollectors)

        // Collapsed: the slot loads again with no placeholder, as it drew nothing before (KB-35 criterion 3).
        collapseLayer()
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertNativeSlotEqualsReservation("collapsed, slot loading again after its failure", 0)
        assertEquals("slot collected again", 2, slotFeed.collections)

        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertPresent(SLOT_AD)
        assertNativeSlotEqualsReservation("ad after the failure", NATIVE_RESERVATION_DP)
    }

    /**
     * The space the native slot takes (what it draws and its 16 dp bottom margin, nothing when it draws nothing) and
     * the bottom space of the content are equal, and [expectedDp] high.
     */
    private fun assertNativeSlotEqualsReservation(state: String, expectedDp: Int) {
        val slot = bounds(BottomAdBarTestTags.SLOT)
        val drawn = (slot.bottom - slot.top).value
        val taken = if (drawn > TOLERANCE) drawn + BottomAdBarDimens.SlotMargin.value else 0f
        val reservation = SCREEN_HEIGHT - contentBottom()
        assertEquals("$state: space kept equals the space the slot takes", taken, reservation, TOLERANCE)
        assertEquals("$state: space the slot takes", expectedDp.toFloat(), taken, TOLERANCE)
        assertEquals("$state: padding given to the content", expectedDp.toFloat(), reservations.last().value, TOLERANCE)
    }

    // The banner slot of the bar (D-04, B-06): the space kept always equals the frame drawn (issue #527).

    @Test
    fun bannerSlot_reservationEqualsFrame_noSendLoadingAdKept() {
        flags = barFlags(nativeBottom = false, collap = false)
        bannerVisible.value = false
        setTimedBar()
        assertEquals(1, banners.createdCount)
        assertEquals(0, banners.last.sendCount)
        assertBannerFrameEqualsReservation("no send yet", BANNER_HEIGHT_DP)

        timedUpdate { bannerVisible.value = true }
        assertEquals(1, banners.last.sendCount)
        assertBannerFrameEqualsReservation("first load in flight", BANNER_HEIGHT_DP)

        // Control: with an ad both are the frame height, not 0, so the equality is not met by two zeros.
        timedUpdate { banners.last.emitLoaded() }
        assertPresent(FAKE_BANNER_TAG)
        assertBannerFrameEqualsReservation("ad shown", BANNER_HEIGHT_DP)

        // A failure after an ad keeps the frame and the ad it had.
        timedUpdate { banners.last.emitFailed(3) }
        assertBannerFrameEqualsReservation("failure after an ad", BANNER_HEIGHT_DP)

        // B-06: consent withdrawn after an ad keeps the frame and the space.
        timedUpdate { canRequest = false }
        assertEquals(0, banners.last.destroyCount)
        assertBannerFrameEqualsReservation("consent withdrawn after an ad", BANNER_HEIGHT_DP)
    }

    @Test
    fun bannerSlot_reservationEqualsFrame_failedBeforeAd_retry_adAgain() {
        flags = barFlags(nativeBottom = false, collap = false)
        setTimedBar()
        val start = composeRule.mainClock.currentTime
        val view = banners.last
        assertEquals(1, view.sendCount)
        // Control: loading keeps the frame height.
        assertBannerFrameEqualsReservation("first load in flight", BANNER_HEIGHT_DP)

        timedUpdate { view.emitFailed(3) }
        assertEquals(0, view.destroyCount)
        assertBannerFrameEqualsReservation("failed before the first ad", 0)

        // B-02: the view retries 2 s later in the empty frame; nothing is kept until an ad loads (D-04).
        advanceTo(start + RETRY_AFTER_FIRST_FAILURE_MILLIS)
        assertEquals(2, view.sendCount)
        assertBannerFrameEqualsReservation("retry in flight", 0)

        timedUpdate { view.emitLoaded() }
        assertPresent(FAKE_BANNER_TAG)
        assertBannerFrameEqualsReservation("ad after the failure", BANNER_HEIGHT_DP)
    }

    /** [setBar] with the clock of Compose stopped: the test moves it ([advanceTo], [timedUpdate]). */
    private fun setTimedBar() {
        composeRule.mainClock.autoAdvance = false
        setBar()
        frames()
    }

    /** Two frames: one for a change, one for the space the slot reports back to the bar. */
    private fun frames() {
        repeat(2) {
            composeRule.mainClock.advanceTimeByFrame()
            composeRule.waitForIdle()
        }
    }

    private fun timedUpdate(change: () -> Unit) {
        composeRule.runOnIdle(change)
        frames()
    }

    private fun advanceTo(timeMillis: Long) {
        val step = timeMillis - composeRule.mainClock.currentTime
        if (step > 0) composeRule.mainClock.advanceTimeBy(step, ignoreFrameDuration = true)
        frames()
    }

    /** The banner frame drawn by the bar and the bottom space of the content are equal, and [expectedDp] high. */
    private fun assertBannerFrameEqualsReservation(state: String, expectedDp: Int) {
        val slot = bounds(BottomAdBarTestTags.SLOT)
        val frame = (slot.bottom - slot.top).value
        val reservation = SCREEN_HEIGHT - contentBottom()
        assertEquals("$state: space kept equals the frame drawn", frame, reservation, TOLERANCE)
        assertEquals("$state: frame drawn", expectedDp.toFloat(), frame, TOLERANCE)
        assertEquals("$state: padding given to the content", expectedDp.toFloat(), reservations.last().value, TOLERANCE)
    }

    @Test
    fun layer_notCountedInReservation() {
        flags = barFlags(nativeBottom = true, collap = false)
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        val before = contentBottom()

        update { flags = barFlags(nativeBottom = true, collap = true) }
        update { layerFeed.load(FakeLoadedNativeAd()) }
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertContentBottom("content with the layer shown", before)
    }

    @Test
    fun availabilityRepeatedTrue_reservationStable() {
        setBar()
        // The slot reports true again on each new ad (refresh in place, T-040).
        update { slotFeed.load(FakeLoadedNativeAd()) }
        update { slotFeed.load(FakeLoadedNativeAd()) }
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertEquals(listOf(0.dp, 122.dp), reservations)
    }

    @Test
    fun availabilityFalseAfterSlotNotAllowed_reservationZero() {
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        // Control: the slot with its ad keeps its space.
        assertContentBottom("with an ad", SLOT_TOP_DP)

        update { slotAllowed = false }
        assertContentBottom("slot no longer allowed", SCREEN_HEIGHT)
    }

    // The collapsible layer (D-03, KB-4).

    @Test
    fun layer_takesOnlyWidth_touchAboveReachesContent() {
        setBar()
        loadLayerOpen()
        val layer = bounds(CollapsibleNativeAdTestTags.AD)
        assertEquals("layer height", 299f, (layer.bottom - layer.top).value, TOLERANCE)
        assertEquals("layer top", 576f, layer.top.value, TOLERANCE)
        // The box of the slot stays in the bar while the slot gives way (D-07), as wide as the slot it holds.
        val slot = bounds(BottomAdBarTestTags.SLOT)
        assertEquals("as wide as the slot", (slot.right - slot.left).value, (layer.right - layer.left).value, TOLERANCE)

        val x = (layer.left + layer.right) / 2
        tapAt(x, layer.top - 10.dp)
        assertEquals("a tap 10 dp above the layer reaches the content", 1, contentTaps)

        // Control: a tap inside the layer (on its media) does not reach the content.
        tapAt(x, layer.top + 60.dp)
        assertEquals(1, contentTaps)
    }

    @Test
    fun collapse_revealsSlot() {
        setBar()
        loadLayerOpen()
        val contentBefore = contentBottom()

        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        // KB-35 criterion 3: the slot comes back loading, its placeholder where the slot is.
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals("placeholder at the slot", SLOT_TOP_DP, bounds(NativeAdTestTags.PLACEHOLDER).top.value, TOLERANCE)
        assertContentBottom("space kept after collapsing", contentBefore)
    }

    // Switches changed while the bar shows (L-061).

    @Test
    fun collapSwitchedOffWhileShown_layerRemovedAndAdDestroyed_slotStays() {
        setBar()
        val (slotAd, layerAd) = loadLayerOpen()
        val contentBefore = contentBottom()
        // Control: the layer is shown before the switch; the slot gave way to it (D-07).
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertEquals("slot ad destroyed when the layer opens", 1, slotAd.destroyCount)

        update { flags = barFlags(nativeBottom = true, collap = false) }

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, layerAd.destroyCount)
        // The slot comes back loading, the content where it was.
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertContentBottom("space kept", contentBefore)
    }

    @Test
    fun nativeBottomSwitchedOff_nativeAdDestroyed_bannerSlotShown() {
        setBar()
        val (slotAd, layerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, layerAd.destroyCount)
        assertEquals(0, banners.createdCount)

        update { flags = barFlags(nativeBottom = false, collap = false) }

        assertEquals(1, slotAd.destroyCount)
        assertEquals(1, layerAd.destroyCount)
        assertAbsent(SLOT_AD)
        assertEquals(1, banners.createdCount)
        assertPresent(FAKE_BANNER_TAG)

        // The banner has no margin: it spans the screen at the bottom.
        val banner = bounds(FAKE_BANNER_TAG)
        assertEquals(SCREEN_HEIGHT, banner.bottom.value, TOLERANCE)
        assertEquals(0f, banner.left.value, TOLERANCE)
        update { banners.last.emitLoaded() }
        assertContentBottom("banner with an ad", SCREEN_HEIGHT - BANNER_HEIGHT_DP)
    }

    @Test
    fun placementSwitchedOff_barGone_reservationZero_adsDestroyed() {
        setBar()
        val (slotAd, layerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, layerAd.destroyCount)
        assertContentBottom("control: bar with an ad", SLOT_TOP_DP)

        update { enabled = false }

        assertAbsent(SLOT_AD)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, slotAd.destroyCount)
        assertEquals(1, layerAd.destroyCount)
        assertContentBottom("bar gone", SCREEN_HEIGHT)
    }

    @Test
    fun adFreeMidSession_barGone_reservationZero_adsDestroyed() {
        // KB-30, criterion 3 (L-061): the signal of the app reaches the bar through the real switch of the controller.
        val settings = FakeRemoteSettings().apply { set(AdsSettings.placementEnabled(placement), true) }
        val adFree = MutableStateFlow(false)
        val controller = testAdsController(
            unitIds = AdUnitIds { null },
            flags = DefaultAdsFlags(settings, adFreeStatus(adFree)),
        )
        controller.openConsentGateForTest()
        composeRule.setContent { Bar(enabledFlow = controller.isEnabled(placement)) }
        composeRule.waitForIdle()
        val (slotAd, layerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, layerAd.destroyCount)
        assertContentBottom("control: bar with an ad", SLOT_TOP_DP)

        update { adFree.value = true }

        assertAbsent(SLOT_AD)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, slotAd.destroyCount)
        assertEquals(1, layerAd.destroyCount)
        assertContentBottom("bar gone", SCREEN_HEIGHT)

        // Control (L-055): the signal back off, the bar comes back; then the old path, is_show_<code> switched off
        // with the same build, also takes it away.
        update { adFree.value = false }
        val (againSlotAd, againLayerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, againLayerAd.destroyCount)
        assertContentBottom("control: bar back", SLOT_TOP_DP)
        update { settings.set(AdsSettings.placementEnabled(placement), false) }
        assertAbsent(SLOT_AD)
        assertEquals(1, againSlotAd.destroyCount)
        assertContentBottom("control: placement off", SCREEN_HEIGHT)
    }

    private fun adFreeStatus(flow: StateFlow<Boolean>): AdFreeStatus = object : AdFreeStatus {
        override val adFree = flow
    }

    @Test
    fun consentWithdrawn_barGone_adsDestroyed() {
        setBar()
        val (slotAd, layerAd) = loadBoth()
        // Control: the collapsed layer still holds its ad.
        assertEquals(0, layerAd.destroyCount)
        assertContentBottom("control: bar with an ad", SLOT_TOP_DP)

        update { canRequest = false }

        assertAbsent(SLOT_AD)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, slotAd.destroyCount)
        assertEquals(1, layerAd.destroyCount)
        assertContentBottom("bar gone", SCREEN_HEIGHT)
    }

    @Test
    fun consentWithdrawn_bannerWithAd_barAndReservationKept() {
        flags = barFlags(nativeBottom = false, collap = false)
        setBar()
        update { banners.last.emitLoaded() }
        // Control: the banner with its ad keeps its height.
        assertContentBottom("banner with an ad", SCREEN_HEIGHT - BANNER_HEIGHT_DP)

        update { canRequest = false }

        // B-06: the frame stays with its height, the ad hidden; the content keeps the space.
        assertEquals(1, banners.createdCount)
        assertEquals(0, banners.last.destroyCount)
        assertContentBottom("consent withdrawn", SCREEN_HEIGHT - BANNER_HEIGHT_DP)
    }

    @Test
    fun placementOnAgain_barBuiltFresh() {
        setBar()
        loadBoth()
        update { enabled = false }
        // The slot collected at entry, then again once the layer was collapsed (D-07).
        assertEquals(2, slotFeed.collections)

        update { enabled = true }

        assertEquals(3, slotFeed.collections)
        assertEquals(2, layerFeed.collections)
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        // D-04: the new slot's placeholder keeps its space from the first load.
        assertContentBottom("new slot loading", SLOT_TOP_DP)
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("new slot with an ad", SLOT_TOP_DP)
    }

    // The native slot gives way to the open collapsible layer (D-07, KB-35, plan T-090).

    private fun layerAdOf(headline: String = "Layer ad") =
        FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = headline))

    /** KB-35 criterion 1: the slot loads as soon as the screen shows, not after the layer (regression guard). */
    @Test
    fun entry_slotCollectsAtOnce_beforeLayerAd() {
        setBar()
        assertEquals(1, slotFeed.collections)
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals("the layer still loads", NativeAdState.Loading, layerFeed.state.value)
        assertEquals(1, layerFeed.collections)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
    }

    /** KB-35 criterion 2: the layer opens on its first ad over the loading slot. */
    @Test
    fun layerOpens_slotLoading_noPlaceholder_noCollect_reservationKept() {
        setBar()
        // Control: the slot loads with its placeholder.
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals(1, slotFeed.activeCollectors)

        update { layerFeed.load(layerAdOf()) }

        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
        assertEquals("no slot collection", 0, slotFeed.activeCollectors)
        assertEquals("the slot collection was cancelled", 1, slotFeed.cancellations)
        assertEquals(NATIVE_RESERVATION_DP.dp, reservations.last())
        assertEquals("slot box", 0f, slotHeight(), TOLERANCE)
    }

    /** KB-35 criterion 2: the layer opens late, over a slot with an ad. */
    @Test
    fun layerOpensLate_slotAdDestroyed_reservationKept() {
        setBar()
        val slotAd = FakeLoadedNativeAd()
        update { slotFeed.load(slotAd) }
        // Control: the slot shows its ad.
        assertPresent(SLOT_AD)

        update { layerFeed.load(layerAdOf()) }

        assertEquals(1, slotAd.destroyCount)
        assertAbsent(SLOT_AD)
        assertEquals(NATIVE_RESERVATION_DP.dp, reservations.last())
        assertContentBottom("space kept", SLOT_TOP_DP)
    }

    /** KB-35 criterion 2: the layer opens again (CL-03) over the slot that got a new ad while it was collapsed. */
    @Test
    fun reexpand_slotAdDestroyedAgain() {
        setBar()
        update { layerFeed.load(layerAdOf()) }
        collapseLayer()
        val newSlotAd = FakeLoadedNativeAd()
        update { slotFeed.load(newSlotAd) }
        // Control: collapsed, the slot shows its new ad.
        assertPresent(SLOT_AD)
        assertEquals(0, newSlotAd.destroyCount)

        composeRule.mainClock.advanceTimeBy(REEXPAND_MILLIS)
        composeRule.waitForIdle()

        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, newSlotAd.destroyCount)
        assertAbsent(SLOT_AD)
        assertEquals(NATIVE_RESERVATION_DP.dp, reservations.last())
        assertContentBottom("space kept", SLOT_TOP_DP)
    }

    /** KB-35 criterion 2: collapsed, then open again before the slot got its ad: the slot's load is dropped. */
    @Test
    fun collapseThenReexpandBeforeSlotAd_requestDropped() {
        setBar()
        update { layerFeed.load(layerAdOf()) }
        collapseLayer()
        // Control: collapsed, the slot collects again.
        assertEquals(2, slotFeed.collections)
        assertEquals(1, slotFeed.activeCollectors)

        composeRule.mainClock.advanceTimeBy(REEXPAND_MILLIS)
        composeRule.waitForIdle()
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertEquals(0, slotFeed.activeCollectors)

        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertAbsent(SLOT_AD)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
    }

    /** KB-35 criterion 3: collapsed, the slot shows its placeholder at once where it is, and loads anew. */
    @Test
    fun collapse_slotShowsPlaceholder_newCollection() {
        setBar()
        loadLayerOpen()
        val contentBefore = contentBottom()

        collapseLayer()

        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals("placeholder at the slot", SLOT_TOP_DP, bounds(NativeAdTestTags.PLACEHOLDER).top.value, TOLERANCE)
        assertEquals(2, slotFeed.collections)
        assertEquals(NATIVE_RESERVATION_DP.dp, reservations.last())
        assertContentBottom("space kept", contentBefore)
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertPresent(SLOT_AD)
    }

    /** KB-35 criterion 3: the open layer loses its ad with none to replace it: the slot comes back. */
    @Test
    fun layerLosesAd_slotBack() {
        setBar()
        update { layerFeed.load(layerAdOf()) }
        // Control: the slot gave way.
        assertAbsent(NativeAdTestTags.PLACEHOLDER)

        update { layerFeed.state.value = NativeAdState.Unavailable }

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals(2, slotFeed.collections)
    }

    /** D-07: a layer that gets no ad never opens; the slot goes on as usual. */
    @Test
    fun layerFails_slotRuns() {
        setBar()
        update { layerFeed.state.value = NativeAdState.Unavailable }
        val slotAd = FakeLoadedNativeAd()
        update { slotFeed.load(slotAd) }
        assertEquals(1, slotFeed.activeCollectors)
        assertPresent(SLOT_AD)
        assertEquals(0, slotAd.destroyCount)

        // Control: an ad for the layer opens it, and the slot gives way.
        update { layerFeed.load(layerAdOf()) }
        assertEquals(0, slotFeed.activeCollectors)
    }

    /** D-07: the open layer refreshes in place: the slot keeps giving way, nothing blinks. */
    @Test
    fun layerRefresh_slotStaysYielded() {
        setBar()
        update { layerFeed.load(layerAdOf("Layer ad A")) }
        // Control: the slot gave way.
        assertEquals(0, slotFeed.activeCollectors)

        update { layerFeed.load(layerAdOf("Layer ad B")) }

        assertEquals(1, slotFeed.collections)
        assertEquals(0, slotFeed.activeCollectors)
        assertAbsent(NativeAdTestTags.PLACEHOLDER)
    }

    /** D-07: `is_show_native_collap` switched off while the slot gives way: the layer goes, the slot comes back. */
    @Test
    fun collapOffWhileYielded_slotBack() {
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        update { layerFeed.load(layerAdOf()) }
        // Control: the slot gave way.
        assertAbsent(SLOT_AD)
        assertEquals(0, slotFeed.activeCollectors)

        update { flags = barFlags(nativeBottom = true, collap = false) }

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertEquals(2, slotFeed.collections)
        assertContentBottom("space kept", SLOT_TOP_DP)
    }

    /** D-01, D-07: only the native slot gives way, and only to a layer that shows its ad. */
    @Test
    fun yields_onlyNativeSlotWithLayerShowing() {
        listOf(BottomSlotKind.NATIVE, BottomSlotKind.BANNER, null).forEach { kind ->
            listOf(true, false).forEach { collapsible ->
                listOf(true, false).forEach { showing ->
                    val expected = kind == BottomSlotKind.NATIVE && collapsible && showing
                    assertEquals(
                        "kind=$kind collapsible=$collapsible showing=$showing",
                        expected,
                        bottomSlotYields(kind, collapsible, showing),
                    )
                }
            }
        }
    }

    /** The log lines of the slot giving way and coming back (ADR-0019). */
    @Test
    fun yieldLog_lines() {
        assertEquals(
            "Bottom bar sample_native_small: slot yields to the collapsible layer",
            bottomSlotYieldLog(placement, yielded = true),
        )
        assertEquals(
            "Bottom bar sample_native_small: slot back after the collapsible layer",
            bottomSlotYieldLog(placement, yielded = false),
        )
    }

    // No controller, state of the content, restoration.

    @Test
    fun withoutAdsController_drawsContentOnly_noPadding() {
        composeRule.setContent {
            AdsTestTheme {
                BottomAdBarScaffold(placement) { padding ->
                    Box(Modifier.fillMaxSize().padding(padding).testTag(CONTENT))
                }
            }
        }
        composeRule.waitForIdle()
        assertContentBottom("no padding without a controller", SCREEN_HEIGHT)
        assertAbsent(BottomAdBarTestTags.SLOT)
        assertAbsent(BottomAdBarTestTags.LAYER)
    }

    @Test
    fun barToggled_contentStateKept() {
        composeRule.setContent {
            Bar { padding ->
                var count by rememberSaveable { mutableIntStateOf(0) }
                DefaultContent(padding)
                Text(
                    text = "count $count",
                    modifier = Modifier.testTag(COUNTER).clickable { count++ },
                )
            }
        }
        composeRule.waitForIdle()
        repeat(3) { composeRule.onNodeWithTag(COUNTER).performClick() }
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("control: bar shown", SLOT_TOP_DP)

        update { enabled = false }
        assertContentBottom("control: bar gone", SCREEN_HEIGHT)
        assertCounter(3)

        update { enabled = true }
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("control: bar back", SLOT_TOP_DP)
        assertCounter(3)
    }

    private fun assertCounter(expected: Int) {
        val text = composeRule.onNodeWithTag(COUNTER).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString { it.text }
        assertEquals("count $expected", text)
    }

    @Test
    fun restore_reservationFollowsNewSlot() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Bar() }
        composeRule.waitForIdle()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("before restoring", SLOT_TOP_DP)

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        // The ad does not survive the rebuild: the new slot loads with its placeholder, which keeps the space (D-04).
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertAbsent(SLOT_AD)
        assertContentBottom("restored, slot loading again", SLOT_TOP_DP)

        // Control: the new slot's state, not the old ad, sets the space: a failure gives it back.
        update { slotFeed.state.value = NativeAdState.Unavailable }
        assertContentBottom("restored slot failed", SCREEN_HEIGHT)

        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("slot reports an ad again", SLOT_TOP_DP)
    }

    // Ad units of the layers (L-072, B-04).

    @Test
    fun layerUnitIdNull_noLoad_slotStays() {
        layerUnitIds = NoLayerUnits
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }

        assertEquals(0, layerFeed.collections)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertPresent(SLOT_AD)
        assertContentBottom("slot keeps its space", SLOT_TOP_DP)

        // D-07: a layer with no ad unit never opens, so the slot never gives way, even with an ad in its feed.
        update { layerFeed.load(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))) }
        assertPresent(SLOT_AD)
        assertEquals("the slot still collects", 1, slotFeed.activeCollectors)
    }

    @Test
    fun bannerUnitIdNull_noLoad_noReservation() {
        layerUnitIds = NoLayerUnits
        flags = barFlags(nativeBottom = false, collap = false)
        setBar()
        assertEquals(0, banners.createdCount)
        assertContentBottom("no banner unit", SCREEN_HEIGHT)

        // Control: with a unit, the same bar builds the banner and keeps its height once it has an ad.
        update { layerUnitIds = AllUnits }
        assertEquals(1, banners.createdCount)
        update { banners.last.emitLoaded() }
        assertContentBottom("banner with an ad", SCREEN_HEIGHT - BANNER_HEIGHT_DP)
    }

    // The real production unit source with fake unit strings (plan T-056, KB-17).

    @Test
    fun configuredSlotUnits_bannerBuiltAndKeepsHeight() {
        // Control: without the banner slot key, no banner is built and the content keeps the whole screen.
        layerUnitIds = ConfiguredAdUnitIds("poses_bottom=unit/1")
        flags = barFlags(nativeBottom = false, collap = false)
        setBar()
        assertEquals(0, banners.createdCount)
        assertContentBottom("no banner slot key", SCREEN_HEIGHT)

        update { layerUnitIds = ConfiguredAdUnitIds("poses_bottom=unit/1;banner=unit/b") }
        assertEquals(1, banners.createdCount)
        update { banners.last.emitLoaded() }
        assertContentBottom("banner with an ad", SCREEN_HEIGHT - BANNER_HEIGHT_DP)
    }

    @Test
    fun configuredSlotUnits_layerLoads() {
        // Control: without the collapsible slot key, the layer never loads and the slot keeps its ad.
        layerUnitIds = ConfiguredAdUnitIds("poses_bottom=unit/1")
        setBar()
        val slotAd = FakeLoadedNativeAd()
        update { slotFeed.load(slotAd) }
        assertEquals(0, layerFeed.collections)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertPresent(SLOT_AD)

        update { layerUnitIds = ConfiguredAdUnitIds("poses_bottom=unit/1;native_collap=unit/c") }
        assertEquals(1, layerFeed.collections)
        update { layerFeed.load(FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))) }
        // D-07: the layer opens, the slot gives way and its ad is destroyed.
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertAbsent(SLOT_AD)
        assertEquals(1, slotAd.destroyCount)
    }

    @Test
    fun scaffold_rejectsNonNativeSmallPlacement() {
        val error = runCatching {
            composeRule.setContent { BottomAdBarScaffold(AdPlacement.GALLERY_NATIVE_MEDIUM) {} }
            composeRule.waitForIdle()
        }.exceptionOrNull()
        assertTrue("expected IllegalArgumentException, got $error", error is IllegalArgumentException)
    }

    @Test
    fun layersChange_logged() {
        setBar()
        update { flags = barFlags(nativeBottom = true, collap = false) }
        update { enabled = false }
        assertEquals(
            listOf(
                BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true),
                BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = false),
                null,
            ),
            layerChanges,
        )
        assertEquals(
            "Bottom bar poses_bottom: slot=NATIVE collapsible=true",
            bottomBarLayersLog(placement, BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true)),
        )
        assertEquals("Bottom bar poses_bottom: off", bottomBarLayersLog(placement, null))
    }

    private fun tapAt(x: Dp, y: Dp) {
        val point = with(composeRule.density) { Offset(x.toPx(), y.toPx()) }
        composeRule.onRoot().performTouchInput { click(point) }
        composeRule.waitForIdle()
    }

    private companion object {
        const val CONTENT = "bar_content"
        const val COUNTER = "bar_counter"
        const val SLOT_AD = "bar_slot_ad"
        const val SCREEN_HEIGHT = 891f
        const val BANNER_HEIGHT_DP = 62

        /** The native slot (106 dp at font scale 1.0, D-02) and its 16 dp bottom margin. */
        const val NATIVE_RESERVATION_DP = 122

        /** The top of the native slot on the 411 x 891 dp frame: 891 - 16 - 106 dp. */
        const val SLOT_TOP_DP = 769f

        /** The retry of a banner after its first failure (B-02: n x 2 s), with room for the frames of the failure. */
        const val RETRY_AFTER_FIRST_FAILURE_MILLIS = 2_100L

        /** Past the 30 s after which the collapsed layer of the bar opens again (CL-03), with room for its frames. */
        const val REEXPAND_MILLIS = 30_100L
        const val TOLERANCE = 1f

        val DEFAULT_FLAGS: AdsGlobalFlags = runBlocking { DefaultAdsFlags(FakeRemoteSettings()).global.first() }

        fun barFlags(nativeBottom: Boolean, collap: Boolean): AdsGlobalFlags = DEFAULT_FLAGS.copy(
            showNativeBottom = nativeBottom,
            showNativeCollap = nativeBottom && collap,
            expandCollapAfterSeconds = 30,
        )

        /** Units for every layer. */
        val AllUnits = object : AdUnitIds {
            override fun of(placement: AdPlacement): String = "unit"

            override fun of(placement: AdPlacement, format: AdFormat): String = "unit"
        }

        /** No unit for the banner and the collapsible layer, as on production today (question 2 of the plan). */
        val NoLayerUnits = object : AdUnitIds {
            override fun of(placement: AdPlacement): String = "unit"

            override fun of(placement: AdPlacement, format: AdFormat): String? =
                if (format == placement.format) "unit" else null
        }
    }
}
