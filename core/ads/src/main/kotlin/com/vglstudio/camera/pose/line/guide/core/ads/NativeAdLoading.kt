package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.withTimeoutOrNull

/** A native ad the SDK has returned. Whoever holds it destroys it once it leaves the screen. */
internal interface LoadedNativeAd {
    val assets: NativeAdAssets

    /** True once the SDK recorded an impression of this ad (P-01a); a pool never hands it out again. */
    val impressionRecorded: Boolean get() = false

    fun destroy()
}

/** Where the loading of one native ad stands; drives what the slot draws. */
internal sealed interface NativeAdState {
    /** Waiting for the SDK: the slot keeps the ad's height with the placeholder. */
    data object Loading : NativeAdState

    data class Loaded(val ad: LoadedNativeAd) : NativeAdState

    /** No ad (SDK error, timeout, no ad unit): the slot gives its space back. */
    data object Unavailable : NativeAdState
}

/** Starts one native request; calls exactly one of the callbacks. */
internal fun interface NativeAdLoader {
    fun load(unitId: String, size: NativeAdSize, onLoaded: (LoadedNativeAd) -> Unit, onFailed: (code: Int) -> Unit)
}

private const val TAG = "Ads"

/**
 * Loads one ad: [NativeAdState.Loaded] or [NativeAdState.Unavailable] once the request ends or [timeoutMillis] has
 * passed (a negative value counts as 0). An ad that arrives after the timeout, or after the calling coroutine was
 * cancelled, is destroyed at once: the screen has already given its space back. The caller owns a Loaded ad and
 * destroys it.
 */
internal suspend fun loadOneNativeAd(
    loader: NativeAdLoader,
    unitId: String,
    size: NativeAdSize,
    timeoutMillis: Long,
    logger: AppLogger,
    placementKey: String,
): NativeAdState {
    val timeout = timeoutMillis.coerceAtLeast(0L)
    if (timeout == 0L) {
        logger.info(TAG, "Native $placementKey timed out after 0 ms")
        return NativeAdState.Unavailable
    }
    val request = NativeAdRequest(logger, placementKey)
    loader.load(unitId, size, request::onLoaded, request::onFailed)
    var finished = false
    try {
        val result = withTimeoutOrNull(timeout) { request.result.await() }
        finished = true
        if (result != null) return result
        // The request may have ended at the same instant as the timeout; then its result wins.
        return request.abandon() ?: NativeAdState.Unavailable.also {
            logger.info(TAG, "Native $placementKey timed out after $timeout ms")
        }
    } finally {
        if (!finished) {
            // Cancelled while waiting: nobody takes the ad any more.
            (request.abandon() as? NativeAdState.Loaded)?.ad?.let { destroyAd(it, logger, placementKey) }
        }
    }
}

/**
 * The states of one native ad for a slot on screen: [NativeAdState.Loading], then the result of [loadOneNativeAd].
 * A Loaded ad is destroyed when the collection is cancelled (the slot left the screen or the placement was switched
 * off).
 */
internal fun nativeAdStates(
    loader: NativeAdLoader,
    unitId: String,
    size: NativeAdSize,
    timeoutMillis: Long,
    logger: AppLogger,
    placementKey: String,
): Flow<NativeAdState> = flow {
    emit(NativeAdState.Loading)
    val state = loadOneNativeAd(loader, unitId, size, timeoutMillis, logger, placementKey)
    if (state is NativeAdState.Loaded) {
        try {
            emit(state)
            awaitCancellation()
        } finally {
            destroyAd(state.ad, logger, placementKey)
        }
    } else {
        emit(state)
    }
}

private fun destroyAd(ad: LoadedNativeAd, logger: AppLogger, placementKey: String) {
    ad.destroy()
    logger.info(TAG, "Native $placementKey destroyed")
}

/** One request whose callbacks may arrive after nobody waits for them any more. */
private class NativeAdRequest(private val logger: AppLogger, private val placementKey: String) {
    val result = CompletableDeferred<NativeAdState>()
    private val lock = Any()
    private var abandoned = false

    fun onLoaded(ad: LoadedNativeAd) {
        val kept = synchronized(lock) { !abandoned && result.complete(NativeAdState.Loaded(ad)) }
        if (kept) logger.info(TAG, "Native $placementKey loaded") else destroyAd(ad, logger, placementKey)
    }

    fun onFailed(code: Int) {
        val reported = synchronized(lock) { !abandoned && result.complete(NativeAdState.Unavailable) }
        if (reported) logger.info(TAG, "Native $placementKey failed: $code")
    }

    /** Stops waiting; returns the result if the request had already ended, so its ad is not lost. */
    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    fun abandon(): NativeAdState? = synchronized(lock) {
        abandoned = true
        if (result.isCompleted) result.getCompleted() else null
    }
}
