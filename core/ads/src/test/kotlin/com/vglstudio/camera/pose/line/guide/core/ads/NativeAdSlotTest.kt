package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.NativeAdView
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The stateful native slot: gate, what each loading state draws, availability reported to the app, and destroying
 * the ad (plans T-010, T-027).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdSlotTest {
    @get:Rule
    val composeRule = createComposeRule()

    private fun setSlotContent(state: NativeAdState, size: NativeAdSize, keepSpaceWhenEmpty: Boolean = false) {
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = state,
                    size = size,
                    keepSpaceWhenEmpty = keepSpaceWhenEmpty,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, size) },
                )
            }
        }
    }

    @Test
    fun loading_showsPlaceholder() {
        setSlotContent(NativeAdState.Loading, NativeAdSize.SMALL)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun unavailable_takesNoSpace() {
        var state by mutableStateOf<NativeAdState>(NativeAdState.Loading)
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = state,
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                )
            }
        }
        // Control: the same slot takes 80 dp while loading, so 0 dp below is the slot giving its space back.
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(80.dp)

        state = NativeAdState.Unavailable
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun unavailable_keepsSpace_whenDeclaredNextToButton() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = NativeAdState.Unavailable,
                    size = size,
                    keepSpaceWhenEmpty = true,
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, size) },
                )
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(80.dp)
        composeRule.onAllNodes(hasAnyText, useUnmergedTree = true).assertCountEquals(0)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(410.dp)
        composeRule.onAllNodes(hasAnyText, useUnmergedTree = true).assertCountEquals(0)
    }

    @Test
    fun buttonDoesNotMove_whenAdBecomesUnavailable_withKeptSpace() {
        var state by mutableStateOf<NativeAdState>(NativeAdState.Loading)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdSlotContent(
                        state = state,
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = true,
                        modifier = Modifier.width(CARD_WIDTH),
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                    )
                    Text("App button", modifier = Modifier.testTag(BUTTON))
                }
            }
        }
        val before = composeRule.onNodeWithTag(BUTTON).getUnclippedBoundsInRoot()
        // Control: the button sits below the 80 dp placeholder, so the comparison below is not 0 against 0.
        assertEquals(80f, before.top.value, 0.5f)

        state = NativeAdState.Unavailable
        composeRule.waitForIdle()

        composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER).assertCountEquals(0)
        val after = composeRule.onNodeWithTag(BUTTON).getUnclippedBoundsInRoot()
        assertEquals(before.top.value, after.top.value, 0.5f)
    }

    @Test
    fun withoutAdsController_takesNoSpace() {
        composeRule.setContent {
            AppTheme {
                NativeAdSlot(AdPlacement.SAMPLE_NATIVE_SMALL, Modifier.width(CARD_WIDTH).testTag(SLOT))
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
    }

    @Test
    fun nonNativeFormat_isRejected() {
        assertThrows(IllegalArgumentException::class.java) { requireNativeFormat(AdFormat.BANNER) }
        assertThrows(IllegalArgumentException::class.java) { requireNativeFormat(AdFormat.INTERSTITIAL) }
    }

    @Test
    fun nativeSize_followsPlacementFormat() {
        assertEquals(NativeAdSize.SMALL, requireNativeFormat(AdFormat.NATIVE_SMALL))
        assertEquals(NativeAdSize.MEDIUM, requireNativeFormat(AdFormat.NATIVE_MEDIUM))
        assertEquals(NativeAdSize.SMALL, requireNativeFormat(AdPlacement.SAMPLE_NATIVE_SMALL.format))
    }

    @Test
    fun nativeLoadSize_mapsFormats() {
        assertEquals(null, AdFormat.BANNER.nativeLoadSize())
        assertEquals(null, AdFormat.INTERSTITIAL.nativeLoadSize())
        assertEquals(NativeAdSize.SMALL, AdFormat.NATIVE_SMALL.nativeLoadSize())
        assertEquals(NativeAdSize.MEDIUM, AdFormat.NATIVE_MEDIUM.nativeLoadSize())
        // The pool of a flow step loads its ads at the size of the 250 dp card (plan T-032).
        assertEquals(NativeAdSize.STEP, AdFormat.NATIVE_STEP.nativeLoadSize())
    }

    @Test
    fun inCollection_loadsAsCard() {
        assertEquals(NativeAdSize.CARD, AdFormat.NATIVE_IN_COLLECTION.nativeLoadSize())
        // Control: medium keeps its own size, so CARD above is not the size of every pooled format.
        assertEquals(NativeAdSize.MEDIUM, AdFormat.NATIVE_MEDIUM.nativeLoadSize())
    }

    @Test
    fun leavingComposition_destroysAd() {
        val loader = FakeNativeAdLoader()
        val ad = FakeLoadedNativeAd()
        val states = nativeAdStates(loader, "unit", NativeAdSize.SMALL, 10_000, RecordingAppLogger(), "key")
        var shown by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                if (shown) {
                    NativeAdSlotHost(
                        states = states,
                        size = NativeAdSize.SMALL,
                        keepSpaceWhenEmpty = false,
                        modifier = Modifier.width(CARD_WIDTH),
                        adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.runOnIdle { loader.deliver(ad) }
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        assertEquals(0, ad.destroyCount)

        shown = false
        composeRule.waitForIdle()

        assertEquals(1, ad.destroyCount)
    }

    @Test
    fun swappedAd_recreatesBoundView() {
        val ad1 = FakeLoadedNativeAd()
        val ad2 = FakeLoadedNativeAd(sampleNativeAdAssets.copy(headline = "Second ad"))
        var state by mutableStateOf<NativeAdState>(NativeAdState.Loaded(ad1))
        val binds = mutableListOf<Pair<LoadedNativeAd, NativeAdView>>()
        var frames = 0
        composeRule.setContent {
            AppTheme {
                NativeAdSlotContent(
                    state = state,
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    modifier = Modifier.width(CARD_WIDTH),
                ) { ad ->
                    remember { frames++ }
                    NativeAdViewHost(bind = { binds += ad to it }) { binder ->
                        NativeAdLayout(ad.assets, NativeAdSize.SMALL, binder, showAdChoicesMarker = false)
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(1, frames)
        assertEquals(listOf<LoadedNativeAd>(ad1), binds.map { it.first }.distinct())

        // Control (L-055): the same ad emitted again builds no new frame.
        state = NativeAdState.Loaded(ad1)
        composeRule.waitForIdle()
        assertEquals(1, frames)

        state = NativeAdState.Loaded(ad2)
        composeRule.waitForIdle()

        assertEquals("no new NativeAdView for the new ad", 2, frames)
        assertEquals(1, binds.count { it.first === ad2 })
        assertEquals(2, binds.map { it.second }.distinct().size)
        composeRule.onNodeWithText("Second ad", useUnmergedTree = true).assertExists()
    }

    @Test
    fun boundNativeAd_withFakeAd_drawsCard() {
        composeRule.setContent {
            AppTheme {
                BoundNativeAd(FakeLoadedNativeAd(sampleNativeAdAssets), NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
            }
        }
        composeRule.onNodeWithText("Headline of the ad", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(80.dp)
    }

    @Test
    fun bottomSlotHeightConstant_equalsDrawnCardAndPlaceholderHeight() {
        // One constant for the drawn bottom slot and the space kept for it (D-02, D-04, AR-02).
        assertEquals(80f, NativeAdDimens.BottomSlotHeight.value, 0.5f)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeAdCard(sampleNativeAdAssets, NativeAdSize.SMALL, Modifier.width(CARD_WIDTH))
                    NativeAdPlaceholder(NativeAdSize.SMALL, Modifier.width(CARD_WIDTH), animated = false)
                }
            }
        }
        val card = composeRule.onNodeWithTag(NativeAdTestTags.CARD).getUnclippedBoundsInRoot()
        val placeholder = composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).getUnclippedBoundsInRoot()
        assertEquals(NativeAdDimens.BottomSlotHeight.value, (card.bottom - card.top).value, 0.5f)
        assertEquals(NativeAdDimens.BottomSlotHeight.value, (placeholder.bottom - placeholder.top).value, 0.5f)
    }

    @Test
    fun slotAllowed_falseWithoutConsent() {
        // Control: with every condition given the slot is allowed, so false below is the missing consent.
        assertTrue(nativeSlotAllowed(canRequestAds = true, enabled = true, unitId = "unit"))
        assertFalse(nativeSlotAllowed(canRequestAds = false, enabled = true, unitId = "unit"))
    }

    @Test
    fun slotAllowed_falseWhenPlacementOff() {
        assertTrue(nativeSlotAllowed(canRequestAds = true, enabled = true, unitId = "unit"))
        assertFalse(nativeSlotAllowed(canRequestAds = true, enabled = false, unitId = "unit"))
    }

    @Test
    fun slotAllowed_falseWithoutUnitId() {
        assertTrue(nativeSlotAllowed(canRequestAds = true, enabled = true, unitId = "unit"))
        assertFalse(nativeSlotAllowed(canRequestAds = true, enabled = true, unitId = null))
    }

    @Test
    fun slotAllowed_trueWhenAllGiven() {
        assertTrue(nativeSlotAllowed(canRequestAds = true, enabled = true, unitId = "unit"))
        assertFalse(nativeSlotAllowed(canRequestAds = false, enabled = false, unitId = null))
    }

    private fun setGate(
        states: MutableStateFlow<NativeAdState>,
        reports: MutableList<Boolean>,
        allowed: () -> Boolean = { true },
    ) {
        composeRule.setContent {
            AppTheme {
                NativeAdSlotGate(
                    allowed = allowed(),
                    states = states,
                    size = NativeAdSize.SMALL,
                    keepSpaceWhenEmpty = false,
                    onAdAvailabilityChange = { reports += it },
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun availability_reportsTrueOnLoadedFalseOnUnavailable_notOnLoading() {
        val states = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
        val reports = mutableListOf<Boolean>()
        setGate(states, reports)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertExists()
        assertEquals(emptyList<Boolean>(), reports)

        states.value = NativeAdState.Loaded(FakeLoadedNativeAd())
        composeRule.waitForIdle()
        assertEquals(listOf(true), reports)

        states.value = NativeAdState.Unavailable
        composeRule.waitForIdle()
        assertEquals(listOf(true, false), reports)
    }

    @Test
    fun availability_falseThenTrue_whenAdComesAfterFailure() {
        val states = MutableStateFlow<NativeAdState>(NativeAdState.Loading)
        val reports = mutableListOf<Boolean>()
        setGate(states, reports)

        states.value = NativeAdState.Unavailable
        composeRule.waitForIdle()
        assertEquals(listOf(false), reports)

        states.value = NativeAdState.Loaded(FakeLoadedNativeAd())
        composeRule.waitForIdle()
        assertEquals(listOf(false, true), reports)
    }

    @Test
    fun availability_reportsFalse_whenPlacementTurnsOffAfterLoaded() {
        val states = MutableStateFlow<NativeAdState>(NativeAdState.Loaded(FakeLoadedNativeAd()))
        val reports = mutableListOf<Boolean>()
        var allowed by mutableStateOf(true)
        setGate(states, reports) { allowed }
        // Control: the loaded ad was reported once, before the placement turns off.
        assertEquals(listOf(true), reports)

        allowed = false
        composeRule.waitForIdle()
        assertEquals(listOf(true, false), reports)
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
    }

    // The native slot of the bottom bar (plan T-030): same gate as NativeAdSlot, first refresh after the layer.

    private fun setBottomSlot(feed: FakeNativeFeed, reports: MutableList<Boolean>) {
        composeRule.setContent {
            AppTheme {
                BottomNativeSlotHost(
                    allowed = true,
                    newStates = { feed.states() },
                    firstRefreshDelayMillis = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
                    onAdAvailabilityChange = { reports += it },
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNativeSlot_loading_showsPlaceholder80dp() {
        val reports = mutableListOf<Boolean>()
        setBottomSlot(FakeNativeFeed(), reports)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(80.dp)
        assertEquals("nothing reported while loading", emptyList<Boolean>(), reports)
    }

    @Test
    fun bottomNativeSlot_unavailable_takesNoSpace_reportsFalse() {
        val feed = FakeNativeFeed()
        val reports = mutableListOf<Boolean>()
        setBottomSlot(feed, reports)
        // Control: the slot takes 80 dp while loading.
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(80.dp)

        composeRule.runOnIdle { feed.state.value = NativeAdState.Unavailable }
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
        assertEquals(listOf(false), reports)
    }

    /** Builds the bar with a native slot that records the first refresh delay it asks its stream for. */
    private fun setBarRecordingDelay(collapsible: () -> Boolean, feed: FakeNativeFeed, asked: MutableList<Long>) {
        composeRule.setContent {
            AppTheme {
                BottomAdBarHost(
                    layers = BottomAdBarLayers(BottomSlotKind.NATIVE, collapsible = collapsible()),
                    nativeSlot = { delay, onAvailability, modifier ->
                        BottomNativeSlotHost(
                            allowed = true,
                            newStates = { first ->
                                asked += first
                                feed.states()
                            },
                            firstRefreshDelayMillis = delay,
                            onAdAvailabilityChange = onAvailability,
                            modifier = modifier,
                            adContent = { NativeAdCard(it.assets, NativeAdSize.SMALL) },
                        )
                    },
                    bannerSlot = { _, _, _ -> },
                    layer = {},
                    content = {},
                )
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun bottomNativeSlot_withLayer_firstRefreshAt60500ms() {
        val asked = mutableListOf<Long>()
        val feed = FakeNativeFeed()
        var collapsible by mutableStateOf(true)
        setBarRecordingDelay({ collapsible }, feed, asked)
        assertEquals(listOf(60_500L), asked)

        // Turning the layer off does not build the stream again (the slot keeps its ad, nothing loads again).
        collapsible = false
        composeRule.waitForIdle()
        assertEquals(listOf(60_500L), asked)
        assertEquals(1, feed.collections)
    }

    @Test
    fun bottomNativeSlot_withoutLayer_firstRefreshAt60000ms() {
        val asked = mutableListOf<Long>()
        setBarRecordingDelay({ false }, FakeNativeFeed(), asked)
        assertEquals(listOf(60_000L), asked)
    }

    private companion object {
        val CARD_WIDTH = 379.dp
        const val SLOT = "slot"
        const val BUTTON = "button"
        val hasAnyText = SemanticsMatcher("has text") { SemanticsProperties.Text in it.config }
    }
}
