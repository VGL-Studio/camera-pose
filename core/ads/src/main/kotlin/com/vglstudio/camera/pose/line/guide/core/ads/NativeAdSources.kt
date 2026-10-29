package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

/**
 * The [NativeAdSource] of each (placement, format), testable without [AdsController] (plan T-040). Two formats of
 * one placement get two sources (L-068): the bottom slot and the collapsible layer of T-030. Each placement with a
 * source is watched in [scope] for [enabled] and [canRequestAds]: either turning false shuts down every source of
 * the placement (pending ad and load in flight destroyed, refreshing slots stop, L-061). One watcher per placement,
 * for the life of the process; placements are a fixed enum, so there are few. Main thread only.
 */
internal class NativeAdSources(
    private val enabled: (AdPlacement) -> Flow<Boolean>,
    private val canRequestAds: StateFlow<Boolean>,
    private val newLoad: (AdPlacement, AdFormat, NativeLoadUse) -> suspend () -> NativeAdState,
    private val sendGate: suspend () -> Boolean,
    private val clock: MonotonicClock,
    private val scope: CoroutineScope,
    private val logger: AppLogger,
) {
    private val sources = mutableMapOf<Pair<AdPlacement, AdFormat>, NativeAdSource>()

    /** Latest switch and consent per watched placement; not known yet counts as allowed (the slot checked both). */
    private val allowedNow = mutableMapOf<AdPlacement, Boolean>()

    fun source(placement: AdPlacement, format: AdFormat = placement.format): NativeAdSource {
        sources[placement to format]?.let { return it }
        val watched = sources.keys.any { it.first == placement }
        val source = NativeAdSource(
            load = { use -> newLoad(placement, format, use)() },
            sendGate = sendGate,
            clock = clock,
            scope = scope,
            logger = logger,
            placementKey = nativeSlotKey(placement, format),
            allowed = { canRequestAds.value && allowedNow[placement] != false },
        )
        sources[placement to format] = source
        if (!watched) watch(placement)
        return source
    }

    private fun watch(placement: AdPlacement) {
        scope.launch {
            combine(enabled(placement), canRequestAds) { on, consent -> on && consent }
                .distinctUntilChanged()
                .collect { allowed ->
                    allowedNow[placement] = allowed
                    if (!allowed) {
                        sources.filterKeys { it.first == placement }.values.forEach { it.shutDown() }
                    }
                }
        }
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
 * [retryingNativeAdStates] (claims of `SINGLE_AD`, 5 s retries with no ad), then [refreshingNativeAdStates] (60 s
 * refresh in foreground). Ends, with nothing more sent, once the source is shut down.
 */
internal fun singleAdSlotStates(
    source: NativeAdSource,
    foreground: StateFlow<Boolean>,
    mayRequestNow: () -> Boolean,
    retryResets: StateFlow<Int>,
    clock: MonotonicClock,
    logger: AppLogger,
    placementKey: String,
    firstTickDelayMillis: Long = NativeAdPolicy.REFRESH_INTERVAL_MILLIS,
): Flow<NativeAdState> = flow {
    source.attach()
    try {
        emitAll(
            retryingNativeAdStates(
                load = { source.claim(NativeLoadUse.SINGLE_AD).toStateOrNull() },
                foreground = foreground,
                mayRequestNow = mayRequestNow,
                retryResets = retryResets,
                logger = logger,
                placementKey = placementKey,
                refresh = { first ->
                    refreshingNativeAdStates(
                        first,
                        source,
                        foreground,
                        mayRequestNow,
                        retryResets,
                        clock,
                        logger,
                        placementKey,
                        firstTickDelayMillis,
                    )
                },
            ),
        )
    } finally {
        source.detach()
    }
}
