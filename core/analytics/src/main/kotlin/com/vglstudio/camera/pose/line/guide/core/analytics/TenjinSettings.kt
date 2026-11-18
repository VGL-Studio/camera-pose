package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/**
 * Remote Config switches of the Tenjin events (ADR-1010; spec ads, question 21 c): the revenue (T-099), then one per
 * ad funnel event (T-100), in the order of AppsFlyerSettings. On by default. Read by TenjinSwitches, at every event.
 * Named by the app: none is in the official AdsKit key table.
 */
object TenjinSettings {
    private const val SUFFIX = "Tenjin event switch (ADR-1010). Name set by the app, not an AdsKit key."

    val IS_LOG_TENJIN_AD_REVENUE: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_ad_revenue",
        description = "Sends the revenue of every paid AdMob impression to Tenjin. $SUFFIX",
    )

    val IS_LOG_TENJIN_INTERS_AD_ELIGIBLE: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_inters_ad_eligible",
        description = "Sends inters_ad_eligible to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_INTERS_API_CALLED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_inters_api_called",
        description = "Sends inters_api_called to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_INTERS_DISPLAYED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_inters_displayed",
        description = "Sends inters_displayed to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_REWARDED_AD_ELIGIBLE: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_rewarded_ad_eligible",
        description = "Sends rewarded_ad_eligible to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_REWARDED_API_CALLED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_rewarded_api_called",
        description = "Sends rewarded_api_called to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_REWARDED_AD_DISPLAYED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_rewarded_ad_displayed",
        description = "Sends rewarded_ad_displayed to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_NATIVE_AD_ELIGIBLE: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_native_ad_eligible",
        description = "Sends native_ad_eligible to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_NATIVE_API_CALLED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_native_api_called",
        description = "Sends native_api_called to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_NATIVE_AD_DISPLAYED: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_native_ad_displayed",
        description = "Sends native_ad_displayed to Tenjin, with no param. $SUFFIX",
    )

    val IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES: RemoteKey<Boolean> = key(
        name = "is_log_tenjin_inters_displayed_n_times",
        description = "Sends inters_displayed_{N}_times to Tenjin, with no param. $SUFFIX",
    )

    /** Every key of the module. */
    fun all(): List<RemoteKey<*>> = switches()

    /** The same keys as [all], typed: every one is a switch. */
    fun switches(): List<RemoteKey<Boolean>> = listOf(
        IS_LOG_TENJIN_AD_REVENUE,
        IS_LOG_TENJIN_INTERS_AD_ELIGIBLE,
        IS_LOG_TENJIN_INTERS_API_CALLED,
        IS_LOG_TENJIN_INTERS_DISPLAYED,
        IS_LOG_TENJIN_REWARDED_AD_ELIGIBLE,
        IS_LOG_TENJIN_REWARDED_API_CALLED,
        IS_LOG_TENJIN_REWARDED_AD_DISPLAYED,
        IS_LOG_TENJIN_NATIVE_AD_ELIGIBLE,
        IS_LOG_TENJIN_NATIVE_API_CALLED,
        IS_LOG_TENJIN_NATIVE_AD_DISPLAYED,
        IS_LOG_TENJIN_INTERS_DISPLAYED_N_TIMES,
    )

    private fun key(name: String, description: String): RemoteKey<Boolean> =
        RemoteKey.boolean(name = name, default = true, description = description)
}
