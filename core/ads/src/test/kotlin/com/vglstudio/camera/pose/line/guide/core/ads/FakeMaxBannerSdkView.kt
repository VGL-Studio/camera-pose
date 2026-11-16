package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.view.View
import androidx.test.core.app.ApplicationProvider

/**
 * The `MaxAdView` of AppLovin MAX for tests (plan T-071): counts `loadAd`, `startAutoRefresh`, `stopAutoRefresh` and
 * `destroy`, keeps every call in [calls] in order, and answers through the callbacks on command ([emitLoaded],
 * [emitFailed], [emitClicked], [emitRevenue]: plan T-072). Its [view] is built on first use, so a test without
 * Robolectric never touches it.
 */
internal class FakeMaxBannerSdkView(val calls: MutableList<String> = mutableListOf()) : MaxBannerSdkView {
    private var onLoaded: (MaxAdInfo) -> Unit = {}
    private var onFailed: (Int, String?, List<MaxNetworkAttempt>) -> Unit = { _, _, _ -> }
    private var onClicked: () -> Unit = {}
    private var onRevenue: (MaxAdInfo) -> Unit = {}

    override val view: View by lazy { View(ApplicationProvider.getApplicationContext<Context>()) }

    val loadCount: Int get() = calls.count { it == LOAD }
    val startCount: Int get() = calls.count { it == START }
    val stopCount: Int get() = calls.count { it == STOP }
    val destroyCount: Int get() = calls.count { it == DESTROY }

    override fun setCallbacks(
        onLoaded: (MaxAdInfo) -> Unit,
        onFailed: (code: Int, message: String?, networks: List<MaxNetworkAttempt>) -> Unit,
        onClicked: () -> Unit,
        onRevenue: (MaxAdInfo) -> Unit,
    ) {
        calls += CALLBACKS
        this.onLoaded = onLoaded
        this.onFailed = onFailed
        this.onClicked = onClicked
        this.onRevenue = onRevenue
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

    fun emitLoaded(info: MaxAdInfo = MaxAdInfo.EMPTY) = onLoaded(info)

    fun emitFailed(code: Int, message: String? = null, networks: List<MaxNetworkAttempt> = emptyList()) =
        onFailed(code, message, networks)

    fun emitClicked() = onClicked()

    /** The SDK reports the revenue of a show (`MaxAdRevenueListener`, plan T-072). */
    fun emitRevenue(info: MaxAdInfo) = onRevenue(info)

    companion object {
        const val BUILD = "build"
        const val CALLBACKS = "setCallbacks"
        const val LOAD = "loadAd"
        const val START = "startAutoRefresh"
        const val STOP = "stopAutoRefresh"
        const val DESTROY = "destroy"
    }
}
