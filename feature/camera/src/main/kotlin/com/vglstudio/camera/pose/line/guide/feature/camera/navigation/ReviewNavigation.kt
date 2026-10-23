package com.vglstudio.camera.pose.line.guide.feature.camera.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewRoute
import kotlinx.serialization.Serializable

/**
 * Navigation destination of the review screen, put above the camera. Without [savedPhotoUri] it shows the photo just
 * taken; with it, the saved photo opened from the last photo slot of the camera (spec camera, KB-6).
 */
@Serializable
data class ReviewDestination(val savedPhotoUri: String? = null)

fun NavGraphBuilder.reviewScreen(onLeave: () -> Unit) {
    composable<ReviewDestination> {
        ReviewRoute(onLeave = onLeave)
    }
}
