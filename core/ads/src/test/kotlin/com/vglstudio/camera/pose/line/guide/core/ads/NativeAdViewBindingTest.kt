package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.widget.ImageView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertHeightIsEqualTo
import androidx.compose.ui.test.getUnclippedBoundsInRoot
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.DpRect
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The shared SDK binding through [NativeAdViewHost], with a [bind] that counts calls instead of calling the SDK
 * (setNativeAd cannot run on the JVM, plan T-010 section 4.1): asset views are registered, unregistered when the
 * layout changes, bound again, and keep their sizes inside the NativeAdView; the media scale type is set before the
 * bind (O-07).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class NativeAdViewBindingTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var binds = 0
    private var adView: NativeAdView? = null

    private fun countingBind(view: NativeAdView) {
        binds++
        adView = view
    }

    private fun setHostedLayout(size: NativeAdSize, assets: NativeAdAssets = sampleNativeAdAssets) {
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(assets, size, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.waitForIdle()
    }

    @Test
    fun customLayout_withFakeAd_drawsContentWithPreviewBinder() {
        composeRule.setContent {
            AppTheme {
                BoundNativeAd(FakeLoadedNativeAd(sampleNativeAdAssets)) { binder ->
                    Column {
                        binder.Asset(NativeAdAsset.HEADLINE, Modifier.testTag("h")) { Text("Custom headline") }
                        binder.Media(Modifier.size(200.dp, 120.dp).testTag("m"))
                    }
                }
            }
        }
        composeRule.onNodeWithText("Custom headline", useUnmergedTree = true).assertExists()
        composeRule.onNodeWithTag("m", useUnmergedTree = true).assertHeightIsEqualTo(120.dp)
    }

    @Test
    fun registersEveryAssetView_small() {
        setHostedLayout(NativeAdSize.SMALL)
        val view = checkNotNull(adView)
        assertNotNull(view.headlineView)
        assertNotNull(view.advertiserView)
        assertNotNull(view.callToActionView)
        assertNotNull(view.iconView)
        assertNull(view.mediaView)
        // Small draws no star row, so nothing is registered for it (plan T-060).
        assertNull(view.starRatingView)
        assertTrue(binds >= 1)
    }

    /** KB-2 (plan T-060): the star row of medium is registered with the SDK while the ad has stars, and only then. */
    @Test
    fun registersStarRatingView_medium_whenRated() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 4.0))
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(assets, NativeAdSize.MEDIUM, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.waitForIdle()
        val view = checkNotNull(adView)
        val stars = view.starRatingView
        assertNotNull("star row registered at rating 4.0", stars)
        assertNotSame(view.headlineView, stars)
        assertNotSame(view.iconView, stars)
        assertNotSame(view.callToActionView, stars)
        val expectedPx = with(composeRule.density) { NativeAdDimens.StarRowHeight.toPx() }
        assertEquals("star row view height in px", expectedPx, checkNotNull(stars).height.toFloat(), 1f)

        val bindsBefore = binds
        assets = sampleNativeAdAssets.copy(starRating = 0.0)
        composeRule.waitForIdle()
        assertNull("no star row registered at rating 0", view.starRatingView)
        assertTrue("bind ran $binds times, $bindsBefore before the rating went to 0", binds > bindsBefore)

        assets = sampleNativeAdAssets.copy(starRating = 4.0)
        composeRule.waitForIdle()
        assertNotNull("star row registered again at rating 4.0", view.starRatingView)
    }

    /** KB-2 (plan T-060): the star row of the full native ad is registered while the ad has stars. */
    @Test
    fun registersStarRatingView_full() {
        var assets by mutableStateOf(sampleNativeAdAssets.copy(starRating = 4.0))
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(assets, NativeAdSize.FULL, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.waitForIdle()
        val view = checkNotNull(adView)
        assertNotNull("star row registered at rating 4.0", view.starRatingView)

        assets = sampleNativeAdAssets.copy(starRating = null)
        composeRule.waitForIdle()
        assertNull("no star row registered without a rating", view.starRatingView)
    }

    /**
     * KB-1 (plan T-060), drawn the way a real ad is (each part a nested view of the SDK binding): a long call to
     * action of small stops at 120 dp, so the headline and the "Ad" label keep room; a short one keeps 80 dp.
     */
    @Test
    fun sdkLayout_small_longCta_ctaAtMost120dp_headlineVisible() {
        var assets by mutableStateOf(longFullNativeAdAssets)
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(assets, NativeAdSize.SMALL, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.waitForIdle()
        val cta = bounds(NativeAdTestTags.CTA)
        val headline = bounds(NativeAdTestTags.HEADLINE)
        val badge = bounds(NativeAdTestTags.BADGE)
        val measured = "CTA ${cta.w()} dp, headline ${headline.w()} dp, Ad label ${badge.w()} dp"
        assertEquals("long call to action width; $measured", 120f, cta.w(), 0.5f)
        assertTrue("headline has no width; $measured", headline.w() > 0f)
        assertTrue("Ad label has no width; $measured", badge.w() > 0f)
        assertEquals("card height; $measured", 80f, bounds(NativeAdTestTags.CARD).h(), 0.5f)

        assets = sampleNativeAdAssets
        composeRule.waitForIdle()
        assertEquals("short call to action width", 80f, bounds(NativeAdTestTags.CTA).w(), 0.5f)
    }

    @Test
    fun registersEveryAssetView_medium() {
        setHostedLayout(NativeAdSize.MEDIUM)
        val view = checkNotNull(adView)
        assertNotNull(view.headlineView)
        assertNotNull(view.advertiserView)
        assertNotNull(view.bodyView)
        assertNotNull(view.callToActionView)
        assertNotNull(view.iconView)
        assertTrue(view.mediaView is MediaView)
        assertTrue(binds >= 1)
    }

    @Test
    fun sdkLayout_keepsCardHeights() {
        var size by mutableStateOf(NativeAdSize.SMALL)
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(sampleNativeAdAssets, size, binder, showAdChoicesMarker = false)
                }
            }
        }
        composeRule.onNodeWithTag(NativeAdTestTags.CARD, useUnmergedTree = true).assertHeightIsEqualTo(80.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(34.dp)

        size = NativeAdSize.MEDIUM
        composeRule.waitForIdle()
        composeRule.onNodeWithTag(NativeAdTestTags.CARD, useUnmergedTree = true).assertHeightIsEqualTo(410.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.CTA, useUnmergedTree = true).assertHeightIsEqualTo(44.dp)
        composeRule.onNodeWithTag(NativeAdTestTags.MEDIA, useUnmergedTree = true).assertHeightIsEqualTo(196.dp)
        // The star row registered with the SDK keeps its 16 dp (plan T-060).
        composeRule.onNodeWithTag(NativeAdTestTags.STAR_ROW, useUnmergedTree = true).assertHeightIsEqualTo(16.dp)
    }

    private fun bounds(tag: String): DpRect =
        composeRule.onNodeWithTag(tag, useUnmergedTree = true).getUnclippedBoundsInRoot()

    private fun DpRect.w(): Float = (right - left).value

    private fun DpRect.h(): Float = (bottom - top).value

    @Test
    fun removingMedia_unregistersAndBindsAgain() {
        var collapsed by mutableStateOf(false)
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    Column {
                        binder.Asset(NativeAdAsset.HEADLINE, Modifier) { Text("Headline") }
                        if (!collapsed) binder.Media(Modifier.size(200.dp, 160.dp))
                    }
                }
            }
        }
        composeRule.waitForIdle()
        val view = checkNotNull(adView)
        assertTrue(view.mediaView is MediaView)
        val bindsBefore = binds

        collapsed = true
        composeRule.waitForIdle()

        assertNull(view.mediaView)
        assertNotNull(view.headlineView)
        assertTrue("bind ran $binds times, $bindsBefore before collapsing", binds > bindsBefore)
    }

    @Test
    fun assetContent_seesCompositionLocals() {
        composeRule.setContent {
            AppTheme {
                CompositionLocalProvider(LocalProbe provides "Seen through the nested view") {
                    NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                        binder.Asset(NativeAdAsset.HEADLINE, Modifier) { Text(LocalProbe.current) }
                    }
                }
            }
        }
        composeRule.onNodeWithText("Seen through the nested view", useUnmergedTree = true).assertExists()
    }

    @Test
    fun mediaScaleType_setBeforeSetNativeAd() {
        // O-07: the image scale type is set before the ad is bound to the views, so the image is not cropped.
        val calls = mutableListOf<String>()
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(
                    bind = { calls += "setNativeAd" },
                    modifier = Modifier.width(CARD_WIDTH),
                    newMediaView = { context -> RecordingMediaView(context, calls) },
                ) { binder ->
                    binder.Media(Modifier.size(200.dp, 120.dp))
                }
            }
        }
        composeRule.waitForIdle()

        val scale = calls.indexOf("setImageScaleType(FIT_CENTER)")
        val bind = calls.indexOf("setNativeAd")
        assertTrue("calls: $calls", scale >= 0)
        assertTrue("calls: $calls", bind > scale)
    }

    /** A MediaView that records the scale type it is given. */
    private class RecordingMediaView(context: Context, private val calls: MutableList<String>) : MediaView(context) {
        override fun setImageScaleType(scaleType: ImageView.ScaleType) {
            calls += "setImageScaleType($scaleType)"
            super.setImageScaleType(scaleType)
        }
    }

    private companion object {
        val CARD_WIDTH = 379.dp
        val LocalProbe = staticCompositionLocalOf { "default" }
    }
}
