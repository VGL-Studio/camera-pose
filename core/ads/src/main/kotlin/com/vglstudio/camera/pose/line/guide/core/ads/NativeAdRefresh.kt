package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Ads"

/**
 * The in-place refresh of a single-ad native slot that shows [first] (CL-02, O-05; plan T-040):
 *
 * - A tick [firstTickDelayMillis] after the first ad, then 60 s after each swap. A tick that comes while [foreground]
 *   is false (in background, or covered by a full-screen ad when it is the in-screen window, KB-27) is skipped, not
 *   saved: the next one is 60 s later, nothing runs when the app comes back or the cover goes (L-A19).
 * - A tick in foreground asks the gate ([awaitSendAllowed]), then claims from [source] for at most 10 s. An ad is
 *   emitted, then the old one destroyed; the failure count goes back to 0.
 * - No ad, 10 s gone or an error keep the old ad and retry after `nativeDelayMillis(n, holdsAd = true)` (2, 4, 8, 16,
 *   32 then 60 s), cut short by [retryResets] (S-24); the retry waits for foreground and asks the gate. A closed gate
 *   or a blocked claim keeps the ad, counts no failure and waits for the next tick.
 * - [NativeAdSource.closed] moving (placement off, consent withdrawn) destroys the ad shown, emits Unavailable and
 *   ends. Cancelling the collection destroys the ad shown.
 */
internal fun refreshingNativeAdStates(
    first: NativeAdState.Loaded,
    source: NativeAdSource,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    clock: MonotonicClock,
    logger: AppLogger,
    placementKey: String,
    firstTickDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
): Flow<NativeAdState> = flow {
    val since = source.closed.value
    fun isClosed() = source.closed.value != since
    var shown = first.ad
    try {
        emit(first)
        var wait = firstTickDelayMillis
        ticks@ while (true) {
            source.untilClosed(since) { delay(wait) } ?: break@ticks
            wait = NativeAdPolicy.REFRESH_INTERVAL_MILLIS
            if (!foreground.value) {
                logger.info(TAG, "Native $placementKey refresh: skipped (background)")
                continue@ticks
            }
            if (!awaitSendAllowed(foreground, mayRequestNow)) {
                logger.info(TAG, "Native $placementKey refresh: blocked")
                continue@ticks
            }
            var failures = 0
            tries@ while (true) {
                val startedAt = clock.nowMillis()
                val claim = source.claimWithin(logger, placementKey)
                if (claim is NativeClaim.Ad) {
                    val fresh = claim.ad
                    var taken = false
                    try {
                        if (isClosed()) break@ticks
                        emit(NativeAdState.Loaded(fresh))
                        val old = shown
                        shown = fresh
                        taken = true
                        old.destroy()
                    } finally {
                        if (!taken) fresh.destroy()
                    }
                    logger.info(TAG, "Native $placementKey refresh: loaded")
                    break@tries
                }
                if (isClosed()) break@ticks
                if (claim == NativeClaim.Blocked) {
                    logger.info(TAG, "Native $placementKey refresh: blocked")
                    break@tries
                }
                failures++
                val retryIn = AdRetryPolicy.nativeDelayMillis(failures, holdsAd = true)
                logger.info(TAG, "Native $placementKey refresh: failed, retry in $retryIn ms (failures $failures)")
                logger.debug(TAG, "Native $placementKey refresh waited ${clock.nowMillis() - startedAt} ms")
                val generation = retryResets.value
                source.untilClosed(since) {
                    withTimeoutOrNull(retryIn) { retryResets.first { it != generation } }
                    Unit
                } ?: break@ticks
                val allowed = source.untilClosed(since) { awaitSendAllowed(foreground, mayRequestNow) } ?: break@ticks
                if (!allowed) {
                    logger.info(TAG, "Native $placementKey refresh: blocked")
                    break@tries
                }
            }
        }
        emit(NativeAdState.Unavailable)
    } finally {
        shown.destroy()
        logger.info(TAG, "Native $placementKey destroyed")
    }
}

/**
 * Claims a refresh ad for at most [NativeAdPolicy.REFRESH_WAIT_MILLIS]; null when none came in time or the claim
 * threw. The claim is kept in a variable, not returned from the timeout block: an ad that comes at the instant of
 * the timeout is either taken here or given back to the source, never lost.
 */
private suspend fun NativeAdSource.claimWithin(logger: AppLogger, placementKey: String): NativeClaim? {
    var claim: NativeClaim? = null
    try {
        withTimeoutOrNull(NativeAdPolicy.REFRESH_WAIT_MILLIS) { claim = claim(NativeLoadUse.REFRESH) }
    } catch (e: CancellationException) {
        throw e
    } catch (e: Exception) {
        logger.info(TAG, "Native $placementKey refresh claim failed: ${e.javaClass.simpleName}")
    }
    return claim
}

/** Runs [block] until the source is shut down after [since]; null then. Only for blocks that hold no ad. */
private suspend fun <T : Any> NativeAdSource.untilClosed(since: Long, block: suspend () -> T): T? =
    merge(flow<T?> { emit(block()) }, closed.filter { it != since }.map { null }).first()
