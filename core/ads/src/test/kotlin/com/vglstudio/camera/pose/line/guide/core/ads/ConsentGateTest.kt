package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The consent rules of docs/ops/ads-business-rules.md, section S, on the pure state machine (plan T-020). */
class ConsentGateTest {
    private fun gate(shortcut: Boolean) = ConsentGate().apply { onShortcutChanged(shortcut) }

    private val ConsentGate.current get() = state.value

    @Test
    fun initialState_isClosedWithNoPath() {
        val gate = ConsentGate()
        assertNull(gate.current.path)
        assertFalse(gate.current.allowed)
        assertFalse(gate.current.settled)
        assertFalse(gate.mayRequestNow(foreground = true))
        // Control (L-055): a clean grant opens the same gate.
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(gate.current.allowed)
        assertTrue(gate.mayRequestNow(foreground = true))
    }

    @Test
    fun cachedConsent_shortcutOn_opensAtRequestSent() {
        val gate = gate(shortcut = true)
        gate.onRequestSent(umpCanRequestAds = true)
        assertEquals(ConsentPath.CACHED_EARLY, gate.current.path)
        assertTrue(gate.current.allowed)
        assertTrue(gate.current.temporary)
        assertFalse(gate.current.settled)
    }

    @Test
    fun umpFailureOf_mapsCodes() {
        assertEquals(UmpFailure.NETWORK, umpFailureOf(2, activityGone = false))
        assertEquals(UmpFailure.NETWORK, umpFailureOf(4, activityGone = false))
        listOf(1, 3, 99).forEach { assertEquals("code $it", UmpFailure.CONFIGURATION, umpFailureOf(it, false)) }
        listOf(1, 2, 3, 4, 99).forEach { assertEquals("code $it", UmpFailure.INTERRUPTED, umpFailureOf(it, true)) }
    }

    @Test
    fun shortcutActive_needsBoth() {
        assertTrue(shortcutActive(enabledInBuild = true, remoteEnabled = true))
        assertFalse(shortcutActive(enabledInBuild = true, remoteEnabled = false))
        assertFalse(shortcutActive(enabledInBuild = false, remoteEnabled = true))
        assertFalse(shortcutActive(enabledInBuild = false, remoteEnabled = false))
    }

    @Test
    fun networkFailure_shortcutOn_allowsWithoutSettling() {
        val update = gate(shortcut = true)
        update.onRequestSent(umpCanRequestAds = false)
        update.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertEquals(ConsentPath.ERROR_ALLOWED, update.current.path)
        assertTrue(update.current.allowed)
        assertTrue(update.current.temporary)
        assertFalse(update.current.settled)
        assertTrue(update.regatherDue)

        val form = gate(shortcut = true)
        form.onRequestSent(umpCanRequestAds = false)
        form.onUpdateSucceeded(umpCanRequestAds = false)
        form.onFormFinished(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertEquals(ConsentPath.ERROR_ALLOWED, form.current.path)
        assertTrue(form.current.allowed)
        assertFalse(form.current.settled)
        assertTrue(form.regatherDue)
    }

    @Test
    fun networkFailure_afterCachedEarly_keepsCachedEarly() {
        val gate = gate(shortcut = true)
        gate.onRequestSent(umpCanRequestAds = true)
        gate.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = true)
        assertEquals(ConsentPath.CACHED_EARLY, gate.current.path)
        assertTrue(gate.current.allowed)
        // KB-11, criterion 3: CACHED_EARLY kept through a failed round is asked again (S-09, plan T-066).
        assertTrue(gate.regatherDue)
        // Control: a clean result settles and ends the regather.
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(failure = null, umpCanRequestAds = true)
        assertFalse(gate.regatherDue)
        assertTrue(gate.current.settled)
    }

    @Test
    fun errorAllowed_background_mayNotRequest() {
        val gate = gate(shortcut = true)
        gate.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertFalse(gate.mayRequestNow(foreground = false))
        assertTrue(gate.mayRequestNow(foreground = true))

        // Control: real rights work in background too.
        val granted = gate(shortcut = false)
        granted.onUpdateSucceeded(umpCanRequestAds = true)
        granted.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(granted.mayRequestNow(foreground = false))

        val cached = gate(shortcut = true)
        cached.onRequestSent(umpCanRequestAds = true)
        assertTrue(cached.mayRequestNow(foreground = false))
    }

    @Test
    fun temporaryThenRequired_closesAtUpdate() {
        val gate = gate(shortcut = true)
        gate.onRequestSent(umpCanRequestAds = true)
        assertTrue(gate.current.allowed)
        gate.onUpdateSucceeded(umpCanRequestAds = false)
        assertFalse(gate.current.allowed)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, gate.current.path)
    }

    @Test
    fun shortcutOff_networkFailureGivesNothing() {
        val gate = gate(shortcut = false)
        gate.onRequestSent(umpCanRequestAds = false)
        assertFalse(gate.current.allowed)
        gate.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertFalse(gate.current.allowed)
        assertNotEquals(ConsentPath.ERROR_ALLOWED, gate.current.path)
        assertFalse(gate.current.settled)

        val noConsent = gate(shortcut = false)
        noConsent.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertFalse(noConsent.current.allowed)
        assertNotEquals(ConsentPath.ERROR_ALLOWED, noConsent.current.path)

        // Control (L-055): the same failure with the shortcut on allows.
        val on = gate(shortcut = true)
        on.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertTrue(on.current.allowed)
    }

    @Test
    fun shortcutTurnedOff_revokesErrorAllowedOnly() {
        val error = gate(shortcut = true)
        error.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertTrue(error.current.allowed)
        error.onShortcutChanged(false)
        assertFalse(error.current.allowed)
        assertFalse(error.current.settled)
        assertFalse(error.regatherDue)
        assertEquals(ConsentPath.ERROR_ALLOWED, error.current.path)

        val cached = gate(shortcut = true)
        cached.onRequestSent(umpCanRequestAds = true)
        cached.onShortcutChanged(false)
        assertTrue(cached.current.allowed)
        assertEquals(ConsentPath.CACHED_EARLY, cached.current.path)

        val granted = gate(shortcut = true)
        granted.onUpdateSucceeded(umpCanRequestAds = true)
        granted.onFormFinished(failure = null, umpCanRequestAds = true)
        granted.onShortcutChanged(false)
        assertTrue(granted.current.allowed)
        assertEquals(ConsentPath.GRANTED, granted.current.path)
    }

    @Test
    fun turningOn_revokesNothing() {
        val gate = gate(shortcut = true)
        gate.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertTrue(gate.current.allowed)
        gate.onShortcutChanged(true)
        assertTrue(gate.current.allowed)
        gate.onShortcutChanged(false)
        val revoked = gate.current
        gate.onShortcutChanged(true)
        assertEquals(revoked, gate.current)
        assertFalse(gate.current.allowed)
    }

    @Test
    fun shortcutOff_formErrorAfterGrant_settles() {
        val gate = gate(shortcut = false)
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(UmpFailure.CONFIGURATION, umpCanRequestAds = true)
        assertTrue(gate.current.settled)
        assertTrue(gate.current.allowed)
    }

    @Test
    fun shortcutOff_formErrorWithoutGrant_doesNotSettle() {
        val gate = gate(shortcut = false)
        gate.onUpdateSucceeded(umpCanRequestAds = false)
        gate.onFormFinished(UmpFailure.CONFIGURATION, umpCanRequestAds = false)
        assertFalse(gate.current.settled)
        assertFalse(gate.current.allowed)
        // Control: a clean result settles.
        gate.onFormFinished(failure = null, umpCanRequestAds = false)
        assertTrue(gate.current.settled)
    }

    @Test
    fun shortcutOn_formError_doesNotSettle() {
        val gate = gate(shortcut = true)
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(UmpFailure.CONFIGURATION, umpCanRequestAds = true)
        assertFalse(gate.current.settled)
        // Control: a clean result settles.
        gate.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(gate.current.settled)
    }

    @Test
    fun settleRule_readsShortcutAtResultTime() {
        val gate = gate(shortcut = true)
        gate.onRequestSent(umpCanRequestAds = false)
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onShortcutChanged(false)
        gate.onFormFinished(UmpFailure.CONFIGURATION, umpCanRequestAds = true)
        assertTrue(gate.current.settled)
    }

    @Test
    fun updateFailure_neverSettles() {
        listOf(true, false).forEach { shortcut ->
            listOf(UmpFailure.NETWORK, UmpFailure.CONFIGURATION).forEach { failure ->
                val gate = gate(shortcut)
                gate.onUpdateFailed(failure, umpCanRequestAds = true)
                assertFalse("$shortcut $failure", gate.current.settled)
            }
        }
        // Control: a clean result settles.
        val clean = gate(shortcut = false)
        clean.onUpdateSucceeded(umpCanRequestAds = true)
        clean.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(clean.current.settled)
    }

    @Test
    fun configurationError_givesNoTemporaryRight() {
        val gate = gate(shortcut = true)
        gate.onUpdateFailed(UmpFailure.CONFIGURATION, umpCanRequestAds = false)
        assertFalse(gate.current.allowed)
        assertNotEquals(ConsentPath.ERROR_ALLOWED, gate.current.path)
        // Control: CACHED_EARLY is kept through a configuration error.
        val cached = gate(shortcut = true)
        cached.onRequestSent(umpCanRequestAds = true)
        cached.onUpdateFailed(UmpFailure.CONFIGURATION, umpCanRequestAds = true)
        assertTrue(cached.current.allowed)
        assertEquals(ConsentPath.CACHED_EARLY, cached.current.path)
    }

    @Test
    fun interruptedForm_isNotSettled() {
        val gate = gate(shortcut = true)
        gate.onUpdateSucceeded(umpCanRequestAds = false)
        gate.onFormFinished(UmpFailure.INTERRUPTED, umpCanRequestAds = false)
        assertEquals(ConsentPath.INTERRUPTED_REGATHER, gate.current.path)
        assertFalse(gate.current.settled)
        assertFalse(gate.current.allowed)
        assertFalse(gate.current.temporary)
    }

    @Test
    fun privacyForm_withdrawOrGrant() {
        val gate = gate(shortcut = false)
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(gate.current.allowed)
        gate.onPrivacyFormClosed(umpCanRequestAds = false)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, gate.current.path)
        assertFalse(gate.current.allowed)
        assertTrue(gate.current.settled)
        gate.onPrivacyFormClosed(umpCanRequestAds = true)
        assertEquals(ConsentPath.GRANTED, gate.current.path)
        assertTrue(gate.current.allowed)
        assertTrue(gate.current.settled)
    }

    @Test
    fun reset_backToInitialKeepsShortcut() {
        val gate = gate(shortcut = true)
        gate.onUpdateSucceeded(umpCanRequestAds = true)
        gate.onFormFinished(failure = null, umpCanRequestAds = true)
        assertTrue(gate.current.settled)
        gate.onReset()
        assertNull(gate.current.path)
        assertFalse(gate.current.allowed)
        assertFalse(gate.current.settled)
        // CACHED_EARLY opens early again after a reset.
        gate.onRequestSent(umpCanRequestAds = true)
        assertEquals(ConsentPath.CACHED_EARLY, gate.current.path)

        // The shortcut is still on after a reset: a network failure gives ERROR_ALLOWED.
        val on = gate(shortcut = true)
        on.onUpdateSucceeded(umpCanRequestAds = true)
        on.onFormFinished(failure = null, umpCanRequestAds = true)
        on.onReset()
        on.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertEquals(ConsentPath.ERROR_ALLOWED, on.current.path)
        // Control: the same steps with the shortcut off give no ERROR_ALLOWED.
        val off = gate(shortcut = false)
        off.onUpdateSucceeded(umpCanRequestAds = true)
        off.onFormFinished(failure = null, umpCanRequestAds = true)
        off.onReset()
        off.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertNotEquals(ConsentPath.ERROR_ALLOWED, off.current.path)
    }

    @Test
    fun cachedConsent_switchOff_opensAtRequestSent() {
        val gate = gate(shortcut = false)
        gate.onRequestSent(umpCanRequestAds = true)
        assertEquals(ConsentPath.CACHED_EARLY, gate.current.path)
        assertTrue(gate.current.allowed)
        assertTrue(gate.current.temporary)
        assertFalse(gate.current.settled)
        assertTrue(gate.mayRequestNow(foreground = false))

        // Control (L-055): no consent of an earlier session, no early opening.
        val none = gate(shortcut = false)
        none.onRequestSent(umpCanRequestAds = false)
        assertNull(none.current.path)
        assertFalse(none.current.allowed)
    }

    @Test
    fun cachedConsent_switchOff_keptThroughUpdateFailure() {
        listOf(UmpFailure.NETWORK, UmpFailure.CONFIGURATION).forEach { failure ->
            val gate = gate(shortcut = false)
            gate.onRequestSent(umpCanRequestAds = true)
            // The round still runs: no regather yet.
            assertFalse("$failure", gate.regatherDue)
            gate.onUpdateFailed(failure, umpCanRequestAds = true)
            assertEquals("$failure", ConsentPath.CACHED_EARLY, gate.current.path)
            assertNotEquals("$failure", ConsentPath.ERROR_ALLOWED, gate.current.path)
            assertTrue("$failure", gate.current.allowed)
            assertFalse("$failure", gate.current.settled)
            assertTrue("$failure", gate.regatherDue)

            // Control (L-055): the same failure with no consent of an earlier session gives nothing.
            val none = gate(shortcut = false)
            none.onUpdateFailed(failure, umpCanRequestAds = true)
            assertFalse("$failure", none.current.allowed)
            assertNotEquals("$failure", ConsentPath.ERROR_ALLOWED, none.current.path)
            assertFalse("$failure", none.regatherDue)
        }
    }

    @Test
    fun cachedConsent_umpNoLongerAllows_closesOnFailure() {
        fun assertClosed(case: String, gate: ConsentGate) {
            assertEquals(case, ConsentPath.DENIED_OR_REQUIRED, gate.current.path)
            assertFalse(case, gate.current.allowed)
            assertFalse(case, gate.current.temporary)
            assertFalse(case, gate.current.settled)
            assertFalse(case, gate.regatherDue)
            assertFalse(case, gate.mayRequestNow(foreground = true))
        }

        fun cached(shortcut: Boolean) = gate(shortcut).apply {
            onRequestSent(umpCanRequestAds = true)
            assertEquals(ConsentPath.CACHED_EARLY, state.value.path)
            assertTrue(state.value.allowed)
        }

        val configuration = cached(shortcut = true)
        configuration.onUpdateFailed(UmpFailure.CONFIGURATION, umpCanRequestAds = false)
        assertClosed("configuration, switch on", configuration)

        val networkOff = cached(shortcut = false)
        networkOff.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertClosed("network, switch off", networkOff)

        val formError = cached(shortcut = true)
        formError.onUpdateSucceeded(umpCanRequestAds = true)
        assertTrue(formError.current.allowed)
        formError.onFormFinished(UmpFailure.CONFIGURATION, umpCanRequestAds = false)
        assertClosed("form error, switch on", formError)

        // As with no consent of an earlier session: foreground only (S-04, S-08).
        val networkOn = cached(shortcut = true)
        networkOn.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertEquals(ConsentPath.ERROR_ALLOWED, networkOn.current.path)
        assertTrue(networkOn.current.allowed)
        assertFalse(networkOn.mayRequestNow(foreground = false))
    }

    @Test
    fun cachedConsent_switchOff_thenRequired_closesAtUpdate() {
        val gate = gate(shortcut = false)
        gate.onRequestSent(umpCanRequestAds = true)
        assertTrue(gate.current.allowed)
        gate.onUpdateSucceeded(umpCanRequestAds = false)
        assertFalse(gate.current.allowed)
        assertEquals(ConsentPath.DENIED_OR_REQUIRED, gate.current.path)
        assertFalse(gate.current.temporary)
    }

    @Test
    fun switchOff_revokesErrorAllowed_nextOpenStillCachedEarly() {
        val first = gate(shortcut = true)
        first.onUpdateFailed(UmpFailure.NETWORK, umpCanRequestAds = false)
        assertEquals(ConsentPath.ERROR_ALLOWED, first.current.path)
        assertTrue(first.current.allowed)
        first.onShortcutChanged(false)
        assertFalse(first.current.allowed)

        // The next open of the app: the stored switch value is off.
        val next = gate(shortcut = false)
        next.onRequestSent(umpCanRequestAds = true)
        assertEquals(ConsentPath.CACHED_EARLY, next.current.path)
        assertTrue(next.current.allowed)
        next.onShortcutChanged(true)
        next.onShortcutChanged(false)
        assertEquals(ConsentPath.CACHED_EARLY, next.current.path)
        assertTrue(next.current.allowed)
    }

    @Test
    fun cachedConsent_switchOff_formErrorAfterGrant_settles() {
        listOf(UmpFailure.CONFIGURATION, UmpFailure.NETWORK).forEach { failure ->
            val gate = gate(shortcut = false)
            gate.onRequestSent(umpCanRequestAds = true)
            assertEquals("$failure", ConsentPath.CACHED_EARLY, gate.current.path)
            gate.onUpdateSucceeded(umpCanRequestAds = true)
            gate.onFormFinished(failure, umpCanRequestAds = true)
            assertEquals("$failure", ConsentPath.GRANTED, gate.current.path)
            assertTrue("$failure", gate.current.settled)
            assertTrue("$failure", gate.current.allowed)
            assertFalse("$failure", gate.current.temporary)

            // Control (L-055): with the switch on the same steps keep CACHED_EARLY, not settled.
            val on = gate(shortcut = true)
            on.onRequestSent(umpCanRequestAds = true)
            on.onUpdateSucceeded(umpCanRequestAds = true)
            on.onFormFinished(failure, umpCanRequestAds = true)
            assertEquals("$failure", ConsentPath.CACHED_EARLY, on.current.path)
            assertFalse("$failure", on.current.settled)
        }
    }
}
