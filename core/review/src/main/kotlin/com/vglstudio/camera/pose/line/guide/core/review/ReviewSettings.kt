package com.vglstudio.camera.pose.line.guide.core.review

/**
 * Remote Config keys of the review module (ADR-0003) and their values before any remote value arrives.
 * The defaults are business decisions of spec camera, KB-10.
 */
internal object ReviewSettings {
    /** Photos saved since install before the review dialog may be asked for. */
    const val MIN_SAVED_PHOTOS = "in_app_review_min_saved_photos"
    const val MIN_SAVED_PHOTOS_DEFAULT = 3L

    /** Days between two requests of the review dialog. */
    const val MIN_DAYS_BETWEEN = "in_app_review_min_days_between"
    const val MIN_DAYS_BETWEEN_DEFAULT = 30L
}
