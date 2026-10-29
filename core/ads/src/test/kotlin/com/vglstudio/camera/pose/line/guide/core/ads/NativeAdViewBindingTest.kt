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
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.ads.nativead.MediaView
import com.google.android.gms.ads.nativead.NativeAdView
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import org.junit.Assert.assertNotNull
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

    private fun setHostedLayout(size: NativeAdSize) {
        composeRule.setContent {
            AppTheme {
                NativeAdViewHost(bind = ::countingBind, modifier = Modifier.width(CARD_WIDTH)) { binder ->
                    NativeAdLayout(sampleNativeAdAssets, size, binder, showAdChoicesMarker = false)
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
        assertTrue(binds >= 1)
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
    }

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
