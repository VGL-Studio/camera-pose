package com.vglstudio.camera.pose.line.guide.core.analytics

import android.content.Context
import com.appsflyer.AppsFlyerLib
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

/**
 * The calls :core:analytics makes to AppsFlyer (ADR-0027). Tests use a fake: they never call the SDK (decision 5).
 */
internal interface AppsFlyerSink {
    fun init(devKey: String)

    fun start()

    fun logEvent(name: String, params: Map<String, Any>?)
}

/**
 * The AppsFlyer SDK: the only file of the repo that uses it (AppsFlyerSdkUseTest). `init` reads the TCF consent
 * string of UMP (ADR-0013) and sets no customer id, no conversion listener and no debug log; events go through the
 * plain event call of the SDK, never its ad revenue module (section 8.3).
 */
internal class AppsFlyerLibSink @Inject constructor(@param:ApplicationContext private val context: Context) :
    AppsFlyerSink {
    override fun init(devKey: String) {
        val lib = AppsFlyerLib.getInstance()
        // Before init, so the SDK reads the consent string from its first request (ADR-0027, decision 3).
        lib.enableTCFDataCollection(true)
        lib.init(devKey, null, context)
    }

    override fun start() {
        AppsFlyerLib.getInstance().start(context)
    }

    override fun logEvent(name: String, params: Map<String, Any>?) {
        AppsFlyerLib.getInstance().logEvent(context, name, params)
    }
}
