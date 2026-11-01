package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.RequestConfiguration
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/**
 * Debug settings of the ads SDK, bound in :app per build flavor (plan T-058, KB-28), like [ConsentDebugOptions].
 * Staging uses [staging]: the developer's own phones as AdMob test devices (key `ads.testDeviceIds` of the developer's
 * local.properties, never in git) and one log line per network of each load ([AdNetworkLog]). Production uses [NONE].
 */
data class AdsDebugOptions(val testDeviceIds: List<String>, val logNetworkResponses: Boolean) {
    companion object {
        /** No test device, no per-network log. */
        val NONE = AdsDebugOptions(emptyList(), false)

        /** The test devices of [rawTestDeviceIds] (ids separated by commas, may be empty) and the per-network log. */
        fun staging(rawTestDeviceIds: String) =
            AdsDebugOptions(testDeviceIdsOf(rawTestDeviceIds), logNetworkResponses = true)
    }
}

/** The ids of [raw], separated by commas: trimmed, blank ones and repeats dropped, in their order. */
internal fun testDeviceIdsOf(raw: String): List<String> =
    raw.split(',').map { it.trim() }.filter { it.isNotEmpty() }.distinct()

/**
 * [base] with the test devices [ids], keeping its other fields so a later setting is not overwritten; null when there
 * is no id, so nothing is set.
 */
internal fun requestConfigurationWithTestDevices(base: RequestConfiguration, ids: List<String>): RequestConfiguration? =
    if (ids.isEmpty()) null else base.toBuilder().setTestDeviceIds(ids).build()

/**
 * Sets the test devices of [options] on the SDK ([current] and [set] are those of `MobileAds`), before it starts.
 * Nothing is read nor set without a device. Logs the number of ids only, never an id (ADR-0019).
 */
internal fun applyTestDevices(
    options: AdsDebugOptions,
    current: () -> RequestConfiguration,
    set: (RequestConfiguration) -> Unit,
    logger: AppLogger,
) {
    if (options.testDeviceIds.isEmpty()) return
    val configuration = requestConfigurationWithTestDevices(current(), options.testDeviceIds) ?: return
    set(configuration)
    logger.info(TAG, "Test devices set: ${options.testDeviceIds.size}")
}

private const val TAG = "Ads"
