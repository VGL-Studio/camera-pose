package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdViewAdListener
import com.applovin.mediation.MaxError
import com.applovin.mediation.ads.MaxAdView
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

private const val TAG = "Ads"

/** The `MaxAdView` of AppLovin MAX as [MaxBannerAdView] uses it; a fake in tests (plan T-071). */
internal interface MaxBannerSdkView {
    val view: View

    fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit, onClicked: () -> Unit)

    fun loadAd()

    fun startAutoRefresh()

    fun stopAutoRefresh()

    fun destroy()
}

/**
 * A real `MaxAdView` of [unitId] (plan T-071, ADR-0037): the adaptive banner of MAX as wide as the slot
 * ([BannerSizeKind.ANCHORED]) or the 300 x 250 MREC ([BannerSizeKind.MEDIUM_RECTANGLE]); a collapsible banner never
 * loads from MAX, so [BannerSizeKind.LARGE_ANCHORED] throws [IllegalArgumentException]. Built with the constructor
 * that takes a [Context]: the one with `MaxAdViewConfiguration` throws while the MAX SDK has not started (plan T-071,
 * section 4.1). `stopAutoRefresh` stops at once, not after the refresh being counted down (KB-27). No revenue
 * listener yet (T-072).
 */
internal class AppLovinMaxBannerView(unitId: String, kind: BannerSizeKind, widthDp: Int, context: Context) :
    MaxBannerSdkView {
    private val adView: MaxAdView = when (kind) {
        BannerSizeKind.ANCHORED -> MaxAdView(unitId, MaxAdFormat.BANNER, context).apply {
            setExtraParameter("adaptive_banner", "true")
            setExtraParameter("adaptive_banner_width", widthDp.toString())
        }

        BannerSizeKind.MEDIUM_RECTANGLE -> MaxAdView(unitId, MaxAdFormat.MREC, context)
        BannerSizeKind.LARGE_ANCHORED -> throw IllegalArgumentException("A collapsible banner does not load from MAX")
    }.apply { setExtraParameter("allow_pause_auto_refresh_immediately", "true") }

    override val view: View get() = adView

    override fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit, onClicked: () -> Unit) {
        adView.setListener(
            object : MaxAdViewAdListener {
                override fun onAdLoaded(ad: MaxAd) = onLoaded()

                override fun onAdLoadFailed(adUnitId: String, error: MaxError) = onFailed(error.code)

                override fun onAdClicked(ad: MaxAd) = onClicked()

                override fun onAdDisplayed(ad: MaxAd) = Unit

                override fun onAdHidden(ad: MaxAd) = Unit

                override fun onAdDisplayFailed(ad: MaxAd, error: MaxError) = Unit

                override fun onAdExpanded(ad: MaxAd) = Unit

                override fun onAdCollapsed(ad: MaxAd) = Unit
            },
        )
    }

    override fun loadAd() = adView.loadAd()

    override fun startAutoRefresh() = adView.startAutoRefresh()

    override fun stopAutoRefresh() = adView.stopAutoRefresh()

    override fun destroy() = adView.destroy()
}

/**
 * The height of a MAX banner of [kind] in a slot [widthDp] wide: the adaptive height of MAX for the anchored banner,
 * 250 dp for the MREC. A collapsible banner has no MAX size and throws.
 */
internal fun maxBannerHeightDp(context: Context, kind: BannerSizeKind, widthDp: Int): Int = when (kind) {
    BannerSizeKind.ANCHORED -> MaxAdFormat.BANNER.getAdaptiveSize(widthDp, context).height
    BannerSizeKind.MEDIUM_RECTANGLE -> MEDIUM_RECTANGLE_HEIGHT_DP
    BannerSizeKind.LARGE_ANCHORED -> throw IllegalArgumentException("A collapsible banner does not load from MAX")
}

/**
 * The banner of AppLovin MAX of one frame (plan T-071, KB-31), drawn as [format] and reported under [placement].
 *
 * - The SDK view is built by [newSdkView] on the first [send] only (question 6 of the plan, G2): the frame sends to
 *   MAX only once MAX is ready and the send passed the screen and consent checks, so no `MaxAdView` exists before.
 *   The wrapper never gets the ad unit: it lives in [newSdkView] only, so no log line can hold it.
 * - [pause] stops the auto refresh of MAX, [resume] starts it (B-06, B-07, KB-27); a pause before the view exists is
 *   kept, and the view built later stops its auto refresh right after its first load.
 * - Loads and clicks go to [telemetry] like those of AdMob: the load counter of MAX is `<slot key>/max`, apart from
 *   AdMob in the same frame (L-068); a failure is labelled by [maxLoadStateAfterError]; a click skips the next resume
 *   ad (A-14). Revenue of MAX is T-072.
 */
internal class MaxBannerAdView(
    private val newSdkView: () -> MaxBannerSdkView,
    private val placement: AdPlacement,
    private val format: AdFormat,
    private val telemetry: AdTelemetry,
    private val now: () -> Long,
    private val logger: AppLogger,
) : BannerAdView {
    // Callbacks come one at a time; the buffer keeps tryEmit from dropping a result the loop has not read yet.
    private val emitted = MutableSharedFlow<BannerLoadResult>(extraBufferCapacity = RESULT_BUFFER)

    override val results: Flow<BannerLoadResult> = emitted

    private val placementKey = nativeSlotKey(placement, format)
    private val maxSlot = "$placementKey/max"
    private val loadPlacement = loadPlacementOf(format, placement)

    private var sdk by mutableStateOf<MaxBannerSdkView?>(null)

    /** The last of [pause] and [resume] was a pause. */
    private var wantPaused = false

    override fun send() {
        val existing = sdk
        val view = existing ?: newSdkView().also { built ->
            built.setCallbacks(onLoaded = ::onLoaded, onFailed = ::onFailed, onClicked = ::onClicked)
            sdk = built
            logger.info(TAG, "Banner $placementKey MAX view created")
        }
        telemetry.requestSent(maxSlot, now())
        view.loadAd()
        if (existing == null && wantPaused) view.stopAutoRefresh()
    }

    override fun pause() {
        wantPaused = true
        sdk?.stopAutoRefresh()
    }

    override fun resume() {
        wantPaused = false
        sdk?.startAutoRefresh()
    }

    override fun destroy() {
        sdk?.destroy()
    }

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) {
        val current = sdk ?: return
        key(current) {
            AndroidView(
                factory = { current.view },
                modifier = modifier,
                update = { it.visibility = if (showAd) View.VISIBLE else View.INVISIBLE },
            )
        }
    }

    private fun onLoaded() {
        logger.info(TAG, "Banner $placementKey MAX loaded")
        telemetry.loadSucceeded(format, maxSlot, loadPlacement, now())
        emitted.tryEmit(BannerLoadResult.Loaded)
    }

    private fun onFailed(code: Int) {
        logger.info(TAG, "Banner $placementKey MAX failed (code $code)")
        telemetry.maxLoadFailed(format, maxSlot, loadPlacement, code, now())
        emitted.tryEmit(BannerLoadResult.Failed(code))
    }

    private fun onClicked() = telemetry.clicked(format, placement)

    private companion object {
        const val RESULT_BUFFER = 16
    }
}
