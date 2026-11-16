package com.vglstudio.camera.pose.line.guide.core.ads

import android.os.Bundle
import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdFormat
import com.applovin.mediation.MaxAdWaterfallInfo
import com.applovin.mediation.MaxError
import com.applovin.mediation.MaxMediatedNetworkInfo
import com.applovin.mediation.MaxNetworkResponseInfo
import com.applovin.mediation.nativeAds.MaxNativeAd
import com.applovin.sdk.AppLovinSdkUtils

/**
 * Hand-written fakes of the interfaces of the MAX SDK that [maxAdInfoOf] and [maxNetworkAttemptsOf] read (plan
 * T-072). Unlike a relaxed mock, every getter :core:ads must never read throws [AssertionError]: the credentials of a
 * network (they hold its ad unit), the message of an error, the message of the mediated network and the load failure
 * info. A getter the code has no reason to read throws too, so a new read shows up at once.
 */
internal class FakeMaxAd(
    private val adUnitId: String = "FAKE_MAX_UNIT_0123",
    private val revenue: Double = 0.0,
    private val networkName: String? = "AppLovin",
    private val revenuePrecision: String? = "exact",
    private val waterfall: MaxAdWaterfallInfo? = null,
    /** The getters that throw [IllegalStateException], as an SDK in a bad state might. */
    private val throwing: Set<String> = emptySet(),
) : MaxAd {
    private fun <T> read(name: String, value: T): T {
        if (name in throwing) throw IllegalStateException("$name failed")
        return value
    }

    override fun getAdUnitId(): String = read("getAdUnitId", adUnitId)

    override fun getRevenue(): Double = read("getRevenue", revenue)

    override fun getNetworkName(): String? = read("getNetworkName", networkName)

    override fun getRevenuePrecision(): String? = read("getRevenuePrecision", revenuePrecision)

    override fun getWaterfall(): MaxAdWaterfallInfo? = read("getWaterfall", waterfall)

    override fun getFormat(): MaxAdFormat = forbidden("getFormat")

    override fun getSize(): AppLovinSdkUtils.Size = forbidden("getSize")

    override fun getNetworkPlacement(): String = forbidden("getNetworkPlacement")

    override fun getPlacement(): String = forbidden("getPlacement")

    override fun getRequestLatencyMillis(): Long = forbidden("getRequestLatencyMillis")

    override fun getCreativeId(): String = forbidden("getCreativeId")

    override fun getDspName(): String = forbidden("getDspName")

    override fun getDspId(): String = forbidden("getDspId")

    override fun getAdValue(key: String): String = forbidden("getAdValue")

    override fun getAdValue(key: String, defaultValue: String): String = forbidden("getAdValue")

    override fun getNativeAd(): MaxNativeAd = forbidden("getNativeAd")

    override fun getAdReviewCreativeId(): String = forbidden("getAdReviewCreativeId")

    override fun toString(): String = forbidden("toString")
}

internal class FakeMaxWaterfall(private val responses: List<MaxNetworkResponseInfo>) : MaxAdWaterfallInfo {
    override fun getNetworkResponses(): List<MaxNetworkResponseInfo> = responses

    override fun getLoadedAd(): MaxAd = forbidden("getLoadedAd")

    override fun getName(): String = forbidden("getName")

    override fun getTestName(): String = forbidden("getTestName")

    override fun getLatencyMillis(): Long = forbidden("getLatencyMillis")

    override fun toString(): String = forbidden("toString")
}

internal class FakeMaxNetworkResponse(
    private val network: MaxMediatedNetworkInfo?,
    private val latencyMillis: Long,
    private val state: MaxNetworkResponseInfo.AdLoadState,
    private val error: MaxError? = null,
    private val stateThrows: Boolean = false,
) : MaxNetworkResponseInfo {
    override fun getMediatedNetwork(): MaxMediatedNetworkInfo? = network

    override fun getLatencyMillis(): Long = latencyMillis

    override fun getAdLoadState(): MaxNetworkResponseInfo.AdLoadState =
        if (stateThrows) throw IllegalStateException("load state") else state

    override fun getError(): MaxError? = error

    override fun getCredentials(): Bundle = forbidden("getCredentials")

    override fun isBidding(): Boolean = forbidden("isBidding")

    override fun toString(): String = forbidden("toString")
}

internal class FakeMaxNetwork(private val name: String?, private val adapterClassName: String?) :
    MaxMediatedNetworkInfo {
    override fun getName(): String? = name

    override fun getAdapterClassName(): String? = adapterClassName

    override fun getAdapterVersion(): String = forbidden("getAdapterVersion")

    override fun getSdkVersion(): String = forbidden("getSdkVersion")

    override fun getInitializationStatus(): MaxMediatedNetworkInfo.InitializationStatus =
        forbidden("getInitializationStatus")

    override fun toString(): String = forbidden("toString")
}

internal class FakeMaxError(private val code: Int, private val waterfall: MaxAdWaterfallInfo? = null) : MaxError {
    override fun getCode(): Int = code

    override fun getWaterfall(): MaxAdWaterfallInfo? = waterfall

    override fun getMessage(): String = forbidden("getMessage")

    override fun getMediatedNetworkErrorCode(): Int = forbidden("getMediatedNetworkErrorCode")

    override fun getMediatedNetworkErrorMessage(): String = forbidden("getMediatedNetworkErrorMessage")

    override fun getRequestLatencyMillis(): Long = forbidden("getRequestLatencyMillis")

    override fun getAdLoadFailureInfo(): String = forbidden("getAdLoadFailureInfo")

    override fun toString(): String = forbidden("toString")
}

private fun forbidden(name: String): Nothing = throw AssertionError("$name must not be read")
