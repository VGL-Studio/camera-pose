package com.vglstudio.camera.pose.line.guide.core.analytics

import android.content.Context
import android.os.Bundle
import com.google.firebase.analytics.FirebaseAnalytics
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * Sends the ad metrics to Firebase Analytics (docs/ops/ads-business-rules.md, section 8.2; ADR-0012). The names are
 * the app's own: AdMob linked to Firebase already logs `ad_impression`, so no event is named so (no double count).
 */
internal class FirebaseAdMetrics @Inject constructor(@param:ApplicationContext private val context: Context) :
    AdMetrics {
    private val firebase by lazy { FirebaseAnalytics.getInstance(context) }

    override fun log(event: AdMetricEvent) {
        val bundle = Bundle()
        event.firebaseParams().forEach { (key, value) ->
            when (value) {
                is Double -> bundle.putDouble(key, value)
                is Long -> bundle.putLong(key, value)
                else -> bundle.putString(key, value.toString())
            }
        }
        firebase.logEvent(event.firebaseName(), bundle)
    }
}

/** The Firebase name of [this] event. Exhaustive on purpose: a new event must be named here. */
internal fun AdMetricEvent.firebaseName(): String = when (this) {
    is AdMetricEvent.ImpressionRevenue -> "ad_impression_placement"
    is AdMetricEvent.LoadSuccess -> "ad_load_success"
    is AdMetricEvent.LoadFail -> "ad_load_fail"
    is AdMetricEvent.Opportunity -> "ad_opportunity"
    is AdMetricEvent.ShowFail -> "ad_show_fail"
    is AdMetricEvent.Clicked -> "ad_clicked"
    is AdMetricEvent.RewardEarned -> "ad_reward_earned"
    is AdMetricEvent.RewardDialog -> "reward_dialog"
}

/**
 * The Firebase params of [this] event, named as in section 8.2: `value` is a [Double], `load_attempt` and
 * `latency_ms` are [Long], every other one a [String]. A param with no value (null or empty) is left out, never sent
 * empty (A 06:87).
 */
internal fun AdMetricEvent.firebaseParams(): Map<String, Any> {
    val params: List<Pair<String, Any?>> = when (this) {
        is AdMetricEvent.ImpressionRevenue -> listOf(
            "ad_format" to adFormat,
            "ad_platform" to AD_PLATFORM,
            "ad_source" to adSource,
            "ad_unit_name" to adUnitName,
            "currency" to currency,
            "value" to value,
            "ad_placement" to adPlacement,
        )
        is AdMetricEvent.LoadSuccess -> listOf(
            "ad_format" to adFormat,
            "ad_placement" to adPlacement,
            "load_attempt" to loadAttempt?.toLong(),
            "latency_ms" to latencyMs,
        )
        is AdMetricEvent.LoadFail -> listOf(
            "ad_format" to adFormat,
            "ad_placement" to adPlacement,
            "load_attempt" to loadAttempt?.toLong(),
            "latency_ms" to latencyMs,
            "load_error_code" to loadErrorCode,
            "load_state" to loadState,
        )
        is AdMetricEvent.Opportunity -> listOf(
            "ad_format" to adFormat,
            "ad_placement" to adPlacement,
            "opportunity_result" to result,
            "load_state" to loadState,
        )
        is AdMetricEvent.ShowFail -> listOf(
            "ad_format" to adFormat,
            "ad_placement" to adPlacement,
            "show_error_code" to showErrorCode,
            "show_fail_reason" to showFailReason,
        )
        is AdMetricEvent.Clicked -> listOf(
            "ad_format" to adFormat,
            "ad_placement" to adPlacement,
        )
        // No `ad_format` for the two rewarded events (section 8.2).
        is AdMetricEvent.RewardEarned -> listOf(
            "ad_placement" to adPlacement,
        )
        is AdMetricEvent.RewardDialog -> listOf(
            "ad_placement" to adPlacement,
            "dialog_action" to dialogAction,
        )
    }
    return buildMap {
        params.forEach { (key, value) ->
            if (value != null && value != "") put(key, value)
        }
    }
}

/** Ads are served through AdMob, mediation included. */
private const val AD_PLATFORM = "admob"
