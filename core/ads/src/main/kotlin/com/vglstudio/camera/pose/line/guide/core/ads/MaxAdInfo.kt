package com.vglstudio.camera.pose.line.guide.core.ads

import com.applovin.mediation.MaxAd
import com.applovin.mediation.MaxAdWaterfallInfo
import com.applovin.mediation.MaxNetworkResponseInfo

/** The result of one network MAX tried for a load. */
internal enum class MaxNetworkResult { LOADED, FAILED, NOT_TRIED }

/** One network MAX tried for a load: its name, latency and result; never its credentials or an error message. */
internal data class MaxNetworkAttempt(
    val networkName: String,
    val latencyMillis: Long,
    val result: MaxNetworkResult,
    val errorCode: Int?,
)

/**
 * What :core:ads reads of one MAX ad (plan T-072): the revenue (dollars), the network, the adapter of the network
 * that loaded it (the funnel of AppsFlyer) and the networks tried (the network log). [adUnitId] only reaches the
 * revenue event, never a log line: [toString] leaves it out.
 */
internal class MaxAdInfo(
    val adUnitId: String,
    val revenueUsd: Double,
    val networkName: String?,
    val revenuePrecision: String?,
    val adapterClassName: String?,
    val networks: List<MaxNetworkAttempt>,
) {
    override fun toString(): String = "MaxAdInfo(network=$networkName, networks=${networks.size})"

    companion object {
        val EMPTY = MaxAdInfo("", 0.0, null, null, null, emptyList())
    }
}

/**
 * Reads [ad] once, each getter on its own: a getter that throws leaves its field empty and never breaks the callback
 * of the SDK (L-A21). The adapter is the one of the network the waterfall reports loaded. Null is [MaxAdInfo.EMPTY].
 */
internal fun maxAdInfoOf(ad: MaxAd?): MaxAdInfo {
    if (ad == null) return MaxAdInfo.EMPTY
    val waterfall = readOrNull { ad.waterfall }
    val loaded = waterfall?.let { info ->
        readOrNull { info.networkResponses }?.firstOrNull { readOrNull { it.adLoadState } == AD_LOADED }
    }
    return MaxAdInfo(
        adUnitId = readOrNull { ad.adUnitId }.orEmpty(),
        revenueUsd = readOrNull { ad.revenue }?.let(::maxRevenueValue) ?: 0.0,
        networkName = readOrNull { ad.networkName },
        revenuePrecision = readOrNull { ad.revenuePrecision },
        adapterClassName = loaded?.let { response -> readOrNull { response.mediatedNetwork?.adapterClassName } },
        networks = maxNetworkAttemptsOf(waterfall),
    )
}

/**
 * The networks of [waterfall], in the order of the SDK (KB-28): the name of each (its adapter class when the SDK
 * gives no name, `unknown` when neither), its latency, its result and its error code. Never reads the credentials (they
 * hold the ad units of the networks), a message, the load failure info or a `toString()`. Null is no network.
 */
internal fun maxNetworkAttemptsOf(waterfall: MaxAdWaterfallInfo?): List<MaxNetworkAttempt> =
    readOrNull { waterfall?.networkResponses }.orEmpty().map { response ->
        val network = readOrNull { response.mediatedNetwork }
        val name = readOrNull { network?.name }?.takeIf { it.isNotEmpty() }
            ?: readOrNull { network?.adapterClassName }?.takeIf { it.isNotEmpty() }
            ?: UNKNOWN_NETWORK
        val result = when (readOrNull { response.adLoadState }) {
            MaxNetworkResponseInfo.AdLoadState.AD_LOADED -> MaxNetworkResult.LOADED
            MaxNetworkResponseInfo.AdLoadState.FAILED_TO_LOAD -> MaxNetworkResult.FAILED
            MaxNetworkResponseInfo.AdLoadState.AD_LOAD_NOT_ATTEMPTED, null -> MaxNetworkResult.NOT_TRIED
        }
        val errorCode = if (result == MaxNetworkResult.FAILED) readOrNull { response.error?.code } else null
        MaxNetworkAttempt(name, readOrNull { response.latencyMillis } ?: 0L, result, errorCode)
    }

/**
 * One log line per network of [attempts]: `Network <key>: <name>, <ms> ms, <result>`, the result `loaded`,
 * `error <code>` or `not tried`; `Network <key>: none tried` when MAX tried none. The same lines as AdMob
 * ([networkResponseLines]); the key of MAX ends with `/max`.
 */
internal fun maxNetworkLines(key: String, attempts: List<MaxNetworkAttempt>): List<String> {
    if (attempts.isEmpty()) return listOf("Network $key: none tried")
    return attempts.map { attempt ->
        val result = when (attempt.result) {
            MaxNetworkResult.LOADED -> "loaded"
            MaxNetworkResult.FAILED -> "error ${attempt.errorCode}"
            MaxNetworkResult.NOT_TRIED -> "not tried"
        }
        "Network $key: ${attempt.networkName}, ${attempt.latencyMillis} ms, $result"
    }
}

/** T-03 for MAX: dollars as they are, never divided; a negative value or not a number is 0 (question 2 of the plan). */
internal fun maxRevenueValue(revenue: Double): Double = if (revenue.isFinite() && revenue >= 0.0) revenue else 0.0

/** [read], or null when the SDK throws: no getter of MAX may break its callback. */
private inline fun <T> readOrNull(read: () -> T?): T? = try {
    read()
} catch (e: Exception) {
    null
}

private const val UNKNOWN_NETWORK = "unknown"
private val AD_LOADED = MaxNetworkResponseInfo.AdLoadState.AD_LOADED
