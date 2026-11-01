package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider

/**
 * The `MaxAdView` of AppLovin MAX for tests (plan T-071): counts `loadAd`, `startAutoRefresh`, `stopAutoRefresh` and
 * `destroy`, keeps every call in [calls] in order, and answers through the callbacks on command ([emitLoaded],
 * [emitFailed], [emitClicked]). Its [view] is built on first use, so a test without Robolectric never touches it.
 */
internal class FakeMaxBannerSdkView(val calls: MutableList<String> = mutableListOf()) : MaxBannerSdkView {
    private var onLoaded: () -> Unit = {}
    private var onFailed: (Int) -> Unit = {}
    private var onClicked: () -> Unit = {}

    override val view: View by lazy { View(ApplicationProvider.getApplicationContext<Context>()) }

    val loadCount: Int get() = calls.count { it == LOAD }
    val startCount: Int get() = calls.count { it == START }
    val stopCount: Int get() = calls.count { it == STOP }
    val destroyCount: Int get() = calls.count { it == DESTROY }

    override fun setCallbacks(onLoaded: () -> Unit, onFailed: (code: Int) -> Unit, onClicked: () -> Unit) {
        calls += CALLBACKS
        this.onLoaded = onLoaded
        this.onFailed = onFailed
        this.onClicked = onClicked
    }

    override fun loadAd() {
        calls += LOAD
    }

    override fun startAutoRefresh() {
        calls += START
    }

    override fun stopAutoRefresh() {
        calls += STOP
    }

    override fun destroy() {
        calls += DESTROY
    }

    fun emitLoaded() = onLoaded()

    fun emitFailed(code: Int) = onFailed(code)

    fun emitClicked() = onClicked()

    companion object {
        const val BUILD = "build"
        const val CALLBACKS = "setCallbacks"
        const val LOAD = "loadAd"
        const val START = "startAutoRefresh"
        const val STOP = "stopAutoRefresh"
        const val DESTROY = "destroy"
    }
}
