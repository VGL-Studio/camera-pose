package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.google.android.play.core.review.ReviewManager

/** Shows the store review dialog. The store decides whether it really shows. */
internal fun interface ReviewPrompt {
    suspend fun show(activity: Activity)
}

internal class PlayReviewPrompt(private val manager: ReviewManager) : ReviewPrompt {
    override suspend fun show(activity: Activity) {
        val info = manager.requestReview()
        manager.launchReview(activity, info)
    }
}
