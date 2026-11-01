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
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
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
 */
internal class FakeNativeFeed {
    val state = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
    var collections = 0
        private set

    fun states(): Flow<NativeAdState> = flow {
        collections++
        state.value = NativeAdState.Loading
        try {
            emitAll(state)
        } finally {
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

    private val slotFeed = FakeNativeFeed()
    private val layerFeed = FakeNativeFeed()
    private val banners = FakeBannerAdViews()
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
        AppTheme(darkTheme = false) {
            val enabledFromFlow = enabledFlow?.let { remember(it) { it }.collectAsState(initial = false).value }
            val barEnabled = enabledFromFlow ?: enabled
            val nativeAllowed = enabledFromFlow?.let { nativeSlotAllowed(canRequest, it, "unit") } ?: slotAllowed
            val choice = rememberBottomAdBarLayers(canRequest, barEnabled, flags)
            BottomAdBarHost(
                layers = choice.layers,
                nativeSlot = { delay, onAvailability, modifier ->
                    SideEffect { recordedDelays += delay }
                    NativeAdSlotGate(
                        allowed = nativeAllowed,
                        states = remember { slotFeed.states() },
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = false,
                        onAdAvailabilityChange = onAvailability,
                        modifier = modifier,
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL, Modifier.testTag(SLOT_AD)) },
                    )
                },
                bannerSlot = { onAvailability, onHeight, modifier ->
                    SideEffect { onHeight(BANNER_HEIGHT_DP.dp) }
                    BannerSlotHost(
                        gateOpen = canRequest,
                        enabled = barEnabled,
                        unitId = layerUnitIds.of(placement, AdFormat.BANNER),
                        heightDp = BANNER_HEIGHT_DP,
                        refresh = BannerRefresh.BY_AD_UNIT,
                        visible = MutableStateFlow(true),
                        covered = MutableStateFlow(false),
                        mayRequestNow = { canRequest },
                        retryResets = MutableStateFlow(0),
                        logger = RecordingAppLogger(),
                        placementKey = placement.key,
                        newView = banners::newView,
                        modifier = modifier,
                        onAdAvailabilityChange = onAvailability,
                    )
                },
                layer = { modifier ->
                    CollapsibleNativeAdLayerHost(
                        states = remember { layerFeed.states() },
                        expandAfterSeconds = 30,
                        clock = { composeRule.mainClock.currentTime },
                        state = rememberCollapsibleAdState(),
                        onCollapsed = {},
                        onReexpanded = {},
                        modifier = modifier,
                        allowed = layerUnitIds.of(placement, AdFormat.NATIVE_COLLAPSIBLE) != null,
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

    private fun loadBoth(): Pair<FakeLoadedNativeAd, FakeLoadedNativeAd> {
        val slotAd = FakeLoadedNativeAd()
        val layerAd = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Layer ad"))
        update {
            slotFeed.load(slotAd)
            layerFeed.load(layerAd)
        }
        return slotAd to layerAd
    }

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
            bottomAdBarLayers(canRequestAds = false, enabled = true, flags = nativeOn, bannerHadAd = true),
        )
        // Control: with consent the same switches give the native slot and the layer.
        assertEquals(
            BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = true),
            bottomAdBarLayers(canRequestAds = true, enabled = true, flags = nativeOn, bannerHadAd = true),
        )
        assertNull(bottomAdBarLayers(canRequestAds = false, enabled = true, flags = nativeOn, bannerHadAd = false))
        assertNull(bottomAdBarLayers(canRequestAds = false, enabled = false, flags = nativeOn, bannerHadAd = true))
    }

    // Space kept by the content (D-04, AR-02).

    @Test
    fun reservation_nativeWithAd_is80PlusMargin() {
        assertEquals(96.dp, bottomBarReservation(BottomSlotKind.NATIVE, slotHasAd = true, bannerHeight = 62.dp))
        assertEquals(96.dp, BottomAdBarDefaults.nativeReservation)
    }

    @Test
    fun reservation_bannerWithAd_isBannerHeight() {
        assertEquals(62.dp, bottomBarReservation(BottomSlotKind.BANNER, slotHasAd = true, bannerHeight = 62.dp))
    }

    @Test
    fun reservation_noAd_isZero() {
        assertEquals(0.dp, bottomBarReservation(BottomSlotKind.NATIVE, slotHasAd = false, bannerHeight = 62.dp))
        assertEquals(0.dp, bottomBarReservation(BottomSlotKind.BANNER, slotHasAd = false, bannerHeight = 62.dp))
        // Control: the same slot with an ad keeps space (L-055).
        assertEquals(96.dp, bottomBarReservation(BottomSlotKind.NATIVE, slotHasAd = true, bannerHeight = 62.dp))
    }

    @Test
    fun reservation_notAllowed_isZero() {
        assertEquals(0.dp, bottomBarReservation(null, slotHasAd = true, bannerHeight = 62.dp))
    }

    @Test
    fun reservedSlot_equalsDrawnSlotHeight() {
        val reserved = bottomBarReservation(BottomSlotKind.NATIVE, slotHasAd = true, bannerHeight = 0.dp) -
            BottomAdBarDimens.SlotMargin
        assertEquals(NativeAdDimens.BottomSlotHeight, reserved)
        setBar()
        loadBoth()
        val slot = bounds(SLOT_AD)
        assertEquals(reserved.value, (slot.bottom - slot.top).value, TOLERANCE)
    }

    @Test
    fun contentBottom_meetsSlotTop_whenSlotHasAd() {
        flags = barFlags(nativeBottom = true, collap = false)
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        val slot = bounds(SLOT_AD)
        assertEquals("slot top", 795f, slot.top.value, TOLERANCE)
        assertEquals("slot bottom", 875f, slot.bottom.value, TOLERANCE)
        assertContentBottom("content ends at the slot top", slot.top.value)

        // Control: no ad, the content goes down to the bottom of the screen.
        update { slotFeed.state.value = NativeAdState.Unavailable }
        assertContentBottom("content with no ad", SCREEN_HEIGHT)
    }

    @Test
    fun slotLoading_reservesNothing() {
        setBar()
        // D-02: the 80 dp placeholder is shown; D-04: no space is kept for it.
        assertEquals(80f, bounds(NativeAdTestTags.PLACEHOLDER).let { (it.bottom - it.top).value }, TOLERANCE)
        assertContentBottom("content while loading", SCREEN_HEIGHT)

        // Control: the ad arrives and the space is kept.
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("content with an ad", 795f)
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
        assertEquals(listOf(0.dp, 96.dp), reservations)
    }

    @Test
    fun availabilityFalseAfterSlotNotAllowed_reservationZero() {
        setBar()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        // Control: the slot with its ad keeps its space.
        assertContentBottom("with an ad", 795f)

        update { slotAllowed = false }
        assertContentBottom("slot no longer allowed", SCREEN_HEIGHT)
    }

    // The collapsible layer (D-03, KB-4).

    @Test
    fun layer_takesOnlyWidth_touchAboveReachesContent() {
        setBar()
        loadBoth()
        val layer = bounds(CollapsibleNativeAdTestTags.AD)
        assertEquals("layer height", 250f, (layer.bottom - layer.top).value, TOLERANCE)
        assertEquals("layer top", 625f, layer.top.value, TOLERANCE)
        val slot = bounds(SLOT_AD)
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
        loadBoth()
        val slotBefore = bounds(SLOT_AD)
        val contentBefore = contentBottom()

        composeRule.onNodeWithTag(CollapsibleNativeAdTestTags.COLLAPSE, useUnmergedTree = true).performClick()
        composeRule.waitForIdle()

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        val slotAfter = bounds(SLOT_AD)
        assertEquals(slotBefore.top.value, slotAfter.top.value, TOLERANCE)
        assertContentBottom("space kept after collapsing", contentBefore)
    }

    // Switches changed while the bar shows (L-061).

    @Test
    fun collapSwitchedOffWhileShown_layerRemovedAndAdDestroyed_slotStays() {
        setBar()
        val (slotAd, layerAd) = loadBoth()
        val slotBefore = bounds(SLOT_AD)
        // Control: the layer is shown before the switch.
        assertPresent(CollapsibleNativeAdTestTags.AD)

        update { flags = barFlags(nativeBottom = true, collap = false) }

        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, layerAd.destroyCount)
        assertEquals(0, slotAd.destroyCount)
        assertEquals(slotBefore.top.value, bounds(SLOT_AD).top.value, TOLERANCE)
    }

    @Test
    fun nativeBottomSwitchedOff_nativeAdDestroyed_bannerSlotShown() {
        setBar()
        val (slotAd, layerAd) = loadBoth()
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
        assertContentBottom("control: bar with an ad", 795f)

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
        assertContentBottom("control: bar with an ad", 795f)

        update { adFree.value = true }

        assertAbsent(SLOT_AD)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertEquals(1, slotAd.destroyCount)
        assertEquals(1, layerAd.destroyCount)
        assertContentBottom("bar gone", SCREEN_HEIGHT)

        // Control (L-055): the signal back off, the bar comes back; then the old path, is_show_<code> switched off
        // with the same build, also takes it away.
        update { adFree.value = false }
        val (againSlotAd, _) = loadBoth()
        assertContentBottom("control: bar back", 795f)
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
        assertContentBottom("control: bar with an ad", 795f)

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
        assertEquals(1, slotFeed.collections)

        update { enabled = true }

        assertEquals(2, slotFeed.collections)
        assertEquals(2, layerFeed.collections)
        assertPresent(NativeAdTestTags.PLACEHOLDER)
        assertContentBottom("new slot loading", SCREEN_HEIGHT)
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("new slot with an ad", 795f)
    }

    // No controller, state of the content, restoration.

    @Test
    fun withoutAdsController_drawsContentOnly_noPadding() {
        composeRule.setContent {
            AppTheme {
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
        assertContentBottom("control: bar shown", 795f)

        update { enabled = false }
        assertContentBottom("control: bar gone", SCREEN_HEIGHT)
        assertCounter(3)

        update { enabled = true }
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("control: bar back", 795f)
        assertCounter(3)
    }

    private fun assertCounter(expected: Int) {
        val text = composeRule.onNodeWithTag(COUNTER).fetchSemanticsNode()
            .config[SemanticsProperties.Text].joinToString { it.text }
        assertEquals("count $expected", text)
    }

    @Test
    fun restore_reservationZeroUntilAvailableAgain() {
        val restoration = StateRestorationTester(composeRule)
        restoration.setContent { Bar() }
        composeRule.waitForIdle()
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("before restoring", 795f)

        restoration.emulateSavedInstanceStateRestore()
        composeRule.waitForIdle()
        assertContentBottom("restored, slot loading again", SCREEN_HEIGHT)

        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertContentBottom("slot reports an ad again", 795f)
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
        assertContentBottom("slot keeps its space", 795f)
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
        update { slotFeed.load(FakeLoadedNativeAd()) }
        assertEquals(0, layerFeed.collections)
        assertAbsent(CollapsibleNativeAdTestTags.AD)
        assertPresent(SLOT_AD)

        update { layerUnitIds = ConfiguredAdUnitIds("poses_bottom=unit/1;native_collap=unit/c") }
        assertEquals(1, layerFeed.collections)
        loadBoth()
        assertPresent(CollapsibleNativeAdTestTags.AD)
        assertPresent(SLOT_AD)
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
