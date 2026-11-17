package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * The Tenjin SDK key of this build (ADR-1010), provided by :app from its build flavor: staging reads `tenjin.sdkKey`
 * of local.properties, production `-PtenjinSdkKey` (optional). With no key, Tenjin is never touched and nothing is
 * sent.
 */
data class TenjinConfig(val sdkKey: String) {
    /** A key is set, so Tenjin may run in this build. */
    val enabled: Boolean get() = sdkKey.isNotBlank()

    /** Never prints the key: it must not reach a log or an error message. */
    override fun toString(): String = "TenjinConfig(sdkKey=${if (enabled) "set" else "none"})"
}
