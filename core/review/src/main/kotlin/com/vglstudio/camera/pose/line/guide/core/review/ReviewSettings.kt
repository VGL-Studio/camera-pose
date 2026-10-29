package com.vglstudio.camera.pose.line.guide.core.review

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/**
 * Remote Config keys of the review module (ADR-0003) and their values before any remote value arrives.
 * The defaults are business decisions of spec camera, KB-10. Public only for the list of the app's keys
 * (AppRemoteKeys of :app, ADR-0023).
 */
object ReviewSettings {
    /** Photos saved since install before the review dialog may be asked for. */
    internal const val MIN_SAVED_PHOTOS = "in_app_review_min_saved_photos"
    internal const val MIN_SAVED_PHOTOS_DEFAULT = 3L

    /** Days between two requests of the review dialog. */
    internal const val MIN_DAYS_BETWEEN = "in_app_review_min_days_between"
    internal const val MIN_DAYS_BETWEEN_DEFAULT = 30L

    /** The key of [MIN_SAVED_PHOTOS] (ADR-0023); name kept from before the naming rule (no unit suffix). */
    internal val MIN_SAVED_PHOTOS_KEY: RemoteKey<Long> = RemoteKey.long(
        MIN_SAVED_PHOTOS,
        MIN_SAVED_PHOTOS_DEFAULT,
        "Photos saved since install before the Play review dialog may be asked for.",
    )

    /** The key of [MIN_DAYS_BETWEEN] (ADR-0023); name kept from before the naming rule (no unit suffix). */
    internal val MIN_DAYS_BETWEEN_KEY: RemoteKey<Long> = RemoteKey.long(
        MIN_DAYS_BETWEEN,
        MIN_DAYS_BETWEEN_DEFAULT,
        "Days between two requests of the Play review dialog.",
    )

    /** Every key of the review module. */
    fun all(): List<RemoteKey<*>> = listOf(MIN_SAVED_PHOTOS_KEY, MIN_DAYS_BETWEEN_KEY)
}
