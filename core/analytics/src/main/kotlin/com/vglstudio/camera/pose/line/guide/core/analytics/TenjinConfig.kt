package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * The Tenjin SDK key of this build (ADR-1010), provided by :app from its build flavor: staging reads `tenjin.sdkKey`
 * of local.properties, production `-PtenjinSdkKey`. With no key, Tenjin is never touched and nothing is sent.
 */
data class TenjinConfig(val sdkKey: String) {
    /** A key is set, so Tenjin may run in this build. */
    val enabled: Boolean get() = false
}
