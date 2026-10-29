package com.vglstudio.camera.pose.line.guide.core.ads

/** What the full-screen gate decided for one turn (docs/ops/ads-business-rules.md, sections 3 and 8.2). */
internal enum class FullScreenShowDecision { SHOW, CONSENT_CLOSED, DISABLED, BUSY, NOT_READY, THROTTLED }

/** The order of the full-screen gate: consent, placement switch, busy (F-01), not ready, interval. */
internal fun decideFullScreenShow(
    consentOpen: Boolean,
    enabled: Boolean,
    busy: Boolean,
    ready: Boolean,
    intervalPassed: Boolean,
): FullScreenShowDecision = when {
    !consentOpen -> FullScreenShowDecision.CONSENT_CLOSED
    !enabled -> FullScreenShowDecision.DISABLED
    busy -> FullScreenShowDecision.BUSY
    !ready -> FullScreenShowDecision.NOT_READY
    !intervalPassed -> FullScreenShowDecision.THROTTLED
    else -> FullScreenShowDecision.SHOW
}
