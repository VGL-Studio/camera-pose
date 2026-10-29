package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull

private const val TAG = "Ads"

/**
 * P-02: how many ads a pool of this format holds, counting the ones in use; null for a format with no pool.
 * Exhaustive on purpose, with no `else`: a new [AdFormat] decides here.
 */
internal fun AdFormat.poolCapacity(): Int? = when (this) {
    AdFormat.NATIVE_IN_COLLECTION -> 5
    AdFormat.NATIVE_MEDIUM -> 3
    AdFormat.NATIVE_STEP -> 3
    AdFormat.BANNER,
    AdFormat.INTERSTITIAL,
    AdFormat.NATIVE_SMALL,
    AdFormat.NATIVE_COLLAPSIBLE,
    AdFormat.NATIVE_FULL_SCREEN,
    AdFormat.APP_OPEN,
    AdFormat.REWARDED,
    AdFormat.BANNER_COLLAPSIBLE,
    -> null
}

/** The formats whose ads come from a [NativeAdPool] (P-02). */
internal val AdFormat.usesPool: Boolean get() = poolCapacity() != null

/**
 * One pool per (format, ad unit) (plan T-028, question 1): placements of one format with the same ad unit share it,
 * and the placement of an ad is set when it is claimed (P-04).
 */
internal data class NativePoolKey(val format: AdFormat, val unitId: String)

/** The placement a loaded ad is shown for; empty until its show (PL-03). Full-screen ads and pools use it. */
internal class PlacementCell {
    @Volatile var value: AdPlacement? = null
}

/** One ad a pool loaded, with the cell its metrics read the placement from (P-04). */
internal class PoolLoad(val ad: LoadedNativeAd, val holder: PlacementCell)

/** What a cell of a pool takes its ad from ([pooledCellStates]). */
internal interface PooledNativeAds {
    /** An ad for [holder], or null; never one in use. */
    fun claim(holder: AdPlacement): LoadedNativeAd?

    /** The cell left the screen with [ad]. */
    fun release(ad: LoadedNativeAd)

    /** No ad can come for now (P-05): the cell hides, or keeps a static placeholder, and keeps trying. */
    val exhausted: Boolean
}

/**
 * The native pool of one (format, ad unit) (docs/ops/ads-business-rules.md, P-01 to P-07; plan T-028):
 *
 * - P-01: loads one ad per request; [claim] hands out the free ads in turn (round robin), never one in use.
 * - P-02, P-03: refills one request at a time up to [capacity], counting the ads in use.
 * - P-01a: [release] destroys an ad the SDK recorded an impression of, and refills; puts any other one back at the end.
 * - O-04: a free ad is destroyed from 55 minutes after its load, by [claim] and by [refill]; so is a free ad whose
 *   impression the SDK reported after it was given back (P-01a).
 * - P-04: the placement of an ad is set in its [PlacementCell] when it is claimed.
 * - P-05: [exhausted] when no free ad, no request in flight, and nothing can be sent now.
 * - P-08 (close of the ticket): [close] destroys the free ads, cancels the load in flight (a late ad is destroyed),
 *   drops the retry; an ad in use given back after it is destroyed. [reopen] opens it again.
 * - O-05, S-15, L-072: every send goes through [sendNext], which asks [sendGate] (foreground, then the consent gate);
 *   a closed gate sends nothing, counts no failure and plans no retry. A failed load retries after
 *   [AdRetryPolicy.nativeDelayMillis] (holding an ad when the pool holds one), or when [retryResets] moves (S-24).
 * - S-11: [claim] gives nothing while [canShowNow] is false.
 *
 * Main thread only ([scope] runs on the main thread).
 */
internal class NativeAdPool(
    val key: NativePoolKey,
    private val capacity: Int,
    private val load: suspend () -> PoolLoad?,
    private val sendGate: suspend () -> Boolean,
    /** SDK ready, foreground and the consent gate open: a request could be sent now. */
    private val canLoadNow: () -> Boolean,
    /** Consent given: a loaded ad may go on screen (S-11). */
    private val canShowNow: () -> Boolean,
    private val retryResets: StateFlow<Int>,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
    /** Format code and number of the pool, never the ad unit (ADR-0019). */
    private val logKey: String,
) : PooledNativeAds {
    private class Entry(val ad: LoadedNativeAd, val holder: PlacementCell, val loadedAtMillis: Long)

    private val free = ArrayDeque<Entry>()
    private val inUse = mutableMapOf<LoadedNativeAd, Entry>()

    /** The send in progress: waiting for the gate, loading, or waiting for its retry. */
    private var job: Job? = null

    /** A request was handed to the SDK and has no result yet. */
    private var requestSent = false
    private var waitingRetry = false
    private var failures = 0
    private var closed = false

    /** Moves at every close, so a send of an earlier opening changes nothing. */
    private var session = 0L

    /** A send is in progress (gate, load or retry wait). */
    val isLoading: Boolean get() = job != null

    override val exhausted: Boolean
        get() {
            if (free.isNotEmpty()) return false
            if (closed) return true
            if (requestSent || inUse.size >= capacity) return false
            return waitingRetry || !canLoadNow()
        }

    override fun claim(holder: AdPlacement): LoadedNativeAd? {
        if (!canShowNow()) return null
        pruneExpired()
        val entry = free.removeFirstOrNull()
        if (entry == null) {
            refill()
            return null
        }
        inUse[entry.ad] = entry
        entry.holder.value = holder
        logger.info(TAG, "Pool $logKey claimed by ${holder.key}")
        refill()
        return entry.ad
    }

    override fun release(ad: LoadedNativeAd) {
        val entry = inUse.remove(ad) ?: return
        when {
            closed -> {
                ad.destroy()
                logger.info(TAG, "Pool $logKey released: destroyed (closed)")
            }
            ad.impressionRecorded -> {
                ad.destroy()
                logger.info(TAG, "Pool $logKey released: destroyed (impression)")
                refill()
            }
            else -> {
                free.addLast(entry)
                logger.info(TAG, "Pool $logKey released: back")
            }
        }
    }

    /** Drops the expired free ads, then sends the next request when the pool is open, idle and not full. */
    fun refill() {
        pruneExpired()
        if (closed || job != null || free.size + inUse.size >= capacity) return
        val opening = session
        // Kept in job before it starts: a send that ends without suspending still clears it.
        val started = scope.launch(start = CoroutineStart.LAZY) { sendNext(opening) }
        job = started
        started.start()
    }

    /** Has a free ad, waits for a retry, is closed or cannot send now: the preload group may move on (S-22). */
    fun settled(): Boolean = free.isNotEmpty() || waitingRetry || closed || !canLoadNow()

    /** Consent withdrawn or every placement of the pool switched off (L-061). */
    fun close() {
        if (closed) return
        closed = true
        session++
        job?.cancel()
        job = null
        requestSent = false
        waitingRetry = false
        free.forEach { it.ad.destroy() }
        free.clear()
        logger.info(TAG, "Pool $logKey closed")
    }

    /** Consent and a placement of the pool are back. */
    fun reopen() {
        if (!closed) return
        closed = false
        failures = 0
        logger.info(TAG, "Pool $logKey reopened")
    }

    /** The one send of the pool (L-072): gate, load, then the next refill or the retry wait. */
    private suspend fun sendNext(opening: Long) {
        if (!sendGate()) {
            if (session == opening) {
                job = null
                logger.info(TAG, "Pool $logKey request blocked")
            }
            return
        }
        requestSent = true
        val result = try {
            load()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Never the message of the error: it may hold anything (O-09).
            logger.info(TAG, "Pool $logKey failed: 0 (${e.javaClass.simpleName})")
            null
        } finally {
            if (session == opening) requestSent = false
        }
        if (session != opening) {
            result?.ad?.destroy()
            return
        }
        if (result != null) {
            failures = 0
            free.addLast(Entry(result.ad, result.holder, clock.nowMillis()))
            logger.info(TAG, "Pool $logKey loaded (free ${free.size}/$capacity)")
            job = null
            refill()
            return
        }
        failures++
        waitingRetry = true
        val generation = retryResets.value
        val wait = AdRetryPolicy.nativeDelayMillis(failures, holdsAd = free.size + inUse.size > 0)
        logger.info(TAG, "Pool $logKey failed, retry in $wait ms (failures $failures)")
        withTimeoutOrNull(wait) { retryResets.first { it != generation } }
        if (session != opening) return
        waitingRetry = false
        job = null
        refill()
    }

    /**
     * Drops the free ads that may not be shown any more: 55 minutes old (O-04), or with an impression the SDK reported
     * after the cell gave the ad back (P-01a). The caller refills.
     */
    private fun pruneExpired() {
        val now = clock.nowMillis()
        val impressed = free.filter { it.ad.impressionRecorded }
        val expired = free.filter {
            !it.ad.impressionRecorded &&
                now - it.loadedAtMillis >= NativeAdPolicy.MAX_AGE_MILLIS
        }
        if (impressed.isEmpty() && expired.isEmpty()) return
        free.removeAll(impressed + expired)
        impressed.forEach {
            it.ad.destroy()
            logger.info(TAG, "Pool $logKey destroyed (impression after release)")
        }
        expired.forEach {
            it.ad.destroy()
            logger.info(TAG, "Pool $logKey expired")
        }
    }
}
