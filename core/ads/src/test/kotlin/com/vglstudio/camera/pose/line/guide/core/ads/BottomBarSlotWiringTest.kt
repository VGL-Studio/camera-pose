package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import android.view.View
import android.view.ViewGroup
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

/**
 * The slots of a bottom bar through their production wiring under a real [AdsController] (D-04, issue #527): the
 * banner slot ([BottomBannerSlot]) reports the height its frame takes, 0 after a failure before its first ad; the
 * native slot ([BottomNativeSlot]) reports that it takes space while its placeholder shows, not after a failure.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class BottomBarSlotWiringTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<ComponentActivity>()

    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val placement = AdPlacement.POSES_BOTTOM
    private val settings = FakeRemoteSettings().apply { set(AdsSettings.placementEnabled(placement), true) }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    @Test
    fun bottomBannerSlot_failedBeforeAd_reportsNoHeight() {
        val controller = testAdsController(flags = DefaultAdsFlags(settings), maxConfig = MaxConfig.NONE)
            .also { it.openConsentGateForTest() }
        val reported = mutableListOf<Dp>()
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                Box(Modifier.width(411.dp)) {
                    BottomBannerSlot(placement, onAdAvailabilityChange = {}, onHeight = { reported += it })
                }
            }
        }
        composeRule.waitForIdle()

        // Control: while the first load is in flight the frame takes its height, and the bar is told so.
        val frameHeight = bannerAdSize(composeRule.activity, AdFormat.BANNER, 411).height.dp
        assertNotEquals(0.dp, frameHeight)
        assertEquals(frameHeight, reported.last())

        val adView = adViews(composeRule.activity.window.decorView).single()
        composeRule.runOnIdle {
            adView.adListener.onAdFailedToLoad(LoadAdError(3, "No fill.", "com.google.android.gms.ads", null, null))
        }
        composeRule.waitForIdle()

        // D-04: the frame of the bar takes no space after a failure before its first ad, and the bar keeps none.
        assertEquals(0.dp, reported.last())
    }

    @Test
    fun bottomNativeSlot_placeholderTakesSpace_failureDoesNot() {
        val loader = FakeNativeAdLoader()
        val controller = testAdsController(context = application, flags = DefaultAdsFlags(settings))
            .also { it.nativeLoaderForTest = loader }
        controller.start(application, AppActivityFilter { true })
        idle()
        controller.openConsentGateForTest()
        controller.admobReadyForTest()
        controller.setAppForegroundForTest(true)
        idle()
        val reported = mutableListOf<Boolean>()
        composeRule.setContent {
            CompositionLocalProvider(LocalAdsController provides controller) {
                BottomNativeSlot(placement, firstRefreshExtraMillis = 0L, onTakesSpaceChange = { reported += it })
            }
        }
        composeRule.waitForIdle()
        idle()
        composeRule.waitForIdle()

        // D-02, D-04: the placeholder shows while the first load is in flight, and the slot takes space.
        assertEquals(1, placeholders())
        assertTrue("requests sent", loader.requests.isNotEmpty())
        assertEquals(listOf(true), reported)

        // Control: every open request fails, the slot draws nothing and no longer takes space.
        composeRule.runOnIdle { while (loader.open > 0) loader.fail(3) }
        idle()
        composeRule.waitForIdle()
        assertEquals(0, placeholders())
        assertEquals(listOf(true, false), reported)
    }

    private fun placeholders(): Int =
        composeRule.onAllNodesWithTag(NativeAdTestTags.PLACEHOLDER, useUnmergedTree = true).fetchSemanticsNodes().size

    private fun adViews(view: View): List<AdView> = when (view) {
        is AdView -> listOf(view)
        is ViewGroup -> (0 until view.childCount).flatMap { adViews(view.getChildAt(it)) }
        else -> emptyList()
    }
}
