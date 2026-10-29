package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState

/**
 * `af_native_ad_eligible` (section 8.3; plan T-033): a native slot that passed the gate of the app ([allowed]) came
 * into the composition. Once each time it comes in: a recomposition does not count; leaving and coming back, or
 * [allowed] turning true, does. Preloads never count. Called by the shared gates of native slots
 * ([NativeAdSlotGate], [NativeFullScreenStepGate], [InListAdRow], [CollapsibleNativeAdLayer]); NativeEligibleCoverageTest
 * fails when a file draws a native ad without one. The event has no param, so any native format names it.
 */
@Composable
internal fun ReportNativeEligible(allowed: Boolean) {
    val ads = LocalAdsController.current
    NativeEligibleEffect(allowed) { ads?.telemetry?.eligible(AdFormat.NATIVE_SMALL) }
}

/** [ReportNativeEligible] without an ads controller: runs [report] once each time [allowed] comes into the screen. */
@Composable
internal fun NativeEligibleEffect(allowed: Boolean, report: () -> Unit) {
    val currentReport by rememberUpdatedState(report)
    LaunchedEffect(allowed) {
        if (allowed) currentReport()
    }
}
