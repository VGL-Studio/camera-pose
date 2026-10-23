package com.vglstudio.camera.pose.line.guide.feature.camera.review

/**
 * The order in which the review screen is left (spec camera, KB-9, KB-10; plans T-018, T-019). A photo saved in this
 * visit asks for the full-screen ad first; [afterReviewAd] goes back once the ad request ends. Otherwise it goes back
 * at once and does not ask for a store review: only a visit that saved a photo does ([askReview] is for
 * [afterReviewAd]).
 */
internal fun leaveReview(
    savedThisVisit: Boolean,
    showAd: () -> Unit,
    goBack: () -> Unit,
    @Suppress("UNUSED_PARAMETER") askReview: () -> Unit = {},
) {
    if (savedThisVisit) showAd() else goBack()
}

/**
 * The ad request ended, whether the ad was shown or skipped: back to the camera. The store review is asked for
 * after that, on the camera, and not when the ad was shown in the same exit.
 */
internal fun afterReviewAd(shown: Boolean, goBack: () -> Unit, askReview: () -> Unit = {}) {
    goBack()
    if (!shown) askReview()
}
