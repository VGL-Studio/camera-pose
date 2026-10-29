package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.lang.ref.WeakReference
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Runs the consent rounds of :core:ads over [ConsentPlatform] and [ConsentGate]: gather, regather, privacy form,
 * reset (S-01 to S-19). Calls [onGateOpened] each time the gate opens: the first time to start the SDK, later to
 * preload again. Logs every path, round, result, settle, revoke, regather and reset under the tag `Ads`, with UMP
 * error codes only (ADR-0019). Main thread only.
 */
internal class ConsentCoordinator(
    private val platform: ConsentPlatform,
    private val gate: ConsentGate,
    private val suppressor: ForegroundAdSuppressor,
    private val isActivityGone: (Activity) -> Boolean,
    private val isResumed: (Activity) -> Boolean,
    private val fullScreenShowing: () -> Boolean,
    private val allowReset: Boolean,
    private val logger: AppLogger,
    private val onGateOpened: (firstTime: Boolean) -> Unit,
) {
    private val _finished = MutableStateFlow(false)

    /** The round of this start has its final result, or the result was settled before. */
    val finished: StateFlow<Boolean> = _finished.asStateFlow()

    private val _consentFormOpen = MutableStateFlow(false)

    /**
     * True while the consent form really shows: from the hold of [CONSENT_FORM] to the close of the form, an error or
     * an interruption included (plan T-033: AppsFlyer does not start under an open form).
     */
    val consentFormOpen: StateFlow<Boolean> = _consentFormOpen.asStateFlow()

    private val _privacyOptionsRequired = MutableStateFlow(false)

    /** UMP requires the privacy options entry point ([PrivacyOptionsButton], S-14). */
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired.asStateFlow()

    private var started = false
    private var shortcutKnown = false
    private var waitingActivity: WeakReference<Activity>? = null
    private var running = false

    /** The activity of the last round, for the regather (S-09). */
    private var askedActivity: WeakReference<Activity>? = null

    /** A regather event came while it could not run yet (activity not resumed, full-screen ad on screen). */
    private var regatherWanted = false
    private var lastForeground: Boolean? = null
    private var lastConnected: Boolean? = null

    private var lastAllowed = false
    private var everOpened = false
    private var lastLoggedPath: ConsentPath? = null

    private val state get() = gate.state.value

    /** S-01: false when already started. */
    fun start(): Boolean {
        if (started) return false
        started = true
        return true
    }

    /** HD-04: the shortcut (build switch and remote key), before the first round and again on every change. */
    fun onShortcutChanged(active: Boolean) {
        gate.onShortcutChanged(active)
        val wasRevoked = lastAllowed && !state.allowed
        if (wasRevoked) logger.info(TAG, "Consent temporary right revoked: shortcut off")
        afterGateChange()
        if (!shortcutKnown) {
            shortcutKnown = true
            waitingActivity?.get()?.let { activity ->
                waitingActivity = null
                gather(activity)
            }
        }
    }

    /** S-02: update, form if required, then read whether ads may be requested. */
    fun gather(activity: Activity) {
        if (!started) {
            logger.warn(TAG, "Consent gather before start: ignored")
            return
        }
        if (!shortcutKnown) {
            logger.info(TAG, "Consent gather waits for the shortcut value")
            waitingActivity = WeakReference(activity)
            return
        }
        if (state.settled) {
            _finished.value = true
            return
        }
        // S-07: a second round while the first one runs shares its result.
        if (running) return
        running = true
        askedActivity = WeakReference(activity)
        logger.info(TAG, "Consent update sent")
        var answered = false
        platform.requestUpdate(
            activity,
            onSuccess = {
                answered = true
                onUpdateSucceeded(activity)
            },
            onFailure = { code ->
                answered = true
                onUpdateFailed(code)
            },
        )
        // S-03: read right after sending, unless UMP has already answered.
        if (!answered) {
            gate.onRequestSent(platform.canRequestAds())
            afterGateChange()
        }
    }

    /** S-09: back to foreground. */
    fun onForegroundChanged(foreground: Boolean) {
        val previous = lastForeground
        lastForeground = foreground
        if (previous == false && foreground) requestRegather("foreground")
    }

    /** S-09: Internet back. */
    fun onInternetChanged(connected: Boolean) {
        val previous = lastConnected
        lastConnected = connected
        if (previous == false && connected) requestRegather("internet back")
    }

    /** S-09: a preload is about to run again (T-021). */
    fun onPreloadRequested() {
        requestRegather("preload")
    }

    /** S-09: a regather that waited for the asked activity runs once it is resumed. */
    fun onActivityResumed(activity: Activity) {
        if (regatherWanted && askedActivity?.get() === activity) runRegatherIfPossible()
    }

    /** S-14, S-16: the privacy options form, holding the suppression while it is open. */
    fun showPrivacyOptions(activity: Activity) {
        val hold = suppressor.acquire(PRIVACY_FORM)
        var closed = false
        logger.info(TAG, "Privacy options form opened")
        platform.showPrivacyForm(activity) { code ->
            hold.release()
            if (closed) return@showPrivacyForm
            closed = true
            if (code != null) logger.warn(TAG, "Privacy options form failed: $code")
            gate.onPrivacyFormClosed(platform.canRequestAds())
            _privacyOptionsRequired.value = platform.privacyOptionsRequired()
            if (!state.allowed) logger.info(TAG, "Consent withdrawn")
            afterGateChange()
        }
    }

    /** S-18: staging only; clears the stored consent and the settled result. */
    fun reset() {
        if (!allowReset) {
            logger.info(TAG, "Consent reset ignored: not allowed in this build")
            return
        }
        platform.reset()
        gate.onReset()
        running = false
        regatherWanted = false
        _finished.value = false
        _consentFormOpen.value = false
        lastLoggedPath = null
        logger.info(TAG, "Consent reset")
        afterGateChange()
    }

    fun mayRequestNow(foreground: Boolean): Boolean = gate.mayRequestNow(foreground)

    private fun onUpdateSucceeded(activity: Activity) {
        logger.info(TAG, "Consent update done")
        gate.onUpdateSucceeded(platform.canRequestAds())
        _privacyOptionsRequired.value = platform.privacyOptionsRequired()
        afterGateChange()
        // S-16: only a form that really shows holds the suppression.
        val hold = if (platform.consentFormRequired()) suppressor.acquire(CONSENT_FORM) else null
        if (hold != null) _consentFormOpen.value = true
        var closed = false
        platform.showFormIfRequired(activity) { code ->
            hold?.release()
            _consentFormOpen.value = false
            if (closed) return@showFormIfRequired
            closed = true
            val failure = code?.let { umpFailureOf(it, isActivityGone(activity)) }
            if (code != null) logger.warn(TAG, "Consent form failed: $code ($failure)")
            gate.onFormFinished(failure, platform.canRequestAds())
            _privacyOptionsRequired.value = platform.privacyOptionsRequired()
            onRoundFinished()
        }
    }

    private fun onUpdateFailed(code: Int) {
        val failure = umpFailureOf(code, activityGone = false)
        logger.warn(TAG, "Consent update failed: $code ($failure)")
        gate.onUpdateFailed(failure, platform.canRequestAds())
        _privacyOptionsRequired.value = platform.privacyOptionsRequired()
        onRoundFinished()
    }

    private fun onRoundFinished() {
        running = false
        _finished.value = true
        if (state.settled) logger.info(TAG, "Consent settled")
        afterGateChange()
    }

    private fun requestRegather(reason: String) {
        if (!gate.regatherDue) return
        logger.info(TAG, "Consent regather due: $reason")
        regatherWanted = true
        runRegatherIfPossible()
    }

    private fun runRegatherIfPossible() {
        if (!gate.regatherDue) {
            regatherWanted = false
            return
        }
        val activity = askedActivity?.get() ?: return
        if (running || !isResumed(activity) || fullScreenShowing()) return
        regatherWanted = false
        logger.info(TAG, "Consent regather")
        gather(activity)
    }

    /** Logs a new path and calls [onGateOpened] when the gate goes from closed to open. */
    private fun afterGateChange() {
        val current = state
        if (current.path != null && current.path != lastLoggedPath) {
            lastLoggedPath = current.path
            logger.info(TAG, "Consent path: ${current.path}")
        }
        val opened = current.allowed && !lastAllowed
        lastAllowed = current.allowed
        if (opened) {
            val firstTime = !everOpened
            everOpened = true
            onGateOpened(firstTime)
        }
    }

    private companion object {
        const val TAG = "Ads"
        const val CONSENT_FORM = "consent_form"
        const val PRIVACY_FORM = "privacy_form"
    }
}
