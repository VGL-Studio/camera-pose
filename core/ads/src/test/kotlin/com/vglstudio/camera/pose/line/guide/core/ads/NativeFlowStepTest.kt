package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.Snapshot
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
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
 * Native ads of a flow step (plan T-032, KB-20): the gate of the 250 dp card and of the full page, no space with no
 * ad, availability reported, the block of resume ads while the full page shows (G-03), the Next button of the app
 * never covered and never moved, the switch of the placement given, and L-061. Flows that would retry forever are
 * fixed fakes, or run with the clock stopped (L-048).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeFlowStepTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val logger = RecordingAppLogger()
    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, logger)

    /** A pool for one test, seen through a recorder of releases. */
    private class RecordingPool(private val pool: PooledNativeAds) : PooledNativeAds {
        val released = mutableListOf<LoadedNativeAd>()

        override fun claim(holder: AdPlacement): LoadedNativeAd? = pool.claim(holder)

        override fun release(ad: LoadedNativeAd) {
            released += ad
            pool.release(ad)
        }

        override val exhausted: Boolean get() = pool.exhausted
    }

    private fun realStepStates(pool: PooledNativeAds): (Flow<Any>) -> Flow<NativeAdState> = { steps ->
        stepAdStates(pool, AdPlacement.GALLERY_NATIVE_MEDIUM, steps, logger, "native_onboarding")
    }

    /** A flow that counts its collections and holds one loaded ad until cancelled. */
    private class CountingStates(private val ad: LoadedNativeAd) {
        var collections = 0
        val flow: Flow<NativeAdState> = flow {
            collections++
            emit(NativeAdState.Loaded(ad))
            awaitCancellation()
        }
    }

    private fun bounds(tag: String) = composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun assertNoNode(tag: String) {
        assertTrue(
            "$tag should not be drawn",
            composeRule.onAllNodesWithTag(tag, useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
    }

    private fun overlaps(a: DpRect, b: DpRect) =
        a.left < b.right && a.right > b.left && a.top < b.bottom && a.bottom > b.top

    // Formats.

    @Test
    fun requireStepFormat_rejectsOtherFormats() {
        AdFormat.entries.filter { it != AdFormat.NATIVE_STEP }.forEach { format ->
            assertThrows("$format", IllegalArgumentException::class.java) { requireStepFormat(format) }
        }
        AdFormat.entries.filter { it != AdFormat.NATIVE_FULL_SCREEN }.forEach { format ->
            assertThrows("$format", IllegalArgumentException::class.java) { requireFullScreenStepFormat(format) }
        }
        // Control: the format of each one passes.
        requireStepFormat(AdFormat.NATIVE_STEP)
        requireFullScreenStepFormat(AdFormat.NATIVE_FULL_SCREEN)
    }

    @Test
    fun fullScreenStep_withoutAdsController_drawsNothing_notReady() {
        var ready = true
        composeRule.setContent {
            AppTheme {
                NativeFullScreenStep(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, Modifier.size(200.dp).testTag(SLOT))
                ready = rememberNativeFullScreenStepReady(AdPlacement.GALLERY_NATIVE_FULL_SCREEN)
            }
        }
        composeRule.onNodeWithTag(SLOT).assertExists()
        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertFalse(ready)
    }

    // The gate of the card.

    @Test
    fun stepAd_drawsOnlyWhenAllowed() {
        val states = CountingStates(FakeLoadedNativeAd())
        var allowed by mutableStateOf(false)
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeStepAdHost(
                        allowed = allowed,
                        step = 1,
                        newStates = { states.flow },
                        modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    )
                    Text("below", Modifier.testTag(BELOW))
                }
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
        assertEquals("nothing collected while not allowed", 0, states.collections)
        assertNoNode(NativeAdTestTags.CARD)

        allowed = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertHeightIsEqualTo(250.dp)
        assertEquals(1, states.collections)
    }

    @Test
    fun stepAd_stepChange_showsAnotherAdOfThePool() {
        val pool = RecordingPool(immediatePool(numberedAds(3), logger = logger).also { it.refill() })
        var step by mutableStateOf(0)
        composeRule.setContent {
            AppTheme {
                NativeStepAdHost(
                    allowed = true,
                    step = step,
                    newStates = realStepStates(pool),
                    modifier = Modifier.width(CARD_WIDTH),
                )
            }
        }
        composeRule.onNodeWithText("Ad 1", useUnmergedTree = true).assertExists()

        step = 1
        composeRule.waitForIdle()

        composeRule.onNodeWithText("Ad 2", useUnmergedTree = true).assertExists()
        assertTrue(
            "the ad of the step before is gone",
            composeRule.onAllNodesWithText("Ad 1", useUnmergedTree = true).fetchSemanticsNodes().isEmpty(),
        )
        assertEquals(listOf("Ad 1"), pool.released.map { it.assets.headline })
    }

    @Test
    fun stepAd_unavailable_takesNoSpace() {
        composeRule.setContent {
            AppTheme {
                Column {
                    NativeStepAdHost(
                        allowed = true,
                        step = 1,
                        newStates = { flowOf(NativeAdState.Unavailable) },
                        modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    )
                    Text("below", Modifier.testTag(BELOW))
                }
            }
        }
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
        assertEquals(0f, bounds(BELOW).top.value, 0.5f)
        assertNoNode(NativeAdTestTags.PLACEHOLDER)
    }

    @Test
    fun stepAd_loading_keeps250dp() {
        // Control of the test above: while loading, the placeholder keeps the 250 dp of the card.
        composeRule.mainClock.autoAdvance = false
        composeRule.setContent {
            AppTheme {
                NativeStepAdHost(
                    allowed = true,
                    step = 1,
                    newStates = { MutableStateFlow(NativeAdState.Loading) },
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                )
            }
        }
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
        composeRule.onNodeWithTag(NativeAdTestTags.PLACEHOLDER).assertHeightIsEqualTo(250.dp)
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(250.dp)
    }

    @Test
    fun stepAd_allowedTurnsFalse_hidesAndReportsFalse() {
        val pool = RecordingPool(immediatePool(numberedAds(3), logger = logger).also { it.refill() })
        val reports = mutableListOf<Boolean>()
        var allowed by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                NativeStepAdHost(
                    allowed = allowed,
                    step = 1,
                    newStates = realStepStates(pool),
                    modifier = Modifier.width(CARD_WIDTH).testTag(SLOT),
                    onAdAvailabilityChange = { reports += it },
                )
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertExists()
        assertEquals(listOf(true), reports)
        assertTrue(pool.released.isEmpty())

        allowed = false
        composeRule.waitForIdle()

        assertNoNode(NativeAdTestTags.CARD)
        composeRule.onNodeWithTag(SLOT).assertHeightIsEqualTo(0.dp)
        assertEquals(listOf(true, false), reports)
        assertEquals("the ad goes back to the pool", listOf("Ad 1"), pool.released.map { it.assets.headline })
    }

    @Test
    fun stepCard_doesNotSuppress() {
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    NativeStepAdHost(
                        allowed = true,
                        step = 1,
                        newStates = { flowOf(NativeAdState.Loaded(FakeLoadedNativeAd())) },
                        modifier = Modifier.width(CARD_WIDTH),
                    )
                }
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD).assertExists()
        assertFalse("the card does not cover the screen", suppressor.suppressed.value)
    }

    // The full page.

    private fun fullPageStates(
        ad: LoadedNativeAd?,
        takes: MutableList<LoadedNativeAd?> = mutableListOf(),
        released: MutableList<LoadedNativeAd> = mutableListOf(),
    ): Flow<NativeAdState> = fullScreenStepStates(
        take = { ad.also { takes += it } },
        ensureLoading = {},
        release = { released += it },
    )

    @Test
    fun fullScreenStep_drawsOnlyWhenAllowed() {
        val takes = mutableListOf<LoadedNativeAd?>()
        var allowed by mutableStateOf(false)
        composeRule.setContent {
            AppTheme {
                NativeFullScreenStepGate(
                    allowed = allowed,
                    states = fullPageStates(FakeLoadedNativeAd(FakeNativeAdAssets.Design), takes),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertEquals("nothing taken while not allowed", 0, takes.size)

        allowed = true
        composeRule.waitForIdle()

        composeRule.onNodeWithTag(NativeFullScreenAdTestTags.CTA).assertExists()
        composeRule.onNodeWithText(FakeNativeAdAssets.Design.headline).assertExists()
        assertEquals(1, takes.size)
    }

    @Test
    fun fullScreenStep_hasNoControls() {
        composeRule.setContent {
            AppTheme {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = fullPageStates(FakeLoadedNativeAd(FakeNativeAdAssets.Design)),
                    modifier = Modifier.fillMaxSize(),
                )
            }
        }
        composeRule.onNodeWithTag(NativeFullScreenAdTestTags.CTA).assertExists()
        assertNoNode(NativeFullScreenAdTestTags.CLOSE)
        assertNoNode(NativeFullScreenAdTestTags.COUNTDOWN)
    }

    @Test
    fun fullScreenStep_unavailable_drawsNothing() {
        val reports = mutableListOf<Boolean>()
        composeRule.setContent {
            AppTheme {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = flowOf(NativeAdState.Unavailable),
                    modifier = Modifier.fillMaxSize(),
                    onAdAvailabilityChange = { reports += it },
                )
            }
        }
        listOf(
            NativeFullScreenAdTestTags.CTA,
            NativeFullScreenAdTestTags.MEDIA,
            NativeFullScreenAdTestTags.ICON,
            NativeFullScreenAdTestTags.PLACEHOLDER,
        ).forEach(::assertNoNode)
        assertEquals(listOf(false), reports)
    }

    @Test
    fun fullScreenStep_reportsTrueOnAd() {
        val reports = mutableListOf<Boolean>()
        composeRule.setContent {
            AppTheme {
                NativeFullScreenStepGate(
                    allowed = true,
                    states = fullPageStates(FakeLoadedNativeAd(FakeNativeAdAssets.Design)),
                    modifier = Modifier.fillMaxSize(),
                    onAdAvailabilityChange = { reports += it },
                )
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf(true), reports)
    }

    @Test
    fun fullPage_suppressesResumeAds_whileShown() {
        val states =
            MutableStateFlow<NativeAdState>(NativeAdState.Loaded(FakeLoadedNativeAd(FakeNativeAdAssets.Design)))
        var present by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    if (present) {
                        NativeFullScreenStepGate(
                            allowed = true,
                            states = states,
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue("held while the page shows its ad", suppressor.suppressed.value)

        // Control: a page with no ad does not hold.
        states.value = NativeAdState.Unavailable
        composeRule.waitForIdle()
        assertFalse(suppressor.suppressed.value)

        states.value = NativeAdState.Loaded(FakeLoadedNativeAd(FakeNativeAdAssets.Design))
        composeRule.waitForIdle()
        assertTrue(suppressor.suppressed.value)

        present = false
        composeRule.waitForIdle()
        assertFalse("released when the page leaves", suppressor.suppressed.value)
    }

    @Test
    fun fullPage_notShown_neitherTakesNorSuppresses() {
        val takes = mutableListOf<LoadedNativeAd?>()
        var shown by mutableStateOf(false)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    NativeFullScreenStepGate(
                        allowed = true,
                        states = fullPageStates(FakeLoadedNativeAd(FakeNativeAdAssets.Design), takes),
                        modifier = Modifier.fillMaxSize(),
                        shown = shown,
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals("a page built next to the one on screen takes nothing", 0, takes.size)
        assertFalse(suppressor.suppressed.value)

        // Control: the same page once it is the page on screen.
        shown = true
        composeRule.waitForIdle()
        assertEquals(1, takes.size)
        assertTrue(suppressor.suppressed.value)
    }

    @Test
    fun fullScreenStep_allowedTurnsFalse_releasesAndUnsuppresses() {
        val ad = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        val released = mutableListOf<LoadedNativeAd>()
        val reports = mutableListOf<Boolean>()
        var allowed by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    NativeFullScreenStepGate(
                        allowed = allowed,
                        states = fullPageStates(ad, released = released),
                        modifier = Modifier.fillMaxSize(),
                        onAdAvailabilityChange = { reports += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf(true), reports)
        assertTrue(suppressor.suppressed.value)

        allowed = false
        composeRule.waitForIdle()

        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertEquals(listOf(true, false), reports)
        assertEquals(listOf<LoadedNativeAd>(ad), released)
        assertFalse(suppressor.suppressed.value)
    }

    // The height of the page (plan T-041).

    @Test
    fun fullScreenStep_belowMinHeight_takesNothing() {
        val ad = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        val takes = mutableListOf<LoadedNativeAd?>()
        var height by mutableStateOf(151.dp)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    Box(Modifier.fillMaxWidth().height(height)) {
                        NativeFullScreenStepGate(
                            allowed = true,
                            states = remember(height) { fullPageStates(ad, takes) },
                            modifier = Modifier.fillMaxSize(),
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertTrue("nothing taken in 151 dp: $takes", takes.isEmpty())
        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertNoNode(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER)
        assertFalse(suppressor.suppressed.value)

        // Control (L-055): the least height takes the ad and draws it.
        height = 152.dp
        composeRule.waitForIdle()
        assertEquals(1, takes.size)
        composeRule.onNodeWithTag(NativeFullScreenAdTestTags.CTA).assertExists()
    }

    @Test
    fun fullScreenStep_shrinksBelowMinHeight_releasesAndReportsFalse() {
        val ad = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        val released = mutableListOf<LoadedNativeAd>()
        val reports = mutableListOf<Boolean>()
        var height by mutableStateOf(484.dp)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    Box(Modifier.fillMaxWidth().height(height)) {
                        NativeFullScreenStepGate(
                            allowed = true,
                            states = remember { fullPageStates(ad, released = released) },
                            modifier = Modifier.fillMaxSize(),
                            onAdAvailabilityChange = { reports += it },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        assertEquals(listOf(true), reports)
        assertTrue(suppressor.suppressed.value)

        // Control: shrinking to the least height keeps the ad.
        height = 152.dp
        composeRule.waitForIdle()
        assertTrue("kept at 152 dp: $released", released.isEmpty())
        assertEquals(listOf(true), reports)

        height = 151.dp
        composeRule.waitForIdle()

        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertEquals(listOf<LoadedNativeAd>(ad), released)
        assertEquals(listOf(true, false), reports)
        assertFalse(suppressor.suppressed.value)
    }

    /**
     * KB-20 (plan T-049): the page area of a phone in landscape with its system bars and camera cutout (186.5 dp at
     * 440 dpi, FB-20261028-01) takes the ad and draws it.
     */
    @Test
    @Config(qualifiers = "w873dp-h393dp-440dpi")
    fun fullScreenStep_phoneLandscape_takesAd() {
        val ad = FakeLoadedNativeAd(FakeNativeAdAssets.Design)
        val takes = mutableListOf<LoadedNativeAd?>()
        val reports = mutableListOf<Boolean>()
        var height by mutableStateOf(151.dp)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    Box(Modifier.fillMaxWidth().height(height)) {
                        NativeFullScreenStepGate(
                            allowed = true,
                            states = remember(height) { fullPageStates(ad, takes) },
                            modifier = Modifier.fillMaxSize(),
                            onAdAvailabilityChange = { reports += it },
                        )
                    }
                }
            }
        }
        composeRule.waitForIdle()
        // Control (L-055): just below the least height nothing is taken and nothing drawn.
        assertTrue("nothing taken in 151 dp: $takes", takes.isEmpty())
        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertNoNode(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER)

        height = 186.5.dp
        composeRule.waitForIdle()
        assertEquals("one ad taken in 186.5 dp: $takes", 1, takes.size)
        composeRule.onNodeWithTag(NativeFullScreenAdTestTags.CTA).assertExists()
        assertNoNode(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER)
        assertEquals(listOf(true), reports)
        assertTrue(suppressor.suppressed.value)
    }

    @Test
    fun fullScreenStep_waiting_drawsStaticPlaceholder_noSuppress() {
        var allowed by mutableStateOf(true)
        composeRule.setContent {
            AppTheme {
                AdsHostContent(suppression, suppressor) {
                    NativeFullScreenStepGate(
                        allowed = allowed,
                        states = remember { fullPageStates(null) },
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER, useUnmergedTree = true).assertExists()
        assertNoNode(NativeFullScreenAdTestTags.CTA)
        assertFalse("the placeholder does not block resume ads", suppressor.suppressed.value)

        // Control: not allowed (placement off, consent closed), no placeholder.
        allowed = false
        composeRule.waitForIdle()
        assertNoNode(NativeFlowStepTestTags.FULL_PAGE_PLACEHOLDER)
    }

    // The Next button of the app.

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun nextButton_samePlace_notCovered_withFullPage() {
        var hasAd by mutableStateOf(true)
        var nexts = 0
        composeRule.setContent {
            AppTheme {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().testTag(STEP_AREA)) {
                        if (hasAd) {
                            NativeFullScreenStepGate(
                                allowed = true,
                                states = fullPageStates(FakeLoadedNativeAd(FakeNativeAdAssets.Longest)),
                                modifier = Modifier.fillMaxSize(),
                            )
                        } else {
                            Text("Step content")
                        }
                    }
                    PrimaryButton(
                        text = "Next",
                        onClick = { nexts++ },
                        modifier = Modifier.padding(Spacing.md).fillMaxWidth().testTag(NEXT),
                    )
                }
            }
        }
        val withAd = bounds(NEXT)
        val area = bounds(STEP_AREA)
        val page = bounds(NativeFlowStepTestTags.FULL_PAGE)
        val cta = bounds(NativeFullScreenAdTestTags.CTA)
        assertEquals(52f, (withAd.bottom - withAd.top).value, 0.5f)
        assertEquals("the page fills the area of the step", area, page)
        assertFalse("CTA $cta covers Next $withAd", overlaps(cta, withAd))
        assertFalse("page $page covers Next $withAd", overlaps(page, withAd))
        assertTrue("CTA inside the screen: $cta", cta.bottom.value <= 640f)

        composeRule.onNodeWithTag(NEXT).performClick()
        composeRule.waitForIdle()
        assertEquals("a tap on Next reaches the app", 1, nexts)

        hasAd = false
        composeRule.waitForIdle()
        assertEquals("Next stays in place", withAd, bounds(NEXT))
    }

    @Test
    @Config(qualifiers = "w360dp-h640dp-xxhdpi")
    fun nextButton_samePlace_notCovered_withCard() {
        val states = MutableStateFlow<NativeAdState>(NativeAdState.Loaded(FakeLoadedNativeAd(longFullNativeAdAssets)))
        var nexts = 0
        composeRule.setContent {
            AppTheme {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f).fillMaxWidth().testTag(STEP_AREA)) { Text("Step content") }
                    NativeStepAdHost(
                        allowed = true,
                        step = 1,
                        newStates = { states },
                        modifier = Modifier.padding(horizontal = Spacing.md).fillMaxWidth(),
                    )
                    PrimaryButton(
                        text = "Next",
                        onClick = { nexts++ },
                        modifier = Modifier.padding(Spacing.md).fillMaxWidth().testTag(NEXT),
                    )
                }
            }
        }
        val withAd = bounds(NEXT)
        val card = bounds(NativeAdTestTags.CARD)
        assertEquals(250f, (card.bottom - card.top).value, 0.5f)
        assertFalse("card $card covers Next $withAd", overlaps(card, withAd))
        assertTrue("card below the step: $card", card.top >= bounds(STEP_AREA).bottom)

        composeRule.onNodeWithTag(NEXT).performClick()
        composeRule.waitForIdle()
        assertEquals(1, nexts)

        states.value = NativeAdState.Unavailable
        composeRule.waitForIdle()
        assertNoNode(NativeAdTestTags.CARD)
        assertEquals("Next stays in place", withAd, bounds(NEXT))
        assertEquals("the step takes the space back", withAd.top - Spacing.md, bounds(STEP_AREA).bottom)
    }

    // The switch of the placement.

    @Test
    fun stepAllowed_readsOnlyItsPlacementSwitch() {
        val asked = mutableListOf<AdPlacement>()
        val canRequestAds = MutableStateFlow(true)
        var placement by mutableStateOf(AdPlacement.GALLERY_NATIVE_MEDIUM)
        var allowed: Boolean? = null
        composeRule.setContent {
            allowed = rememberStepAllowed(
                placement = placement,
                canRequestAds = canRequestAds,
                enabled = {
                    asked += it
                    flowOf(it == AdPlacement.GALLERY_NATIVE_SMALL)
                },
                unitId = { "unit" },
            )
        }
        composeRule.waitForIdle()
        assertEquals(false, allowed)
        assertEquals(listOf(AdPlacement.GALLERY_NATIVE_MEDIUM), asked.distinct())

        // Control: the placement that is on.
        placement = AdPlacement.GALLERY_NATIVE_SMALL
        composeRule.waitForIdle()
        assertEquals(true, allowed)

        canRequestAds.value = false
        composeRule.waitForIdle()
        assertEquals("no consent, no ad", false, allowed)
    }

    @Test
    fun stepAllowed_falseWithoutUnitId() {
        var allowed: Boolean? = null
        composeRule.setContent {
            allowed = rememberStepAllowed(
                placement = AdPlacement.GALLERY_NATIVE_SMALL,
                canRequestAds = MutableStateFlow(true),
                enabled = { flowOf(true) },
                unitId = { null },
            )
        }
        composeRule.waitForIdle()
        assertEquals(false, allowed)
    }

    // Knowing beforehand whether the full page has an ad.

    @Test
    fun stepReady_falseWhenDisallowed() {
        var calls = 0
        var ready: Boolean? = null
        composeRule.setContent {
            ready = rememberStepReady(allowed = false) {
                calls++
                true
            }
        }
        composeRule.waitForIdle()
        assertEquals(false, ready)
        assertEquals(0, calls)
    }

    @Test
    fun stepReady_trueWhenReady() {
        composeRule.mainClock.autoAdvance = false
        var loaded = false
        var ready: Boolean? = null
        composeRule.setContent { ready = rememberStepReady(allowed = true) { loaded } }
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
        assertEquals(false, ready)

        loaded = true
        composeRule.mainClock.advanceTimeBy(PoolCellPolicy.RETRY_MILLIS + FRAME_MILLIS)
        assertEquals(true, ready)
    }

    @Test
    fun stepReady_allowedTurnsFalse_midFlow() {
        composeRule.mainClock.autoAdvance = false
        var allowed by mutableStateOf(true)
        var ready: Boolean? = null
        composeRule.setContent { ready = rememberStepReady(allowed = allowed) { true } }
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
        assertEquals(true, ready)

        allowed = false
        // The clock is stopped: the change is sent to the composition by hand, then one frame, far less than the
        // 500 ms between two reads, so false comes from the recomposition itself.
        Snapshot.sendApplyNotifications()
        composeRule.mainClock.advanceTimeBy(FRAME_MILLIS)
        assertEquals(false, ready)
    }

    private companion object {
        const val SLOT = "slot"
        const val BELOW = "below"
        const val STEP_AREA = "stepArea"
        const val NEXT = "next"
        const val FRAME_MILLIS = 32L
        val CARD_WIDTH = 379.dp
    }
}
