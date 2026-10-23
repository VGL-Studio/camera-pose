package com.vglstudio.camera.pose.line.guide.core.review

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

/** Provided by the app shell; absent in tests and previews, where asking does nothing. */
val LocalInAppReview = staticCompositionLocalOf<InAppReview?> { null }

/** Placeholder until the code step (plan T-019). */
@Composable
fun rememberRequestInAppReview(): (savedPhotos: Int) -> Unit = {}
