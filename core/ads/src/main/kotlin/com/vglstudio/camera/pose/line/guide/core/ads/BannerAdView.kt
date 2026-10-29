package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Bundle
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError
import java.util.UUID
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow

/** One banner of the SDK, as the slot sees it ([BannerSlotHost], [runBannerLoads]). */
internal interface BannerAdView {
    /** Results of the loads, sent by the app or refreshed by the SDK; no old result is replayed. */
    val results: Flow<BannerLoadResult>

    /** Sends one request now. */
    fun send()

    fun pause()

    fun resume()

    fun destroy()

    /** Draws the banner; with [showAd] false the view stays in place but is not seen (B-06). */
    @Composable
    fun Content(showAd: Boolean, modifier: Modifier)
}

/**
 * The request of a banner. A collapsible banner ([BannerRefresh.MANUAL]) asks AdMob to collapse at the bottom with a
 * new [requestId] each time, so every reload opens as a new expanded banner (B-03); the same id would count as the
 * same expansion.
 */
internal fun bannerAdRequest(refresh: BannerRefresh, requestId: () -> String): AdRequest = when (refresh) {
    BannerRefresh.BY_AD_UNIT -> AdRequest.Builder().build()
    BannerRefresh.MANUAL -> {
        val extras = Bundle().apply {
            putString(COLLAPSIBLE_KEY, COLLAPSIBLE_BOTTOM)
            putString(COLLAPSIBLE_REQUEST_ID_KEY, requestId())
        }
        AdRequest.Builder().addNetworkExtrasBundle(AdMobAdapter::class.java, extras).build()
    }
}

private const val COLLAPSIBLE_KEY = "collapsible"
private const val COLLAPSIBLE_BOTTOM = "bottom"
private const val COLLAPSIBLE_REQUEST_ID_KEY = "collapsible_request_id"

/**
 * A real [AdView] of [placement], drawn as [format]: the placement's own, or the banner layer of a bottom bar, reported
 * under the bar's placement (plan T-030, D-05). Its paid events, load results and clicks go to the ad metrics (plan
 * T-023): no zero-value fallback for banners (T-02); only the first result after a send has a latency, the refreshes
 * of the SDK have none (A 06:157). Load results also go to [results] for [runBannerLoads].
 */
internal class SdkBannerAdView(
    private val adView: AdView,
    private val refresh: BannerRefresh,
    private val placement: AdPlacement,
    private val unitId: String,
    private val telemetry: AdTelemetry,
    private val now: () -> Long,
    private val load: (AdRequest) -> Unit = adView::loadAd,
    private val newRequestId: () -> String = { UUID.randomUUID().toString() },
    private val format: AdFormat = placement.format,
) : BannerAdView {
    // Callbacks come one at a time; the buffer keeps tryEmit from dropping a result the loop has not read yet.
    private val emitted = MutableSharedFlow<BannerLoadResult>(extraBufferCapacity = RESULT_BUFFER)

    override val results: Flow<BannerLoadResult> = emitted

    /** The key of the load counter: one per placement and format (L-068). */
    private val slotKey = nativeSlotKey(placement, format)

    init {
        attachListeners()
    }

    override fun send() {
        telemetry.requestSent(slotKey, now())
        load(bannerAdRequest(refresh, newRequestId))
    }

    override fun pause() = adView.pause()

    override fun resume() = adView.resume()

    override fun destroy() = adView.destroy()

    @Composable
    override fun Content(showAd: Boolean, modifier: Modifier) {
        AndroidView(
            factory = { adView },
            modifier = modifier,
            update = { it.visibility = if (showAd) View.VISIBLE else View.INVISIBLE },
        )
    }

    private fun attachListeners() {
        val loadPlacement = loadPlacementOf(format, placement)
        adView.setOnPaidEventListener { value ->
            val adapter = adView.responseInfo?.loadedAdapterResponseInfo
            telemetry.revenue(
                format,
                unitId,
                { placement },
                value.valueMicros,
                value.currencyCode,
                adapter?.adSourceName,
                adapter?.adapterClassName,
            )
        }
        val slot = slotKey
        adView.adListener = object : AdListener() {
            override fun onAdLoaded() {
                telemetry.loadSucceeded(format, slot, loadPlacement, now())
                emitted.tryEmit(BannerLoadResult.Loaded)
            }

            override fun onAdFailedToLoad(error: LoadAdError) {
                telemetry.loadFailed(format, slot, loadPlacement, error.code, error.message, now())
                emitted.tryEmit(BannerLoadResult.Failed(error.code))
            }

            override fun onAdClicked() = telemetry.clicked(format, placement)
        }
    }

    private companion object {
        const val RESULT_BUFFER = 16
    }
}
