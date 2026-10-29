package com.vglstudio.camera.pose.line.guide.core.analytics

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
 * AppsFlyer is not in this app (spec ads, question 3): no SDK, no permission, nothing sent. Every call does
 * nothing. Turning AppsFlyer on needs its own spec and ticket, which bring back the SDK sink of the template.
 */
internal class NoOpAppsFlyerSink @Inject constructor() : AppsFlyerSink {
    override fun init(devKey: String) = Unit

    override fun start() = Unit

    override fun logEvent(name: String, params: Map<String, Any>?) = Unit
}
