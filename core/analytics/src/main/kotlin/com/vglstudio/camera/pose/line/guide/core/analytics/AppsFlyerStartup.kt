package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * Starts every attribution SDK of this module: AppsFlyer (ADR-0027, decisions 1 and 3) and Tenjin (ADR-1010), through
 * FanOutAppsFlyerStartup. :app calls [prepare] in `Application.onCreate`; :core:ads calls [start] once consent allows
 * it (with the ads SDK, on an answer of the user, or after the wait of the loading screen). Without a key of an SDK
 * both do nothing for it. Tenjin also waits for [AttributionConsent] before it sends anything.
 */
interface AppsFlyerStartup {
    /** Initializes the SDK, reading the TCF consent string; sends nothing. A second call does nothing. */
    fun prepare()

    /** Starts the SDK, the first time only; events before it are dropped. */
    fun start()
}
