package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey

/** Remote Config switches of the Tenjin events (ADR-1010, spec ads question 21 c). */
object TenjinSettings {
    val IS_LOG_TENJIN_AD_REVENUE: RemoteKey<Boolean> = RemoteKey.boolean(
        name = "is_log_tenjin_ad_revenue",
        default = true,
        description = "Skeleton of plan T-099.",
    )

    fun all(): List<RemoteKey<*>> = emptyList()

    fun switches(): List<RemoteKey<Boolean>> = emptyList()
}
