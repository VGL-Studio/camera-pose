package com.vglstudio.camera.pose.line.guide.core.ads

/** `load_attempt` and `latency_ms` of one load result. */
internal data class LoadOutcome(val attempt: Int, val latencyMs: Long?)

/**
 * Failures in a row and the send time of the pending request, per slot (a placement code, or `preload:<format>`).
 * State of the loading path, not of the metrics: updated on every request and result whatever the metric switches
 * say, so the retry wait of T-021 ([failuresInARow], F-05, O-05) never stalls when the metrics are off.
 */
internal class LoadAttemptCounter {
    private val lock = Any()
    private val failures = mutableMapOf<String, Int>()
    private val sentAt = mutableMapOf<String, Long>()

    fun onRequestSent(slot: String, now: Long) {
        synchronized(lock) { sentAt[slot] = now }
    }

    /** `attempt` counts the failures before it; the count goes back to 0. */
    fun onLoaded(slot: String, now: Long): LoadOutcome = synchronized(lock) {
        val outcome = outcome(slot, now)
        failures -= slot
        outcome
    }

    fun onFailed(slot: String, now: Long): LoadOutcome = synchronized(lock) {
        val outcome = outcome(slot, now)
        failures[slot] = outcome.attempt
        outcome
    }

    fun failuresInARow(slot: String): Int = synchronized(lock) { failures[slot] ?: 0 }

    /** The latency is only for the first result of a request sent by :core:ads (not a banner refresh, A 06:157). */
    private fun outcome(slot: String, now: Long): LoadOutcome {
        val sent = sentAt.remove(slot)
        return LoadOutcome(attempt = (failures[slot] ?: 0) + 1, latencyMs = sent?.let { (now - it).coerceAtLeast(0) })
    }
}
