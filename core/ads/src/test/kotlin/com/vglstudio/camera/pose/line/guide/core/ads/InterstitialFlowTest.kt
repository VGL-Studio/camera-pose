package com.vglstudio.camera.pose.line.guide.core.ads

import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The order of the steps of a full-screen ad request (plan T-018, version 2). Every case puts a shown run next to
 * a blocked run, so a flow that always returns one answer fails.
 */
class InterstitialFlowTest {
    private val placement = AdPlacement.REVIEW_INTERSTITIAL
    private val reviewCap = BreakCap(firstBreak = 2, breaksBetween = 3, minIntervalSeconds = 120)
    private val breaks = InterstitialBreaks()
    private val fake = FakeSteps()

    private class FakeSteps {
        var consent = true
        var enabled = true

        /** Answers of `networkDialogShown`, one per call; false when empty. */
        val dialogShown = ArrayDeque<Boolean>()
        var cap = BreakCap.NONE
        var loaded = true
        var quiet = true
        var now = 0L
        val calls = mutableListOf<String>()
        val logs = mutableListOf<String>()
        var shows = 0
        var preloads = 0

        val steps = InterstitialSteps(
            canRequestAds = {
                calls += "canRequestAds"
                consent
            },
            placementEnabled = {
                calls += "placementEnabled"
                enabled
            },
            networkDialogShown = {
                calls += "networkDialogShown"
                dialogShown.removeFirstOrNull() ?: false
            },
            breakCap = {
                calls += "breakCap"
                cap
            },
            hasLoadedAd = {
                calls += "hasLoadedAd"
                loaded
            },
            preload = {
                calls += "preload"
                preloads++
            },
            waitUntilQuiet = {
                calls += "waitUntilQuiet"
                quiet
            },
            show = {
                calls += "show"
                shows++
            },
            nowMillis = { now },
            log = { logs += it },
        )
    }

    private suspend fun run(newFake: FakeSteps = fake, newBreaks: InterstitialBreaks = breaks): Boolean {
        newFake.calls.clear()
        return runInterstitial(placement, newBreaks, newFake.steps)
    }

    @Test
    fun allPass_showsInOrder() = runTest {
        fake.cap = reviewCap
        assertFalse(run())
        assertEquals(0, fake.shows)
        assertTrue(run())
        assertEquals(1, fake.shows)
        assertEquals(
            listOf(
                "canRequestAds",
                "placementEnabled",
                "networkDialogShown",
                "breakCap",
                "hasLoadedAd",
                "waitUntilQuiet",
                "networkDialogShown",
                "show",
            ),
            fake.calls,
        )
    }

    @Test
    fun blockedBreak_stillCounts() = runTest {
        fake.cap = reviewCap
        fake.consent = false
        assertFalse(run())
        assertFalse("the cap is read after the consent check", "breakCap" in fake.calls)
        fake.consent = true
        assertTrue("the blocked run was break 1, so this is break 2", run())
        // Contrast: a fresh flow whose first run is allowed is break 1 and is skipped by the cap.
        val fresh = FakeSteps().apply { cap = reviewCap }
        assertFalse(run(fresh, InterstitialBreaks()))
    }

    @Test
    fun notAllowed_skipsBeforeWaiting() = runTest {
        fake.consent = false
        assertFalse(run())
        fake.consent = true
        fake.enabled = false
        assertFalse(run())
        fake.enabled = true
        fake.dialogShown += true
        assertFalse(run())
        assertFalse("waitUntilQuiet" in fake.calls)
        assertEquals(0, fake.shows)
        assertTrue(run())
        assertEquals(1, fake.shows)
    }

    @Test
    fun capBlocks_beforeLoadCheck() = runTest {
        fake.cap = reviewCap
        assertFalse(run())
        assertTrue(run())
        assertFalse(run())
        assertFalse("hasLoadedAd" in fake.calls)
        assertFalse(run())
        fake.now = 120_000
        assertTrue(run())
        assertEquals(2, fake.shows)
    }

    @Test
    fun notLoaded_preloadsWithoutWaiting() = runTest {
        fake.loaded = false
        assertFalse(run())
        assertEquals(1, fake.preloads)
        assertFalse("waitUntilQuiet" in fake.calls)
        assertEquals(0, fake.shows)
        fake.loaded = true
        assertTrue(run())
        assertEquals(1, fake.preloads)
    }

    @Test
    fun guardRefuses_doesNotShow() = runTest {
        fake.quiet = false
        assertFalse(run())
        assertEquals(0, fake.shows)
        fake.quiet = true
        assertTrue(run())
        assertEquals(1, fake.shows)
    }

    @Test
    fun networkDialogDuringWait_doesNotShow() = runTest {
        fake.dialogShown += listOf(false, true)
        assertFalse(run())
        assertEquals(0, fake.shows)
        assertTrue("waitUntilQuiet" in fake.calls)
        fake.dialogShown += listOf(false, false)
        assertTrue(run())
        assertEquals(1, fake.shows)
    }

    @Test
    fun shownBreak_isRecorded() = runTest {
        fake.cap = reviewCap
        assertFalse(run()) // break 1
        assertTrue(run()) // break 2
        fake.now = 600_000
        assertFalse(run()) // break 3
        assertFalse(run()) // break 4
        assertTrue(run()) // break 5
        // Contrast: when break 2 is refused by the guard, nothing is recorded and break 3 is shown.
        val other = FakeSteps().apply { cap = reviewCap }
        val otherBreaks = InterstitialBreaks()
        assertFalse(run(other, otherBreaks)) // break 1
        other.quiet = false
        assertFalse(run(other, otherBreaks)) // break 2, refused
        other.quiet = true
        assertTrue(run(other, otherBreaks)) // break 3
    }

    @Test
    fun skips_areLogged() = runTest {
        assertEquals(listOf("skipped: not_allowed"), skipLogs({ consent = false }))
        assertEquals(listOf("skipped: cap"), skipLogs({ cap = reviewCap }))
        assertEquals(listOf("skipped: not_loaded"), skipLogs({ loaded = false }))
        assertEquals(listOf("skipped: guard"), skipLogs({ quiet = false }))
        assertEquals(listOf("skipped: network_dialog"), skipLogs({ dialogShown += listOf(false, true) }))
        val shown = FakeSteps()
        assertTrue(run(shown, InterstitialBreaks()))
        assertTrue(shown.logs.none { it.startsWith("skipped") })
    }

    private suspend fun skipLogs(prepare: FakeSteps.() -> Unit): List<String> {
        val other = FakeSteps().apply(prepare)
        run(other, InterstitialBreaks())
        return other.logs.toList()
    }
}
