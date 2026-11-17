package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/**
 * Remote Config switches of the Tenjin events (ADR-1010; spec ads, question 21 c). On by default. Read by
 * TenjinSwitches, at every event. Named by the app: none is in the official AdsKit key table.
 */
object TenjinSettings {
    val IS_LOG_TENJIN_AD_REVENUE: RemoteKey<Boolean> = RemoteKey.boolean(
        name = "is_log_tenjin_ad_revenue",
        default = true,
        description = "Sends the revenue of every paid AdMob impression to Tenjin. Tenjin event switch (ADR-1010). " +
            "Name set by the app, not an AdsKit key.",
    )

    /** Every key of the module. */
    fun all(): List<RemoteKey<*>> = switches()

    /** The same keys as [all], typed: every one is a switch. */
    fun switches(): List<RemoteKey<Boolean>> = listOf(IS_LOG_TENJIN_AD_REVENUE)
}
