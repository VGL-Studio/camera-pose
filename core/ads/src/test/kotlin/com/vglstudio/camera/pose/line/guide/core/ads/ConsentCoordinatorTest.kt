package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The consent rounds of :core:ads with a fake UMP (plan T-020, section 4). */
class ConsentCoordinatorTest {
    private val activityA = mockk<Activity>(relaxed = true)
    private val activityB = mockk<Activity>(relaxed = true)

    /** One process: a fake UMP, the real gate and suppressor, and what the coordinator reported. */
    private inner class Env(allowReset: Boolean = false) {
        val platform = FakeConsentPlatform()
        val gate = ConsentGate()
        val suppressor = ForegroundAdSuppressor()
        val logger = RecordingAppLogger()
        var activityGone = false
        val resumed = mutableSetOf(activityA)
        var adShowing = false
        val opened = mutableListOf<Boolean>()
        val coordinator = ConsentCoordinator(
            platform = platform,
            gate = gate,
            suppressor = suppressor,
            isActivityGone = { activityGone },
            isResumed = { it in resumed },
            fullScreenShowing = { adShowing },
            allowReset = allowReset,
            logger = logger,
            onGateOpened = { opened += it },
        )

        val state get() = gate.state.value
        val updates get() = platform.count("requestUpdate")

        fun started(shortcut: Boolean) = apply {
            coordinator.start()
            coordinator.onShortcutChanged(shortcut)
        }

        /** Consent given in the form (shortcut off). */
        fun granted() = apply {
            started(shortcut = false)
            platform.canRequest = true
            coordinator.gather(activityA)
            platform.completeUpdate()
            platform.closeForm(null)
        }

        /** ERROR_ALLOWED on activity A (shortcut on, network failure of the update). */
        fun errorAllowed() = apply {
            started(shortcut = true)
            coordinator.gather(activityA)
            platform.failUpdate(INTERNET_ERROR)
            check(state.path == ConsentPath.ERROR_ALLOWED)
        }
    }

    @Test
    fun cachedConsent_shortcutOn_opensBeforeUpdateReturns() {
        val env = Env().started(shortcut = true)
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        assertTrue(env.state.allowed)
        assertEquals(listOf(true), env.opened)
    }

    @Test
    fun cachedConsent_shortcutOff_opensBeforeUpdateReturns() {
        val env = Env().started(shortcut = false)
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        // CACHED_EARLY is always on (ADR-0038, plan T-066).
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        assertTrue(env.state.allowed)
        assertEquals(listOf(true), env.opened)
        env.platform.completeUpdate()
        env.platform.closeForm(null)
        assertEquals(ConsentPath.GRANTED, env.state.path)
        assertTrue(env.state.allowed)
        // The SDK starts once: GRANTED does not open the gate again.
        assertEquals(listOf(true), env.opened)
        assertTrue(env.state.settled)
    }

    @Test
    fun gatherWaitsForShortcutValue_thenCachedEarlyWithSwitchOff() {
        val env = Env()
        env.coordinator.start()
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        assertEquals(0, env.updates)
        assertTrue(env.opened.isEmpty())
        env.coordinator.onShortcutChanged(false)
        assertEquals(1, env.updates)
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        assertEquals(listOf(true), env.opened)
    }

    /** CACHED_EARLY with the switch off, then a network failure of the update while UMP still allows ads. */
    private fun cachedKeptAfterFailure(): Env = Env().started(shortcut = false).apply {
        platform.canRequest = true
        coordinator.gather(activityA)
        platform.failUpdate(INTERNET_ERROR)
        assertTrue(state.allowed)
        assertEquals(ConsentPath.CACHED_EARLY, state.path)
    }

    @Test
    fun cachedConsent_updateFails_regathersOnInternetBackAndForeground() {
        val env = cachedKeptAfterFailure()
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        assertTrue(env.state.allowed)
        assertTrue(env.coordinator.finished.value)
        assertFalse(env.state.settled)
        assertEquals(1, env.updates)

        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
        assertEquals(listOf(activityA, activityA), env.platform.updateActivities)

        env.platform.failUpdate(INTERNET_ERROR)
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        env.coordinator.onForegroundChanged(false)
        env.coordinator.onForegroundChanged(true)
        assertEquals(3, env.updates)

        env.platform.canRequest = true
        env.platform.completeUpdate()
        env.platform.closeForm(null)
        assertEquals(ConsentPath.GRANTED, env.state.path)
        assertTrue(env.state.settled)

        // Control (L-055): no regather once settled.
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(3, env.updates)
    }

    @Test
    fun cachedConsent_regather_skippedWhenActivityPausedOrAdShowing() {
        val env = cachedKeptAfterFailure()

        env.resumed.clear()
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)

        env.adShowing = true
        env.resumed += activityA
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)

        // Control (L-055): the waiting regather runs once A is resumed with no ad on screen.
        env.adShowing = false
        env.coordinator.onActivityResumed(activityA)
        assertEquals(2, env.updates)
    }

    @Test
    fun cachedConsent_regatherFindsRequired_closesBeforeForm() {
        val env = cachedKeptAfterFailure()
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
        env.platform.canRequest = false
        env.platform.formRequired = true
        var allowedAtForm: Boolean? = null
        env.platform.onFormShown = { allowedAtForm = env.state.allowed }
        env.platform.completeUpdate()
        assertEquals(false, allowedAtForm)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, env.state.path)
        // Control: the gate opened once, at CACHED_EARLY, and not again.
        assertEquals(listOf(true), env.opened)
    }

    @Test
    fun cachedConsent_updateFailsWithoutConsent_closesGate() {
        val env = Env().started(shortcut = false)
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        assertTrue(env.state.allowed)
        env.platform.canRequest = false
        env.platform.failUpdate(INTERNAL_ERROR)
        assertFalse(env.state.allowed)
        assertFalse(env.coordinator.mayRequestNow(true))
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, env.state.path)
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)
    }

    @Test
    fun cachedConsent_switchOff_formError_settledNextGatherSkipsUmp() {
        val env = Env().started(shortcut = false)
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        assertEquals(listOf(true), env.opened)
        env.platform.completeUpdate()
        env.platform.closeForm(INTERNAL_ERROR)
        assertEquals(ConsentPath.GRANTED, env.state.path)
        assertTrue(env.state.settled)
        assertEquals(listOf(true), env.opened)
        env.coordinator.gather(activityA)
        assertEquals(1, env.updates)
        assertTrue(env.coordinator.finished.value)

        // Control (L-055): with the switch on the same steps do not settle, so the next gather asks UMP.
        val on = Env().started(shortcut = true)
        on.platform.canRequest = true
        on.coordinator.gather(activityA)
        on.platform.completeUpdate()
        on.platform.closeForm(INTERNAL_ERROR)
        assertFalse(on.state.settled)
        on.coordinator.gather(activityA)
        assertEquals(2, on.updates)
    }

    @Test
    fun outsideRegulatedRegion_grantsWithoutForm() {
        val env = Env().started(shortcut = false)
        env.platform.canRequest = true
        env.platform.formRequired = false
        var suppressedAtForm: Boolean? = null
        env.platform.onFormShown = { suppressedAtForm = env.suppressor.suppressed.value }
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        env.platform.closeForm(null)
        assertEquals(ConsentPath.GRANTED, env.state.path)
        assertTrue(env.state.settled)
        assertTrue(env.state.allowed)
        assertEquals(1, env.platform.count("showFormIfRequired"))
        assertEquals(false, suppressedAtForm)
    }

    @Test
    fun errorAllowed_internetBack_regathersOnSameActivity() {
        val env = Env().errorAllowed()
        assertEquals(1, env.updates)
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
        assertEquals(listOf(activityA, activityA), env.platform.updateActivities)
        env.platform.canRequest = true
        env.platform.completeUpdate()
        env.platform.closeForm(null)
        assertEquals(ConsentPath.GRANTED, env.state.path)
        assertTrue(env.state.settled)

        // Control: no regather once granted.
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
    }

    @Test
    fun errorAllowed_foreground_regathers() {
        val env = Env().errorAllowed()
        env.coordinator.onForegroundChanged(false)
        assertEquals(1, env.updates)
        env.coordinator.onForegroundChanged(true)
        assertEquals(2, env.updates)
        assertEquals(activityA, env.platform.updateActivities.last())
    }

    @Test
    fun regather_skippedWhenActivityPausedOrOtherOrAdShowing() {
        val env = Env().errorAllowed()

        env.resumed.clear()
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)

        env.resumed.add(activityB)
        env.coordinator.onForegroundChanged(false)
        env.coordinator.onForegroundChanged(true)
        assertEquals(1, env.updates)

        env.resumed.clear()
        env.resumed.add(activityA)
        env.adShowing = true
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)

        // Control (L-055): the same event regathers once A is resumed with no ad on screen.
        env.adShowing = false
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
    }

    @Test
    fun errorAllowed_foreground_regathersWhenAskedActivityResumes() {
        // On a device the foreground signal comes from onActivityStarted, before any activity is resumed.
        val env = Env().errorAllowed()
        env.resumed.clear()
        env.coordinator.onForegroundChanged(false)
        env.coordinator.onForegroundChanged(true)
        assertEquals(1, env.updates)

        // Control: another activity resuming does not run the waiting regather.
        env.resumed += activityB
        env.coordinator.onActivityResumed(activityB)
        assertEquals(1, env.updates)

        env.resumed += activityA
        env.coordinator.onActivityResumed(activityA)
        assertEquals(2, env.updates)
        assertEquals(activityA, env.platform.updateActivities.last())
    }

    @Test
    fun errorAllowed_internetBackWhilePaused_regathersWhenAskedActivityResumes() {
        val env = Env().errorAllowed()
        env.resumed.clear()
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(1, env.updates)

        env.resumed += activityA
        env.coordinator.onActivityResumed(activityA)
        assertEquals(2, env.updates)
    }

    @Test
    fun temporaryThenRequired_closesBeforeForm() {
        val env = Env().started(shortcut = true)
        env.platform.canRequest = true
        env.coordinator.gather(activityA)
        assertEquals(ConsentPath.CACHED_EARLY, env.state.path)
        env.platform.canRequest = false
        env.platform.formRequired = true
        var allowedAtForm: Boolean? = null
        env.platform.onFormShown = { allowedAtForm = env.state.allowed }
        env.platform.completeUpdate()
        assertEquals(false, allowedAtForm)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, env.state.path)
        env.platform.canRequest = true
        env.platform.closeForm(null)
        assertTrue(env.state.allowed)
        assertEquals(listOf(true, false), env.opened)
    }

    @Test
    fun settled_nextGatherSkipsUmp() {
        val env = Env().started(shortcut = false)
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        env.platform.canRequest = true
        env.platform.closeForm(INTERNAL_ERROR)
        assertTrue(env.state.settled)
        env.coordinator.gather(activityA)
        assertEquals(1, env.updates)
        assertTrue(env.coordinator.finished.value)
    }

    @Test
    fun shortcutOn_formError_doesNotSettle() {
        val env = Env().started(shortcut = true)
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        env.platform.canRequest = true
        env.platform.closeForm(INTERNAL_ERROR)
        assertFalse(env.state.settled)
        env.coordinator.gather(activityA)
        assertEquals(2, env.updates)
    }

    @Test
    fun requiredForm_holdsSuppressionWhileOpen() {
        val env = Env().started(shortcut = false)
        env.platform.formRequired = true
        var suppressedAtForm: Boolean? = null
        env.platform.onFormShown = { suppressedAtForm = env.suppressor.suppressed.value }
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        assertEquals(true, suppressedAtForm)
        env.platform.closeForm(null)
        assertFalse(env.suppressor.suppressed.value)

        // Closed with an error, the callback called twice: released once, never below zero.
        val failing = Env().started(shortcut = false)
        failing.platform.formRequired = true
        failing.coordinator.gather(activityA)
        failing.platform.completeUpdate()
        assertTrue(failing.suppressor.suppressed.value)
        failing.platform.closeForm(INTERNAL_ERROR, twice = true)
        assertFalse(failing.suppressor.suppressed.value)
        failing.suppressor.acquire("test")
        assertTrue(failing.suppressor.suppressed.value)

        // Control: no hold when UMP says the form is not required.
        val notRequired = Env().started(shortcut = false)
        var notRequiredAtForm: Boolean? = null
        notRequired.platform.onFormShown = { notRequiredAtForm = notRequired.suppressor.suppressed.value }
        notRequired.coordinator.gather(activityA)
        notRequired.platform.completeUpdate()
        assertEquals(false, notRequiredAtForm)
    }

    @Test
    fun consentFormOpen_trueOnlyWhileFormShows() {
        val env = Env(allowReset = true).started(shortcut = false)
        env.platform.formRequired = true
        var openAtForm: Boolean? = null
        env.platform.onFormShown = { openAtForm = env.coordinator.consentFormOpen.value }
        assertFalse(env.coordinator.consentFormOpen.value)
        env.coordinator.gather(activityA)
        // Updating: no form yet.
        assertFalse(env.coordinator.consentFormOpen.value)
        env.platform.completeUpdate()
        assertEquals(true, openAtForm)
        assertTrue(env.coordinator.consentFormOpen.value)
        env.platform.closeForm(null)
        assertFalse(env.coordinator.consentFormOpen.value)

        // Closed with an error, the callback called twice: closed.
        val failing = Env().started(shortcut = false)
        failing.platform.formRequired = true
        failing.coordinator.gather(activityA)
        failing.platform.completeUpdate()
        assertTrue(failing.coordinator.consentFormOpen.value)
        failing.platform.closeForm(INTERNAL_ERROR, twice = true)
        assertFalse(failing.coordinator.consentFormOpen.value)

        // A reset while the form shows clears it.
        val reset = Env(allowReset = true).started(shortcut = false)
        reset.platform.formRequired = true
        reset.coordinator.gather(activityA)
        reset.platform.completeUpdate()
        assertTrue(reset.coordinator.consentFormOpen.value)
        reset.coordinator.reset()
        assertFalse(reset.coordinator.consentFormOpen.value)

        // Control: UMP says no form is required, so none shows.
        val notRequired = Env().started(shortcut = false)
        var notRequiredAtForm: Boolean? = null
        notRequired.platform.onFormShown = { notRequiredAtForm = notRequired.coordinator.consentFormOpen.value }
        notRequired.coordinator.gather(activityA)
        notRequired.platform.completeUpdate()
        assertEquals(false, notRequiredAtForm)
        notRequired.platform.closeForm(null)
        assertFalse(notRequired.coordinator.consentFormOpen.value)
    }

    @Test
    fun privacyForm_alwaysHoldsSuppression() {
        val env = Env().granted()
        var suppressedAtForm: Boolean? = null
        env.platform.onFormShown = { suppressedAtForm = env.suppressor.suppressed.value }
        env.coordinator.showPrivacyOptions(activityA)
        assertEquals(true, suppressedAtForm)
        env.platform.closeForm(INTERNAL_ERROR, twice = true)
        assertFalse(env.suppressor.suppressed.value)
        env.suppressor.acquire("test")
        assertTrue(env.suppressor.suppressed.value)
    }

    @Test
    fun interruptedForm_regathersNextTime() {
        val env = Env().started(shortcut = false)
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        env.activityGone = true
        env.platform.closeForm(INTERNAL_ERROR)
        assertEquals(ConsentPath.INTERRUPTED_REGATHER, env.state.path)
        assertFalse(env.state.settled)
        env.activityGone = false
        env.coordinator.gather(activityB)
        assertEquals(2, env.updates)
    }

    @Test
    fun privacyFormWithdrawn_closesEveryGate() {
        val env = Env().granted()
        assertTrue(env.coordinator.mayRequestNow(true))
        env.coordinator.showPrivacyOptions(activityA)
        env.platform.canRequest = false
        env.platform.closeForm(null)
        assertFalse(env.state.allowed)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, env.state.path)
        assertFalse(env.coordinator.mayRequestNow(true))
        assertFalse(env.coordinator.mayRequestNow(false))
    }

    @Test
    fun privacyFormRegranted_initsOrPreloads() {
        val env = Env().granted()
        assertEquals(listOf(true), env.opened)
        env.coordinator.showPrivacyOptions(activityA)
        env.platform.canRequest = false
        env.platform.closeForm(null)
        env.coordinator.showPrivacyOptions(activityA)
        env.platform.canRequest = true
        env.platform.closeForm(null)
        assertEquals(listOf(true, false), env.opened)

        val denied = Env().started(shortcut = false)
        denied.coordinator.gather(activityA)
        denied.platform.completeUpdate()
        denied.platform.closeForm(null)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, denied.state.path)
        assertTrue(denied.opened.isEmpty())
        denied.coordinator.showPrivacyOptions(activityA)
        denied.platform.canRequest = true
        denied.platform.closeForm(null)
        assertEquals(listOf(true), denied.opened)
    }

    @Test
    fun start_twice_isIgnored() {
        val env = Env()
        assertTrue(env.coordinator.start())
        assertFalse(env.coordinator.start())
    }

    @Test
    fun gather_updatesThenShowsFormThenReads() {
        val env = Env().started(shortcut = false)
        env.coordinator.gather(activityA)
        env.platform.completeUpdate()
        val beforeClose = env.platform.calls.size
        env.platform.closeForm(null)
        val calls = env.platform.calls
        assertTrue(calls.indexOf("requestUpdate") >= 0)
        assertTrue(calls.indexOf("requestUpdate") < calls.indexOf("showFormIfRequired"))
        assertTrue(calls.subList(beforeClose, calls.size).contains("canRequestAds"))
    }

    @Test
    fun secondGatherWhileRunning_sharesTheFirst() {
        val env = Env().started(shortcut = false)
        env.coordinator.gather(activityA)
        env.coordinator.gather(activityA)
        assertEquals(1, env.updates)
        env.platform.completeUpdate()
        assertEquals(1, env.platform.count("showFormIfRequired"))
    }

    @Test
    fun everyPath_isLogged() {
        val cached = Env().started(shortcut = true).apply {
            platform.canRequest = true
            coordinator.gather(activityA)
        }
        val granted = Env().granted()
        val denied = Env().started(shortcut = false).apply {
            coordinator.gather(activityA)
            platform.completeUpdate()
            platform.closeForm(null)
        }
        val error = Env().errorAllowed()
        val interrupted = Env().started(shortcut = false).apply {
            coordinator.gather(activityA)
            platform.completeUpdate()
            activityGone = true
            platform.closeForm(INTERNAL_ERROR)
        }
        mapOf(
            ConsentPath.CACHED_EARLY to cached,
            ConsentPath.GRANTED to granted,
            ConsentPath.DENIED_OR_REQUIRED to denied,
            ConsentPath.ERROR_ALLOWED to error,
            ConsentPath.INTERRUPTED_REGATHER to interrupted,
        ).forEach { (path, env) ->
            assertEquals(path, env.state.path)
            val lines = env.logger.entries.filter {
                it.level == LogLevel.INFO && it.tag == "Ads" && it.message.contains(path.name)
            }
            assertEquals("$path: ${env.logger.entries}", 1, lines.size)
            assertTrue(env.logger.entries.none { it.message.contains(env.platform.errorMessage) })
        }
    }

    @Test
    fun regather_reusesFirstParameters() {
        val env = Env().errorAllowed()
        env.coordinator.onInternetChanged(false)
        env.coordinator.onInternetChanged(true)
        assertEquals(2, env.updates)
        assertEquals(1, env.platform.parametersBuilt)
    }

    @Test
    fun reset_onlyWhenAllowed() {
        val denied = Env(allowReset = false).granted()
        denied.coordinator.reset()
        assertEquals(0, denied.platform.count("reset"))
        assertEquals(ConsentPath.GRANTED, denied.state.path)

        val allowed = Env(allowReset = true).granted()
        allowed.coordinator.reset()
        assertEquals(1, allowed.platform.count("reset"))
        assertNull(allowed.state.path)
        assertFalse(allowed.state.allowed)
        allowed.coordinator.gather(activityA)
        assertEquals(2, allowed.updates)
    }

    @Test
    fun finished_atFinalResult_clearedByReset() {
        val env = Env(allowReset = true).started(shortcut = false)
        env.coordinator.gather(activityA)
        assertFalse(env.coordinator.finished.value)
        env.platform.failUpdate(INTERNET_ERROR)
        assertTrue(env.coordinator.finished.value)
        env.coordinator.reset()
        assertFalse(env.coordinator.finished.value)
    }

    @Test
    fun beforeStart_gatesClosedAndSignalsReadable() {
        val env = Env()
        env.coordinator.gather(activityA)
        assertEquals(0, env.updates)
        assertNull(env.state.path)
        assertFalse(env.state.allowed)
        assertFalse(env.coordinator.finished.value)
        assertEquals(1, env.logger.entries.count { it.level == LogLevel.WARN && it.tag == "Ads" })

        env.coordinator.start()
        env.coordinator.onShortcutChanged(false)
        env.coordinator.gather(activityA)
        assertEquals(1, env.updates)
        // Control (L-055): the same gate opens once consent is given.
        env.platform.canRequest = true
        env.platform.completeUpdate()
        env.platform.closeForm(null)
        assertTrue(env.state.allowed)
    }

    @Test
    fun gatherWaitsForShortcutValue() {
        val env = Env()
        env.coordinator.start()
        env.coordinator.gather(activityA)
        assertEquals(0, env.updates)
        env.coordinator.onShortcutChanged(true)
        assertEquals(1, env.updates)
        env.platform.failUpdate(INTERNET_ERROR)
        assertEquals(ConsentPath.ERROR_ALLOWED, env.state.path)
    }

    private companion object {
        const val INTERNAL_ERROR = 1
        const val INTERNET_ERROR = 2
    }
}
