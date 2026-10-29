package com.vglstudio.camera.pose.line.guide.core.analytics

/**
 * Starts AppsFlyer (ADR-0027, decisions 1 and 3). :app calls [prepare] in `Application.onCreate`; :core:ads calls
 * [start] once consent allows it (with the ads SDK, on an answer of the user, or after the wait of the loading screen).
 * Without a dev key both do nothing.
 */
interface AppsFlyerStartup {
    /** Initializes the SDK, reading the TCF consent string; sends nothing. A second call does nothing. */
    fun prepare()

    /** Starts the SDK, the first time only; events before it are dropped. */
    fun start()
}
