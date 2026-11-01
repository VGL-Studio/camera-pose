package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update

/** K-02: how long the combo waits for the interstitial to leave the screen, and how often it looks. */
internal const val INTER_GONE_WAIT_MILLIS = 2_000L
internal const val INTER_GONE_POLL_MILLIS = 200L

private const val MILLIS_PER_SECOND = 1_000L

/** K-02: the countdown of the native full-screen ad when `time_close_native` is not set or out of range (JBase). */
internal const val NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS = 5L

/** K-02: the values of `time_close_native` taken as they are, in seconds (JBase). */
internal val NATIVE_CLOSE_COUNTDOWN_SECONDS = 1L..30L

/** K-02: `time_close_native` as seconds: 1 to 30 as they are, any other value [NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS]. */
internal fun nativeCloseCountdownSeconds(remote: Long): Long =
    if (remote in NATIVE_CLOSE_COUNTDOWN_SECONDS) remote else NATIVE_CLOSE_COUNTDOWN_DEFAULT_SECONDS

/**
 * K-02: the seconds shown by the countdown, rounded up (5, 4, 3, 2, 1 for a countdown of 5 s), never more than
 * [countdownSeconds], 0 once [deadline] has passed.
 */
internal fun countdownSecondsLeft(deadline: Long, now: Long, countdownSeconds: Long): Int {
    val left = deadline - now
    if (left <= 0) return 0
    val seconds = (left + MILLIS_PER_SECOND - 1) / MILLIS_PER_SECOND
    return seconds.coerceAtMost(countdownSeconds).toInt()
}

/**
 * K-02: asks [isGone] at once, then every [INTER_GONE_POLL_MILLIS], at most [INTER_GONE_WAIT_MILLIS]; true when the
 * interstitial left the screen, false when the wait is over. Uses the time of the coroutine.
 */
internal suspend fun awaitInterGone(isGone: () -> Boolean): Boolean {
    if (isGone()) return true
    var waited = 0L
    while (waited < INTER_GONE_WAIT_MILLIS) {
        delay(INTER_GONE_POLL_MILLIS)
        waited += INTER_GONE_POLL_MILLIS
        if (isGone()) return true
    }
    return false
}

/**
 * What the overlay of a native full-screen turn draws: the ad, or the placeholder while it loads ([ad] null); the
 * seconds left of the countdown; whether the load failed. Holds no clock.
 */
internal data class NativeFullScreenView(val ad: LoadedNativeAd?, val secondsLeft: Int, val loadFailed: Boolean) {
    /** K-03: the close button works once the countdown is over, or at once when the load failed. */
    val closeEnabled: Boolean get() = secondsLeft == 0 || loadFailed

    /** K-08, question 17 of the spec: the back button closes exactly when the close button would. */
    val backCloses: Boolean get() = closeEnabled
}

/** K-07, section 8.2: `shown` always goes with `ready`; else `not_ready` with the load state taken at the opening. */
internal fun comboOpportunity(everShown: Boolean, loadStateAtOpen: String): Pair<String, String> =
    if (everShown) SHOWN to READY else NOT_READY to loadStateAtOpen

/** The values of [FullScreenShowDecision.opportunityResult] and [loadStateAtDecision] the combo records. */
private const val SHOWN = "shown"
private const val NOT_READY = "not_ready"
private const val READY = "ready"

/**
 * "The slot has a new load result" per placement (L-068): the native full-screen slot loads several placements at
 * once, so a result of one placement never wakes a wait of another. Main thread only.
 */
internal class NativeLoadResults {
    private val generations = MutableStateFlow<Map<AdPlacement, Int>>(emptyMap())

    /** A load of [placement] ended, with an ad or an error. */
    fun signal(placement: AdPlacement) {
        generations.update { it + (placement to (it[placement] ?: 0) + 1) }
    }

    /** The number of results of [placement] so far; taken before a load, so a quick result is not missed. */
    fun generation(placement: AdPlacement): Int = generations.value[placement] ?: 0

    /** Waits until [placement] has a result after [since]. */
    suspend fun await(placement: AdPlacement, since: Int) {
        generations.first { (it[placement] ?: 0) != since }
    }
}
