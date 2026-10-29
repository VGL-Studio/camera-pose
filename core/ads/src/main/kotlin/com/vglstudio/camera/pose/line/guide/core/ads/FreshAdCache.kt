package com.vglstudio.camera.pose.line.guide.core.ads

/**
 * Preloaded ads per placement, each usable for [maxAgeMillis] after it was loaded (Google advises against showing a
 * native ad older than one hour). Times come from a monotonic clock (`SystemClock.elapsedRealtime`). An ad that is
 * replaced or has expired goes to [onDiscard], which destroys it. Main thread only.
 */
internal class FreshAdCache<T>(private val maxAgeMillis: Long = MAX_AGE_MILLIS, private val onDiscard: (T) -> Unit) {
    private class Entry<T>(val ad: T, val loadedAtMillis: Long)

    private val entries = mutableMapOf<AdPlacement, Entry<T>>()

    /** Keeps [ad] for [key], discarding the ad kept before it. */
    fun put(key: AdPlacement, ad: T, nowMillis: Long) {
        entries.put(key, Entry(ad, nowMillis))?.let { onDiscard(it.ad) }
    }

    fun hasFresh(key: AdPlacement, nowMillis: Long): Boolean = entries[key]?.isFresh(nowMillis) == true

    /** Removes and returns the ad of [key]; null, and the ad discarded, when it is older than [maxAgeMillis]. */
    fun take(key: AdPlacement, nowMillis: Long): T? {
        val entry = entries.remove(key) ?: return null
        if (entry.isFresh(nowMillis)) return entry.ad
        onDiscard(entry.ad)
        return null
    }

    /** Whether an ad of [key] is kept, fresh or not. */
    fun contains(key: AdPlacement): Boolean = key in entries

    private fun Entry<T>.isFresh(nowMillis: Long): Boolean = nowMillis - loadedAtMillis < maxAgeMillis

    companion object {
        const val MAX_AGE_MILLIS = 60 * 60 * 1_000L
    }
}

/** The cache of preloaded native full-screen ads: 55 minutes, like every native ad (O-04, plan T-040). */
internal fun <T> nativeFullScreenCache(onDiscard: (T) -> Unit): FreshAdCache<T> =
    FreshAdCache(NativeAdPolicy.MAX_AGE_MILLIS, onDiscard)
