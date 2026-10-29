package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.ResumeAdRulesTest.Companion.ALL_PASS
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** One recorded `ad_opportunity` of the resume ad. */
internal data class ResumeOpportunity(val placement: AdPlacement, val result: String, val loadState: String)

/**
 * [ResumeAdPort] for tests: [gate] is what the controller reads, [placementNow] and [suppressedNow] what it reads
 * again right before showing. [liveSuppressed], when given, replaces both suppressed reads (a real ResumeSuppression).
 */
internal class FakeResumeAdPort(var gate: ResumeGate = ALL_PASS, private val liveSuppressed: (() -> Boolean)? = null) :
    ResumeAdPort {
    var placementNow: AdPlacement? = AdPlacement.RESUME
    var suppressedNow = false
    var loadState = "idle"
    var gateReads = 0
    val calls = mutableListOf<String>()
    val opportunities = mutableListOf<ResumeOpportunity>()
    val loads = mutableListOf<AdPlacement>()
    val appOpenShows = mutableListOf<Pair<Activity, AdPlacement>>()
    val interShows = mutableListOf<Triple<Activity, AdPlacement, Boolean>>()

    override fun readGate(): ResumeGate {
        gateReads++
        calls += "readGate"
        return liveSuppressed?.let { gate.copy(suppressed = it()) } ?: gate
    }

    override fun loadStateOf(placement: AdPlacement): String {
        calls += "loadStateOf:${placement.key}"
        return loadState
    }

    override fun recordOpportunity(placement: AdPlacement, result: String, loadState: String) {
        calls += "record:${placement.key}:$result:$loadState"
        opportunities += ResumeOpportunity(placement, result, loadState)
    }

    override fun loadIfNeeded(placement: AdPlacement) {
        calls += "load:${placement.key}"
        loads += placement
    }

    override fun placementNow(): AdPlacement? = placementNow

    override fun suppressedNow(): Boolean = liveSuppressed?.invoke() ?: suppressedNow

    override fun showAppOpen(activity: Activity, placement: AdPlacement) {
        calls += "showAppOpen:${placement.key}"
        appOpenShows += activity to placement
    }

    override fun showInterstitial(activity: Activity, placement: AdPlacement, combo: Boolean) {
        calls += "showInterstitial:${placement.key}:$combo"
        interShows += Triple(activity, placement, combo)
    }

    val shows: Int get() = appOpenShows.size + interShows.size
}

/** The turn of the resume ad (plan T-025, A-10 to A-15, section 8.2): decided at foreground, shown on resume. */
@RunWith(AndroidJUnit4::class)
class ResumeAdTurnsTest {
    private val logger = RecordingAppLogger()
    private val activity: Activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
    private val port = FakeResumeAdPort()
    private val turns = ResumeAdTurns(port, logger)

    private fun foreground() = turns.onProcessForegroundChanged(true)

    private fun background() = turns.onProcessForegroundChanged(false)

    private fun returnToApp() {
        background()
        foreground()
    }

    /** The first foreground of the process, used up. */
    private fun launched() {
        foreground()
        port.calls.clear()
    }

    @Test
    fun show_waitsForActivityResume_thenShowsAppOpenOnce() {
        launched()
        returnToApp()
        assertEquals(0, port.shows)
        turns.onAppActivityResumed(activity)
        assertEquals(listOf<Pair<Activity, AdPlacement>>(activity to AdPlacement.RESUME), port.appOpenShows)
        // The activity resumed again after the ad closed: no new turn.
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(logger.entries.any { it.message == "Resume ad shown on resume: resume" })
    }

    @Test
    fun interResume_showsInterstitialWithCombo() {
        port.gate = ALL_PASS.copy(placement = AdPlacement.RESUME_INTER)
        port.placementNow = AdPlacement.RESUME_INTER
        launched()
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)
        assertTrue(port.appOpenShows.isEmpty())
    }

    @Test
    fun suppressed_recordsSuppressed_loadsNothing() {
        port.gate = ALL_PASS.copy(suppressed = true, ready = false)
        launched()
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "suppressed", "idle")), port.opportunities)
        assertTrue(port.loads.isEmpty())
        assertEquals(0, port.shows)
        // Control (L-055): the block lowered, the same port loads.
        port.gate = port.gate.copy(suppressed = false)
        returnToApp()
        assertEquals(listOf(AdPlacement.RESUME), port.loads)
    }

    @Test
    fun suppressedAtResume_recordsSuppressedOnce_showsNothing() {
        launched()
        returnToApp()
        // The consent form opened at the regather step, between the decision and the show.
        port.suppressedNow = true
        turns.onAppActivityResumed(activity)
        turns.onAppActivityResumed(activity)
        assertEquals(0, port.shows)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "suppressed", "idle")), port.opportunities)
        // Control: the block gone, the next return shows.
        port.suppressedNow = false
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)
    }

    @Test
    fun busy_recordsBusy_loadsNothing() {
        port.gate = ALL_PASS.copy(busy = true, ready = false)
        launched()
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "busy", "idle")), port.opportunities)
        assertTrue(port.loads.isEmpty())
        assertEquals(0, port.shows)
        // Control: nothing on screen, the same port loads.
        port.gate = port.gate.copy(busy = false)
        returnToApp()
        assertEquals(listOf(AdPlacement.RESUME), port.loads)
    }

    @Test
    fun throttled_recordsThrottled_loadsNothing() {
        port.gate = ALL_PASS.copy(appOpenLockPassed = false, ready = false)
        launched()
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "throttled", "idle")), port.opportunities)
        assertTrue(port.loads.isEmpty())
        assertEquals(0, port.shows)
        // Control: the lock passed, the same port loads.
        port.gate = port.gate.copy(appOpenLockPassed = true)
        returnToApp()
        assertEquals(listOf(AdPlacement.RESUME), port.loads)
    }

    @Test
    fun notReady_recordsNotReadyWithStateBeforeLoad_thenLoads() {
        port.gate = ALL_PASS.copy(ready = false)
        launched()
        returnToApp()
        assertEquals(
            listOf("readGate", "loadStateOf:resume", "record:resume:not_ready:idle", "load:resume"),
            port.calls,
        )
        turns.onAppActivityResumed(activity)
        assertEquals(0, port.shows)
    }

    @Test
    fun blockedEarlier_neverLoads() {
        val notReady = ALL_PASS.copy(ready = false)
        val cases: List<Pair<ResumeGate, ResumeGate>> = listOf(
            notReady.copy(suppressed = true) to notReady,
            notReady.copy(busy = true) to notReady,
            notReady.copy(appOpenLockPassed = false) to notReady,
            notReady.copy(placement = null) to notReady,
            notReady.copy(serving = false) to notReady,
            notReady.copy(startupFinished = false) to notReady,
        )
        cases.forEach { (blocked, opened) ->
            val port = FakeResumeAdPort(gate = blocked)
            val turns = ResumeAdTurns(port, logger)
            turns.onProcessForegroundChanged(true)
            turns.onProcessForegroundChanged(false)
            turns.onProcessForegroundChanged(true)
            turns.onAppActivityResumed(activity)
            assertEquals("$blocked", 0, port.loads.size)
            // Control in the same port: the blocking gate opened, the next return loads once.
            port.gate = opened
            turns.onProcessForegroundChanged(false)
            turns.onProcessForegroundChanged(true)
            assertEquals("$blocked", 1, port.loads.size)
        }
    }

    @Test
    fun firstForeground_isSkipped_recordsNothing() {
        port.gate = ALL_PASS.copy(ready = false)
        foreground()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.gateReads)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.loads.isEmpty())
        assertEquals(0, port.shows)
        assertTrue(logger.entries.any { it.message == "Resume ad FIRST_FOREGROUND: resume" })
        // Control: the same gate on a return records and loads once.
        returnToApp()
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "not_ready", "idle")), port.opportunities)
        assertEquals(listOf(AdPlacement.RESUME), port.loads)
    }

    @Test
    fun firstForeground_consumedWhileNotServing() {
        port.gate = ALL_PASS.copy(serving = false)
        foreground()
        port.gate = ALL_PASS
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)

        // Control: a second foreground with the gate of the first is NOT_SERVING, not FIRST_FOREGROUND.
        val controlLogger = RecordingAppLogger()
        val controlPort = FakeResumeAdPort(gate = ALL_PASS.copy(serving = false, ready = false))
        val control = ResumeAdTurns(controlPort, controlLogger)
        control.onProcessForegroundChanged(true)
        control.onProcessForegroundChanged(false)
        control.onProcessForegroundChanged(true)
        control.onAppActivityResumed(activity)
        assertEquals(0, controlPort.shows)
        assertTrue(controlPort.opportunities.isEmpty())
        assertTrue(controlPort.loads.isEmpty())
        assertTrue(controlLogger.entries.any { it.message == "Resume ad NOT_SERVING: resume" })
    }

    @Test
    fun immediateReturn_isNotTimeGated() {
        launched()
        // Back at once, with no time in background.
        background()
        foreground()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)
    }

    @Test
    fun suppressedWhileOff_recordsNothing() {
        port.gate = ALL_PASS.copy(suppressed = true, placement = null)
        launched()
        returnToApp()
        assertTrue(port.opportunities.isEmpty())
        // Control: resume on, the same block is an opportunity.
        port.gate = port.gate.copy(placement = AdPlacement.RESUME)
        returnToApp()
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "suppressed", "idle")), port.opportunities)
    }

    @Test
    fun off_recordsNothing() {
        port.gate = ALL_PASS.copy(placement = null, ready = false)
        port.placementNow = null
        launched()
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.loads.isEmpty())
        assertEquals(0, port.shows)
        // Control: resume on, not ready, records once.
        port.gate = port.gate.copy(placement = AdPlacement.RESUME)
        returnToApp()
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "not_ready", "idle")), port.opportunities)
    }

    @Test
    fun twoReturns_eachAtMostOneOpportunity() {
        launched()
        port.gate = ALL_PASS.copy(busy = true)
        returnToApp()
        turns.onAppActivityResumed(activity)
        port.gate = ALL_PASS.copy(ready = false)
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(
            listOf(
                ResumeOpportunity(AdPlacement.RESUME, "busy", "idle"),
                ResumeOpportunity(AdPlacement.RESUME, "not_ready", "idle"),
            ),
            port.opportunities,
        )
    }

    @Test
    fun background_beforeResume_dropsTurn_recordsNothing() {
        launched()
        returnToApp()
        // Gone again before an activity of the app resumed.
        background()
        turns.onAppActivityResumed(activity)
        assertEquals(0, port.shows)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(logger.entries.any { it.message == "Resume ad dropped: background" })
        // Control: back again, the activity resumes and the ad shows once.
        foreground()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)
    }

    @Test
    fun switchedOffBeforeResume_showsNothing_recordsNothing() {
        launched()
        returnToApp()
        port.placementNow = null
        turns.onAppActivityResumed(activity)
        assertEquals(0, port.shows)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(logger.entries.any { it.message == "Resume ad dropped: switched" })
        // Control: switched on again, the next return shows once.
        port.placementNow = AdPlacement.RESUME
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(1, port.shows)
    }

    @Test
    fun typeChangedBeforeResume_showsNothing() {
        launched()
        returnToApp()
        port.placementNow = AdPlacement.RESUME_INTER
        turns.onAppActivityResumed(activity)
        assertTrue(port.appOpenShows.isEmpty())
        assertTrue(port.interShows.isEmpty())
        // Control: the next turn with the new type shows the interstitial once.
        port.gate = ALL_PASS.copy(placement = AdPlacement.RESUME_INTER)
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(Triple(activity, AdPlacement.RESUME_INTER, true)), port.interShows)
        assertTrue(port.appOpenShows.isEmpty())
    }

    @Test
    fun switchedOffWhileSuppressed_atResume_recordsNothing() {
        launched()
        returnToApp()
        // The key switched off and the consent form opened, both between the decision and the show (L-A05).
        port.suppressedNow = true
        port.placementNow = null
        turns.onAppActivityResumed(activity)
        assertTrue(port.opportunities.isEmpty())
        assertEquals(0, port.shows)
        assertTrue(logger.entries.any { it.message == "Resume ad dropped: switched" })
        assertTrue(logger.entries.none { it.message == "Resume ad SUPPRESSED on resume: resume" })
        // Control (L-055): switched on again and still blocked, the next return records suppressed once.
        port.placementNow = AdPlacement.RESUME
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME, "suppressed", "idle")), port.opportunities)
        assertEquals(0, port.shows)
    }

    @Test
    fun typeChangedWhileSuppressed_atResume_recordsNothing() {
        launched()
        returnToApp()
        // The type changed and the consent form opened, both between the decision and the show (L-A05).
        port.suppressedNow = true
        port.placementNow = AdPlacement.RESUME_INTER
        turns.onAppActivityResumed(activity)
        assertTrue(port.opportunities.isEmpty())
        assertEquals(0, port.shows)
        assertTrue(logger.entries.any { it.message == "Resume ad dropped: switched" })
        // Control (L-055): the next turn decided with the new type and still blocked records suppressed once.
        port.gate = ALL_PASS.copy(placement = AdPlacement.RESUME_INTER)
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf(ResumeOpportunity(AdPlacement.RESUME_INTER, "suppressed", "idle")), port.opportunities)
        assertEquals(0, port.shows)
    }

    @Test
    fun startupNotFinished_isSilent_thenShowsAfterFinish() {
        launched()
        port.gate = ALL_PASS.copy(startupFinished = false)
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(0, port.shows)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.loads.isEmpty())
        // Control: the startup turn over, the next return shows once.
        port.gate = ALL_PASS
        returnToApp()
        turns.onAppActivityResumed(activity)
        assertEquals(listOf<Pair<Activity, AdPlacement>>(activity to AdPlacement.RESUME), port.appOpenShows)
    }
}
