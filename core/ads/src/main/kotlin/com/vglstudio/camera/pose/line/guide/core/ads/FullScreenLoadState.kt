package com.vglstudio.camera.pose.line.guide.core.ads

/** Where the full-screen ad of one placement is in its loading (F-04). */
internal sealed interface FullScreenLoadState {
    data object Idle : FullScreenLoadState

    data object Loading : FullScreenLoadState

    data object Ready : FullScreenLoadState

    /** A retry is set for [dueAtMillis] of the monotonic clock (set by T-021). */
    data class WaitingRetry(val dueAtMillis: Long) : FullScreenLoadState
}

/**
 * Never loads while loading, ready, or waiting for a retry that is not due; a retry that fell due while the consent
 * gate was closed no longer counts as waiting (A 03:30-32).
 */
internal fun needsLoad(state: FullScreenLoadState, now: Long, gateOpen: Boolean): Boolean = gateOpen &&
    when (state) {
        FullScreenLoadState.Idle -> true
        FullScreenLoadState.Loading, FullScreenLoadState.Ready -> false
        is FullScreenLoadState.WaitingRetry -> now >= state.dueAtMillis
    }
