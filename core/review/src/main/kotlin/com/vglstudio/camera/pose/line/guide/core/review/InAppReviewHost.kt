package com.vglstudio.camera.pose.line.guide.core.review

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.vglstudio.camera.pose.line.guide.core.ads.rememberStartAdHold

/** Provided by the app shell; absent in tests and previews, where asking does nothing. */
val LocalInAppReview = staticCompositionLocalOf<InAppReview?> { null }

/**
 * Returns the action that asks for a store review at a natural break, given the number of photos saved since
 * install. Does nothing without [LocalInAppReview] or an activity (tests, previews). The ads that show by themselves
 * are held from the request until the review flow is done, also after the screen that asked has left (plan T-074,
 * pass 2): the store's dialog is a screen of the system.
 */
@Composable
fun rememberRequestInAppReview(): (savedPhotos: Int) -> Unit =
    rememberRequestInAppReview(startHold = rememberStartAdHold())

/** [rememberRequestInAppReview] with the hold of :core:ads given as [startHold], for tests. */
@Composable
internal fun rememberRequestInAppReview(startHold: () -> (() -> Unit)): (savedPhotos: Int) -> Unit {
    val review = LocalInAppReview.current
    val activity = LocalActivity.current
    return { savedPhotos ->
        if (review != null && activity != null) {
            val release = startHold()
            try {
                review.requestIfDue(activity, savedPhotos, onDone = release)
            } catch (e: RuntimeException) {
                release()
                throw e
            }
        }
    }
}
