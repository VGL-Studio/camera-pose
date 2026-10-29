package com.vglstudio.camera.pose.line.guide.core.ads

/** A-07: an app open ad expires once it is one hour old. */
internal const val APP_OPEN_MAX_AGE_MILLIS = 3_600_000L

/** A-08: the time after the close of an app open ad during which no other app open ad shows. */
internal const val APP_OPEN_LOCK_MILLIS = 1_000L

/** A-08: no app open within one second after the previous one closed; a negative gap counts as passed (AR-01). */
internal fun appOpenLockPassed(lastClosedAtMillis: Long?, nowMillis: Long): Boolean {
    if (lastClosedAtMillis == null) return true
    val gap = nowMillis - lastClosedAtMillis
    return gap < 0 || gap >= APP_OPEN_LOCK_MILLIS
}
