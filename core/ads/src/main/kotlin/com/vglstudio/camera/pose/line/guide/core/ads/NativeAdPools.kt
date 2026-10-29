package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * The [NativeAdPool] of each (format, ad unit) of [placements] (plan T-028), testable without [AdsController], like
 * [NativeAdSources]. Watches, in [scope], the switch of every placement and [canRequestAds]: consent withdrawn, or
 * every placement of a pool switched off, closes that pool (L-061); back on, it opens again. Gives each pooled format
 * one slot of the preload plan ([preloadSlots], HD-02, S-22, S-25). Main thread only.
 */
internal class NativeAdPools(
    private val placements: List<AdPlacement>,
    private val unitId: (AdPlacement) -> String?,
    private val enabled: (AdPlacement) -> Flow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val newPool: (NativePoolKey, AdPlacement) -> NativeAdPool,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    private val pools = mutableMapOf<NativePoolKey, NativeAdPool>()

    /** Latest switch of each placement; not known yet counts as off for the preload plan. */
    private val enabledNow = mutableMapOf<AdPlacement, Boolean>()

    init {
        val pooled = placements.filter { it.format.usesPool }
        if (pooled.isNotEmpty()) {
            scope.launch {
                combine(pooled.map { placement -> enabled(placement) }) { it.toList() }
                    .combine(canRequestAds) { switches, consent -> switches to consent }
                    .collect { (switches, consent) ->
                        pooled.zip(switches).forEach { (placement, on) -> enabledNow[placement] = on }
                        pools.values.forEach { update(it, consent) }
                    }
            }
        }
    }

    /** The pool of [placement]; null for a format with no pool or a placement with no ad unit. */
    fun pool(placement: AdPlacement): NativeAdPool? {
        if (!placement.format.usesPool) return null
        val key = keyOf(placement) ?: return null
        pools[key]?.let { return it }
        val pool = newPool(key, placement)
        pools[key] = pool
        logger.info(TAG, "Pool ${key.format.code} created for ${placement.key}")
        update(pool, canRequestAds.value)
        return pool
    }

    /** One slot of the preload plan for a pooled [format], none for another format. */
    fun preloadSlots(format: AdFormat): List<PreloadSlot> =
        if (format.usesPool) listOf(NativePoolPreloadSlot(format)) else emptyList()

    private fun keyOf(placement: AdPlacement): NativePoolKey? =
        unitId(placement)?.let { NativePoolKey(placement.format, it) }

    /** Closes [pool] when consent is withdrawn or every placement of it is known off; opens it again otherwise. */
    private fun update(pool: NativeAdPool, consent: Boolean) {
        val members = placements.filter { it.format == pool.key.format && keyOf(it) == pool.key }
        val anyOn = members.any { enabledNow[it] != false }
        if (consent && anyOn) pool.reopen() else pool.close()
    }

    /**
     * The preload slot of the pools of [format]: refills the pool of every placement of it switched on (made when
     * needed); settled once each of these pools is.
     */
    private inner class NativePoolPreloadSlot(override val format: AdFormat) : PreloadSlot {
        private fun enabledPools(): List<NativeAdPool> = placements
            .filter { it.format == format && enabledNow[it] == true }
            .mapNotNull { pool(it) }
            .distinct()

        override fun loadIfNeeded() {
            enabledPools().forEach { it.refill() }
        }

        override fun settled(): Boolean = enabledPools().all { it.settled() }
    }

    private companion object {
        const val TAG = "Ads"
    }
}
