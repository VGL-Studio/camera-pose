package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity

/**
 * The interstitial or rewarded ad of AppLovin MAX for tests (plans T-069, T-106): keeps every call in [calls] in order
 * and the activities it was shown on, and answers through its listener on command ([emitLoaded], [emitLoadFailed] and
 * the others); the revenue goes through [onRevenue] ([emitRevenue], plan T-072).
 *
 * With [ignoreLoadWhileShowing], it acts like the one rewarded object of the SDK per ad unit (plan T-106): a [load]
 * between a [show] and the end of that show is dropped with no callback, recorded as [LOAD_IGNORED]. The end of a show
 * is [emitHidden], or [emitDisplayFailed] before any [emitDisplayed]. [onDestroy] runs in [destroy].
 */
internal class FakeMaxInterstitialSdkAd(
    val adUnitId: String,
    private val ignoreLoadWhileShowing: Boolean = false,
    private val onDestroy: () -> Unit = {},
) : MaxInterstitialSdkAd {
    val calls = mutableListOf<String>()
    val shownOn = mutableListOf<Activity>()

    override var listener: MaxInterstitialListener? = null

    override var onRevenue: ((MaxAdInfo) -> Unit)? = null

    val loadCount: Int get() = calls.count { it == LOAD }
    val showCount: Int get() = calls.count { it == SHOW }

    /** A show was called and has not ended yet, as the SDK sees it. */
    private var showing = false

    /** The SDK reported the show on screen since the last show started. */
    private var displayed = false

    override fun load() {
        calls += if (ignoreLoadWhileShowing && showing) LOAD_IGNORED else LOAD
    }

    override fun show(activity: Activity) {
        calls += SHOW
        shownOn += activity
        showing = true
    }

    override fun destroy() {
        calls += DESTROY
        onDestroy()
    }

    fun emitLoaded(info: MaxAdInfo = MaxAdInfo.EMPTY) = listener?.onLoaded(info)

    fun emitLoadFailed(code: Int, networks: List<MaxNetworkAttempt> = emptyList()) =
        listener?.onLoadFailed(code, networks)

    /** The SDK reports the revenue of a show (plan T-072): through [onRevenue], never [listener]. */
    fun emitRevenue(info: MaxAdInfo) = onRevenue?.invoke(info)

    fun emitDisplayed() {
        displayed = true
        showing = true
        listener?.onDisplayed()
    }

    fun emitHidden() {
        showing = false
        displayed = false
        listener?.onHidden()
    }

    fun emitDisplayFailed(code: Int) {
        if (!displayed) showing = false
        listener?.onDisplayFailed(code)
    }

    fun emitClicked() = listener?.onClicked()

    /** The SDK reports the reward (`onUserRewarded`, plan T-106). */
    fun emitRewarded() = listener?.onRewarded()

    companion object {
        const val LOAD = "load"
        const val LOAD_IGNORED = "load_ignored"
        const val SHOW = "show"
        const val DESTROY = "destroy"
    }
}
