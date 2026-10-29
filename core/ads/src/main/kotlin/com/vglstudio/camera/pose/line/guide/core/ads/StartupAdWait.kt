package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.delay

/** What [awaitStartupAd] reads at each tick; :core:ads binds it to [AdsController]. */
internal interface StartupAdProbe {
    fun nowMillis(): Long

    val consentFinished: Boolean

    val consentOpen: Boolean

    val sdkReady: Boolean

    /** The placement the keys choose now (re-read at every tick, L-061). */
    fun chosen(): AdPlacement?

    /** Reads only (F-04). */
    fun isReady(placement: AdPlacement): Boolean

    /** Loads if needed (F-04), records nothing (A-04). */
    fun ensureLoading(placement: AdPlacement)

    /** One `ad_opportunity` not_ready (A-06). */
    fun recordNotReady(placement: AdPlacement)
}

/** The limits of A-03, counted from the start of :core:ads, and the probe tick (question 4 of plan T-024). */
internal data class StartupAdLimits(
    val consentMillis: Long = 10_000,
    val sdkMillis: Long = 35_000,
    val adMillis: Long = 40_000,
    val tickMillis: Long = 250,
)

/**
 * Returns the placement to show, or null. Deadlines count from [startedAtMillis] (question 4). One tick runs at once,
 * the next ones every [StartupAdLimits.tickMillis]: the key off gives up at once (KB-13 criterion 5, L-061); a ready
 * ad shows at once, even past the deadlines; consent not finished in time or finished with the gate closed gives up
 * with nothing recorded; the SDK or the ad not ready in time records one `not_ready` opportunity (A-06). While it
 * waits for the ad it only probes and loads if needed (A-04). Every end is logged with its reason.
 */
internal suspend fun awaitStartupAd(
    probe: StartupAdProbe,
    startedAtMillis: Long,
    limits: StartupAdLimits = StartupAdLimits(),
    logger: AppLogger? = null,
): AdPlacement? {
    fun end(reason: String, result: AdPlacement?): AdPlacement? {
        logger?.info(STARTUP_TAG, "startup ad: $reason after ${probe.nowMillis() - startedAtMillis} ms")
        return result
    }
    while (true) {
        val placement = probe.chosen() ?: return end("key off", null)
        if (probe.isReady(placement)) return end("ready (${placement.key})", placement)
        val elapsed = probe.nowMillis() - startedAtMillis
        if (!probe.consentFinished) {
            if (elapsed >= limits.consentMillis) return end("consent timeout", null)
        } else if (!probe.consentOpen) {
            return end("consent closed", null)
        } else if (!probe.sdkReady) {
            if (elapsed >= limits.sdkMillis) {
                probe.recordNotReady(placement)
                return end("sdk timeout", null)
            }
        } else {
            if (elapsed >= limits.adMillis) {
                probe.recordNotReady(placement)
                return end("ad timeout", null)
            }
            probe.ensureLoading(placement)
        }
        delay(limits.tickMillis)
    }
}

private const val STARTUP_TAG = "Ads"
