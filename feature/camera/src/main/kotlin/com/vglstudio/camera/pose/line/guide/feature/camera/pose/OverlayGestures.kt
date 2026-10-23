package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculateCentroidSize
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.input.pointer.PointerEventPass
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChanged
import kotlin.math.abs

/**
 * Drag with one finger and pinch with two on the area (spec camera, KB-3). [onTransform] gets the move as parts of the
 * area's size and the zoom factor since the last call; [onEnd] is called once when the last finger lifts, when the
 * gesture moved. A touch that stays within the touch slop is a tap and reports nothing. Place it under the controls
 * of the frame, so a touch on a control is not a drag.
 */
internal fun Modifier.overlayGestures(
    enabled: Boolean,
    onTransform: (panX: Float, panY: Float, zoom: Float) -> Unit,
    onEnd: () -> Unit,
): Modifier = if (!enabled) {
    this
} else {
    pointerInput(onTransform, onEnd) {
        awaitEachGesture {
            awaitFirstDown(requireUnconsumed = false)
            var pastSlop = false
            var totalZoom = 1f
            var totalPan = Offset.Zero
            do {
                val event = awaitPointerEvent(PointerEventPass.Main)
                val consumed = event.changes.any { it.isConsumed }
                if (!consumed) {
                    val zoom = event.calculateZoom()
                    val pan = event.calculatePan()
                    if (!pastSlop) {
                        totalZoom *= zoom
                        totalPan += pan
                        val zoomMotion = abs(1 - totalZoom) * event.calculateCentroidSize(useCurrent = false)
                        val slop = viewConfiguration.touchSlop
                        if (zoomMotion > slop || totalPan.getDistance() > slop) {
                            pastSlop = true
                            // Report everything since the touch began, so the slop does not eat the start of the move.
                            onTransform(totalPan.x / size.width, totalPan.y / size.height, totalZoom)
                            event.changes.forEach { if (it.positionChanged()) it.consume() }
                        }
                    } else if (zoom != 1f || pan != Offset.Zero) {
                        onTransform(pan.x / size.width, pan.y / size.height, zoom)
                        event.changes.forEach { if (it.positionChanged()) it.consume() }
                    }
                }
            } while (!consumed && event.changes.any { it.pressed })
            if (pastSlop) onEnd()
        }
    }
}
