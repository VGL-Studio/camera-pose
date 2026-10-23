package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.ui.Modifier

// Skeleton of step 3 (tests first): no gesture is detected yet.
internal fun Modifier.overlayGestures(
    enabled: Boolean,
    onTransform: (panX: Float, panY: Float, zoom: Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = this
