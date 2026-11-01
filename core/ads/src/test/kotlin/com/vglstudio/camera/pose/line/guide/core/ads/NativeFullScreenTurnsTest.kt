package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The turns of the native full-screen ad (plan T-026): the combo after an interstitial (KB-16, K-01 to K-08) and the
 * standalone turn (KB-5), with a fake port in virtual time. The port keeps a real showing flag and a real counter of
 * the resume block, so the checks of F-01 and K-05 read the same state as the controller.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class NativeFullScreenTurnsTest {
    private val logger = RecordingAppLogger()
    private var finishes = 0
    private val onFinished: () -> Unit = { finishes++ }

    private fun TestScope.newPort() = FakeNativeFullScreenPort { testScheduler.currentTime }

    private fun TestScope.turnsOf(
        port: FakeNativeFullScreenPort,
        clock: MonotonicClock = MonotonicClock { testScheduler.currentTime },
    ) = NativeFullScreenTurns(port, clock, backgroundScope, logger)

    /** Moves virtual time to [time] and runs what is due then. */
    private fun TestScope.at(time: Long) {
        advanceTimeBy(time - currentTime)
        runCurrent()
    }

    private fun NativeFullScreenTurns.afterInter(port: FakeNativeFullScreenPort) =
        afterInterstitial(AdPlacement.GALLERY_INTERSTITIAL) {
            port.calls += "onFinished"
            onFinished()
        }

    @Test
    fun comboOn_afterInter_opensNative_finishesOnlyOnClose() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        at(3_000)
        assertTrue(turns.view.value!!.closeEnabled)
        assertEquals(0, finishes)
        turns.close()
        assertEquals(1, finishes)
        assertNull(turns.view.value)
    }

    @Test
    fun comboOff_finishesAtOnce_noView_noLoad() = runTest {
        val port = newPort().apply {
            comboOn = false
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        // Taken on the calling stack, before the key is read.
        assertEquals(listOf("beginPresence", "holdResumeAds"), port.calls)
        runCurrent()
        assertEquals(0L, currentTime)
        assertEquals(1, finishes)
        assertNull(turns.view.value)
        assertEquals(0, port.takeCount)
        assertEquals(0, port.ensureCount)
        assertEquals(1, port.calls.count { it == "releaseResumeAds" })
        assertEquals(1, port.calls.count { it == "endPresence" })
        val finishedAt = port.calls.indexOf("onFinished")
        assertTrue(port.calls.indexOf("releaseResumeAds") < finishedAt)
        assertTrue(port.calls.indexOf("endPresence") < finishedAt)
        assertFalse(port.presence.showing.value)
        assertFalse(port.suppressor.suppressed.value)
        at(10_000)
        assertNull(turns.view.value)
        assertEquals(1, finishes)
    }

    @Test
    fun comboKey_readAtCompletion() = runTest {
        val port = newPort().apply { comboOn = false }
        val turns = turnsOf(port)
        // Off when the interstitial turn starts, on when it completes: the combo opens.
        port.comboOn = true
        port.slot = FakeLoadedNativeAd()
        turns.afterInter(port)
        runCurrent()
        assertNotNull(turns.view.value)
        at(3_000)
        turns.close()
        assertEquals(1, finishes)

        // Control: on when the turn starts, off when it completes: no combo.
        port.comboOn = false
        port.slot = FakeLoadedNativeAd()
        turns.afterInter(port)
        runCurrent()
        assertNull(turns.view.value)
        assertEquals(2, finishes)
    }

    @Test
    fun secondTurn_whilePresenceBusy_finishesAtOnce() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        assertTrue(port.presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, 1, 0L))
        val turns = turnsOf(port)
        turns.afterInter(port)
        assertEquals(1, finishes)
        assertFalse(port.suppressor.suppressed.value)
        runCurrent()
        assertNull(turns.view.value)
        assertEquals(1, finishes)
        assertTrue(logger.entries.any { it.message == "Native combo skipped: busy" })
    }

    @Test
    fun afterInterstitial_doesNotReadInterShown() {
        val names = NativeFullScreenPort::class.java.methods.map { it.name }
        // Control: the port is read by reflection, so an empty list cannot pass.
        assertTrue(names.toString(), "interGone" in names)
        assertTrue(names.toString(), names.none { it.contains("shown", ignoreCase = true) })
    }

    @Test
    fun opensAfterInterGone_deadlineFromThen() = runTest {
        val port = newPort().apply {
            interGoneAt = 600L
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(599)
        assertNull(turns.view.value)
        at(600)
        assertEquals(3, turns.view.value!!.secondsLeft)
        at(1_600)
        assertEquals(2, turns.view.value!!.secondsLeft)
        at(3_599)
        assertFalse(turns.view.value!!.closeEnabled)
        at(3_600)
        assertEquals(0, turns.view.value!!.secondsLeft)
        assertTrue(turns.view.value!!.closeEnabled)
    }

    @Test
    fun interNeverGone_opensAfter2s() = runTest {
        val port = newPort().apply {
            interGoneAt = null
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(1_999)
        assertNull(turns.view.value)
        at(2_000)
        assertEquals(3, turns.view.value!!.secondsLeft)
        at(4_999)
        assertFalse(turns.view.value!!.closeEnabled)
        at(5_000)
        assertTrue(turns.view.value!!.closeEnabled)
    }

    @Test
    fun secondsLeft_followsClock_notTickCount() = runTest {
        var jump = 0L
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port, MonotonicClock { testScheduler.currentTime + jump })
        turns.afterInter(port)
        runCurrent()
        assertEquals(3, turns.view.value!!.secondsLeft)
        // The main thread was busy for 1.5 s: the next tick reads the clock, it does not count ticks.
        jump = 1_500L
        at(1_000)
        assertEquals(1, turns.view.value!!.secondsLeft)
    }

    @Test
    fun close_beforeCountdownEnds_isIgnored() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(1_000)
        turns.close()
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        assertTrue(port.marks.isEmpty())
        // Control: the same close once the countdown is over closes.
        at(3_000)
        turns.close()
        assertNull(turns.view.value)
        assertEquals(1, finishes)
    }

    @Test
    fun loadFailed_closeAtOnce_thenAutoCloseAtDeadline() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertFalse(turns.view.value!!.closeEnabled)
        at(1_000)
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, null)
        runCurrent()
        assertTrue(turns.view.value!!.loadFailed)
        assertTrue(turns.view.value!!.closeEnabled)
        at(2_999)
        assertEquals(0, finishes)
        at(3_000)
        assertEquals(1, finishes)
        assertNull(turns.view.value)
        assertEquals(listOf("not_ready" to "idle"), port.opportunities)
    }

    @Test
    fun loadFailsAfterDeadline_closesAtOnce() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        assertNotNull(turns.view.value)
        assertNull(turns.view.value!!.ad)
        assertTrue(turns.view.value!!.closeEnabled)
        assertEquals(0, finishes)
        at(4_200)
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, null)
        runCurrent()
        assertEquals(4_200L, currentTime)
        assertEquals(1, finishes)
        assertNull(turns.view.value)
        assertEquals(listOf("not_ready" to "idle"), port.opportunities)
    }

    @Test
    fun otherPlacementResult_doesNotFailCombo() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(1_000)
        port.deliver(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, null)
        runCurrent()
        assertFalse(turns.view.value!!.loadFailed)
        assertNull(turns.view.value!!.ad)
        assertEquals(1, port.takeCount)
        at(1_500)
        val ad = FakeLoadedNativeAd()
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, ad)
        runCurrent()
        assertSame(ad, turns.view.value!!.ad)
    }

    @Test
    fun nothingToLoad_isFailedAtOnce() = runTest {
        val port = newPort().apply { ensureLoadingResult = false }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertTrue(turns.view.value!!.loadFailed)
        assertTrue(turns.view.value!!.closeEnabled)
    }

    @Test
    fun adNotYetLoaded_staysOpenAfterDeadline_closable() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        at(10_000)
        val view = turns.view.value!!
        assertNull(view.ad)
        assertFalse(view.loadFailed)
        assertTrue(view.closeEnabled)
        assertEquals(0, finishes)
    }

    @Test
    fun adArrivesLate_isShown() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(1_500)
        val ad = FakeLoadedNativeAd()
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, ad)
        runCurrent()
        assertSame(ad, turns.view.value!!.ad)
        assertFalse(turns.view.value!!.closeEnabled)
    }

    @Test
    fun adArrivesAfterDeadline_isShown() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(4_000)
        val ad = FakeLoadedNativeAd()
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, ad)
        runCurrent()
        assertSame(ad, turns.view.value!!.ad)
        assertTrue(turns.view.value!!.closeEnabled)
        assertEquals(0, finishes)
    }

    @Test
    fun close_twice_runsOnce() = runTest {
        val ad = FakeLoadedNativeAd()
        val port = newPort().apply { slot = ad }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        turns.close()
        turns.close()
        assertEquals(1, finishes)
        assertEquals(1, port.marks.size)
        assertEquals(1, ad.destroyCount)
        assertEquals(1, port.opportunities.size)
        assertEquals(1, port.reloads.size)
    }

    @Test
    fun close_order_markFirst_finishLast() = runTest {
        val port = newPort()
        port.slot = RecordingAd(port.calls)
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        port.calls.clear()
        turns.close()
        assertEquals(
            listOf(
                "recordIntervalMark",
                "destroy",
                "releaseResumeAds",
                "endPresence",
                "recordOpportunity",
                "reload",
                "onFinished",
            ),
            port.calls,
        )
    }

    @Test
    fun close_marksAtCloseTime() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(7_000)
        turns.close()
        assertEquals(listOf(AdPlacement.GALLERY_INTERSTITIAL to 7_000L), port.marks)
    }

    @Test
    fun holdsResumeAds_fromStartUntilClose() = runTest {
        val port = newPort().apply {
            interGoneAt = 600L
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        assertEquals(0L, currentTime)
        assertTrue(port.suppressor.suppressed.value)
        assertTrue(port.presence.showing.value)
        at(3_600)
        assertTrue(port.suppressor.suppressed.value)
        turns.close()
        assertEquals(1, port.calls.count { it == "holdResumeAds" })
        assertEquals(1, port.calls.count { it == "releaseResumeAds" })
        assertEquals(1, port.calls.count { it == "beginPresence" })
        assertEquals(1, port.calls.count { it == "endPresence" })
        assertFalse(port.suppressor.suppressed.value)
        assertFalse(port.presence.showing.value)
    }

    @Test
    fun noGap_presenceAndHoldTakenBeforeAnySuspension() = runTest {
        val port = newPort().apply {
            interGoneAt = null
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        // runCurrent not called: nothing of the scope has run yet.
        assertTrue(port.presence.isBusy(0L))
        assertTrue(port.suppressor.suppressed.value)
        assertFalse(port.presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, 1, 0L))
        at(1_000)
        assertFalse(port.presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, 1, currentTime))
        at(1_999)
        assertNull(turns.view.value)
        assertFalse(port.presence.tryBegin(AdPlacement.GALLERY_INTERSTITIAL, 1, currentTime))
        assertTrue(port.suppressor.suppressed.value)
    }

    @Test
    fun close_metricsThrows_stillReleasesAndFinishes() = runTest {
        val port = newPort().apply {
            slot = FakeLoadedNativeAd()
            throwOnOpportunity = true
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        runCatching { turns.close() }
        assertEquals(1, port.marks.size)
        assertEquals(1, port.calls.count { it == "releaseResumeAds" })
        assertEquals(1, port.calls.count { it == "endPresence" })
        assertEquals(1, finishes)
        assertFalse(port.presence.showing.value)
        assertFalse(port.suppressor.suppressed.value)
    }

    @Test
    fun close_destroysOwnAd_andReloads() = runTest {
        val ad = FakeLoadedNativeAd()
        val other = FakeLoadedNativeAd()
        val port = newPort().apply { slot = ad }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        turns.close()
        assertEquals(1, ad.destroyCount)
        assertEquals(0, other.destroyCount)
        assertEquals(listOf(AdPlacement.NATIVE_AFTER_INTER), port.reloads)
    }

    @Test
    fun adArrivesAfterClose_isNotTaken() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        turns.close()
        assertEquals(1, port.takeCount)
        val late = FakeLoadedNativeAd()
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, late)
        runCurrent()
        assertEquals(1, port.takeCount)
        assertEquals(0, late.destroyCount)
        assertSame(late, port.slot)
    }

    @Test
    fun oneOpportunity_shown() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        turns.close()
        assertEquals(listOf("shown" to "ready"), port.opportunities)
    }

    @Test
    fun oneOpportunity_closedBeforeAd_notReady() = runTest {
        val port = newPort().apply {
            loadState = "idle"
            loadStateAfterEnsure = "loading"
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(3_000)
        turns.close()
        // The state taken when the screen opened, before the load of the combo.
        assertEquals(listOf("not_ready" to "idle"), port.opportunities)
    }

    @Test
    fun oneOpportunity_failed_notReady() = runTest {
        val port = newPort()
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(500)
        port.deliver(AdPlacement.NATIVE_AFTER_INTER, null)
        at(3_000)
        assertEquals(1, finishes)
        assertEquals(listOf("not_ready" to "idle"), port.opportunities)
    }

    @Test
    fun consentClosedAtOpen_takesNoAd_isFailed() = runTest {
        val port = newPort().apply {
            consent = false
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertEquals(0, port.takeCount)
        assertEquals(0, port.ensureCount)
        assertTrue(turns.view.value!!.loadFailed)
        assertNull(turns.view.value!!.ad)
    }

    @Test
    fun comboKeyOff_whileOpen_staysUntilClosed_thenNoNextCombo() = runTest {
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        port.comboOn = false
        at(10_000)
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        turns.close()
        assertEquals(1, finishes)
        port.slot = FakeLoadedNativeAd()
        turns.afterInter(port)
        runCurrent()
        assertNull(turns.view.value)
        assertEquals(2, finishes)
    }

    @Test
    fun standalone_opensAtOnce_withCountdown_marksOwnPlacement() = runTest {
        val port = newPort().apply { interGoneAt = null }
        // The controller took the showing flag at its gate.
        assertTrue(port.presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, 0, 0L))
        val turns = turnsOf(port)
        val ad = FakeLoadedNativeAd()
        turns.showStandalone(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, ad, onFinished)
        assertEquals(3, turns.view.value!!.secondsLeft)
        assertSame(ad, turns.view.value!!.ad)
        assertTrue(port.suppressor.suppressed.value)
        at(2_999)
        turns.close()
        assertEquals(0, finishes)
        at(3_000)
        turns.close()
        assertEquals(1, finishes)
        assertEquals(listOf(AdPlacement.GALLERY_NATIVE_FULL_SCREEN to 3_000L), port.marks)
        assertTrue(port.opportunities.isEmpty())
        assertEquals(1, ad.destroyCount)
        assertEquals(listOf(AdPlacement.GALLERY_NATIVE_FULL_SCREEN), port.reloads)
        assertFalse(port.presence.showing.value)
        assertFalse(port.suppressor.suppressed.value)
        assertEquals(0, port.takeCount)
    }

    @Test
    fun combo_countdownFromPort_fiveSeconds() = runTest {
        val port = newPort().apply {
            countdownSeconds = 5L
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertEquals(5, turns.view.value!!.secondsLeft)
        at(4_999)
        assertEquals(1, turns.view.value!!.secondsLeft)
        turns.close()
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        at(5_000)
        assertTrue(turns.view.value!!.closeEnabled)
        turns.close()
        turns.close()
        assertNull(turns.view.value)
        assertEquals(1, finishes)
    }

    @Test
    fun combo_countdownThirtySeconds() = runTest {
        val port = newPort().apply {
            countdownSeconds = 30L
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertEquals(30, turns.view.value!!.secondsLeft)
        at(29_999)
        assertEquals(1, turns.view.value!!.secondsLeft)
        turns.close()
        assertNotNull(turns.view.value)
        at(30_000)
        assertTrue(turns.view.value!!.closeEnabled)
        turns.close()
        assertEquals(1, finishes)
    }

    @Test
    fun countdownChange_whileOpen_keepsDeadline_nextTurnUsesNew() = runTest {
        val port = newPort().apply {
            countdownSeconds = 5L
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertEquals(5, turns.view.value!!.secondsLeft)
        at(1_000)
        port.countdownSeconds = 30L
        at(5_000)
        // The turn open keeps its deadline: the key is read once, when the screen opens (K-02).
        assertTrue(turns.view.value!!.closeEnabled)
        turns.close()
        assertEquals(1, finishes)

        // The next turn reads the new value.
        port.slot = FakeLoadedNativeAd()
        val next = currentTime
        turns.afterInter(port)
        runCurrent()
        assertEquals(30, turns.view.value!!.secondsLeft)
        at(next + 5_000)
        assertFalse(turns.view.value!!.closeEnabled)
        turns.close()
        assertEquals(1, finishes)
    }

    @Test
    fun interNeverGone_opensAfter2s_thenFullCountdown() = runTest {
        val port = newPort().apply {
            countdownSeconds = 5L
            interGoneAt = null
            slot = FakeLoadedNativeAd()
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        at(1_999)
        assertNull(turns.view.value)
        at(2_000)
        assertEquals(5, turns.view.value!!.secondsLeft)
        at(6_999)
        assertFalse(turns.view.value!!.closeEnabled)
        at(7_000)
        assertTrue(turns.view.value!!.closeEnabled)
    }

    @Test
    fun loadFailed_autoClosesAtEndOfFiveSeconds() = runTest {
        val port = newPort().apply {
            countdownSeconds = 5L
            ensureLoadingResult = false
        }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        assertTrue(turns.view.value!!.loadFailed)
        assertTrue(turns.view.value!!.closeEnabled)
        at(4_999)
        assertNotNull(turns.view.value)
        assertEquals(0, finishes)
        at(5_000)
        assertNull(turns.view.value)
        assertEquals(1, finishes)
    }

    @Test
    fun standalone_usesPortCountdown() = runTest {
        val port = newPort().apply {
            countdownSeconds = 5L
            interGoneAt = null
        }
        assertTrue(port.presence.tryBegin(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, 0, 0L))
        val turns = turnsOf(port)
        turns.showStandalone(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, FakeLoadedNativeAd(), onFinished)
        assertEquals(5, turns.view.value!!.secondsLeft)
        at(4_999)
        assertFalse(turns.view.value!!.closeEnabled)
        turns.close()
        assertEquals(0, finishes)
        at(5_000)
        assertTrue(turns.view.value!!.closeEnabled)
        turns.close()
        assertEquals(1, finishes)
        assertEquals(listOf(AdPlacement.GALLERY_NATIVE_FULL_SCREEN to 5_000L), port.marks)
    }

    @Test
    fun standaloneWhileComboOpen_isRejected() = runTest {
        val comboAd = FakeLoadedNativeAd()
        val port = newPort().apply { slot = comboAd }
        val turns = turnsOf(port)
        turns.afterInter(port)
        runCurrent()
        var standaloneFinishes = 0
        turns.showStandalone(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, FakeLoadedNativeAd()) { standaloneFinishes++ }
        assertEquals(1, standaloneFinishes)
        assertSame(comboAd, turns.view.value!!.ad)
        assertEquals(0, finishes)
    }

    @Test
    fun comboWhileStandaloneOpen_presenceFree_skipsBusy() = runTest {
        // The `turn != null` side of the busy check of the combo: a native turn is open while the showing flag is
        // free (as past the cap of F-02), so only the open turn can stop the combo.
        val port = newPort().apply { slot = FakeLoadedNativeAd() }
        val turns = turnsOf(port)
        val standalone = FakeLoadedNativeAd()
        var standaloneFinishes = 0
        turns.showStandalone(AdPlacement.GALLERY_NATIVE_FULL_SCREEN, standalone) { standaloneFinishes++ }
        turns.afterInter(port)
        assertEquals(1, finishes)
        assertTrue(logger.entries.any { it.message == "Native combo skipped: busy" })
        runCurrent()
        assertSame(standalone, turns.view.value!!.ad)
        assertFalse(port.presence.showing.value)
        assertFalse(port.calls.contains("beginPresence"))
        assertEquals(0, standaloneFinishes)
    }
}

/** A loaded ad that writes its destroy into the call list of the port. */
private class RecordingAd(private val calls: MutableList<String>) : LoadedNativeAd {
    override val assets: NativeAdAssets = sampleNativeAdAssets

    override fun destroy() {
        calls += "destroy"
    }
}

/**
 * A port of [NativeFullScreenTurns] that records its calls in order. The showing flag and the resume block are the
 * real ones; load results go through a real [NativeLoadResults], answered by [deliver].
 */
internal class FakeNativeFullScreenPort(private val now: () -> Long) : NativeFullScreenPort {
    val presence = FullScreenAdPresence()
    val suppressor = ForegroundAdSuppressor()
    private val results = NativeLoadResults()

    val calls = mutableListOf<String>()
    val marks = mutableListOf<Pair<AdPlacement, Long>>()
    val opportunities = mutableListOf<Pair<String, String>>()
    val reloads = mutableListOf<AdPlacement>()

    var comboOn = true

    /** The time the interstitial leaves the screen; null never. */
    var interGoneAt: Long? = 0L
    var consent = true

    /** The ad in the slot of `native_combo`; taken once. */
    var slot: LoadedNativeAd? = null
    var ensureLoadingResult = true
    var loadState = "idle"
    var loadStateAfterEnsure: String? = null
    var throwOnOpportunity = false

    /** `time_close_native` as the controller reads it, already in 1..30 (plan T-053); 3 keeps the older tests. */
    var countdownSeconds = 3L
    var takeCount = 0
        private set
    var ensureCount = 0
        private set

    override suspend fun comboEnabled(): Boolean = comboOn

    override fun beginPresence(): Boolean {
        calls += "beginPresence"
        return presence.tryBegin(AdPlacement.NATIVE_AFTER_INTER, 0, now())
    }

    override fun endPresence() {
        calls += "endPresence"
        presence.onClosed()
    }

    override fun holdResumeAds(): ForegroundAdSuppressor.Hold {
        calls += "holdResumeAds"
        return suppressor.acquire(ResumeSuppression.FULL_SCREEN_PAGE)
    }

    override fun releaseResumeAds(hold: ForegroundAdSuppressor.Hold) {
        calls += "releaseResumeAds"
        hold.release()
    }

    override fun interGone(): Boolean = interGoneAt?.let { now() >= it } ?: false

    override fun consentOpen(): Boolean = consent

    override fun loadStateAtOpen(): String = loadState

    override fun takeAd(): LoadedNativeAd? {
        takeCount++
        return slot.also { slot = null }
    }

    override fun ensureLoading(): Boolean {
        ensureCount++
        loadStateAfterEnsure?.let { loadState = it }
        return ensureLoadingResult
    }

    override fun loadGeneration(placement: AdPlacement): Int = results.generation(placement)

    override suspend fun awaitLoadResult(placement: AdPlacement, since: Int) = results.await(placement, since)

    override fun recordIntervalMark(source: AdPlacement) {
        calls += "recordIntervalMark"
        marks += source to now()
    }

    override fun recordOpportunity(result: String, loadState: String) {
        calls += "recordOpportunity"
        check(!throwOnOpportunity) { "metrics failed" }
        opportunities += result to loadState
    }

    override fun closeCountdownSeconds(): Long = countdownSeconds

    override fun reload(placement: AdPlacement) {
        calls += "reload"
        reloads += placement
    }

    /** A load of [placement] finished: with [ad] in the slot of `native_combo`, or failed when null. */
    fun deliver(placement: AdPlacement, ad: LoadedNativeAd?) {
        if (ad != null && placement == AdPlacement.NATIVE_AFTER_INTER) slot = ad
        results.signal(placement)
    }
}
