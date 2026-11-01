package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged

private const val TAG = "Ads"

/**
 * KB-27, ADR-0036 decision 1, question 36 of the ads spec: an ad inside a screen (banner, native small, collapsible
 * layer, pool cells) may send, refresh or retry only while its screen [shown] and no full-screen ad drawn over the app
 * [covered] it (the showing flag of the full-screen gate, F-01). The one condition every send path reads (L-072).
 */
internal fun inScreenAdsMayRun(shown: Boolean, covered: Boolean): Boolean = shown && !covered

/** [inScreenAdsMayRun] of two flows, for the banner of one screen: its lifecycle [visible] and [covered]. */
internal fun inScreenAdWindowOf(visible: Flow<Boolean>, covered: Flow<Boolean>): Flow<Boolean> =
    combine(visible, covered, ::inScreenAdsMayRun).distinctUntilChanged()

/**
 * The window of the in-screen ads of the process: [open] is [inScreenAdsMayRun] of the last [onForeground] and
 * [onCovered], set on the calling stack (no coroutine between a flag and the window). Logs when a cover pauses or
 * resumes the in-screen ads. Main thread only.
 */
internal class InScreenAdWindow(private val logger: AppLogger) {
    private var foreground = false
    private var covered = false
    private val _open = MutableStateFlow(false)
    val open: StateFlow<Boolean> = _open.asStateFlow()

    fun onForeground(value: Boolean) {
        foreground = value
        update()
    }

    fun onCovered(value: Boolean) {
        if (value == covered) return
        covered = value
        val message = if (value) {
            "In-screen ads paused (full-screen ad on screen)"
        } else {
            "In-screen ads resumed (full-screen ad closed)"
        }
        logger.info(TAG, message)
        update()
    }

    private fun update() {
        _open.value = inScreenAdsMayRun(foreground, covered)
    }
}
