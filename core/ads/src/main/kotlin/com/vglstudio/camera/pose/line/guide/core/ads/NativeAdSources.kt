package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * The native stocks ([NativeAdSource]) of [placements], one per (format, ad unit) (plan T-086, KB-33), testable
 * without [AdsController], like [NativeAdPools]: placements of one format with the same ad unit share a stock, and two
 * formats of one placement get two (L-068): the bottom slot (native small) and the collapsible layer. A stock lives
 * across screens.
 *
 * Watches, in [scope], the switch of every placement that uses a stock ([enabled]), [canRequestAds] and [globalFlags]:
 * - a placement switched off, or consent withdrawn, moves [stopped] of that placement: its slots stop (L-061);
 * - a stock closes ([NativeAdSource.shutDown]) when consent is withdrawn, every placement of it is off, or (the
 *   collapsible stock) the layer is off; it never fills by itself when they come back: the preload plan does (S-23).
 *
 * Each stock fills only by [stockMayFill] (KB-33, criterion 6), and has one slot of the preload plan per format
 * ([preloadSlots], HD-02, S-22, S-25). A placement inside a list ([AdPlacement.nativeEvery]) loads its own rows and
 * uses no stock. Main thread only.
 */
internal class NativeAdSources(
    private val placements: List<AdPlacement>,
    private val unitId: (AdPlacement, AdFormat) -> String?,
    private val enabled: (AdPlacement) -> Flow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val globalFlags: StateFlow<AdsGlobalFlags>,
    private val newLoad: (NativePoolKey, NativeLoadUse, AdPlacement?) -> suspend () -> NativeSourceLoad,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    private val stocks = mutableMapOf<NativePoolKey, NativeAdSource>()

    /** Stocks the watcher closed, until they may open again. */
    private val shut = mutableSetOf<NativePoolKey>()

    /** The placements that use a stock, of any format. */
    private val users = placements.filter { placement -> STOCK_FORMATS.any { placement.usesStock(it) } }

    /** Latest switch of each placement using a stock; not known yet counts as on for a slot, off for the preload. */
    private val enabledNow = mutableMapOf<AdPlacement, Boolean>()

    /** Latest switch and consent of each placement, for [stopped]. */
    private val allowedNow = mutableMapOf<AdPlacement, Boolean>()

    private val stops = mutableMapOf<AdPlacement, MutableStateFlow<Long>>()

    init {
        if (users.isNotEmpty()) {
            scope.launch {
                combine(users.map { placement -> enabled(placement) }) { it.toList() }
                    .combine(canRequestAds) { switches, consent -> switches to consent }
                    .combine(globalFlags) { (switches, consent), flags -> Triple(switches, consent, flags) }
                    .collect { (switches, consent, flags) -> onChanged(switches, consent, flags) }
            }
        }
    }

    /** The stock of [placement] drawn as [format]; the caller checked that the placement has an ad unit. */
    fun source(placement: AdPlacement, format: AdFormat = placement.format): NativeAdSource {
        val unit = requireNotNull(unitId(placement, format)) { "${nativeSlotKey(placement, format)} has no ad unit" }
        return stockOf(NativePoolKey(format, unit))
    }

    /** Moves each time [placement] is switched off or consent withdrawn: its slots stop (L-061). */
    fun stopped(placement: AdPlacement): StateFlow<Long> = stopsOf(placement)

    /** One slot of the preload plan for a stock format (native small, collapsible), none for another format. */
    fun preloadSlots(format: AdFormat): List<PreloadSlot> =
        if (format in STOCK_FORMATS) listOf(NativeStockPreloadSlot(format)) else emptyList()

    private fun stopsOf(placement: AdPlacement) = stops.getOrPut(placement) { MutableStateFlow(0L) }

    private fun stockOf(key: NativePoolKey): NativeAdSource {
        stocks[key]?.let { return it }
        val logKey = "${key.format.code}#${stocks.size + 1}"
        val stock = NativeAdSource(
            load = { use, placement -> newLoad(key, use, placement)() },
            sendGate = sendGate,
            clock = clock,
            scope = scope,
            logger = logger,
            placementKey = logKey,
            allowed = { placement -> mayClaim(key, placement) },
            mayFill = { slotOnScreen ->
                stockMayFill(
                    format = key.format,
                    consent = canRequestAds.value,
                    flags = globalFlags.value,
                    membersOn = membersOf(key).any { enabledNow[it] != false },
                    slotOnScreen = slotOnScreen,
                )
            },
        )
        stocks[key] = stock
        logger.info(TAG, "Native $logKey stock created")
        if (!mayOpen(key, canRequestAds.value, globalFlags.value)) shut += key
        return stock
    }

    /** A claim of a slot of [placement]: consent, the placement not known off, and the layer on for its stock. */
    private fun mayClaim(key: NativePoolKey, placement: AdPlacement): Boolean = canRequestAds.value &&
        allowedNow[placement] != false &&
        (key.format != AdFormat.NATIVE_COLLAPSIBLE || globalFlags.value.showNativeCollap)

    private fun membersOf(key: NativePoolKey): List<AdPlacement> =
        users.filter { it.usesStock(key.format) && unitId(it, key.format) == key.unitId }

    /** Consent, a placement of the stock not known off, and for the collapsible stock its layer on. */
    private fun mayOpen(key: NativePoolKey, consent: Boolean, flags: AdsGlobalFlags): Boolean = consent &&
        membersOf(key).any { enabledNow[it] != false } &&
        (key.format != AdFormat.NATIVE_COLLAPSIBLE || flags.showNativeCollap)

    private fun onChanged(switches: List<Boolean>, consent: Boolean, flags: AdsGlobalFlags) {
        val stopping = users.zip(switches).mapNotNull { (placement, on) ->
            enabledNow[placement] = on
            val allowed = on && consent
            val stopsNow = !allowed && allowedNow[placement] != false
            allowedNow[placement] = allowed
            placement.takeIf { stopsNow }
        }
        stocks.forEach { (key, stock) ->
            if (mayOpen(key, consent, flags)) {
                shut -= key
            } else if (shut.add(key)) {
                stock.shutDown()
            }
        }
        // After the closes: a closed stock already gave its waiters NoAd; an open one blocks those of the placement.
        stopping.forEach(::stop)
    }

    /** Stops the slots of [placement]: [stopped] moves, and their claims waiting on a stock end. */
    private fun stop(placement: AdPlacement) {
        stopsOf(placement).value += 1
        stocks.values.forEach { it.release(placement) }
    }

    /**
     * The preload slot of the stocks of [format]: fills the stock of every placement of it switched on (made when
     * needed), each by its own rule ([stockMayFill]); settled once none of these stocks has a load in flight.
     */
    private inner class NativeStockPreloadSlot(override val format: AdFormat) : PreloadSlot {
        private fun stocksToFill(): List<NativeAdSource> = users
            .filter { it.usesStock(format) && enabledNow[it] == true }
            .mapNotNull { placement -> unitId(placement, format)?.let { stockOf(NativePoolKey(format, it)) } }
            .distinct()

        override fun loadIfNeeded() {
            stocksToFill().forEach { it.fill() }
        }

        override fun settled(): Boolean = stocksToFill().all { it.settled() }
    }

    private companion object {
        const val TAG = "Ads"
        val STOCK_FORMATS = listOf(AdFormat.NATIVE_SMALL, AdFormat.NATIVE_COLLAPSIBLE)
    }
}

/**
 * Whether this placement takes its ads of [format] from a stock: a native small placement for its own slot and the
 * collapsible layer of its bottom bar ([layerFormats]); never a placement inside a list, whose rows load their own.
 */
internal fun AdPlacement.usesStock(format: AdFormat): Boolean = nativeEvery == null &&
    when (format) {
        AdFormat.NATIVE_SMALL -> this.format == AdFormat.NATIVE_SMALL
        AdFormat.NATIVE_COLLAPSIBLE -> AdFormat.NATIVE_COLLAPSIBLE in declaration.layerFormats
        else -> false
    }

/**
 * KB-33, criterion 6: whether a stock of [format] may start a load of its own (its preload slot, the next load after
 * a claim, the reload after 55 minutes). Always consent and a placement of the stock on ([membersOn]); then the native
 * small stock needs `is_show_native_bottom` on or a slot of it on screen ([slotOnScreen]: with the flag off, the bar
 * is a banner, so such a slot is a standalone one); the collapsible stock needs its layer on (`showNativeCollap`,
 * which needs `is_show_native_bottom` too), a slot on screen or not.
 */
internal fun stockMayFill(
    format: AdFormat,
    consent: Boolean,
    flags: AdsGlobalFlags,
    membersOn: Boolean,
    slotOnScreen: Boolean,
): Boolean {
    if (!consent || !membersOn) return false
    return when (format) {
        AdFormat.NATIVE_SMALL -> flags.showNativeBottom || slotOnScreen
        AdFormat.NATIVE_COLLAPSIBLE -> flags.showNativeCollap
        else -> false
    }
}

/**
 * The formats whose slot goes through a [NativeAdSource] and refreshes in place: native small (plan T-040) and the
 * collapsible layer of the bottom bar, one ad and one pending ad (CL-02, plan T-030).
 */
internal fun AdFormat.usesSingleAdSource(): Boolean =
    this == AdFormat.NATIVE_SMALL || this == AdFormat.NATIVE_COLLAPSIBLE

/** The log and counter key of [placement] drawn as [format]: the placement key for its own format. */
internal fun nativeSlotKey(placement: AdPlacement, format: AdFormat): String =
    if (format == placement.format) placement.key else "${placement.key}/${format.code}"

/**
 * The states of one single-ad slot of [source] on screen: attached while collected; the first ad through
 * [retryingNativeAdStates] (claims of `SINGLE_AD`, 5 s retries with no ad), then [refreshingNativeAdStates] (refresh
 * in foreground at [intervalMillis], the first count longer by [firstTickExtraMillis]), each claim for [placement].
 * Ends, with nothing more sent, once [stopped] moves (the placement off or consent withdrawn).
 */
internal fun singleAdSlotStates(
    source: NativeAdSource,
    placement: AdPlacement,
    stopped: StateFlow<Long>,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    clock: MonotonicClock,
    logger: AppLogger,
    placementKey: String,
    intervalMillis: StateFlow<Long>,
    firstTickExtraMillis: Long = 0,
): Flow<NativeAdState> = flow {
    // Read once, before the first claim: a stop while the slot waits still ends its refresh.
    val since = stopped.value
    source.attach()
    try {
        emitAll(
            retryingNativeAdStates(
                load = { source.claim(NativeLoadUse.SINGLE_AD, placement).toStateOrNull() },
                foreground = foreground,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
                logger = logger,
                placementKey = placementKey,
                refresh = { first ->
                    refreshingNativeAdStates(
                        first,
                        source,
                        placement,
                        stopped,
                        foreground,
                        mayRequestNow,
                        retryResets,
                        clock,
                        logger,
                        placementKey,
                        intervalMillis,
                        firstTickExtraMillis,
                        since,
                    )
                },
            ),
        )
    } finally {
        source.detach()
    }
}
