package com.vglstudio.camera.pose.line.guide.core.ads

import com.applovin.mediation.MaxErrorCode
import com.vglstudio.camera.pose.line.guide.core.analytics.AdFunnelKind

/**
 * The pure mappings of the ad metrics (docs/ops/ads-business-rules.md, sections 8.1 and 8.2; plan T-023). The code
 * that listens to the SDK reads its data and calls [AdTelemetry]; every decision of a metric is here.
 */

/** The two groups of lifecycle metrics, each with its own switch (`is_log_ad_fullscreen_events`, `..._slot_...`). */
internal enum class MetricGroup { FULL_SCREEN, SLOT }

/** Format code of T-04. Exhaustive on purpose, with no `else`: a new [AdFormat] must choose its code. */
internal fun AdFormat.metricCode(): String = when (this) {
    AdFormat.INTERSTITIAL -> "INTER"
    AdFormat.REWARDED -> "REWARDED"
    AdFormat.APP_OPEN -> "APP_OPEN"
    AdFormat.BANNER -> "BANNER"
    AdFormat.BANNER_COLLAPSIBLE -> "BANNER"
    AdFormat.BANNER_MREC -> "BANNER_MREC"
    AdFormat.NATIVE_FULL_SCREEN -> "NATIVE_FULL"
    AdFormat.NATIVE_COLLAPSIBLE -> "NATIVE_COLLAP"
    AdFormat.NATIVE_SMALL -> "NATIVE_SMALL"
    AdFormat.NATIVE_STEP -> "NATIVE_ONBOARDING"
    AdFormat.NATIVE_IN_COLLECTION -> "NATIVE_CONTENT"
    AdFormat.NATIVE_MEDIUM -> "NATIVE_MEDIUM"
}

/**
 * The AppsFlyer funnel of a format (section 8.3): interstitial, rewarded and every native format; app open and
 * banners have none. Exhaustive on purpose, with no `else`: a new [AdFormat] must choose.
 */
internal fun AdFormat.funnelKind(): AdFunnelKind? = when (this) {
    AdFormat.INTERSTITIAL -> AdFunnelKind.INTERS
    AdFormat.REWARDED -> AdFunnelKind.REWARDED
    AdFormat.NATIVE_FULL_SCREEN,
    AdFormat.NATIVE_COLLAPSIBLE,
    AdFormat.NATIVE_SMALL,
    AdFormat.NATIVE_STEP,
    AdFormat.NATIVE_IN_COLLECTION,
    AdFormat.NATIVE_MEDIUM,
    -> AdFunnelKind.NATIVE
    AdFormat.APP_OPEN, AdFormat.BANNER, AdFormat.BANNER_COLLAPSIBLE, AdFormat.BANNER_MREC -> null
}

/** Full screen: `INTER`, `REWARDED`, `APP_OPEN`, `NATIVE_FULL`; every other format is a slot (section 8.2). */
internal fun AdFormat.metricGroup(): MetricGroup = when (this) {
    AdFormat.INTERSTITIAL, AdFormat.REWARDED, AdFormat.APP_OPEN, AdFormat.NATIVE_FULL_SCREEN -> MetricGroup.FULL_SCREEN
    else -> MetricGroup.SLOT
}

/** T-03: the SDK reports micros. */
internal fun revenueValue(valueMicros: Long): Double = valueMicros / MICROS_PER_UNIT

/** T-03: an unknown network is `admob`. */
internal fun adSourceOrAdmob(name: String?): String = if (name.isNullOrEmpty()) ADMOB else name

/**
 * `load_state` after a load error of [code] (section 8.2): offline is always `network`; [sdkThrottled] is read on the
 * device from the SDK's message ([isSdkThrottledMessage]), never sent.
 */
internal fun loadStateAfterError(code: Int, offline: Boolean, sdkThrottled: Boolean): String = when {
    offline -> "network"
    code == 3 || code == 9 -> "no_fill"
    code == 1 -> if (sdkThrottled) "sdk_throttled" else "invalid"
    code == 8 || code == 10 || code == 11 -> "invalid"
    code == 0 -> "internal"
    else -> "unknown"
}

/**
 * `load_state` after a load error of AppLovin MAX of [code] (section 8.2; plan T-071, question 2): offline is always
 * `network`; the codes are those of [MaxErrorCode], read with javap on applovin-sdk 13.6.4. MAX has no throttling of
 * its own to read from a message, so no message is read.
 */
internal fun maxLoadStateAfterError(code: Int, offline: Boolean): String = when {
    offline -> "network"
    code == MaxErrorCode.NO_FILL -> "no_fill"
    code == MaxErrorCode.NETWORK_ERROR || code == MaxErrorCode.NETWORK_TIMEOUT || code == MaxErrorCode.NO_NETWORK ->
        "network"
    code == MaxErrorCode.UNSPECIFIED -> "internal"
    else -> "unknown"
}

/**
 * Whether the SDK refused the request by itself after too many failed ones (code 1; question 3 of plan T-023). The
 * message is read on the device only: it may hold the ad unit, so it is never logged or sent.
 */
internal fun isSdkThrottledMessage(message: String?): Boolean =
    message?.contains(SDK_THROTTLED_MESSAGE, ignoreCase = true) == true

/** `show_fail_reason` of a code of `FullScreenContentCallback.onAdFailedToShowFullScreenContent`. */
internal fun showFailReasonOf(code: Int): String = when (code) {
    0 -> "internal"
    1 -> "ad_reused"
    2 -> "not_ready"
    3 -> "app_not_foreground"
    4 -> "mediation_show_error"
    else -> "unknown"
}

/**
 * `ad_placement` of a load (section 8.2): full-screen formats always `preload`; one native small ad and the
 * collapsible one the placement of the screen using the slot, else `preload`; the pooled formats (medium, step,
 * in-collection: plan T-028, PL-02) always `pool`, since the placement of a pooled ad is set when it is claimed
 * (P-04); a banner (MREC too) the placement of the screen, else none.
 */
internal fun loadPlacementOf(format: AdFormat, screenPlacement: AdPlacement?): String? = when (format) {
    AdFormat.INTERSTITIAL, AdFormat.APP_OPEN, AdFormat.REWARDED, AdFormat.NATIVE_FULL_SCREEN -> PRELOAD
    AdFormat.NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE -> screenPlacement?.key ?: PRELOAD
    AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_STEP, AdFormat.NATIVE_IN_COLLECTION -> POOL
    AdFormat.BANNER, AdFormat.BANNER_COLLAPSIBLE, AdFormat.BANNER_MREC -> screenPlacement?.key
}

/** `opportunity_result` of a full-screen turn; a closed consent gate or a switched-off placement is no opportunity. */
internal fun FullScreenShowDecision.opportunityResult(): String? = when (this) {
    FullScreenShowDecision.SHOW -> "shown"
    FullScreenShowDecision.BUSY -> "busy"
    FullScreenShowDecision.NOT_READY -> "not_ready"
    FullScreenShowDecision.THROTTLED -> "throttled"
    FullScreenShowDecision.CONSENT_CLOSED, FullScreenShowDecision.DISABLED -> null
}

/** `load_state` of an opportunity, taken at the decision, before any load the turn starts (section 8.2). */
internal fun loadStateAtDecision(state: FullScreenLoadState, lastErrorLabel: String?): String = when {
    state == FullScreenLoadState.Ready -> "ready"
    state == FullScreenLoadState.Loading -> "loading"
    lastErrorLabel != null -> lastErrorLabel
    else -> "idle"
}

/** `ad_placement` of the loads that serve no screen yet. */
internal const val PRELOAD = "preload"

private const val POOL = "pool"
private const val ADMOB = "admob"
private const val MICROS_PER_UNIT = 1_000_000.0
private const val SDK_THROTTLED_MESSAGE = "Too many recently failed requests"
