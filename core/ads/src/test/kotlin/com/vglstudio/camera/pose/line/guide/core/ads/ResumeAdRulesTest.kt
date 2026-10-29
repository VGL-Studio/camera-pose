package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/**
 * The gates of the resume ad, in the order of A-10 and A-11 (plan T-025): each test sets the gate it checks and
 * every later gate blocking, then opens the gate it checks (control) and gets the result of the next one.
 */
class ResumeAdRulesTest {
    /** Every gate blocks. */
    private val allBlocked = ResumeGate(
        firstForeground = true,
        startupFinished = false,
        serving = false,
        suppressed = true,
        placement = null,
        busy = true,
        appOpenLockPassed = false,
        ready = false,
    )

    @Test
    fun order_firstBeatsAll() {
        assertEquals(ResumeDecision.FIRST_FOREGROUND, decideResumeAd(allBlocked))
        assertEquals(ResumeDecision.STARTUP, decideResumeAd(allBlocked.copy(firstForeground = false)))
    }

    @Test
    fun startup_beforeServing() {
        val gate = allBlocked.copy(firstForeground = false)
        assertEquals(ResumeDecision.STARTUP, decideResumeAd(gate))
        assertEquals(ResumeDecision.NOT_SERVING, decideResumeAd(gate.copy(startupFinished = true)))
    }

    @Test
    fun notServing_beforeSuppressed() {
        val gate = allBlocked.copy(firstForeground = false, startupFinished = true)
        assertEquals(ResumeDecision.NOT_SERVING, decideResumeAd(gate))
        assertEquals(ResumeDecision.SUPPRESSED, decideResumeAd(gate.copy(serving = true)))
    }

    @Test
    fun suppressed_beforeOff() {
        val gate = allBlocked.copy(firstForeground = false, startupFinished = true, serving = true)
        assertEquals(ResumeDecision.SUPPRESSED, decideResumeAd(gate))
        assertEquals(ResumeDecision.OFF, decideResumeAd(gate.copy(suppressed = false)))
    }

    @Test
    fun suppressed_blocksBeforeEverythingButFirstAndServing() {
        assertEquals(ResumeDecision.SUPPRESSED, decideResumeAd(ALL_PASS.copy(suppressed = true)))
        assertEquals(
            ResumeDecision.SUPPRESSED,
            decideResumeAd(ALL_PASS.copy(suppressed = true, placement = AdPlacement.RESUME_INTER)),
        )
        // Control: with the block lowered the same gate shows.
        assertEquals(ResumeDecision.SHOW, decideResumeAd(ALL_PASS))
    }

    @Test
    fun off_beforeBusy() {
        val gate = allBlocked.copy(firstForeground = false, startupFinished = true, serving = true, suppressed = false)
        assertEquals(ResumeDecision.OFF, decideResumeAd(gate))
        assertEquals(ResumeDecision.BUSY, decideResumeAd(gate.copy(placement = AdPlacement.RESUME)))
    }

    @Test
    fun busy_beforeLockAndReady() {
        val gate = ALL_PASS.copy(busy = true, appOpenLockPassed = false, ready = false)
        assertEquals(ResumeDecision.BUSY, decideResumeAd(gate))
        assertEquals(ResumeDecision.THROTTLED, decideResumeAd(gate.copy(busy = false)))
    }

    @Test
    fun lock_appOpenOnly_throttled() {
        assertEquals(ResumeDecision.THROTTLED, decideResumeAd(ALL_PASS.copy(appOpenLockPassed = false)))
        assertEquals(ResumeDecision.SHOW, decideResumeAd(ALL_PASS.copy(appOpenLockPassed = true)))
    }

    @Test
    fun lock_ignoredForInter() {
        // AS-01: the interstitial of the resume reads no time gate.
        val inter = ALL_PASS.copy(placement = AdPlacement.RESUME_INTER, appOpenLockPassed = false)
        assertEquals(ResumeDecision.SHOW, decideResumeAd(inter))
        // Control: the same gate with the app open placement is throttled.
        assertEquals(ResumeDecision.THROTTLED, decideResumeAd(inter.copy(placement = AdPlacement.RESUME)))
    }

    @Test
    fun lock_beforeNotReady() {
        val gate = ALL_PASS.copy(appOpenLockPassed = false, ready = false)
        assertEquals(ResumeDecision.THROTTLED, decideResumeAd(gate))
        assertEquals(ResumeDecision.NOT_READY, decideResumeAd(gate.copy(appOpenLockPassed = true)))
    }

    @Test
    fun allPass_show() {
        assertEquals(ResumeDecision.SHOW, decideResumeAd(ALL_PASS))
        assertEquals(ResumeDecision.NOT_READY, decideResumeAd(ALL_PASS.copy(ready = false)))
    }

    @Test
    fun opportunityResult_mapping() {
        listOf(
            ResumeDecision.FIRST_FOREGROUND,
            ResumeDecision.STARTUP,
            ResumeDecision.NOT_SERVING,
            ResumeDecision.OFF,
            ResumeDecision.SHOW,
        ).forEach { assertNull(it.name, it.opportunityResult()) }
        assertEquals("suppressed", ResumeDecision.SUPPRESSED.opportunityResult())
        assertEquals("busy", ResumeDecision.BUSY.opportunityResult())
        assertEquals("throttled", ResumeDecision.THROTTLED.opportunityResult())
        assertEquals("not_ready", ResumeDecision.NOT_READY.opportunityResult())
    }

    companion object {
        /** Every gate passes, app open placement, not the first foreground. */
        internal val ALL_PASS = ResumeGate(
            firstForeground = false,
            startupFinished = true,
            serving = true,
            suppressed = false,
            placement = AdPlacement.RESUME,
            busy = false,
            appOpenLockPassed = true,
            ready = true,
        )
    }
}
