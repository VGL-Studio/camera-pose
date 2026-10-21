package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Policy guardrails for full-screen ads (ADR-0004), kept free of Android so it runs in unit tests.
 * Times are milliseconds of a monotonic clock.
 * - Frequency cap: at most one full-screen ad per minimum interval.
 * - Never while the user is interacting: wait until the user has not touched the screen for [quietPeriodMillis],
 *   so a tap meant for the app cannot land on the ad.
 */
internal class FullScreenAdGuard(private val quietPeriodMillis: Long = QUIET_PERIOD_MILLIS) {
    private var lastShownAt: Long? = null
    private var lastInteractionAt: Long? = null

    fun onUserInteraction(now: Long) {
        lastInteractionAt = now
    }

    fun onShown(now: Long) {
        lastShownAt = now
    }

    fun decide(now: Long, minIntervalMillis: Long): Decision {
        lastShownAt?.let { shownAt -> if (now - shownAt < minIntervalMillis) return Decision.TooSoon }
        lastInteractionAt?.let { touchedAt ->
            val quietFor = now - touchedAt
            if (quietFor < quietPeriodMillis) return Decision.Wait(quietPeriodMillis - quietFor)
        }
        return Decision.Show
    }

    sealed interface Decision {
        /** The last full-screen ad was too recent: skip this one. */
        data object TooSoon : Decision

        /** The user is interacting: check again after [millis]. */
        data class Wait(val millis: Long) : Decision

        data object Show : Decision
    }

    companion object {
        const val QUIET_PERIOD_MILLIS = 1_000L

        /** How long a full-screen ad may wait for the user to stop interacting before it is skipped. */
        const val MAX_WAIT_MILLIS = 3_000L
    }
}
