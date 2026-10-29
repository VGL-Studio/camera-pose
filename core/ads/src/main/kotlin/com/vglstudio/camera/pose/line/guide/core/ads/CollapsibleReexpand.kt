package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * CL-03: when a collapsed layer opens again, on the monotonic clock: [expandAfterSeconds] (`time_expand_collap_ad`)
 * after the user collapsed it at [collapsedAtMillis]. Null when it never opens again: not collapsed, or 0 seconds
 * (a negative remote value reads as 0).
 */
internal fun collapsibleReexpandAtMillis(collapsedAtMillis: Long?, expandAfterSeconds: Long): Long? {
    if (collapsedAtMillis == null || expandAfterSeconds <= 0) return null
    return collapsedAtMillis + expandAfterSeconds * MILLIS_PER_SECOND
}

/** CL-03: the layer opens again once [reexpandAtMillis] is reached, and only while it has an ad. */
internal fun shouldReexpand(nowMillis: Long, reexpandAtMillis: Long?, hasAd: Boolean): Boolean =
    reexpandAtMillis != null && nowMillis >= reexpandAtMillis && hasAd

private const val MILLIS_PER_SECOND = 1_000L
