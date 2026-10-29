package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** What [ResumeAdTurns] reads and does through [AdsController]; tests pass a fake. */
internal interface ResumeAdPort {
    /** The gate read now, with firstForeground false ([ResumeAdTurns] owns that flag). */
    fun readGate(): ResumeGate

    /** `load_state` of [placement], taken now (section 8.2). */
    fun loadStateOf(placement: AdPlacement): String

    fun recordOpportunity(placement: AdPlacement, result: String, loadState: String)

    /** Loads unless a load runs, an ad is there or a retry waits (F-04). */
    fun loadIfNeeded(placement: AdPlacement)

    /** [resumeAdPlacement] of the flags read now (checked again right before showing, L-061). */
    fun placementNow(): AdPlacement?

    /** The resume block read now (checked again right before showing). */
    fun suppressedNow(): Boolean

    fun showAppOpen(activity: Activity, placement: AdPlacement)

    fun showInterstitial(activity: Activity, placement: AdPlacement, combo: Boolean)
}

/**
 * The resume ad (KB-14, A-10 to A-15, plan T-025): decided each time the process comes to foreground, shown on the
 * next resume of an app activity, at most once per foreground. Main thread only.
 */
internal class ResumeAdTurns(private val port: ResumeAdPort, private val logger: AppLogger) {
    private var seenForeground = false

    /** The placement decided at the last foreground, waiting for an app activity to resume. */
    private var pending: AdPlacement? = null

    fun onProcessForegroundChanged(foreground: Boolean) {
        if (!foreground) {
            if (pending != null) logger.info(TAG, "Resume ad dropped: background")
            pending = null
            return
        }
        val gate = port.readGate().copy(firstForeground = !seenForeground)
        // Used up on every branch, served or not (A-10).
        seenForeground = true
        val decision = decideResumeAd(gate)
        val placement = gate.placement
        logger.info(TAG, "Resume ad $decision: ${placement?.key ?: "none"}")
        when (decision) {
            ResumeDecision.SHOW -> pending = placement
            ResumeDecision.SUPPRESSED, ResumeDecision.BUSY, ResumeDecision.THROTTLED, ResumeDecision.NOT_READY -> {
                // A switched-off resume is no opportunity (L-A05); load_state is taken before the load (8.2).
                if (placement == null) return
                val result = decision.opportunityResult() ?: return
                port.recordOpportunity(placement, result, port.loadStateOf(placement))
                // Blocked at an earlier gate: no load (A-13).
                if (decision == ResumeDecision.NOT_READY) port.loadIfNeeded(placement)
            }
            ResumeDecision.FIRST_FOREGROUND,
            ResumeDecision.STARTUP,
            ResumeDecision.NOT_SERVING,
            ResumeDecision.OFF,
            -> {}
        }
    }

    fun onAppActivityResumed(activity: Activity) {
        val placement = pending ?: return
        pending = null
        // A placement switched off or changed since the decision is no opportunity, even while blocked (L-A05).
        if (port.placementNow() != placement) {
            logger.info(TAG, "Resume ad dropped: switched")
            return
        }
        if (port.suppressedNow()) {
            logger.info(TAG, "Resume ad ${ResumeDecision.SUPPRESSED} on resume: ${placement.key}")
            val result = ResumeDecision.SUPPRESSED.opportunityResult() ?: return
            port.recordOpportunity(placement, result, port.loadStateOf(placement))
            return
        }
        logger.info(TAG, "Resume ad shown on resume: ${placement.key}")
        when (placement.format) {
            AdFormat.APP_OPEN -> port.showAppOpen(activity, placement)
            AdFormat.INTERSTITIAL -> port.showInterstitial(activity, placement, combo = true)
            else -> logger.warn(TAG, "Resume ad ${placement.key}: format not shown")
        }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
