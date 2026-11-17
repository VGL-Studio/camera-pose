package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * When AppsFlyer starts (ADR-0027, decision 3; plan T-033, section 2): with the ads SDK, on an answer of the consent
 * form (a refusal too), or 10 s after start when no consent form is open; an open form is waited for. One test per
 * path (L-072), in virtual time. When the consent is known for Tenjin (plan T-099, step 7a): only once the round
 * finished and settled, or with the consent of an earlier session kept; never by the 10 s path.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AttributionStartTest {
    private val sdkStarted = MutableStateFlow(false)
    private val consentAnswered = MutableStateFlow(false)
    private val formOpen = MutableStateFlow(false)

    /** Starts the wait at time 0; [returnedAt] is the virtual time it returned, null while it waits. */
    private var returnedAt: Long? = null
    private var returns = 0

    private fun TestScope.startWaiting() {
        backgroundScope.launch {
            awaitAttributionStart(sdkStarted, consentAnswered, formOpen, WAIT_MILLIS)
            returns++
            returnedAt = currentTime
        }
        runCurrent()
    }

    private fun TestScope.at(millis: Long, action: () -> Unit = {}) {
        advanceTimeBy(millis - currentTime)
        runCurrent()
        action()
        runCurrent()
    }

    @Test
    fun sdkStartedFirst_returnsAt1s() = runTest {
        startWaiting()
        at(999)
        assertNull(returnedAt)
        at(1_000) { sdkStarted.value = true }
        assertEquals(1_000L, returnedAt)
    }

    @Test
    fun answeredWithoutAds_returnsAt3s() = runTest {
        startWaiting()
        // A refusal: the gate stays closed, the round is answered.
        at(3_000) { consentAnswered.value = true }
        assertEquals(3_000L, returnedAt)
    }

    @Test
    fun neither_returnsAt10s() = runTest {
        startWaiting()
        at(9_999)
        assertNull(returnedAt)
        at(10_000)
        assertEquals(10_000L, returnedAt)
    }

    @Test
    fun formOpenAt10s_waitsForClose() = runTest {
        startWaiting()
        at(2_000) { formOpen.value = true }
        at(10_000)
        assertNull(returnedAt)
        at(14_999)
        assertNull(returnedAt)
        at(15_000) {
            consentAnswered.value = true
            formOpen.value = false
        }
        assertEquals(15_000L, returnedAt)
    }

    @Test
    fun formOpenAt10s_closedWithoutAnswer_returnsAtClose() = runTest {
        startWaiting()
        at(2_000) { formOpen.value = true }
        at(10_000)
        assertNull(returnedAt)
        // Interrupted: closed with no answer.
        at(12_000) { formOpen.value = false }
        assertEquals(12_000L, returnedAt)
    }

    @Test
    fun interruptedAt3s_returnsAt10s() = runTest {
        startWaiting()
        at(1_000) { formOpen.value = true }
        // The round ends at 3 s because the form was interrupted: no answer.
        at(3_000) { formOpen.value = false }
        assertNull(returnedAt)
        at(9_999)
        assertNull(returnedAt)
        at(10_000)
        assertEquals(10_000L, returnedAt)
    }

    @Test
    fun updateFailedAt1s_returnsAt10s() = runTest {
        startWaiting()
        // The UMP update failed at 1 s: the round is over with no answer and no form.
        at(1_000)
        assertNull(returnedAt)
        at(9_999)
        assertNull(returnedAt)
        at(10_000)
        assertEquals(10_000L, returnedAt)
    }

    @Test
    fun both_returnsOnceAtFirst() = runTest {
        startWaiting()
        at(2_000) {
            sdkStarted.value = true
            consentAnswered.value = true
        }
        at(30_000)
        assertEquals(2_000L, returnedAt)
        assertEquals(1, returns)
    }

    // Consent known for the attribution SDKs (plan T-099, step 7a; spec ads, question 21 i).

    private val finished = MutableStateFlow(false)
    private val gateState = MutableStateFlow(ConsentGateState.INITIAL)

    /** Starts the wait for the consent at time 0; [knownAt] is the virtual time it returned, null while it waits. */
    private var knownAt: Long? = null

    private fun TestScope.startWaitingForConsent() {
        backgroundScope.launch {
            awaitAttributionConsentKnown(finished, gateState)
            knownAt = currentTime
        }
        runCurrent()
    }

    @Test
    fun consentKnown_byGateState() {
        assertTrue(attributionConsentKnown(true, GRANTED_SETTLED))
        assertTrue(attributionConsentKnown(true, DENIED_SETTLED))
        assertTrue(attributionConsentKnown(true, CACHED_EARLY))

        assertFalse(attributionConsentKnown(true, ConsentGateState.INITIAL))
        assertFalse(attributionConsentKnown(false, CACHED_EARLY))
        assertFalse(attributionConsentKnown(true, ERROR_ALLOWED))
        assertFalse(attributionConsentKnown(true, INTERRUPTED))
        assertFalse(attributionConsentKnown(true, DENIED_UNSETTLED))
        assertFalse(attributionConsentKnown(false, GRANTED_SETTLED))
    }

    @Test
    fun consentKnown_returnsWhenRoundSettles() = runTest {
        startWaitingForConsent()
        at(2_999)
        assertNull(knownAt)
        at(3_000) {
            gateState.value = GRANTED_SETTLED
            finished.value = true
        }
        assertEquals(3_000L, knownAt)
    }

    @Test
    fun consentKnown_notAt10s_withoutAnswer() = runTest {
        startWaitingForConsent()
        at(10_000)
        assertNull(knownAt)
        at(60_000)
        assertNull(knownAt)
    }

    @Test
    fun consentKnown_cachedEarlyThenRequired_waits() = runTest {
        startWaitingForConsent()
        // CACHED_EARLY is set when the request is sent, before UMP answers: not known yet.
        at(100) { gateState.value = CACHED_EARLY }
        assertNull(knownAt)
        // UMP says the old consent expired: consent must be asked again, nothing settled.
        at(1_000) {
            gateState.value = DENIED_UNSETTLED
            finished.value = true
        }
        at(60_000)
        assertNull(knownAt)
    }

    @Test
    fun consentKnown_errorAllowedThenRegatherSettles_returns() = runTest {
        startWaitingForConsent()
        // A network error lets ads through for now: not known.
        at(1_000) {
            gateState.value = ERROR_ALLOWED
            finished.value = true
        }
        assertNull(knownAt)
        // The round asked again and settles at 8 s.
        at(5_000) { finished.value = false }
        assertNull(knownAt)
        at(8_000) {
            gateState.value = GRANTED_SETTLED
            finished.value = true
        }
        assertEquals(8_000L, knownAt)
    }

    @Test
    fun consentKnown_staleFinished_newRoundUnsettled_waits() = runTest {
        // A finished round of before is still true while the new state is neither settled nor CACHED_EARLY.
        finished.value = true
        gateState.value = INTERRUPTED
        startWaitingForConsent()
        at(1_000) { gateState.value = DENIED_UNSETTLED }
        at(60_000)
        assertNull(knownAt)
    }

    @Test
    fun notifyConsentKnown_callsOnce_whenRoundSettles() = runTest {
        val consent = FakeAttributionConsent()
        val logger = RecordingAppLogger()
        backgroundScope.launch { notifyAttributionConsentKnown(finished, gateState, consent, logger, "Ads") }
        runCurrent()
        at(3_000)
        assertEquals(0, consent.known)

        at(3_000) {
            gateState.value = GRANTED_SETTLED
            finished.value = true
        }
        assertEquals(1, consent.known)
        assertTrue(logger.entries.any { it.tag == "Ads" && it.message == "Attribution consent known" })

        at(4_000) { gateState.value = DENIED_SETTLED }
        at(5_000) { gateState.value = GRANTED_SETTLED }
        assertEquals(1, consent.known)
    }

    private companion object {
        const val WAIT_MILLIS = 10_000L
        val GRANTED_SETTLED = ConsentGateState(ConsentPath.GRANTED, allowed = true, temporary = false, settled = true)
        val DENIED_SETTLED =
            ConsentGateState(ConsentPath.DENIED_OR_REQUIRED, allowed = false, temporary = false, settled = true)
        val DENIED_UNSETTLED =
            ConsentGateState(ConsentPath.DENIED_OR_REQUIRED, allowed = false, temporary = false, settled = false)
        val CACHED_EARLY = ConsentGateState(ConsentPath.CACHED_EARLY, allowed = true, temporary = true, settled = false)
        val ERROR_ALLOWED =
            ConsentGateState(ConsentPath.ERROR_ALLOWED, allowed = true, temporary = true, settled = false)
        val INTERRUPTED =
            ConsentGateState(ConsentPath.INTERRUPTED_REGATHER, allowed = false, temporary = false, settled = false)
    }
}
