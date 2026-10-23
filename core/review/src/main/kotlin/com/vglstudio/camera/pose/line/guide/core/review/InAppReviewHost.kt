package com.vglstudio.camera.pose.line.guide.core.review

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Provided by the app shell; absent in tests and previews, where asking does nothing. */
val LocalInAppReview = staticCompositionLocalOf<InAppReview?> { null }

/**
 * Returns the action that asks for a store review at a natural break, given the number of photos saved since
 * install. Does nothing without [LocalInAppReview] or an activity (tests, previews).
 */
@Composable
fun rememberRequestInAppReview(): (savedPhotos: Int) -> Unit {
    val review = LocalInAppReview.current
    val activity = LocalActivity.current
    return { savedPhotos ->
        if (review != null && activity != null) review.requestIfDue(activity, savedPhotos)
    }
}
