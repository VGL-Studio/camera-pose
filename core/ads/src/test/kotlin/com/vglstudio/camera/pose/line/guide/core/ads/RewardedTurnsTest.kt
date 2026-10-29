package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The rewarded turns and load rounds with a fake port (KB-19, MC-05, R-01 to R-06, F-07, L-068, L-072; plan T-031).
 * The fake fires the SDK callbacks itself ([FakeRewardedPort.fireShowed] and the others).
 */
@OptIn(ExperimentalCoroutinesApi::class)
class RewardedTurnsTest {
    private val logger = RecordingAppLogger()
    private val port = FakeRewardedPort()
    private val turns = RewardedTurns(port, logger).also { port.turns = it }

    /** Outcomes of the single caller of a test; tests with two callers keep a list each. */
    private val outcomes = mutableListOf<RewardedOutcome>()
    private val collect: (RewardedOutcome) -> Unit = { outcomes += it }

    private suspend fun request(
        declaration: AdPlacementDeclaration = rwGetItem,
        to: (RewardedOutcome) -> Unit = collect,
    ) = turns.request(declaration, activity = null, onOutcome = to)

    @Test
    fun switchOff_grantsAtOnce_noShowNoMetric() = runTest {
        port.enabled["rw_get_item"] = false
        port.ready["rw_get_item"] = true

        request()

        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        assertTrue(port.shows.isEmpty())
        assertTrue(port.opportunities.isEmpty())
    }

    @Test
    fun switchedOffWhileTurnPending_secondTapShowsAgain_grantOnce() = runTest {
        // L-061: the key goes off while the first ad is on its way; a second tap is still R-06, not "switched off".
        port.ready["rw_get_item"] = true
        request()
        port.enabled["rw_get_item"] = false
        request()
        port.fireShowed()
        port.fireReward()

        assertEquals(1, outcomes.count { it == RewardedOutcome.Granted })
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item"), port.showAgains)
    }

    @Test
    fun twoTapsAwaitingSwitch_firstOnSecondOff_secondShowsAgain_grantOnce() = runTest {
        // L-061: two taps before any turn exists; the first waits for the switch, the second joins its decision
        // (plan T-042) and never reads the switch, so the key going off meanwhile does not matter: once the first
        // shows the ad, the second is still the second tap of that turn (R-06).
        port.ready["rw_get_item"] = true
        val first = CompletableDeferred<Boolean>()
        val second = CompletableDeferred<Boolean>()
        port.switchGates += first
        port.switchGates += second
        launch { request() }
        launch { request() }
        runCurrent()
        assertTrue(port.shows.isEmpty())

        first.complete(true)
        runCurrent()
        assertEquals(listOf("rw_get_item"), port.shows)
        second.complete(false)
        runCurrent()
        port.fireShowed()
        port.fireReward()

        assertEquals(1, outcomes.count { it == RewardedOutcome.Granted })
        assertEquals(listOf("rw_get_item"), port.showAgains)
    }

    @Test
    fun switchedOffWhileDialogOpen_watchGrantsAtOnce() = runTest {
        port.ready["rw_get_item"] = true
        // The dialog opened while the key was on; it is off when "Watch ad" is tapped (L-061).
        port.enabled["rw_get_item"] = false
        request()
        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        assertTrue(port.shows.isEmpty())

        // Control: on, the ad is shown.
        port.enabled["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item"), port.shows)
    }

    @Test
    fun consentClosed_grantsAtOnce_noOpportunity() = runTest {
        // Question 8 of the plan, option (a) chosen by the reviewer: a closed consent gate counts as switched off.
        port.consentOpen = false
        port.ready["rw_get_item"] = true

        request()
        request(rwAllow)

        assertEquals(listOf(RewardedOutcome.Granted, RewardedOutcome.Granted), outcomes)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.shows.isEmpty())
        assertTrue(port.loads.isEmpty())

        // Control: with the gate open, the same placement shows its ad.
        port.consentOpen = true
        request()
        assertEquals(listOf("rw_get_item"), port.shows)
    }

    @Test
    fun reward_grantsAtRewardCallback_beforeDismiss() = runTest {
        port.ready["rw_get_item"] = true
        request()
        port.fireShowed()
        port.fireReward()

        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item"), port.rewards)

        port.fireDismissed()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
    }

    @Test
    fun notReady_defaultBlock_noGrant_loadsIfNeeded_recordsNotReady() = runTest {
        request()

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(listOf("rw_get_item"), port.loads)
        // F-07 reaches the load round: the fake wires loadIfNeeded back into RewardedTurns.loadIfNeeded.
        assertEquals(listOf("rw_get_item"), port.sends)
        // load_state is taken before the load, although the load moved the fake to `loading` (section 8.2).
        assertEquals("loading", port.loadState["rw_get_item"])
        assertEquals(listOf(Triple("rw_get_item", "not_ready", "idle")), port.opportunities)

        // The user may try again.
        port.ready["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item"), port.shows)
    }

    @Test
    fun notReady_allow_grants() = runTest {
        request(rwAllow)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
    }

    @Test
    fun notReady_allowAfter2_grantsOnSecondTryInARow_thenCountsAgain() = runTest {
        repeat(3) { request(rwExport) }

        assertEquals(listOf(false, true, false), outcomes.map { (it as RewardedOutcome.NoAd).granted })
    }

    @Test
    fun policyOverride_allowAfter2_onGallery() = runTest {
        // The gallery placement declares no policy (Block); the gallery picks one at run time (plan T-036, step 2b).
        val gallery = AdPlacement.GALLERY_RW_ACTION.declaration
        repeat(3) { turns.request(gallery, null, collect, RewardedNoAdPolicy.AllowAfter(2)) }
        assertEquals(listOf(false, true, false), outcomes.map { (it as RewardedOutcome.NoAd).granted })

        // Control (L-055): without the override the line's policy blocks every try.
        outcomes.clear()
        repeat(3) { request(gallery) }
        assertEquals(listOf(false, false, false), outcomes.map { (it as RewardedOutcome.NoAd).granted })
    }

    @Test
    fun policyOverride_appliesWhenShowFails() = runTest {
        val gallery = AdPlacement.GALLERY_RW_ACTION.declaration
        port.ready["gallery_rw_action"] = true
        turns.request(gallery, null, collect, RewardedNoAdPolicy.Allow)
        port.fireShowFailed(0)

        // The turn keeps the policy of its request: the show failure runs the action.
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
    }

    @Test
    fun policyChanged_resetsNoAdCount() = runTest {
        val gallery = AdPlacement.GALLERY_RW_ACTION.declaration
        val allowAfter2 = RewardedNoAdPolicy.AllowAfter(2)
        // One try with no ad under allow_after_2, then the tester picks block, then allow_after_2 again.
        turns.request(gallery, null, collect, allowAfter2)
        turns.request(gallery, null, collect, RewardedNoAdPolicy.Block)
        turns.request(gallery, null, collect, allowAfter2)
        turns.request(gallery, null, collect, allowAfter2)

        // allow_after_2 counts from the start after the change: granted on its second try in a row only.
        assertEquals(listOf(false, false, false, true), outcomes.map { (it as RewardedOutcome.NoAd).granted })

        // Control (L-055): with no change in between, the second try in a row is granted.
        outcomes.clear()
        turns.request(gallery, null, collect, allowAfter2)
        turns.request(gallery, null, collect, allowAfter2)
        assertEquals(listOf(false, true), outcomes.map { (it as RewardedOutcome.NoAd).granted })
    }

    @Test
    fun allowAfter_counterResetsWhenAdShows() = runTest {
        request(rwExport)
        port.ready["rw_export"] = true
        request(rwExport)
        port.fireShowed()
        port.fireDismissed()
        request(rwExport)

        val noAds = outcomes.filterIsInstance<RewardedOutcome.NoAd>()
        assertEquals(2, noAds.size)
        // Control (L-055): without the ad in between, the second try in a row would be granted.
        assertFalse(noAds.last().granted)
    }

    @Test
    fun showFailed_endsTurnWithNoAd_policyApplies() = runTest {
        port.ready["rw_get_item"] = true
        request()
        port.fireShowFailed(0)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)

        val allowed = mutableListOf<RewardedOutcome>()
        port.ready["rw_allow"] = true
        request(rwAllow) { allowed += it }
        port.fireShowFailed(0)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), allowed)

        // The turn is over: a new request shows again, with no `busy`.
        port.ready["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item", "rw_allow", "rw_get_item"), port.shows)
        assertFalse(port.opportunities.any { it.second == "busy" })
    }

    @Test
    fun notReady_offline_reportsNetwork() = runTest {
        port.offline = true

        request()

        assertEquals(listOf(RewardedOutcome.NoAd(network = true, granted = false)), outcomes)
    }

    @Test
    fun lastLoadFailedCode2_reportsNetwork_clearedAtNextSend() = runTest {
        turns.onLoadFailed(rwGetItem, 2, offline = false)
        request()
        assertEquals(RewardedOutcome.NoAd(network = true, granted = false), outcomes.last())

        port.loadState["rw_get_item"] = "idle"
        turns.loadIfNeeded(rwGetItem)
        request()
        assertEquals(RewardedOutcome.NoAd(network = false, granted = false), outcomes.last())

        // Control: another code while online is no network error.
        turns.onLoadFailed(rwGetItem, 3, offline = false)
        request()
        assertEquals(RewardedOutcome.NoAd(network = false, granted = false), outcomes.last())

        // A round blocked at the consent gate is no send: the flag stays.
        port.loadState["rw_get_item"] = "idle"
        port.mayRequest = false
        turns.onLoadFailed(rwGetItem, 2, offline = false)
        turns.loadIfNeeded(rwGetItem)
        request()
        assertEquals(RewardedOutcome.NoAd(network = true, granted = false), outcomes.last())
    }

    @Test
    fun dismissedBeforeReward_noGrant_anyPolicy() = runTest {
        listOf(rwGetItem, rwAllow).forEach { declaration ->
            val got = mutableListOf<RewardedOutcome>()
            port.ready[declaration.code] = true
            request(declaration) { got += it }
            port.fireShowed()
            port.fireDismissed()
            assertEquals(declaration.code, listOf(RewardedOutcome.OnScreen), got)
        }
        assertTrue(port.rewards.isEmpty())

        // Control: the reward before the close grants.
        val got = mutableListOf<RewardedOutcome>()
        port.ready["rw_get_item"] = true
        request { got += it }
        port.fireShowed()
        port.fireReward()
        port.fireDismissed()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), got)
    }

    @Test
    fun secondRequestBeforeOnScreen_showsAgain_recordsBusy() = runTest {
        port.ready["rw_get_item"] = true
        request()
        request()

        assertEquals(listOf("rw_get_item"), port.shows)
        assertEquals(listOf("rw_get_item"), port.showAgains)
        assertEquals(
            listOf(Triple("rw_get_item", "shown", "ready"), Triple("rw_get_item", "busy", "idle")),
            port.opportunities,
        )
        assertTrue(outcomes.isEmpty())
    }

    @Test
    fun secondShowFails_firstShows_turnGoesOn() = runTest {
        port.ready["rw_get_item"] = true
        request()
        request()
        port.fireShowFailed(1)
        port.fireShowed()
        port.fireReward()

        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
    }

    @Test
    fun bothShowsFail_endsWithNoAd() = runTest {
        port.ready["rw_get_item"] = true
        request()
        request()
        port.fireShowFailed(1)
        assertTrue(outcomes.isEmpty())
        port.fireShowFailed(1)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
    }

    @Test
    fun secondShow_duplicateCallbacks_grantOnce() = runTest {
        port.ready["rw_get_item"] = true
        request()
        request()
        port.fireShowed()
        port.fireShowed()
        port.fireReward()
        port.fireReward()

        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item"), port.rewards)
    }

    @Test
    fun getItemAndExport_samePolicyEngine_differentPolicies() = runTest {
        val getItem = mutableListOf<RewardedOutcome>()
        val export = mutableListOf<RewardedOutcome>()
        request(rwGetItem) { getItem += it }
        request(rwExport) { export += it }
        request(rwGetItem) { getItem += it }
        request(rwExport) { export += it }

        assertEquals(listOf(false, false), getItem.map { (it as RewardedOutcome.NoAd).granted })
        assertEquals(listOf(false, true), export.map { (it as RewardedOutcome.NoAd).granted })
    }

    @Test
    fun loadIfNeeded_switchedOff_sendsNothing() = runTest {
        port.enabled["rw_get_item"] = false

        turns.loadIfNeeded(rwGetItem)

        assertTrue(port.sends.isEmpty())
        assertTrue(port.sendBlocked.isEmpty())
        assertEquals(listOf("rw_get_item"), port.cleared)
    }

    @Test
    fun loadIfNeeded_gateClosed_reportsSendBlocked_noSend() = runTest {
        port.mayRequest = false

        turns.loadIfNeeded(rwGetItem)

        assertTrue(port.sends.isEmpty())
        assertEquals(listOf("rw_get_item"), port.sendBlocked)
    }

    @Test
    fun loadIfNeeded_open_sendsOnce() = runTest {
        turns.loadIfNeeded(rwGetItem)
        assertEquals(listOf("rw_get_item"), port.sends)

        // Loading now: no second send.
        assertEquals("loading", port.loadState["rw_get_item"])
        turns.loadIfNeeded(rwGetItem)
        assertEquals(listOf("rw_get_item"), port.sends)
    }

    @Test
    fun presenceRaisedByOther_rewardedStillShows_doesNotLowerIt() = runTest {
        // An interstitial shows (R-06): the rewarded ad is shown anyway.
        port.presenceFree = false
        port.ready["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item"), port.shows)

        port.fireShowed()
        port.fireDismissed()
        assertEquals(0, port.presenceLowered)
    }

    @Test
    fun presenceRaisedByRewarded_loweredOnDismiss() = runTest {
        port.ready["rw_get_item"] = true
        request()
        assertEquals(1, port.presenceRaised)
        assertEquals(0, port.presenceLowered)

        port.fireShowed()
        port.fireDismissed()
        assertEquals(1, port.presenceLowered)
    }

    @Test
    fun stuck_endsPendingTurnsWithoutOutcome() = runTest {
        port.ready["rw_get_item"] = true
        request()
        port.fireShowed()

        turns.onStuck()
        assertEquals(listOf(RewardedOutcome.OnScreen), outcomes)
        assertEquals(1, port.presenceLowered)

        port.ready["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item", "rw_get_item"), port.shows)
        assertTrue(port.showAgains.isEmpty())
        assertFalse(port.opportunities.any { it.second == "busy" })
    }

    @Test
    fun twoCallersSamePlacement_bothGetOnScreen_grantOnlyLatest() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.ready["rw_get_item"] = true
        request { a += it }
        request { b += it }

        port.fireShowed()
        assertEquals(listOf(RewardedOutcome.OnScreen), a)
        assertEquals(listOf(RewardedOutcome.OnScreen), b)

        port.fireReward()
        assertEquals(listOf(RewardedOutcome.OnScreen), a)
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), b)
    }

    @Test
    fun twoCallersSamePlacement_bothFail_onlyLatestMayBeGranted() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.ready["rw_allow"] = true
        request(rwAllow) { a += it }
        request(rwAllow) { b += it }

        port.fireShowFailed(1)
        port.fireShowFailed(1)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), b)
    }

    @Test
    fun twoPlacements_pendingTurnsAndNetworkFlagsApart() = runTest {
        port.ready["rw_get_item"] = true
        port.ready["rw_export"] = true
        request(rwGetItem)
        request(rwExport)

        assertEquals(listOf("rw_get_item", "rw_export"), port.shows)
        assertFalse(port.opportunities.any { it.second == "busy" })

        turns.onLoadFailed(rwExport, 2, offline = false)
        val other = mutableListOf<RewardedOutcome>()
        port.fireDismissed(rwGetItem)
        request(rwGetItem) { other += it }
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), other)
        // Control: the placement that failed reports the network.
        port.fireDismissed(rwExport)
        request(rwExport) { other += it }
        assertEquals(RewardedOutcome.NoAd(network = true, granted = false), other.last())
    }

    @Test
    fun decisions_areLogged_withoutUnitId() = runTest {
        // shown, busy, reward, closed without reward.
        port.ready["rw_get_item"] = true
        request()
        request()
        port.fireShowed()
        port.fireReward()
        port.fireDismissed()
        port.ready["rw_get_item"] = true
        request()
        port.fireShowed()
        port.fireDismissed()
        // not_ready, then network.
        request()
        port.offline = true
        request()
        port.offline = false
        // Load rounds: switched off, consent gate closed.
        port.loadState["rw_export"] = "idle"
        port.enabled["rw_export"] = false
        turns.loadIfNeeded(rwExport)
        port.enabled["rw_export"] = true
        port.mayRequest = false
        turns.loadIfNeeded(rwExport)

        val lines = logger.entries.filter { it.tag == "Ads" }.map { it.message }
        listOf(
            "Rewarded rw_get_item shown",
            "Rewarded rw_get_item skipped: not_ready",
            "Rewarded rw_get_item skipped: network",
            "Rewarded rw_get_item busy: show again",
            "Rewarded rw_get_item reward",
            "Rewarded rw_get_item closed without reward",
            "Rewarded rw_export not requested: switched off",
            "Rewarded rw_export not requested: consent gate closed",
        ).forEach { expected -> assertTrue("$expected in $lines", lines.any { it.startsWith(expected) }) }
        assertFalse(logger.entries.any { it.message.contains(FakeRewardedPort.UNIT_ID) })
    }

    /** Two taps of [declaration] waiting for the switch at the same time, then both gates open with [on]. */
    private fun TestScope.doubleTapAwaitingSwitch(
        declaration: AdPlacementDeclaration,
        on: Boolean = true,
        first: (RewardedOutcome) -> Unit = collect,
        second: (RewardedOutcome) -> Unit = collect,
    ) {
        val gates = List(2) { CompletableDeferred<Boolean>() }
        port.switchGates += gates
        // Three gates, taken in order by whatever reads the switch: the first tap, then a second tap that reads it
        // itself (none when it joins the decision, plan T-042), then the load round of a decision with no ad (F-07).
        // So after a decision with no ad one gate is left when the second tap joined, none when it read the switch;
        // with an ad two are left.
        port.switchGates += CompletableDeferred(on)
        launch { request(declaration, first) }
        launch { request(declaration, second) }
        runCurrent()
        gates.forEach { it.complete(on) }
        runCurrent()
    }

    @Test
    fun doubleTapNoAd_allow_grantsOnce_secondTapBusy() = runTest {
        doubleTapAwaitingSwitch(rwAllow)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
        assertEquals(
            listOf(Triple("rw_allow", "not_ready", "idle"), Triple("rw_allow", "busy", "loading")),
            port.opportunities,
        )
        assertEquals(listOf("rw_allow"), port.loads)
        // The second tap joined the first: it never waited for the switch itself.
        assertEquals(1, port.switchGates.size)
    }

    @Test
    fun doubleTapNoAd_allowAfter2_countsOneTry() = runTest {
        doubleTapAwaitingSwitch(rwExport)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(1, port.switchGates.size)

        // Control (L-055): the next opening is the second try in a row, so the counter was kept, not dropped.
        port.switchGates.clear()
        request(rwExport)
        assertEquals(listOf(false, true), outcomes.map { (it as RewardedOutcome.NoAd).granted })
    }

    @Test
    fun doubleTapNoAd_block_oneOutcome_userMayTryAgain() = runTest {
        doubleTapAwaitingSwitch(rwGetItem)

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(1, port.opportunities.count { it.second == "not_ready" })
        assertEquals(1, port.opportunities.count { it.second == "busy" })
        assertEquals(1, port.switchGates.size)

        // The user may try again.
        port.switchGates.clear()
        port.ready["rw_get_item"] = true
        request()
        assertEquals(listOf("rw_get_item"), port.shows)
    }

    @Test
    fun secondTapWhileLoading_joinsDecision() = runTest {
        val load = CompletableDeferred<Unit>()
        port.loadGates += load
        launch { request(rwAllow) }
        runCurrent()
        assertEquals(listOf(Triple("rw_allow", "not_ready", "idle")), port.opportunities)

        launch { request(rwAllow) }
        runCurrent()
        assertTrue(outcomes.isEmpty())
        assertEquals(listOf("rw_allow"), port.loads)

        load.complete(Unit)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
        assertEquals(listOf("not_ready", "busy"), port.opportunities.map { it.second })
        assertEquals(listOf("rw_allow"), port.loads)
    }

    @Test
    fun doubleTapAwaitingSwitch_off_grantsOnce_noOpportunity() = runTest {
        val first = CompletableDeferred<Boolean>()
        port.switchGates += first
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        launch { request() }
        runCurrent()

        first.complete(false)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        assertEquals(1, port.switchGates.size)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.shows.isEmpty())
    }

    @Test
    fun twoCallersOverlapNoAd_allow_onlyLatestGranted() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()

        doubleTapAwaitingSwitch(rwAllow, first = { a += it }, second = { b += it })

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), b)
    }

    @Test
    fun doubleTapWithAdAwaitingSwitch_showsAgainBusy_grantOnce() = runTest {
        port.ready["rw_get_item"] = true
        doubleTapAwaitingSwitch(rwGetItem)

        assertEquals(listOf("rw_get_item"), port.shows)
        assertEquals(listOf("rw_get_item"), port.showAgains)
        assertEquals(
            listOf(Triple("rw_get_item", "shown", "ready"), Triple("rw_get_item", "busy", "idle")),
            port.opportunities,
        )
        port.fireShowed()
        port.fireReward()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
    }

    @Test
    fun showFailsInsideTheCall_endsTurnOnce() = runTest {
        port.ready["rw_get_item"] = true
        port.failShowAtOnce = 0

        request()

        assertEquals(listOf("rw_get_item"), port.shows)
        assertEquals(1, outcomes.size)
        assertTrue(outcomes.single() is RewardedOutcome.NoAd)
        // Control: a show that does not fail keeps the turn waiting for its ad.
        port.failShowAtOnce = null
        port.ready["rw_get_item"] = true
        request()
        assertEquals(1, outcomes.size)
        assertEquals(listOf("rw_get_item", "rw_get_item"), port.shows)
    }

    @Test
    fun doubleTap_showFailsInsideTheCall_latestGetsPolicy_joinedIsBusy() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.ready["rw_allow"] = true
        port.failShowAtOnce = 0

        doubleTapAwaitingSwitch(rwAllow, first = { a += it }, second = { b += it })

        // Like a decision with no ad: the latest caller gets the policy, the other closes blocked.
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), b)
        assertEquals(
            listOf(Triple("rw_allow", "shown", "ready"), Triple("rw_allow", "busy", "idle")),
            port.opportunities,
        )
        assertTrue(port.showAgains.isEmpty())
        // Control (L-055): a show that does not fail keeps both taps on the turn, the latest rewarded.
        a.clear()
        b.clear()
        port.opportunities.clear()
        port.switchGates.clear()
        port.failShowAtOnce = null
        port.ready["rw_allow"] = true
        val joinsBefore = logger.entries.count { it.message.endsWith("tap joins the one being decided") }
        doubleTapAwaitingSwitch(rwAllow, first = { a += it }, second = { b += it })
        // The second tap of the control joins the decision too, like the name of the test says.
        assertEquals(joinsBefore + 1, logger.entries.count { it.message.endsWith("tap joins the one being decided") })
        // It never read the switch itself, and with an ad no load round takes a gate.
        assertEquals(2, port.switchGates.size)
        port.fireReward()
        assertTrue(a.isEmpty())
        assertEquals(listOf(RewardedOutcome.Granted), b)
    }

    @Test
    fun decisionsOfTwoPlacementsApart() = runTest {
        val allow = mutableListOf<RewardedOutcome>()
        val export = mutableListOf<RewardedOutcome>()
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        launch { request(rwAllow) { allow += it } }
        runCurrent()

        launch { request(rwExport) { export += it } }
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), export)

        gate.complete(true)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), allow)
        assertEquals(
            listOf(Triple("rw_export", "not_ready", "idle"), Triple("rw_allow", "not_ready", "idle")),
            port.opportunities,
        )
        assertFalse(port.opportunities.any { it.second == "busy" })
    }

    /** Warn lines of the rewarded turns that contain [text]. */
    private fun warnings(text: String): Int =
        logger.entries.count { it.level == LogLevel.WARN && it.tag == "Ads" && it.message.contains(text) }

    @Test
    fun decisionStuckAtSwitch_endsAfterWait_noAdPolicy_joinedBusy() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        val other = mutableListOf<RewardedOutcome>()
        port.ready["rw_get_item"] = true
        // The switch never answers (plan T-047): the decision ends after RewardedTurns.DECISION_WAIT_MILLIS.
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request { a += it } }
        runCurrent()
        launch { request { b += it } }
        runCurrent()
        // Control (L-068): another placement is decided at once meanwhile.
        request(rwAllow) { other += it }
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), other)

        advanceTimeBy(4_999)
        runCurrent()
        assertTrue(a.isEmpty())
        assertTrue(b.isEmpty())

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), b)
        // Nothing is shown after the wait, and no load round is asked for the placement that timed out.
        assertTrue(port.shows.isEmpty())
        assertTrue("rw_get_item" !in port.loads)
        assertEquals(
            listOf(Triple("rw_get_item", "not_ready", "ready"), Triple("rw_get_item", "busy", "ready")),
            port.opportunities.filter { it.first == "rw_get_item" },
        )
        assertEquals(1, warnings("not done in time"))

        // Control (L-068): the timeout of rw_get_item changes nothing for rw_allow.
        request(rwAllow) { other += it }
        assertEquals(RewardedOutcome.NoAd(network = false, granted = true), other.last())
        assertEquals(2, other.size)
    }

    @Test
    fun decisionStuckAtSwitch_noAd_endsAfterWait() = runTest {
        // One wait for the whole decision: the switch after 3 s leaves 2 s for the load, never 5 s each.
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        port.loadGates += CompletableDeferred<Unit>()
        launch { request() }
        runCurrent()

        advanceTimeBy(3_000)
        gate.complete(true)
        runCurrent()
        advanceTimeBy(1_999)
        runCurrent()
        assertTrue(outcomes.isEmpty())

        advanceTimeBy(2)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(listOf("rw_get_item"), port.loads)
        assertEquals(listOf(Triple("rw_get_item", "not_ready", "idle")), port.opportunities)
    }

    @Test
    fun decisionStuckAtSwitch_offline_reportsNetwork() = runTest {
        port.offline = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = true, granted = false)), outcomes)

        // Control (L-055): online, the same timeout is no network failure.
        port.offline = false
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(2, outcomes.size)
        assertEquals(RewardedOutcome.NoAd(network = false, granted = false), outcomes.last())
    }

    @Test
    fun afterStuckDecision_nextTapShowsAd() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        val c = mutableListOf<RewardedOutcome>()
        port.ready["rw_get_item"] = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request { a += it } }
        runCurrent()
        launch { request { b += it } }
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()

        // A new decision: the switch answers, and the ad the timed-out decision never took out is shown.
        request { c += it }
        assertEquals(listOf("rw_get_item"), port.shows)
        assertTrue(port.showAgains.isEmpty())
        port.fireShowed()
        port.fireReward()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), c)

        // Control (L-055): the taps of the timed-out decision get nothing more.
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), b)
    }

    @Test
    fun decisionStuckAtLoad_endsAfterWait_policyOnce() = runTest {
        val first = mutableListOf<RewardedOutcome>()
        val second = mutableListOf<RewardedOutcome>()
        port.loadGates += CompletableDeferred<Unit>()
        launch { request(rwAllow) { first += it } }
        launch { request(rwAllow) { second += it } }
        runCurrent()

        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), first)
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), second)
        assertEquals(
            listOf(Triple("rw_allow", "not_ready", "idle"), Triple("rw_allow", "busy", "idle")),
            port.opportunities,
        )
        assertEquals(1, warnings("not done in time"))

        // Control: the load is no longer stuck, the next tap has its outcome at once.
        val third = mutableListOf<RewardedOutcome>()
        request(rwAllow) { third += it }
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), third)
    }

    @Test
    fun loadRound_switchStuck_releasesAfterWait() = runTest {
        port.switchGates += CompletableDeferred<Boolean>()
        launch { turns.loadIfNeeded(rwGetItem) }
        runCurrent()
        // The round still waiting holds the placement.
        turns.loadIfNeeded(rwGetItem)
        assertTrue(port.sends.isEmpty())

        advanceTimeBy(5_001)
        runCurrent()
        // The round that timed out reads no step (plan T-047): nothing cleared, nothing blocked.
        assertTrue(port.cleared.isEmpty())
        assertTrue(port.sendBlocked.isEmpty())
        turns.loadIfNeeded(rwGetItem)
        assertEquals(listOf("rw_get_item"), port.sends)
        assertEquals(1, warnings("load round: switch not read in time"))
    }

    @Test
    fun stuckDuringDecision_decisionStillEndsAfterWait() = runTest {
        // Question 2 of plan T-047: onStuck() does not end a running decision; it ends after its own wait.
        port.ready["rw_get_item"] = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        runCurrent()

        turns.onStuck()
        runCurrent()
        assertTrue(outcomes.isEmpty())
        assertEquals(0, port.presenceLowered)

        advanceTimeBy(5_001)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
    }

    @Test
    fun syncShowedAndReward_oneCaller_onScreenThenGrantOnce() = runTest {
        port.syncShowedReward = true
        port.ready["rw_get_item"] = true

        request()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item"), port.rewards)

        // Callbacks the SDK repeats later change nothing.
        port.fireShowed()
        port.fireReward()
        port.fireDismissed()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item"), port.rewards)
    }

    @Test
    fun syncShowedAndReward_joinedTap_latestGranted() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.syncShowedReward = true
        port.ready["rw_get_item"] = true
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        launch { request { a += it } }
        launch { request { b += it } }
        runCurrent()
        gate.complete(true)
        runCurrent()

        // Like a turn the SDK reports later: every caller's dialog closes, the latest caller is rewarded.
        assertEquals(listOf(RewardedOutcome.OnScreen), a)
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), b)
        assertEquals(listOf("rw_get_item"), port.rewards)
        assertEquals(listOf("rw_get_item"), port.showAgains)

        // One handler for both taps, like a RewardedAction tapped twice: each outcome once, in order.
        port.fireDismissed()
        port.ready["rw_get_item"] = true
        val next = CompletableDeferred<Boolean>()
        port.switchGates += next
        launch { request() }
        launch { request() }
        runCurrent()
        next.complete(true)
        runCurrent()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
    }

    @Test
    fun syncShowedRewardDismissed_grantOnce_nextTapShowsAgain() = runTest {
        // Guard (plan T-047): a turn that ends inside the show call keeps its outcome.
        port.syncShowedReward = true
        port.syncDismiss = true
        port.ready["rw_get_item"] = true

        request()
        assertEquals(1, outcomes.count { it == RewardedOutcome.Granted })
        assertEquals(listOf("rw_get_item"), port.rewards)

        port.syncShowedReward = false
        port.ready["rw_get_item"] = true
        request()
        assertEquals(2, port.shows.size)
        assertTrue(port.showAgains.isEmpty())
    }

    @Test
    fun syncShowed_oneHandlerThreeTaps_onScreenOnce() = runTest {
        // Plan T-048: one handler that joined one decision three times gets OnScreen once.
        port.syncShowedReward = true
        port.ready["rw_get_item"] = true
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        repeat(3) { launch { request() } }
        runCurrent()
        gate.complete(true)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), outcomes)
        assertEquals(listOf("rw_get_item", "rw_get_item"), port.showAgains)
        // R-06 is kept: every tap that joined is still `busy`.
        assertEquals(listOf("shown", "busy", "busy"), port.opportunities.map { it.second })
        assertEquals(listOf("rw_get_item"), port.rewards)
    }

    @Test
    fun syncShowed_handlerJoinsTwice_onScreenOnce() = runTest {
        // L-068: two handlers, the second joins twice.
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        val hb: (RewardedOutcome) -> Unit = { b += it }
        port.syncShowedReward = true
        port.ready["rw_get_item"] = true
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        launch { request { a += it } }
        launch { request(to = hb) }
        launch { request(to = hb) }
        runCurrent()
        gate.complete(true)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.OnScreen), a)
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), b)
    }

    @Test
    fun syncShowed_firstHandlerJoinsAgain_onScreenOnce() = runTest {
        // Guard: the handler of the first tap joins again, so it gets OnScreen from showAgain and not once more.
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        val ha: (RewardedOutcome) -> Unit = { a += it }
        port.syncShowedReward = true
        port.ready["rw_get_item"] = true
        val gate = CompletableDeferred<Boolean>()
        port.switchGates += gate
        launch { request(to = ha) }
        launch { request { b += it } }
        launch { request(to = ha) }
        runCurrent()
        gate.complete(true)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), a)
        assertEquals(listOf(RewardedOutcome.OnScreen), b)
    }

    @Test
    fun tapAfterOnScreen_sameHandler_onScreenAgain() = runTest {
        // Guard (plan T-048, option (c)): a tap after the ad covered the screen gets OnScreen again at once, so the
        // dialog of a turn still hanging closes.
        port.ready["rw_get_item"] = true
        request()
        port.fireShowed()
        request()
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.OnScreen), outcomes)

        port.fireReward()
        assertEquals(
            listOf(RewardedOutcome.OnScreen, RewardedOutcome.OnScreen, RewardedOutcome.Granted),
            outcomes,
        )
        assertEquals(listOf("rw_get_item"), port.showAgains)
        assertEquals(
            listOf(Triple("rw_get_item", "shown", "ready"), Triple("rw_get_item", "busy", "idle")),
            port.opportunities,
        )
    }

    @Test
    fun tapAfterOnScreen_otherHandler_onScreenAtOnce() = runTest {
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.ready["rw_get_item"] = true
        request { a += it }
        port.fireShowed()
        request { b += it }
        assertEquals(listOf(RewardedOutcome.OnScreen), b)

        port.fireReward()
        assertEquals(listOf(RewardedOutcome.OnScreen), a)
        assertEquals(listOf(RewardedOutcome.OnScreen, RewardedOutcome.Granted), b)
    }

    @Test
    fun decisionStuckAtSwitch_consentClosed_grantsLikeClosedGate() = runTest {
        // MC-05, plan T-048: a closed gate runs the action whether the switch is on or off.
        val a = mutableListOf<RewardedOutcome>()
        val b = mutableListOf<RewardedOutcome>()
        port.consentOpen = false
        port.ready["rw_get_item"] = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request { a += it } }
        runCurrent()
        launch { request { b += it } }
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), a)
        assertEquals(listOf(RewardedOutcome.Granted), b)
        assertTrue(port.opportunities.isEmpty())
        assertTrue(port.loads.isEmpty())
        assertTrue(port.shows.isEmpty())
        assertEquals(1, warnings("not done in time"))
        assertEquals(
            1,
            logger.entries.count {
                it.tag == "Ads" && it.message.startsWith("Rewarded rw_get_item granted: consent gate closed")
            },
        )
    }

    @Test
    fun decisionStuckAtSwitch_consentClosed_countsNoTry() = runTest {
        port.consentOpen = false
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request(rwExport) }
        runCurrent()
        advanceTimeBy(5_001)
        runCurrent()

        port.consentOpen = true
        request(rwExport)
        // The timeout counted no try: this is the first try of allow_after_2.
        assertEquals(listOf(RewardedOutcome.Granted, RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
    }

    @Test
    fun decisionStuckAtLoad_consentClosedMeanwhile_staysNoAd() = runTest {
        // Guard: past the switch, `not_ready` is recorded while the gate was open; the T-047 path stays.
        port.loadGates += CompletableDeferred<Unit>()
        launch { request(rwAllow) }
        runCurrent()
        port.consentOpen = false
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = true)), outcomes)
        assertEquals(listOf(Triple("rw_allow", "not_ready", "idle")), port.opportunities)
    }

    @Test
    fun decisionStuckAtSwitch_consentClosesMeanwhile_grants() = runTest {
        // L-072: the gate is read when the wait ends, not at the tap.
        port.ready["rw_get_item"] = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        runCurrent()
        port.consentOpen = false
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.Granted), outcomes)
        assertTrue(port.opportunities.isEmpty())
    }

    @Test
    fun decisionStuckAtSwitch_consentOpensMeanwhile_staysNoAd() = runTest {
        port.consentOpen = false
        port.ready["rw_get_item"] = true
        port.switchGates += CompletableDeferred<Boolean>()
        launch { request() }
        runCurrent()
        port.consentOpen = true
        advanceTimeBy(5_001)
        runCurrent()

        assertEquals(listOf(RewardedOutcome.NoAd(network = false, granted = false)), outcomes)
        assertEquals(listOf(Triple("rw_get_item", "not_ready", "ready")), port.opportunities)
    }

    /**
     * The port of [RewardedTurns] without the SDK: switches and states per placement code, every call recorded.
     * [ready] turns false once the ad is taken out to be shown, like the cache of [AdsController].
     */
    private class FakeRewardedPort : RewardedPort {
        lateinit var turns: RewardedTurns
        var consentOpen = true
        var mayRequest = true
        var offline = false
        var presenceFree = true
        val enabled = mutableMapOf<String, Boolean>()
        val ready = mutableMapOf<String, Boolean>()
        val loadState = mutableMapOf<String, String>()

        /** When not empty, each call of [enabled] waits for the next of these instead of answering at once. */
        val switchGates = ArrayDeque<CompletableDeferred<Boolean>>()

        /** When not empty, each call of [loadIfNeeded] waits for the next of these before the load round. */
        val loadGates = ArrayDeque<CompletableDeferred<Unit>>()

        val sends = mutableListOf<String>()
        val sendBlocked = mutableListOf<String>()
        val cleared = mutableListOf<String>()
        val shows = mutableListOf<String>()
        val showAgains = mutableListOf<String>()
        val loads = mutableListOf<String>()
        val opportunities = mutableListOf<Triple<String, String, String>>()
        val rewards = mutableListOf<String>()
        var failShowAtOnce: Int? = null

        /** The SDK reports "on screen" and the reward inside the show call (a mediation SDK may, plan T-047). */
        var syncShowedReward = false

        /** With [syncShowedReward], the SDK also reports the close inside the show call. */
        var syncDismiss = false
        var presenceRaised = 0
        var presenceLowered = 0
        private var lastShown: AdPlacementDeclaration? = null

        fun fireShowed(declaration: AdPlacementDeclaration = lastShown!!) = turns.onShowed(declaration)

        fun fireReward(declaration: AdPlacementDeclaration = lastShown!!) = turns.onReward(declaration)

        fun fireDismissed(declaration: AdPlacementDeclaration = lastShown!!) = turns.onDismissed(declaration)

        fun fireShowFailed(code: Int, declaration: AdPlacementDeclaration = lastShown!!) =
            turns.onShowFailed(declaration, code)

        override suspend fun enabled(declaration: AdPlacementDeclaration): Boolean =
            switchGates.removeFirstOrNull()?.await() ?: enabled[declaration.code] ?: true

        override fun consentOpen(): Boolean = consentOpen

        override fun mayRequestNow(): Boolean = mayRequest

        override fun offline(): Boolean = offline

        override fun ready(declaration: AdPlacementDeclaration): Boolean = ready[declaration.code] == true

        override fun needsLoad(declaration: AdPlacementDeclaration): Boolean =
            !ready(declaration) && loadState(declaration) != "loading"

        override fun loadState(declaration: AdPlacementDeclaration): String = when {
            ready(declaration) -> "ready"
            else -> loadState[declaration.code] ?: "idle"
        }

        override suspend fun loadIfNeeded(declaration: AdPlacementDeclaration) {
            loads += declaration.code
            loadGates.removeFirstOrNull()?.await()
            turns.loadIfNeeded(declaration)
        }

        override fun clearLoading(declaration: AdPlacementDeclaration) {
            cleared += declaration.code
            loadState[declaration.code] = "idle"
        }

        override fun send(declaration: AdPlacementDeclaration) {
            sends += declaration.code
            loadState[declaration.code] = "loading"
        }

        override fun sendBlocked(declaration: AdPlacementDeclaration) {
            sendBlocked += declaration.code
        }

        override fun take(declaration: AdPlacementDeclaration): Boolean {
            if (!ready(declaration)) return false
            ready[declaration.code] = false
            return true
        }

        override fun show(declaration: AdPlacementDeclaration, activity: Activity?) {
            lastShown = declaration
            shows += declaration.code
            // The show fails inside the call, as AdsController does with no ad or no activity.
            failShowAtOnce?.let { turns.onShowFailed(declaration, it) }
            if (syncShowedReward) {
                turns.onShowed(declaration)
                turns.onReward(declaration)
                if (syncDismiss) turns.onDismissed(declaration)
            }
        }

        override fun showAgain(declaration: AdPlacementDeclaration, activity: Activity?) {
            lastShown = declaration
            showAgains += declaration.code
        }

        override fun raisePresence(declaration: AdPlacementDeclaration, activity: Activity?): Boolean {
            if (!presenceFree) return false
            presenceRaised++
            return true
        }

        override fun lowerPresence() {
            presenceLowered++
        }

        override fun recordOpportunity(declaration: AdPlacementDeclaration, result: String, loadState: String) {
            opportunities += Triple(declaration.code, result, loadState)
        }

        override fun recordReward(declaration: AdPlacementDeclaration) {
            rewards += declaration.code
        }

        companion object {
            const val UNIT_ID = "ca-app-pub-0000000000000000/1111111111"
        }
    }

    private companion object {
        val rwGetItem = AdPlacementDeclaration("rw_get_item", AdFormat.REWARDED)
        val rwExport = AdPlacementDeclaration(
            "rw_export",
            AdFormat.REWARDED,
            noAdPolicy = RewardedNoAdPolicy.AllowAfter(2),
        )
        val rwAllow = AdPlacementDeclaration("rw_allow", AdFormat.REWARDED, noAdPolicy = RewardedNoAdPolicy.Allow)
    }
}
