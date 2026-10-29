package com.vglstudio.camera.pose.line.guide.core.analytics

import android.content.Context
import android.telephony.TelephonyManager
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

/**
 * AppsFlyer behind the interfaces of this module (ADR-0027; docs/ops/ads-business-rules.md, section 8.3):
 *
 * - With no dev key (staging, CI) the SDK is never touched, `init` included, and every event is dropped.
 * - [prepare] initializes the SDK once; [start] starts it once, after [prepare]. Events before [start] are dropped.
 * - Every impression revenue becomes `af_ad_revenue`; lifecycle metrics stay in Firebase. The funnel of :core:ads
 *   becomes the `af_*` funnel events.
 * - Each event goes through its own switch, read at every event ([AppsFlyerSwitches]).
 * - An error of the SDK is swallowed and logged by class name only, so the ads and Firebase keep working.
 *
 * Logs under the tag `Analytics` (ADR-0019): event names only, never the key, an ad unit or a value.
 */
@Singleton
internal class AppsFlyerTracker @Inject constructor(
    private val config: AppsFlyerConfig,
    private val sink: AppsFlyerSink,
    private val switches: AppsFlyerSwitches,
    private val country: CountrySource,
    private val logger: AppLogger,
) : AppsFlyerStartup,
    AdFunnel,
    AdMetrics {
    private val prepareCalled = AtomicBoolean(false)
    private val ready = AtomicBoolean(false)
    private val started = AtomicBoolean(false)

    override fun prepare() {
        if (!prepareCalled.compareAndSet(false, true)) return
        if (!config.enabled) {
            logger.info(TAG, "AppsFlyer off: no dev key in this build")
            return
        }
        try {
            sink.init(config.devKey)
            ready.set(true)
            logger.info(TAG, "AppsFlyer ready")
        } catch (e: Exception) {
            logger.warn(TAG, "AppsFlyer not initialized: ${e.javaClass.simpleName}")
        }
    }

    override fun start() {
        if (!config.enabled || !ready.get()) return
        if (!started.compareAndSet(false, true)) return
        try {
            sink.start()
            logger.info(TAG, "AppsFlyer started")
        } catch (e: Exception) {
            logger.warn(TAG, "AppsFlyer not started: ${e.javaClass.simpleName}")
        }
    }

    /** Only the impression revenue goes to AppsFlyer; load, opportunity, show failure and click do not (section 8.3). */
    override fun log(event: AdMetricEvent) {
        if (event !is AdMetricEvent.ImpressionRevenue) return
        send(REVENUE_EVENT, REVENUE_SWITCH) { afRevenue(event, country.current()).params }
    }

    override fun log(event: AdFunnelEvent) {
        val afEvent = event.toAfEvent() ?: return
        send(afEvent.name, event.switch()) { afEvent.params }
    }

    /** The one gate of every event: key, switch, started; the params are built only when the event is sent. */
    private fun send(name: String, switch: RemoteKey<Boolean>, params: () -> Map<String, Any>?) {
        when {
            !config.enabled -> logger.debug(TAG, "AppsFlyer off: $name not sent")
            !switches.isOn(switch) -> logger.debug(TAG, "AppsFlyer $name switched off")
            !started.get() -> logger.debug(TAG, "AppsFlyer $name before start")
            else -> try {
                sink.logEvent(name, params())
            } catch (e: Exception) {
                // Class name only: the message of the error may hold anything.
                logger.warn(TAG, "AppsFlyer $name not sent: ${e.javaClass.simpleName}")
            }
        }
    }

    private companion object {
        const val TAG = "Analytics"
        const val REVENUE_EVENT = "af_ad_revenue"
    }
}

/** The country of `af_ad_revenue` ([countryCode]). */
internal interface CountrySource {
    fun current(): String
}

/** Reads the SIM and the network country, which need no permission from API 26 to 36, then the device language. */
internal class AndroidCountrySource @Inject constructor(@param:ApplicationContext private val context: Context) :
    CountrySource {
    override fun current(): String {
        val telephony = context.getSystemService(TelephonyManager::class.java)
        return countryCode(telephony?.simCountryIso, telephony?.networkCountryIso, Locale.getDefault())
    }
}
