package com.vglstudio.camera.pose.line.guide.core.analytics

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Tenjin behind the interfaces of this module (ADR-1010; spec ads, question 21):
 *
 * - With no SDK key (CI, a build without `tenjin.sdkKey`) the SDK is never touched, `getInstance` included.
 * - [prepare] builds the SDK once. It connects once both [start] (with the start of AppsFlyer) and [onConsentKnown]
 *   (from :core:ads) came, in any order, then again each time the whole app comes back to foreground ([AppForeground]).
 * - Right before every connection and every revenue it applies the TCF consent of UMP; when the string says no, it
 *   neither connects nor sends (opting out in the SDK would still send the connection).
 * - The revenue of every paid AdMob impression goes to Tenjin ([tenjinAdMobPayload]) through its switch, read at every
 *   event ([TenjinSwitches]). Nothing else of the ad metrics does.
 * - An error of the SDK is swallowed and logged by class name only, so the ads, AppsFlyer and Firebase keep working.
 *
 * Logs under the tag `Analytics` (ADR-0019): never the key, an ad unit or a value.
 */
@Singleton
internal class TenjinTracker @Inject constructor(
    private val config: TenjinConfig,
    private val sink: TenjinSink,
    private val switches: TenjinSwitches,
    private val foreground: AppForeground,
    private val logger: AppLogger,
) : AppsFlyerStartup,
    AdMetrics,
    AdFunnel,
    AttributionConsent {
    private val prepareCalled = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)
    private val started = AtomicBoolean(false)
    private val consentKnown = AtomicBoolean(false)
    private val firstConnectDone = AtomicBoolean(false)

    override fun prepare() {
        if (!prepareCalled.compareAndSet(false, true)) return
        if (!config.enabled) {
            logger.info(TAG, "Tenjin off: no SDK key in this build")
            return
        }
        if (call("init") { sink.init(config.sdkKey) } != null) {
            ready.set(true)
            logger.info(TAG, "Tenjin ready")
        }
    }

    override fun start() {
        if (!config.enabled || !ready.get()) return
        if (!started.compareAndSet(false, true)) return
        foreground.onEachReturn { if (consentKnown.get()) connectNow() }
        connectIfReady()
    }

    override fun onConsentKnown() {
        if (!config.enabled) return
        if (!consentKnown.compareAndSet(false, true)) return
        connectIfReady()
    }

    /** Only the impression revenue goes to Tenjin; load, opportunity, show failure and click do not. */
    override fun log(event: AdMetricEvent) {
        if (event !is AdMetricEvent.ImpressionRevenue) return
        when {
            !config.enabled -> return
            !switches.isOn(REVENUE_SWITCH) -> logger.debug(TAG, "Tenjin ad revenue switched off")
            !started.get() -> logger.debug(TAG, "Tenjin ad revenue before start")
            !consentKnown.get() -> logger.debug(TAG, "Tenjin ad revenue before consent known")
            else -> {
                val payload = tenjinAdMobPayload(event) ?: return
                if (call("consent") { sink.applyConsent() } != true) {
                    logger.debug(TAG, "Tenjin ad revenue opted out")
                    return
                }
                call("ad revenue") { sink.adMobImpression(payload) }
            }
        }
    }

    override fun log(event: AdFunnelEvent) {
    }

    private fun connectIfReady() {
        if (started.get() && consentKnown.get() && firstConnectDone.compareAndSet(false, true)) connectNow()
    }

    /** The one gate of every connection: the consent of UMP, applied right before it. */
    private fun connectNow() {
        if (call("consent") { sink.applyConsent() } != true) {
            logger.info(TAG, "Tenjin not connected: no consent")
            return
        }
        if (call("connect") { sink.connect() } != null) logger.info(TAG, "Tenjin connected")
    }

    /** Runs one call of the SDK; null when it threw, logged by class name only (its message may hold anything). */
    private fun <T> call(step: String, block: () -> T): T? = try {
        block()
    } catch (e: Exception) {
        logger.warn(TAG, "Tenjin $step failed: ${e.javaClass.simpleName}")
        null
    }

    private companion object {
        const val TAG = "Analytics"
        val REVENUE_SWITCH = TenjinSettings.IS_LOG_TENJIN_AD_REVENUE
    }
}
