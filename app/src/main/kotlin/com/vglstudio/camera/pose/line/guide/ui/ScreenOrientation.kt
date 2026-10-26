package com.vglstudio.camera.pose.line.guide.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.util.DisplayMetrics
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** From this smallest width the screen is large (an unfolded foldable, a tablet): the Material "compact" limit. */
internal const val LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP = 600

internal const val ORIENTATION_LOG_TAG = "Orientation"

/**
 * The smallest width in dp from four sources, 0 when a source is unknown: the activity's configuration, the system
 * configuration, the largest window (Android 11 and later) and the physical panel at its stable density.
 */
internal data class SmallestWidths(val activity: Int, val system: Int, val window: Int, val panel: Int) {
    fun all(): List<Int> = listOf(activity, system, window, panel)

    override fun toString() = "activity $activity, system $system, window $window, panel $panel"
}

// Skeleton of step 3 (plan T-056): the tests compile and fail on their checks.
@Suppress("UNUSED_PARAMETER")
internal fun screenOrientationFor(smallestWidthsDp: List<Int>): Int = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT

@Suppress("UNUSED_PARAMETER")
internal fun physicalSmallestWidthDp(widthPx: Int, heightPx: Int, densityDpi: Int): Int = 0

@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
internal fun Activity.screenSmallestWidthsDp(
    stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE,
): SmallestWidths = SmallestWidths(0, 0, 0, 0)

@Suppress("UnusedReceiverParameter", "UNUSED_PARAMETER")
fun Activity.applyScreenOrientation(logger: AppLogger, stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE) {
}
