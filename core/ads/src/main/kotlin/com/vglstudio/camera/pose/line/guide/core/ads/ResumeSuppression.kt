package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * Whether the ads that show by themselves (resume ad, T-025) are blocked now (G-01 to G-05), on top of the counter
 * of [ForegroundAdSuppressor]: blocked while the leave mark is set (G-03, way 3), or from the moment a hold starts
 * until the app's root reports that no hold is left (G-02, G-04). Main thread only.
 */
internal class ResumeSuppression(private val suppressor: ForegroundAdSuppressor, private val logger: AppLogger) {
    /** The log source of the leave mark ([LEAVE_APP] or [AD_CLICK]), null when none is set. */
    private var leaveMark: String? = null
    private val _awaitingReport = MutableStateFlow(false)
    private val _resumeAdsSuppressed = MutableStateFlow(false)

    /** What resume ads (T-025) read. Raised at once when a hold starts (G-02), lowered only by the shell's report. */
    val resumeAdsSuppressed: StateFlow<Boolean> = _resumeAdsSuppressed.asStateFlow()

    /** True while a hold was seen and the shell has not reported it gone; the shell reads it to recompose. */
    val awaitingReport: StateFlow<Boolean> = _awaitingReport.asStateFlow()

    init {
        // Unconfined and undispatched: the collector runs inside acquire(), so a hold raises the block at once, even
        // for a source that holds the counter directly (the consent form of T-020).
        CoroutineScope(Dispatchers.Unconfined).launch(start = CoroutineStart.UNDISPATCHED) {
            suppressor.suppressed.collect { if (it) raise() }
        }
    }

    /** Starts a hold of [source] and logs it (G-03, ways 1 and 2). */
    fun hold(source: String): ForegroundAdSuppressor.Hold {
        val hold = suppressor.acquire(source)
        raise()
        logger.info(TAG, "Resume ads held: $source")
        return hold
    }

    /** Releases [hold] and logs it. */
    fun release(hold: ForegroundAdSuppressor.Hold) {
        hold.release()
        logger.info(TAG, "Resume ads released: ${hold.source}")
    }

    /** G-04: the root reads the suppressor in composition and reports here on the next recomposition. */
    fun onShellReported(suppressedInComposition: Boolean) {
        // A stale report never lowers a hold that is still on.
        _awaitingReport.value = suppressedInComposition || suppressor.suppressed.value
        update()
    }

    /** G-03, way 3: after leaving the app on purpose succeeded. */
    fun markLeavingApp() {
        setLeaveMark(LEAVE_APP)
    }

    /**
     * A-14: the user clicked an ad of any format ([AdTelemetry.clicked]), so the next return to the app shows no
     * resume ad. Same mark as [markLeavingApp], a flag and not a counter; released like it (G-05).
     */
    fun markAdClicked() {
        setLeaveMark(AD_CLICK)
    }

    /**
     * G-03 way 3, G-05: the next resume of an app activity releases the mark, whether or not the app went to
     * background; SDK activities are filtered out by AdsActivityTracker.
     */
    fun onActivityResumed() {
        val source = leaveMark ?: return
        leaveMark = null
        logger.info(TAG, "Resume ads released: $source")
        update()
    }

    private fun setLeaveMark(source: String) {
        leaveMark = source
        logger.info(TAG, "Resume ads held: $source")
        update()
    }

    private fun raise() {
        _awaitingReport.value = true
        update()
    }

    private fun update() {
        _resumeAdsSuppressed.value = leaveMark != null || _awaitingReport.value
    }

    companion object {
        /** Log sources of the holds (plan T-022, section 3.1, step 10). */
        const val SYSTEM_SCREEN = "system_screen"
        const val FULL_SCREEN_PAGE = "full_screen_page"
        const val LEAVE_APP = "leave_app"
        const val AD_CLICK = "ad_click"

        private const val TAG = "Ads"
    }
}
