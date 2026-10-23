package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Limits of the outline adjustment (spec camera, KB-3).
const val MIN_SCALE = 0.4f
const val MAX_SCALE = 1.6f
const val MIN_OPACITY = 0.2f
const val MAX_OPACITY = 1f
const val DEFAULT_OPACITY = 1f

/** The centre of the outline box stays inside the frame: offsets are parts of the frame from its centre. */
const val MAX_OFFSET = 0.5f

fun clampScale(value: Float): Float = value.coerceIn(MIN_SCALE, MAX_SCALE)

fun clampOpacity(value: Float): Float = value.coerceIn(MIN_OPACITY, MAX_OPACITY)

fun clampOffset(value: Float): Float = value.coerceIn(-MAX_OFFSET, MAX_OFFSET)

/** A part (0 to 2) as a whole percent: 0.704 is 70. */
fun percent(value: Float): Int = (value * 100).roundToInt()

/** Where the overlay tool column goes: [x] from the left of the container, [y] from the top of the frame, in dp. */
data class ToolColumnPlacement(
    /** True: in the free margin beside the frame. False: inside the frame, [x] still from the container. */
    val outsideFrame: Boolean,
    val x: Float,
    val y: Float,
    /** How far the column goes below the lowest place the frame allows for it, in dp. Only inside the frame. */
    val overflow: Float,
)

private const val COLUMN_WIDTH = 48f
private const val COLUMN_HEIGHT = 4 * 48f + 3 * 8f
private const val GAP = 16f
private const val SMALL_GAP = 8f
private const val DEFAULT_TOP = 150f
private const val ZOOM_HEIGHT = 48f
private const val NARROW_FRAME = 264f

/**
 * Places the four tools (spec camera, KB-3; plan T-015, "Chỗ đặt cột công cụ ở khung thấp"). All sizes in dp.
 * [containerWidth] is the width of the box that holds the frame, [stepBottom] the bottom of the step card in the
 * frame. The frame is centred in the container.
 *
 * 1. Margins of 64 or more on both sides: the column is in the right margin, centred there.
 * 2. Otherwise, if it fits in the frame (at 150 from the top, or higher, but not above the step card): there, 16 from
 *    the right edge. A narrow frame keeps the zoom control (48 and 8) clear of the column.
 * 3. Otherwise, with margins from 48: in the right margin, at the frame edge.
 * 4. Otherwise in the frame under the step card, going [ToolColumnPlacement.overflow] dp below the free area.
 */
fun toolColumnPlacement(
    containerWidth: Float,
    frameWidth: Float,
    frameHeight: Float,
    stepBottom: Float,
): ToolColumnPlacement {
    val margin = (containerWidth - frameWidth) / 2
    val outside = ToolColumnPlacement(
        outsideFrame = true,
        x = margin + frameWidth + (margin - COLUMN_WIDTH) / 2,
        y = min(DEFAULT_TOP, frameHeight - COLUMN_HEIGHT),
        overflow = 0f,
    )
    if (margin >= GAP + COLUMN_WIDTH) return outside

    val zoomRoom = if (frameWidth < NARROW_FRAME) ZOOM_HEIGHT + SMALL_GAP else 0f
    val lowestBottom = frameHeight - GAP - zoomRoom
    val highestTop = stepBottom + SMALL_GAP
    val top = max(min(DEFAULT_TOP, lowestBottom - COLUMN_HEIGHT), highestTop)
    val inFrameX = margin + frameWidth - GAP - COLUMN_WIDTH
    if (top + COLUMN_HEIGHT <= lowestBottom) return ToolColumnPlacement(false, inFrameX, top, 0f)

    if (margin >= COLUMN_WIDTH) return outside
    return ToolColumnPlacement(false, inFrameX, highestTop, highestTop + COLUMN_HEIGHT - lowestBottom)
}
