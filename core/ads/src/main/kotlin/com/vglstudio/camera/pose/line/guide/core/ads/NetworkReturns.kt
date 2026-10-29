package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.distinctUntilChanged

/** S-24: changes of usable Internet within this window count as one. */
internal const val NETWORK_SETTLE_MILLIS = 3_000L

/**
 * Emits once each time usable Internet, stable for [windowMillis], comes back after a stable loss. A first stable
 * `true` is no return. A first value `false` (the app opened offline, L-A16) is a loss at once.
 */
internal fun networkReturns(connected: Flow<Boolean>, windowMillis: Long = NETWORK_SETTLE_MILLIS): Flow<Unit> =
    channelFlow {
        var stable: Boolean? = null
        connected.distinctUntilChanged().collectLatest { value ->
            if (stable == null && !value) {
                stable = false
                return@collectLatest
            }
            // Cancelled by the next change: only a value kept for the whole window counts.
            delay(windowMillis)
            val previous = stable
            stable = value
            if (previous == false && value) send(Unit)
        }
    }
