package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * T-02 for one native ad: 2 seconds after its impression with no paid event, one zero-value impression is logged
 * instead. A paid event first cancels the fallback; the ad is logged once whichever comes first. Banners have none.
 */
internal class NativeRevenueFallback {
    private val lock = Any()
    private var logged = false
    private var dueAtMillis: Long? = null

    /** Returns when the fallback falls due. */
    fun onImpression(now: Long): Long = synchronized(lock) {
        val due = now + FALLBACK_DELAY_MILLIS
        if (!logged) dueAtMillis = due
        due
    }

    /** True when this paid event is logged (the first one); cancels the fallback. */
    fun onPaidEvent(): Boolean = synchronized(lock) {
        dueAtMillis = null
        if (logged) return false
        logged = true
        true
    }

    /** True when the zero-value impression must be logged now. */
    fun onDue(now: Long): Boolean = synchronized(lock) {
        val due = dueAtMillis ?: return false
        if (logged || now < due) return false
        logged = true
        dueAtMillis = null
        true
    }

    companion object {
        const val FALLBACK_DELAY_MILLIS = 2_000L
    }
}
