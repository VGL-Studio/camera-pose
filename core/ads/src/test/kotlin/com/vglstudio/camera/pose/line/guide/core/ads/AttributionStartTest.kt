package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * When AppsFlyer starts (ADR-0027, decision 3; plan T-033, section 2): with the ads SDK, on an answer of the consent
 * form (a refusal too), or 10 s after start when no consent form is open; an open form is waited for. One test per
 * path (L-072), in virtual time.
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

    private companion object {
        const val WAIT_MILLIS = 10_000L
    }
}
