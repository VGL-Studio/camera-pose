package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** How ads were allowed (or not) in this process (S-13). Public: the loading screen (T-024) and the gallery read it. */
enum class ConsentPath { CACHED_EARLY, GRANTED, DENIED_OR_REQUIRED, ERROR_ALLOWED, INTERRUPTED_REGATHER }

internal enum class UmpFailure { NETWORK, CONFIGURATION, INTERRUPTED }

/** FormError.ErrorCode.INTERNET_ERROR of UMP (measured in plan T-020, section 4.1). */
private const val UMP_INTERNET_ERROR = 2

/** FormError.ErrorCode.TIME_OUT of UMP. */
private const val UMP_TIME_OUT = 4

/** S-04, S-05: INTERNET_ERROR (2), TIME_OUT (4) are NETWORK; a form error while the activity is gone is INTERRUPTED first. */
internal fun umpFailureOf(errorCode: Int, activityGone: Boolean): UmpFailure = when {
    activityGone -> UmpFailure.INTERRUPTED
    errorCode == UMP_INTERNET_ERROR || errorCode == UMP_TIME_OUT -> UmpFailure.NETWORK
    else -> UmpFailure.CONFIGURATION
}

/** S-12: both the build switch of :app and is_enable_consent_shortcut. */
internal fun shortcutActive(enabledInBuild: Boolean, remoteEnabled: Boolean): Boolean = enabledInBuild && remoteEnabled

internal data class ConsentGateState(
    /** Null before the first result (S-19). */
    val path: ConsentPath?,
    /** The load and show gate (S-10, S-11, S-15). */
    val allowed: Boolean,
    /** Allowed only by CACHED_EARLY or ERROR_ALLOWED. */
    val temporary: Boolean,
    /** S-06. */
    val settled: Boolean,
) {
    companion object {
        val INITIAL = ConsentGateState(path = null, allowed = false, temporary = false, settled = false)
    }
}

/** The consent rules of docs/ops/ads-business-rules.md (S-03 to S-19), with no Android or UMP call. Main thread only. */
internal class ConsentGate {
    private val _state = MutableStateFlow(ConsentGateState.INITIAL)

    val state: StateFlow<ConsentGateState> = _state.asStateFlow()

    /** The consent shortcut (S-12), read when a result comes, never when the request is sent (S-06). */
    private var shortcut = false

    private val current get() = _state.value

    /** S-12: turning the shortcut off revokes at once a right only ERROR_ALLOWED gave; turning it on revokes nothing. */
    fun onShortcutChanged(active: Boolean) {
        shortcut = active
        if (!active && current.path == ConsentPath.ERROR_ALLOWED && current.temporary && !current.settled) {
            _state.value = current.copy(allowed = false, temporary = false)
        }
    }

    /** S-03: consent of an earlier session opens the gate before UMP answers, shortcut on only. */
    fun onRequestSent(umpCanRequestAds: Boolean) {
        if (shortcut && umpCanRequestAds && current.path == null && !current.settled) {
            _state.value = current.copy(path = ConsentPath.CACHED_EARLY, allowed = true, temporary = true)
        }
    }

    /** S-10: UMP says consent must be asked again: a temporary right closes now, before the form. */
    fun onUpdateSucceeded(umpCanRequestAds: Boolean) {
        if (current.temporary && !umpCanRequestAds) {
            _state.value = current.copy(path = ConsentPath.DENIED_OR_REQUIRED, allowed = false, temporary = false)
        }
    }

    /** S-04, S-05, S-06: a failed update never settles, and with the shortcut off it gives nothing (S-12). */
    fun onUpdateFailed(failure: UmpFailure, umpCanRequestAds: Boolean) {
        onFailure(failure, umpCanRequestAds, formStep = false)
    }

    /** S-05, S-06: a clean result always settles; with the shortcut off, a form error settles once UMP allows ads. */
    fun onFormFinished(failure: UmpFailure?, umpCanRequestAds: Boolean) {
        if (failure == null) {
            _state.value = finalState(umpCanRequestAds)
        } else {
            onFailure(failure, umpCanRequestAds, formStep = true)
        }
    }

    /** S-14: the user changed their consent in the privacy options form. */
    fun onPrivacyFormClosed(umpCanRequestAds: Boolean) {
        _state.value = finalState(umpCanRequestAds)
    }

    /** S-18: back to the first state; the shortcut is kept. */
    fun onReset() {
        _state.value = ConsentGateState.INITIAL
    }

    /** S-08, S-15: may a request be sent right now. */
    fun mayRequestNow(foreground: Boolean): Boolean =
        current.allowed && (foreground || current.path != ConsentPath.ERROR_ALLOWED)

    /** S-09: a regather is due (ERROR_ALLOWED and not settled). */
    val regatherDue: Boolean
        get() = current.path == ConsentPath.ERROR_ALLOWED && current.allowed && !current.settled

    private fun finalState(umpCanRequestAds: Boolean) = ConsentGateState(
        path = if (umpCanRequestAds) ConsentPath.GRANTED else ConsentPath.DENIED_OR_REQUIRED,
        allowed = umpCanRequestAds,
        temporary = false,
        settled = true,
    )

    private fun onFailure(failure: UmpFailure, umpCanRequestAds: Boolean, formStep: Boolean) {
        val cachedEarly = current.path == ConsentPath.CACHED_EARLY && current.allowed
        _state.value = when {
            // The user has not answered (S-05): ask again next time, with no temporary right.
            failure == UmpFailure.INTERRUPTED -> ConsentGateState(
                path = ConsentPath.INTERRUPTED_REGATHER,
                allowed = umpCanRequestAds,
                temporary = false,
                settled = false,
            )
            // The real right of the earlier session is kept, never lowered to the network one (S-04, S-05).
            cachedEarly -> current
            failure == UmpFailure.NETWORK && shortcut -> ConsentGateState(
                path = ConsentPath.ERROR_ALLOWED,
                allowed = true,
                temporary = true,
                settled = false,
            )
            // A form error follows the right UMP already has; an update error gives nothing.
            else -> ConsentGateState(
                path = if (umpCanRequestAds && formStep) ConsentPath.GRANTED else ConsentPath.DENIED_OR_REQUIRED,
                allowed = umpCanRequestAds && formStep,
                temporary = false,
                settled = umpCanRequestAds && formStep && !shortcut,
            )
        }
    }
}
