package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.ResponseInfo
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** One network the SDK tried for a load: its adapter class, its latency and its error code (null when none). */
internal data class NetworkResponse(val adapterClassName: String, val latencyMillis: Long, val errorCode: Int?)

/**
 * The networks of [info], in the order of the SDK. Reads only the adapter class, the latency and the error code of
 * each: never the credentials (they hold the ad units of the networks), the names of the ad sources, a `toString()` or
 * the message of an error (ads-business-rules.md, section 8).
 */
internal fun networkResponsesOf(info: ResponseInfo?): List<NetworkResponse> =
    info?.adapterResponses.orEmpty().map { response ->
        NetworkResponse(response.adapterClassName, response.latencyMillis, response.adError?.code)
    }

/**
 * One log line per network of [responses] (plan T-058, KB-28, answer 40):
 * `Network <key>: <adapter class>, <ms> ms, <result>`, where the result is `loaded` for [loadedAdapterClassName],
 * `error <code>` for a network that failed and `no error` for one that did not fail and was not chosen.
 * `Network <key>: none tried` when the SDK tried no network.
 */
internal fun networkResponseLines(
    key: String,
    responses: List<NetworkResponse>,
    loadedAdapterClassName: String?,
): List<String> {
    if (responses.isEmpty()) return listOf("Network $key: none tried")
    return responses.map { response ->
        val result = when {
            response.errorCode != null -> "error ${response.errorCode}"
            response.adapterClassName == loadedAdapterClassName -> "loaded"
            else -> "no error"
        }
        "Network $key: ${response.adapterClassName}, ${response.latencyMillis} ms, $result"
    }
}

/**
 * The per-network log of every load the SDK answers, on at staging only ([AdsDebugOptions.logNetworkResponses]).
 * [key] is the key the load already logs under (placement, slot or `pool`). Off, it reads and writes nothing.
 * Level `info`, like the adapter status of T-015 (ADR-0019).
 */
internal class AdNetworkLog(private val enabled: Boolean, private val logger: AppLogger) {
    fun loaded(key: String, info: ResponseInfo?) {
        if (!enabled) return
        write(networkResponseLines(key, networkResponsesOf(info), info?.loadedAdapterResponseInfo?.adapterClassName))
    }

    /** Reads the response of [error] only, never its message, which may hold an ad unit. */
    fun failed(key: String, error: LoadAdError) {
        if (!enabled) return
        write(networkResponseLines(key, networkResponsesOf(error.responseInfo), loadedAdapterClassName = null))
    }

    private fun write(lines: List<String>) = lines.forEach { logger.info(TAG, it) }

    companion object {
        /** Writes nothing: production, and every loader built without a log. */
        val OFF = AdNetworkLog(false, NoLogger)
    }
}

private object NoLogger : AppLogger {
    override fun debug(tag: String, message: String) = Unit

    override fun info(tag: String, message: String) = Unit

    override fun warn(tag: String, message: String, error: Throwable?) = Unit

    override fun error(tag: String, message: String, error: Throwable?) = Unit
}

private const val TAG = "Ads"
