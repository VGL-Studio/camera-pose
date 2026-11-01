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

/** S-12: ERROR_ALLOWED needs both the build switch of :app and is_enable_consent_shortcut; CACHED_EARLY needs neither (ADR-0038). */
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

    /** The ERROR_ALLOWED switch (S-12), read when a result comes (S-06); CACHED_EARLY does not read it. */
    private var shortcut = false

    /** CACHED_EARLY was kept through a failed round while UMP still allowed ads: a regather is due (S-03, S-09). */
    private var cachedKeptAfterFailure = false

    private val current get() = _state.value

    /** S-12: turning the shortcut off revokes at once a right only ERROR_ALLOWED gave; turning it on revokes nothing. */
    fun onShortcutChanged(active: Boolean) {
        shortcut = active
        if (!active && current.path == ConsentPath.ERROR_ALLOWED && current.temporary && !current.settled) {
            _state.value = current.copy(allowed = false, temporary = false)
        }
    }

    /** S-03: consent of an earlier session opens the gate before UMP answers, in every build (ADR-0038). */
    fun onRequestSent(umpCanRequestAds: Boolean) {
        if (umpCanRequestAds && current.path == null && !current.settled) {
            cachedKeptAfterFailure = false
            _state.value = current.copy(path = ConsentPath.CACHED_EARLY, allowed = true, temporary = true)
        }
    }

    /** S-10: UMP says consent must be asked again: a temporary right closes now, before the form. */
    fun onUpdateSucceeded(umpCanRequestAds: Boolean) {
        if (current.temporary && !umpCanRequestAds) {
            cachedKeptAfterFailure = false
            _state.value = current.copy(path = ConsentPath.DENIED_OR_REQUIRED, allowed = false, temporary = false)
        }
    }

    /**
     * S-04, S-05, S-06: a failed update never settles. CACHED_EARLY is kept while UMP still allows ads; otherwise,
     * with the ERROR_ALLOWED switch off, it gives nothing (S-12).
     */
    fun onUpdateFailed(failure: UmpFailure, umpCanRequestAds: Boolean) {
        onFailure(failure, umpCanRequestAds, formStep = false)
    }

    /**
     * S-05, S-06: a clean result always settles; with the ERROR_ALLOWED switch off, a form error settles once UMP
     * allows ads, CACHED_EARLY included (as GRANTED).
     */
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
        cachedKeptAfterFailure = false
        _state.value = ConsentGateState.INITIAL
    }

    /** S-08, S-15: may a request be sent right now. */
    fun mayRequestNow(foreground: Boolean): Boolean =
        current.allowed && (foreground || current.path != ConsentPath.ERROR_ALLOWED)

    /** S-09: a regather is due (ERROR_ALLOWED, or CACHED_EARLY kept through a failed round; not settled). */
    val regatherDue: Boolean
        get() {
            val path = current.path
            val cachedDue = path == ConsentPath.CACHED_EARLY && cachedKeptAfterFailure
            val dueOnPath = path == ConsentPath.ERROR_ALLOWED || cachedDue
            return dueOnPath && current.allowed && !current.settled
        }

    private fun finalState(umpCanRequestAds: Boolean): ConsentGateState {
        cachedKeptAfterFailure = false
        return ConsentGateState(
            path = if (umpCanRequestAds) ConsentPath.GRANTED else ConsentPath.DENIED_OR_REQUIRED,
            allowed = umpCanRequestAds,
            temporary = false,
            settled = true,
        )
    }

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
            // S-06 with ERROR_ALLOWED off: a form error once UMP allows ads settles, CACHED_EARLY included (KB-11, criterion 8).
            formStep && umpCanRequestAds && !shortcut -> ConsentGateState(
                path = ConsentPath.GRANTED,
                allowed = true,
                temporary = false,
                settled = true,
            )
            // The real right of the earlier session is kept while UMP still allows ads, never lowered to the
            // network one, and asked again (S-03, S-04, S-05, S-09).
            cachedEarly && umpCanRequestAds -> current
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
                settled = false,
            )
        }
        // Only the branch that keeps the real right of the earlier session leaves CACHED_EARLY.
        cachedKeptAfterFailure = current.path == ConsentPath.CACHED_EARLY
    }
}
