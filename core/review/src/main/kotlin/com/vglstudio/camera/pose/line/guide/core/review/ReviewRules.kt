package com.vglstudio.camera.pose.line.guide.core.review

private const val MILLIS_PER_DAY = 86_400_000L

/**
 * Whether the review dialog may be asked for now (spec camera, KB-10): enough photos saved, and either never asked
 * or the last request at least [minDaysBetween] days ago. A last request in the future (the user set the clock
 * back) counts as due, so the app is never stuck; the store limits how often the dialog really shows.
 */
internal fun isReviewDue(
    savedPhotos: Int,
    minSavedPhotos: Long,
    lastAskedAtMillis: Long?,
    nowMillis: Long,
    minDaysBetween: Long,
): Boolean {
    if (savedPhotos < minSavedPhotos) return false
    if (lastAskedAtMillis == null || lastAskedAtMillis > nowMillis) return true
    return nowMillis - lastAskedAtMillis >= minDaysBetween * MILLIS_PER_DAY
}
