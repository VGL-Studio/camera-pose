package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdSize
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertThrows
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The size of each banner format (plan T-067, ADR-0038): a fixed banner at the anchored adaptive size of JBase
 * (B-01, KB-18, KB-32), a collapsible banner at the large anchored size (B-03), and the height the banner slot of a
 * bottom bar reports for the space it keeps (KB-17).
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BannerAdSizeTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val activity get() = composeRule.activity

    @Test
    fun fixedBanner_kindIsAnchored() {
        assertEquals(BannerSizeKind.ANCHORED, bannerSizeKind(AdFormat.BANNER))
    }

    @Test
    fun collapsibleBanner_kindStaysLarge() {
        assertEquals(BannerSizeKind.LARGE_ANCHORED, bannerSizeKind(AdFormat.BANNER_COLLAPSIBLE))
    }

    @Test
    fun nonBannerFormat_throws() {
        BANNER_FORMATS.forEach { format -> bannerSizeKind(format) }
        (AdFormat.entries - BANNER_FORMATS).forEach { format ->
            assertThrows(format.name, IllegalArgumentException::class.java) { bannerSizeKind(format) }
        }
    }

    @Test
    fun mrec_kindIsMediumRectangle() {
        // Control: the other banner formats keep their kinds.
        assertEquals(BannerSizeKind.ANCHORED, bannerSizeKind(AdFormat.BANNER))
        assertEquals(BannerSizeKind.LARGE_ANCHORED, bannerSizeKind(AdFormat.BANNER_COLLAPSIBLE))
        assertEquals(BannerSizeKind.MEDIUM_RECTANGLE, bannerSizeKind(AdFormat.BANNER_MREC))
    }

    @Test
    fun bannerAdSize_mrec_isMediumRectangle() {
        assertEquals(AdSize.MEDIUM_RECTANGLE.width, MEDIUM_RECTANGLE_WIDTH_DP)
        assertEquals(AdSize.MEDIUM_RECTANGLE.height, MEDIUM_RECTANGLE_HEIGHT_DP)
        listOf(320, 411).forEach { width ->
            val actual = bannerAdSize(activity, AdFormat.BANNER_MREC, width)
            assertEquals("width at $width dp", 300, actual.width)
            assertEquals("height at $width dp", 250, actual.height)
            assertEquals("at $width dp", AdSize.MEDIUM_RECTANGLE, actual)
        }
    }

    @Test
    fun fitsWidth_mrecNeeds300() {
        // Control: an anchored banner fits any width.
        assertEquals(true, bannerFitsWidth(BannerSizeKind.ANCHORED, 200))
        assertEquals(false, bannerFitsWidth(BannerSizeKind.MEDIUM_RECTANGLE, 299))
        assertEquals(true, bannerFitsWidth(BannerSizeKind.MEDIUM_RECTANGLE, 300))
        assertEquals(true, bannerFitsWidth(BannerSizeKind.MEDIUM_RECTANGLE, 411))
    }

    @Test
    fun frameWidth_mrec300_othersFill() {
        assertEquals(300, bannerFrameWidthDp(BannerSizeKind.MEDIUM_RECTANGLE))
        assertEquals(null, bannerFrameWidthDp(BannerSizeKind.ANCHORED))
        assertEquals(null, bannerFrameWidthDp(BannerSizeKind.LARGE_ANCHORED))
    }

    @Test
    fun sdkSizes_anchoredAndLarge_measured() {
        assertEquals(56, anchoredSize(360).height)
        assertEquals(64, anchoredSize(411).height)
        assertEquals(113, AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 360).height)
        assertEquals(128, AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 411).height)
    }

    @Test
    fun bannerAdSize_fixed_matchesAnchored() {
        WIDTHS_DP.forEach { width ->
            val expected = anchoredSize(width)
            val actual = bannerAdSize(activity, AdFormat.BANNER, width)
            assertEquals("width at $width dp", expected.width, actual.width)
            assertEquals("height at $width dp", expected.height, actual.height)
        }
        assertEquals(64, bannerAdSize(activity, AdFormat.BANNER, 411).height)
    }

    @Test
    fun bannerAdSize_collapsible_matchesLarge() {
        WIDTHS_DP.forEach { width ->
            val expected = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, width)
            val actual = bannerAdSize(activity, AdFormat.BANNER_COLLAPSIBLE, width)
            assertEquals("width at $width dp", expected.width, actual.width)
            assertEquals("height at $width dp", expected.height, actual.height)
        }
    }

    @Test
    fun bottomBannerSlot_reportsAnchoredHeight() {
        val reported = mutableListOf<Dp>()
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides testAdsController()) {
                Box(Modifier.width(411.dp)) {
                    BottomBannerSlot(
                        AdPlacement.POSES_BOTTOM,
                        onAdAvailabilityChange = {},
                        onHeight = { reported += it },
                    )
                }
            }
        }
        composeRule.waitForIdle()

        val anchored = anchoredSize(411).height.dp
        val large = AdSize.getLargeAnchoredAdaptiveBannerAdSize(activity, 411).height.dp
        assertNotEquals(large, anchored)
        assertEquals(anchored, reported.last())
    }

    @Test
    fun bottomBannerSlot_withMaxBannerUnit_reportsTallestHeight() {
        // Plan T-071: the bar keeps the height of the frame of two platforms, the taller one (here MAX, 90 dp).
        val withMax = mutableListOf<Dp>()
        val withoutMax = mutableListOf<Dp>()
        val maxController = testAdsController(maxConfig = MaxConfig("key", "banner=u1"))
            .also { it.maxBannerHeightForTest = { _, _ -> 90 } }
        val admobController = testAdsController(maxConfig = MaxConfig.NONE)
            .also { it.maxBannerHeightForTest = { _, _ -> 90 } }
        composeRule.setContent {
            Column {
                bottomSlot(maxController, withMax)
                // Control: no MAX key, same height override: the anchored height of AdMob.
                bottomSlot(admobController, withoutMax)
            }
        }
        composeRule.waitForIdle()

        assertEquals(56.dp, anchoredSize(360).height.dp)
        assertEquals(90.dp, withMax.last())
        assertEquals(56.dp, withoutMax.last())
    }

    @Test
    fun admobOrder_withMaxKey_noMaxPlatform_heightIsAdmob() {
        // KB-31: with a MAX key but ads_mediation_order admob, the frame has no MAX platform.
        val admobOrder = FakeRemoteSettings().apply { set(AdsSettings.ADS_MEDIATION_ORDER, "admob") }
        val maxAdmobOrder = FakeRemoteSettings().apply { set(AdsSettings.ADS_MEDIATION_ORDER, "max_admob") }
        val admobReported = mutableListOf<Dp>()
        val maxAdmobReported = mutableListOf<Dp>()
        fun controller(settings: FakeRemoteSettings) = testAdsController(
            flags = DefaultAdsFlags(settings),
            maxConfig = MaxConfig("key", "banner=u1"),
        ).also {
            it.maxBannerHeightForTest = { _, _ -> 90 }
            // The remote keys reach the controller once it started (ads_mediation_order is max_admob before).
            it.start(ApplicationProvider.getApplicationContext(), AppActivityFilter { true })
        }
        val admobController = controller(admobOrder)
        val maxAdmobController = controller(maxAdmobOrder)
        composeRule.setContent {
            Column {
                bottomSlot(admobController, admobReported)
                // Control: the order max_admob reports the height of MAX.
                bottomSlot(maxAdmobController, maxAdmobReported)
            }
        }
        composeRule.waitForIdle()

        assertEquals(MediationOrder.ADMOB, admobController.mediationOrder.value)
        assertEquals(56.dp, admobReported.last())
        assertEquals(90.dp, maxAdmobReported.last())
    }

    /** The banner slot of a bottom bar 360 dp wide under [controller], its heights put in [reported]. */
    @Composable
    private fun bottomSlot(controller: AdsController, reported: MutableList<Dp>) {
        CompositionLocalProvider(LocalAdsController provides controller) {
            Box(Modifier.width(360.dp)) {
                BottomBannerSlot(
                    AdPlacement.POSES_BOTTOM,
                    onAdAvailabilityChange = {},
                    onHeight = { reported += it },
                )
            }
        }
    }

    /** The SDK marks the anchored adaptive size deprecated with no replacement for it (plan T-067, section 2). */
    @Suppress("DEPRECATION")
    private fun anchoredSize(widthDp: Int): AdSize =
        AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, widthDp)

    private companion object {
        /** The formats drawn at a banner size; T-062 adds AdFormat.BANNER_MREC here. */
        val BANNER_FORMATS = setOf(AdFormat.BANNER, AdFormat.BANNER_COLLAPSIBLE, AdFormat.BANNER_MREC)

        val WIDTHS_DP = listOf(320, 360, 411, 600)
    }
}
