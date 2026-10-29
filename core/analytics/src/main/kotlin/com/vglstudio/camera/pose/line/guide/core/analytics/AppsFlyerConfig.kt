package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * The AppsFlyer dev key of this build (ADR-0027, decision 2), provided by :app from its build flavor: production
 * passes `-PappsFlyerDevKey`, staging and CI have none. With no key, AppsFlyer never starts and nothing is sent.
 */
data class AppsFlyerConfig(val devKey: String) {
    /** A key is set, so AppsFlyer may run in this build. */
    val enabled: Boolean get() = devKey.isNotBlank()

    /** Never prints the key: it must not reach a log or an error message. */
    override fun toString(): String = "AppsFlyerConfig(devKey=${if (enabled) "set" else "none"})"
}
