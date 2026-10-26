package com.vglstudio.camera.pose.line.guide.ui

import android.app.Activity
import android.content.pm.ActivityInfo
import android.content.res.Configuration
import android.content.res.Resources
import android.os.Build
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

/**
 * Portrait on a phone; on a large screen the app follows the device (spec camera, KB-11; plan T-056). The screen is
 * large when any of [smallestWidthsDp] is: a portrait app letterboxed on a landscape inner screen may see only its
 * own small window, while another source still reports the real size.
 */
internal fun screenOrientationFor(smallestWidthsDp: List<Int>): Int {
    val large = smallestWidthsDp.any {
        it != Configuration.SMALLEST_SCREEN_WIDTH_DP_UNDEFINED && it >= LARGE_SCREEN_MIN_SMALLEST_WIDTH_DP
    }
    return if (large) ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED else ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
}

/** The shorter side of a panel of [widthPx] x [heightPx] at [densityDpi], in dp; 0 when unknown. */
internal fun physicalSmallestWidthDp(widthPx: Int, heightPx: Int, densityDpi: Int): Int =
    if (widthPx <= 0 || heightPx <= 0 || densityDpi <= 0) {
        0
    } else {
        minOf(widthPx, heightPx) * DisplayMetrics.DENSITY_DEFAULT / densityDpi
    }

/**
 * The four sources of [SmallestWidths]. The panel is not scaled by the compatibility mode of a letterboxed app;
 * `wm size` on an emulator does not change it either.
 */
internal fun Activity.screenSmallestWidthsDp(
    stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE,
): SmallestWidths {
    var window = 0
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
        val bounds = windowManager.maximumWindowMetrics.bounds
        if (bounds.width() > 0 && bounds.height() > 0) {
            window = (minOf(bounds.width(), bounds.height()) / resources.displayMetrics.density).toInt()
        }
    }
    // The display of the activity's window; Context.getDisplay() throws where a context has no display.
    @Suppress("DEPRECATION")
    val mode = runCatching { windowManager.defaultDisplay.mode }.getOrNull()
    val panel = mode?.let { physicalSmallestWidthDp(it.physicalWidth, it.physicalHeight, stableDensityDpi) } ?: 0
    return SmallestWidths(
        activity = resources.configuration.smallestScreenWidthDp,
        system = Resources.getSystem().configuration.smallestScreenWidthDp,
        window = window,
        panel = panel,
    )
}

/**
 * Applies [screenOrientationFor] to this activity and logs the sources and the choice (ADR-0019), so a test session
 * shows which source saw which size. The manifest keeps portrait, so a phone starts without a relaunch; only a large
 * screen changes it, once. The request is kept across a relaunch, so it is set only when it differs.
 * [stableDensityDpi] is for tests: Robolectric reports 160 whatever the density of the qualifiers.
 */
fun Activity.applyScreenOrientation(logger: AppLogger, stableDensityDpi: Int = DisplayMetrics.DENSITY_DEVICE_STABLE) {
    val widths = screenSmallestWidthsDp(stableDensityDpi)
    val orientation = screenOrientationFor(widths.all())
    val name = if (orientation == ActivityInfo.SCREEN_ORIENTATION_PORTRAIT) "portrait" else "unspecified"
    val changed = requestedOrientation != orientation
    if (changed) requestedOrientation = orientation
    logger.info(ORIENTATION_LOG_TAG, "Orientation $name, changed $changed (smallest width dp: $widths)")
}
