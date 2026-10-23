package com.vglstudio.camera.pose.line.guide.feature.camera.review

/**
 * The order in which the review screen is left (spec camera, KB-9; plan T-018). A photo saved in this visit asks for
 * the full-screen ad first; [afterReviewAd] goes back once the ad request ends. Otherwise it goes back at once.
 */
internal fun leaveReview(
    savedThisVisit: Boolean,
    showAd: () -> Unit,
    goBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") askReview: () -> Unit = {},
) {
    if (savedThisVisit) showAd() else goBack()
}

/** The ad request ended, whether the ad was shown or skipped: back to the camera. */
@Suppress("UNUSED_PARAMETER")
internal fun afterReviewAd(shown: Boolean, goBack: () -> Unit, askReview: () -> Unit = {}) = goBack()
