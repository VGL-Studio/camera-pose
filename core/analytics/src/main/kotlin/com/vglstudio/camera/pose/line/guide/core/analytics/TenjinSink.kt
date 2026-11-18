package com.vglstudio.camera.pose.line.guide.core.analytics

import android.content.Context
import com.tenjin.android.TenjinSDK
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import org.json.JSONObject

/**
 * The calls :core:analytics makes to Tenjin (ADR-1010). Tests use a fake: they never call the SDK. [TenjinLibSink] is
 * the only file of the app that imports the SDK (TenjinSdkUseTest).
 */
internal interface TenjinSink {
    /** Builds the SDK for [sdkKey]; sends nothing. */
    fun init(sdkKey: String)

    /** Applies the TCF consent of UMP to the SDK; false when the string says no (purpose 1 not consented). */
    fun applyConsent(): Boolean

    /** Sends the install and the session; the SDK reads the Install Referrer itself. */
    fun connect()

    /** Sends one paid AdMob impression ([tenjinAdMobPayload]). */
    fun adMobImpression(payload: Map<String, Any>)

    /** Sends one event by name, with no value or param (ADR-1010, section 5). */
    fun event(name: String)
}

/** The Tenjin SDK 2.0.2. Called only by [TenjinTracker], after [init]. */
internal class TenjinLibSink @Inject constructor(@param:ApplicationContext private val context: Context) : TenjinSink {
    private var sdk: TenjinSDK? = null

    override fun init(sdkKey: String) {
        sdk = TenjinSDK.getInstance(context.applicationContext, sdkKey).apply {
            setAppStore(TenjinSDK.AppStoreType.googleplay)
        }
    }

    override fun applyConsent(): Boolean = requireNotNull(sdk).optInOutUsingCMP()

    override fun connect() {
        requireNotNull(sdk).connect()
    }

    override fun adMobImpression(payload: Map<String, Any>) {
        requireNotNull(sdk).eventAdImpressionAdMob(JSONObject(payload))
    }

    override fun event(name: String) {
    }
}
