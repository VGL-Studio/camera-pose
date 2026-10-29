package com.vglstudio.camera.pose.line.guide.core.ads

/** What the resume gate decides when the process comes to foreground (A-10, A-11, plan T-025). */
internal enum class ResumeDecision {
    FIRST_FOREGROUND,
    STARTUP,
    NOT_SERVING,
    SUPPRESSED,
    OFF,
    BUSY,
    THROTTLED,
    NOT_READY,
    SHOW,
}

/**
 * What the resume gate reads when the process comes to foreground. [placement] is [resumeAdPlacement], null when
 * off. No time spent in background and no `ads_interval`: the resume ad has no time gate but the app open lock (A-11,
 * AS-01).
 */
internal data class ResumeGate(
    /** The first foreground of this process (A-10). */
    val firstForeground: Boolean,
    /** The startup ad turn of the loading screen is over (question 7 of plan T-025). */
    val startupFinished: Boolean,
    /** The consent gate is open and the SDK started ([AdsController.canRequestAds]). */
    val serving: Boolean,
    /** [ResumeSuppression.resumeAdsSuppressed]. */
    val suppressed: Boolean,
    val placement: AdPlacement?,
    /** [FullScreenAdPresence.isBusy] now. */
    val busy: Boolean,
    /** One second passed since the last app open ad closed ([appOpenLockPassed], A-08). */
    val appOpenLockPassed: Boolean,
    /** A full-screen ad of [placement] is ready; false when [placement] is null. */
    val ready: Boolean,
)

/** A-10, A-11: first foreground, not served, blocked, switched off, full-screen ad on screen, app open lock, no ad. */
internal fun decideResumeAd(gate: ResumeGate): ResumeDecision = when {
    gate.firstForeground -> ResumeDecision.FIRST_FOREGROUND
    !gate.startupFinished -> ResumeDecision.STARTUP
    !gate.serving -> ResumeDecision.NOT_SERVING
    gate.suppressed -> ResumeDecision.SUPPRESSED
    gate.placement == null -> ResumeDecision.OFF
    gate.busy -> ResumeDecision.BUSY
    gate.placement.format == AdFormat.APP_OPEN && !gate.appOpenLockPassed -> ResumeDecision.THROTTLED
    !gate.ready -> ResumeDecision.NOT_READY
    else -> ResumeDecision.SHOW
}

/**
 * `opportunity_result` of a blocked resume (section 8.2); null when the turn is no opportunity (first foreground,
 * startup, not served, off: L-A05) or shows (the show function records `shown`).
 */
internal fun ResumeDecision.opportunityResult(): String? = when (this) {
    ResumeDecision.SUPPRESSED -> "suppressed"
    ResumeDecision.BUSY -> "busy"
    ResumeDecision.THROTTLED -> "throttled"
    ResumeDecision.NOT_READY -> "not_ready"
    ResumeDecision.FIRST_FOREGROUND,
    ResumeDecision.STARTUP,
    ResumeDecision.NOT_SERVING,
    ResumeDecision.OFF,
    ResumeDecision.SHOW,
    -> null
}
