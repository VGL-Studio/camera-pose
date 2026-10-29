package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * The AppsFlyer funnel of one native load (section 8.3; plan T-033): `af_native_api_called` when the ad is loaded,
 * `af_native_ad_displayed` at its first impression only, whatever the path (single-ad slot, list row, refresh, pool,
 * native full screen). Each load has its own, so two ads of one placement never share it (L-068).
 */
internal class NativeFunnel(
    private val telemetry: AdTelemetry,
    private val format: AdFormat,
    private val unitId: String,
) {
    private val lock = Any()
    private var displayed = false

    @Volatile private var adapterClassName: String? = null

    fun onLoaded(adapterClassName: String?) {
        this.adapterClassName = adapterClassName
        telemetry.apiCalled(format, unitId, adapterClassName)
    }

    /** The SDK reports an impression; a repeated one of the same ad sends nothing more. */
    fun onImpression() {
        val first = synchronized(lock) {
            if (displayed) {
                false
            } else {
                displayed = true
                true
            }
        }
        if (first) telemetry.displayed(format, unitId, adapterClassName)
    }
}
