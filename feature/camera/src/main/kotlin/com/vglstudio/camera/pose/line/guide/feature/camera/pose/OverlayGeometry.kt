package com.vglstudio.camera.pose.line.guide.feature.camera.pose

// Skeleton of step 3 (tests first): the constants are final, the functions do not compute yet.
const val MIN_SCALE = 0.4f
const val MAX_SCALE = 1.6f
const val MIN_OPACITY = 0.2f
const val MAX_OPACITY = 1f
const val DEFAULT_OPACITY = 1f

/** Where the overlay tool column goes: [x] from the left of the container, [y] from the top of the frame, in dp. */
data class ToolColumnPlacement(val outsideFrame: Boolean, val x: Float, val y: Float, val overflow: Float)

fun clampScale(value: Float): Float = value

fun clampOpacity(value: Float): Float = value

fun clampOffset(value: Float): Float = value

fun percent(value: Float): Int = value.toInt()

fun toolColumnPlacement(
    containerWidth: Float,
    frameWidth: Float,
    frameHeight: Float,
    stepBottom: Float,
): ToolColumnPlacement = ToolColumnPlacement(
    outsideFrame = false,
    x = (containerWidth - frameWidth) / 2 + frameWidth - 64f,
    y = 150f,
    overflow = 0f,
)
