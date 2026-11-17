package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * The calls :core:analytics makes to Tenjin (ADR-1010). Tests use a fake: they never call the SDK.
 */
internal interface TenjinSink {
    fun init(sdkKey: String)

    /** Applies the TCF consent of UMP to the SDK; false when the string says no. */
    fun applyConsent(): Boolean

    fun connect()

    fun adMobImpression(payload: Map<String, Any>)
}
