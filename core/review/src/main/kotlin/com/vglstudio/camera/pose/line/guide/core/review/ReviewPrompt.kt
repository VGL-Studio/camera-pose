package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import com.google.android.play.core.review.ReviewManager

/** Shows the store review dialog. */
internal fun interface ReviewPrompt {
    suspend fun show(activity: Activity)
}

/** Placeholder until the code step (plan T-019). */
@Suppress("UNUSED_PARAMETER", "UnusedPrivateProperty")
internal class PlayReviewPrompt(private val manager: ReviewManager) : ReviewPrompt {
    override suspend fun show(activity: Activity) {
        TODO("plan T-019")
    }
}
