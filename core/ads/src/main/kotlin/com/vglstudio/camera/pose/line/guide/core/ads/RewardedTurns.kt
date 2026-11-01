package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import kotlinx.coroutines.withTimeoutOrNull

/**
 * What one caller of a rewarded turn gets (R-02 to R-04): [OnScreen] when the ad covers the screen (the dialog
 * closes), [Granted] when the SDK reports the reward (the action runs), [NoAd] when no ad could be shown, with whether
 * it was the network ([NoAd.network], R-03) and whether the policy of the placement lets the action run
 * ([NoAd.granted], MC-05). Closing the ad before the reward gives nothing (R-04).
 */
internal sealed interface RewardedOutcome {
    data object OnScreen : RewardedOutcome

    data object Granted : RewardedOutcome

    data class NoAd(val network: Boolean, val granted: Boolean) : RewardedOutcome
}

/** What [RewardedTurns] asks [AdsController] for; the controller ties it to the SDK. Main thread only. */
internal interface RewardedPort {
    /** `is_show_ads` and `is_show_<code>` (KB-19, criterion 2); waits for the first value. */
    suspend fun enabled(declaration: AdPlacementDeclaration): Boolean

    /** The consent gate is open and the SDK started (S-11). */
    fun consentOpen(): Boolean

    /** A request may be sent right now (S-08, S-15). */
    fun mayRequestNow(): Boolean

    /** No usable Internet now. */
    fun offline(): Boolean

    /** A fresh ad is loaded for [declaration]. */
    fun ready(declaration: AdPlacementDeclaration): Boolean

    /** No ad is loaded, loading or waiting for a retry that is not due (F-04). */
    fun needsLoad(declaration: AdPlacementDeclaration): Boolean

    /** The `load_state` of an opportunity, taken at the decision (section 8.2). */
    fun loadState(declaration: AdPlacementDeclaration): String

    /** "Load if needed" of the controller (F-07), which comes back to [RewardedTurns.loadIfNeeded]. */
    suspend fun loadIfNeeded(declaration: AdPlacementDeclaration)

    /** A round that sends nothing because the placement is off: the slot is no longer loading. */
    fun clearLoading(declaration: AdPlacementDeclaration)

    /** Sends one load request now. */
    fun send(declaration: AdPlacementDeclaration)

    /** The consent gate stopped a round (S-15): nothing sent, no retry. */
    fun sendBlocked(declaration: AdPlacementDeclaration)

    /** Takes the loaded ad out of the slot for this turn; false when there is none. */
    fun take(declaration: AdPlacementDeclaration): Boolean

    /** Shows the ad taken for this turn on [activity]. */
    fun show(declaration: AdPlacementDeclaration, activity: Activity?)

    /** Shows the same ad again: the second tap before it covers the screen (R-06). */
    fun showAgain(declaration: AdPlacementDeclaration, activity: Activity?)

    /** Raises the flag "a full-screen ad shows" on [activity]; false when another full-screen ad holds it. */
    fun raisePresence(declaration: AdPlacementDeclaration, activity: Activity?): Boolean

    fun lowerPresence()

    fun recordOpportunity(declaration: AdPlacementDeclaration, result: String, loadState: String)

    fun recordReward(declaration: AdPlacementDeclaration)

    /**
     * Whether the ad may be shown on [activity] now (F-08, KB-26, criterion 3): [AdsController.isHostUsable]. Asked
     * before the ad is taken out of its slot and before every show again (R-06).
     */
    fun hostUsable(activity: Activity?): Boolean

    /** Sends one `ad_show_fail` with [code] only (section 8.2); no other effect. */
    fun recordShowFailed(declaration: AdPlacementDeclaration, code: Int)
}

/**
 * The rewarded turns and load rounds (docs/ops/ads-business-rules.md, MC-05, R-01 to R-06, F-05 to F-07; plan
 * T-031), kept per placement declaration so two placements never mix (L-068). Main thread only.
 *
 * - [loadIfNeeded] is the only place that decides whether a rewarded load is sent ([rewardedLoadStep], L-072); the
 *   preload plan, the retries (F-05), the reloads (F-06) and "load if needed" (F-07) all come here.
 * - [request] is one tap on "Watch ad": a turn still waiting for its ad shows it again and records `busy` (R-06),
 *   even when the key went off since (L-061); a tap while a decision of the placement still runs joins it, so one
 *   decision serves every tap of the placement until it has its outcome (plan T-042); else a switched-off placement
 *   or a closed consent gate runs the action at once (question 8 of the plan, option (a));
 *   no ad gives [RewardedOutcome.NoAd] by the policy of the placement; else the ad is shown.
 * - The SDK callbacks ([onShowed], [onReward], [onDismissed], [onShowFailed]) end the turn. Every outcome is given at
 *   most once per turn and caller, so callbacks the SDK repeats for the second show run nothing twice (section 3.4);
 *   the one exception is [RewardedOutcome.OnScreen], given again at once to a tap that comes after the ad covered the
 *   screen, so its dialog closes (plan T-048).
 * - It never reads nor moves the `ads_interval` mark (R-05) and has no `FullScreenAdGuard`.
 */
internal class RewardedTurns(private val port: RewardedPort, private val logger: AppLogger) {
    /**
     * One turn waiting for its ad to close: its callers, the latest last, and what the SDK reported so far; [policy]
     * applies when every show of it fails.
     */
    private class Turn(val raisedPresence: Boolean, val policy: RewardedNoAdPolicy) {
        val callers = mutableListOf<(RewardedOutcome) -> Unit>()
        var shows = 1
        var showFailures = 0
        var onScreen = false
        var rewarded = false
    }

    private val turns = mutableMapOf<AdPlacementDeclaration, Turn>()

    /** One tap on "Watch ad": where to show the ad and who gets the outcome. */
    private class Tap(val activity: Activity?, val onOutcome: (RewardedOutcome) -> Unit)

    /** A decision of a placement still running: the tap that started it and the taps that joined it since. */
    private class Deciding(val first: Tap) {
        val followers = mutableListOf<Tap>()
    }

    /** Decisions between a first tap and its outcome, per placement like [turns] (L-068). */
    private val decisions = mutableMapOf<AdPlacementDeclaration, Deciding>()
    private val noAdInARow = mutableMapOf<AdPlacementDeclaration, Int>()

    /** The policy of the last request of each placement; a request with another one counts its tries from 0. */
    private val lastPolicy = mutableMapOf<AdPlacementDeclaration, RewardedNoAdPolicy>()
    private val lastLoadNetworkError = mutableMapOf<AdPlacementDeclaration, Boolean>()

    /** Rounds between their first check and their decision, so a second call meanwhile sends nothing more. */
    private val deciding = mutableSetOf<AdPlacementDeclaration>()

    /**
     * One load round of [declaration]; sends at most one request. The switch is read for at most
     * [DECISION_WAIT_MILLIS] (docs/ops/ads-business-rules.md, section 11; plan T-047): not read in time, the round
     * sends nothing and sets no retry, and the next round may try again.
     */
    suspend fun loadIfNeeded(declaration: AdPlacementDeclaration) {
        if (declaration in deciding || !port.needsLoad(declaration)) return
        deciding += declaration
        val step = try {
            val needsLoad = port.needsLoad(declaration)
            val enabled = withTimeoutOrNull(DECISION_WAIT_MILLIS) { port.enabled(declaration) }
            if (enabled == null) {
                logger.warn(TAG, "Rewarded ${declaration.code} load round: switch not read in time")
                RewardedLoadStep.NOT_NEEDED
            } else {
                rewardedLoadStep(needsLoad, enabled, port.mayRequestNow())
            }
        } finally {
            deciding -= declaration
        }
        when (step) {
            RewardedLoadStep.NOT_NEEDED -> Unit
            RewardedLoadStep.SWITCHED_OFF -> {
                logger.info(TAG, "Rewarded ${declaration.code} not requested: switched off")
                port.clearLoading(declaration)
            }
            RewardedLoadStep.GATE_CLOSED -> {
                logger.info(TAG, "Rewarded ${declaration.code} not requested: consent gate closed")
                port.sendBlocked(declaration)
            }
            RewardedLoadStep.SEND -> {
                // R-03: the network flag is cleared at the start of every load that is sent.
                lastLoadNetworkError -= declaration
                port.send(declaration)
            }
        }
    }

    /** A load of [declaration] failed with [code]; R-03 keeps whether it was the network until the next send. */
    fun onLoadFailed(declaration: AdPlacementDeclaration, code: Int, offline: Boolean) {
        lastLoadNetworkError[declaration] = isNetworkLoadError(code, offline)
    }

    /**
     * One tap on "Watch ad" of [declaration], shown on [activity]; [onOutcome] gets what happens (R-02 to R-06).
     * [policy] applies when no ad can be shown: the line's own by default, or the one picked on the staging ad gallery
     * ([rewardedPolicyOf], plan T-036). A request with another policy than the last one of [declaration] starts the
     * count of tries with no ad again, so `allow_after_<n>` counts from the start after the tester changed it.
     *
     * One decision for every tap of the placement until it has its outcome (plan T-042): a tap while a decision of
     * [declaration] still runs joins it, waits for nothing and counts no try of its own. The policy of the first tap
     * applies; the latest caller gets the outcome, the others a blocked [RewardedOutcome.NoAd] so their dialog closes.
     * A decision that shows the ad shows it again for every tap that joined, recorded as `busy` (R-06); one with no ad
     * records `busy` for every tap that joined.
     *
     * A decision waits (the switch, the load if needed) for at most [DECISION_WAIT_MILLIS] in all (section 11 of the
     * rules, plan T-047); not done in time, it ends like one with no ad, or like a closed consent gate when the switch
     * was still awaited and the gate is closed (plan T-048), and shows nothing, so the next tap is a new decision.
     */
    suspend fun request(
        declaration: AdPlacementDeclaration,
        activity: Activity?,
        onOutcome: (RewardedOutcome) -> Unit,
        policy: RewardedNoAdPolicy = declaration.effectiveNoAdPolicy,
    ) {
        val previousPolicy = lastPolicy.put(declaration, policy)
        if (previousPolicy != null && previousPolicy != policy) noAdInARow -= declaration
        // R-06 first (L-061): a turn still waiting for its ad keeps the second tap, even if the key went off since,
        // so the reward of that ad is the only outcome that runs the action.
        turns[declaration]?.let { pending ->
            showAgain(declaration, pending, activity, onOutcome)
            return
        }
        val tap = Tap(activity, onOutcome)
        decisions[declaration]?.let { running ->
            running.followers += tap
            logger.info(TAG, "Rewarded ${declaration.code} tap joins the one being decided")
            return
        }
        val decision = Deciding(tap)
        decisions[declaration] = decision
        try {
            decide(declaration, decision, policy)
        } finally {
            // Cancelled or failed: the taps that joined get nothing, like a turn ended by onStuck.
            if (decisions[declaration] === decision) decisions -= declaration
        }
    }

    /** What a decision does once its waits are over. */
    private enum class DecisionStep { SWITCHED_OFF, CONSENT_CLOSED, NO_AD, HOST_GONE, SHOW }

    /**
     * The decision of [decision]'s first tap. While it waits (the switch, the load if needed) every other tap of
     * [declaration] joins it, so no turn can start meanwhile. Every wait together lasts at most
     * [DECISION_WAIT_MILLIS]; past it the decision goes on as one with no ad (R-03), with the state read at that
     * moment, and asks for no further load; when the switch was still awaited and the consent gate is closed, it
     * grants like a closed gate (MC-05, plan T-048).
     */
    private suspend fun decide(declaration: AdPlacementDeclaration, decision: Deciding, policy: RewardedNoAdPolicy) {
        val code = declaration.code
        var notReadyRecorded = false
        // Whether no ad is the network's fault (R-03), once the reason was read.
        var networkRead: Boolean? = null
        var switchRead = false
        val waited = withTimeoutOrNull(DECISION_WAIT_MILLIS) {
            if (!port.enabled(declaration)) return@withTimeoutOrNull DecisionStep.SWITCHED_OFF
            switchRead = true
            val reason = noAdReason(
                consentOpen = port.consentOpen(),
                ready = port.ready(declaration),
                offline = port.offline(),
                lastLoadNetworkError = lastLoadNetworkError[declaration] == true,
            )
            networkRead = reason == RewardedNoAdReason.NETWORK
            when {
                reason == RewardedNoAdReason.CONSENT_CLOSED -> DecisionStep.CONSENT_CLOSED
                // F-08 (plan T-055): an ad is there but the activity is not usable; the ad stays in its slot.
                reason == null && !port.hostUsable(decision.first.activity) -> DecisionStep.HOST_GONE
                reason != null || !port.take(declaration) -> {
                    // No ad (R-03, R-04): `not_ready` with the state before the load, then a load (F-07).
                    port.recordOpportunity(declaration, NOT_READY, port.loadState(declaration))
                    notReadyRecorded = true
                    port.loadIfNeeded(declaration)
                    DecisionStep.NO_AD
                }
                // take() does not wait, so the wait cannot end once the ad is out of its slot.
                else -> DecisionStep.SHOW
            }
        }
        if (waited == null) logger.warn(TAG, "Rewarded $code decision not done in time")
        // MC-05: a closed consent gate runs the action whether the switch is on or off, so a timeout at the switch
        // needs no switch.
        val step = waited ?: if (!switchRead && !port.consentOpen()) DecisionStep.CONSENT_CLOSED else null
        when (step) {
            DecisionStep.SWITCHED_OFF -> {
                logger.info(TAG, "Rewarded $code granted: switched off")
                endDecision(declaration, decision, RewardedOutcome.Granted)
            }
            DecisionStep.CONSENT_CLOSED -> {
                // A closed consent gate counts as switched off; no opportunity. Reached when the gate was read closed,
                // and when the wait timed out at the switch while the gate was closed (MC-05, T-048).
                logger.info(TAG, "Rewarded $code granted: consent gate closed")
                endDecision(declaration, decision, RewardedOutcome.Granted)
            }
            DecisionStep.NO_AD, null -> {
                // Timed out while the switch was awaited and the consent gate was open (a closed gate ends above,
                // MC-05): both reads wait for nothing (KB-19, criterion 4).
                val network = networkRead ?: (port.offline() || lastLoadNetworkError[declaration] == true)
                if (!notReadyRecorded) port.recordOpportunity(declaration, NOT_READY, port.loadState(declaration))
                endWithNoAd(declaration, decision, policy, network)
            }
            DecisionStep.HOST_GONE -> endWithHostGone(declaration, decision, policy)
            DecisionStep.SHOW -> showDecided(declaration, decision, policy)
        }
    }

    /**
     * A decision with an ad whose activity is not usable (F-08, KB-26, criterion 3): `shown` then `ad_show_fail`
     * `app_not_foreground`, as when the SDK fails to show with that code; every tap that joined is `busy`; the outcome
     * is the policy, counted once, as for a show that fails (R-04, question 3 of plan T-055). The ad is not taken.
     */
    private fun endWithHostGone(declaration: AdPlacementDeclaration, decision: Deciding, policy: RewardedNoAdPolicy) {
        port.recordOpportunity(declaration, SHOWN, READY)
        port.recordShowFailed(declaration, SHOW_FAILED_APP_NOT_FOREGROUND)
        val granted = countNoAd(declaration, policy)
        logger.info(TAG, "Rewarded ${declaration.code} skipped: host not usable (granted $granted)")
        decision.followers.forEach { port.recordOpportunity(declaration, BUSY, port.loadState(declaration)) }
        endDecision(declaration, decision, RewardedOutcome.NoAd(network = false, granted = granted))
    }

    /**
     * A decision with no ad: the policy (MC-05), counted once for the decision; every tap that joined is `busy`
     * (section 8.2).
     */
    private fun endWithNoAd(
        declaration: AdPlacementDeclaration,
        decision: Deciding,
        policy: RewardedNoAdPolicy,
        network: Boolean,
    ) {
        val granted = countNoAd(declaration, policy)
        val label = if (network) "network" else NOT_READY
        logger.info(TAG, "Rewarded ${declaration.code} skipped: $label (granted $granted)")
        decision.followers.forEach { port.recordOpportunity(declaration, BUSY, port.loadState(declaration)) }
        endDecision(declaration, decision, RewardedOutcome.NoAd(network = network, granted = granted))
    }

    /**
     * Shows the ad taken out for [decision]. What the SDK reports inside the show call is held, then given like the
     * callbacks of a turn that come later (plan T-047): a show that fails inside the call ends like a decision with no
     * ad; otherwise every handler gets [RewardedOutcome.OnScreen] once for this decision, however many taps it joined
     * with, and the latest caller the reward.
     */
    private fun showDecided(declaration: AdPlacementDeclaration, decision: Deciding, policy: RewardedNoAdPolicy) {
        val first = decision.first
        val heldOutcomes = mutableListOf<RewardedOutcome>()
        val held: (RewardedOutcome) -> Unit = { heldOutcomes += it }
        show(declaration, first.activity, policy, held)
        val turn = turns[declaration]
        if (turn == null) {
            // Every tap that joined is `busy`, as with no ad (section 8.2).
            repeat(decision.followers.size) {
                port.recordOpportunity(declaration, BUSY, port.loadState(declaration))
            }
            val outcome = heldOutcomes.lastOrNull() ?: RewardedOutcome.NoAd(network = false, granted = false)
            endDecision(declaration, decision, outcome)
            return
        }
        decisions -= declaration
        turn.callers[turn.callers.indexOf(held)] = first.onOutcome
        // R-06: every tap that joined is a second tap of this turn; a handler that joined more than once gets
        // OnScreen once (plan T-048).
        val screened = mutableSetOf<(RewardedOutcome) -> Unit>()
        decision.followers.forEach {
            val give = it.onOutcome !in screened
            showAgain(declaration, turn, it.activity, it.onOutcome, give)
            if (turn.onScreen && give) screened += it.onOutcome
        }
        // A handler of a tap that joined already got OnScreen from showAgain.
        val firstJoinedToo = decision.followers.any { it.onOutcome === first.onOutcome }
        heldOutcomes.forEach { outcome ->
            when (outcome) {
                RewardedOutcome.OnScreen -> if (!firstJoinedToo) first.onOutcome(outcome)
                RewardedOutcome.Granted -> turn.callers.last()(outcome)
                is RewardedOutcome.NoAd -> first.onOutcome(outcome)
            }
        }
    }

    /**
     * Ends [decision] with [outcome] for its latest caller; the other callers get a blocked [RewardedOutcome.NoAd],
     * like [onShowFailed]. The decision is removed first, so a caller may tap again from its callback.
     */
    private fun endDecision(declaration: AdPlacementDeclaration, decision: Deciding, outcome: RewardedOutcome) {
        decisions -= declaration
        val callers = mutableListOf<(RewardedOutcome) -> Unit>()
        (listOf(decision.first) + decision.followers).forEach { tap ->
            callers -= tap.onOutcome
            callers += tap.onOutcome
        }
        val network = (outcome as? RewardedOutcome.NoAd)?.network ?: false
        callers.dropLast(1).forEach { it(RewardedOutcome.NoAd(network = network, granted = false)) }
        callers.last()(outcome)
    }

    /**
     * R-06: a second tap while the turn waits for its ad: the same ad is shown again, recorded as `busy`.
     * [giveOnScreen] is false for a handler that already got [RewardedOutcome.OnScreen] in the same decision.
     *
     * The activity is checked first (F-08, plan T-055), before the callers change: the reward goes to the last
     * caller, so a caller whose activity is not usable must never become it. Such a tap is `busy` with an
     * `ad_show_fail` `app_not_foreground`, adds no show (so a failure of the first show still ends the turn), and its
     * caller, when not already waiting for the turn, gets a blocked [RewardedOutcome.NoAd] at once so its dialog
     * closes; the turn goes on.
     */
    private fun showAgain(
        declaration: AdPlacementDeclaration,
        turn: Turn,
        activity: Activity?,
        onOutcome: (RewardedOutcome) -> Unit,
        giveOnScreen: Boolean = true,
    ) {
        if (!port.hostUsable(activity)) {
            port.recordOpportunity(declaration, BUSY, port.loadState(declaration))
            port.recordShowFailed(declaration, SHOW_FAILED_APP_NOT_FOREGROUND)
            logger.info(TAG, "Rewarded ${declaration.code} busy: host not usable, not shown again")
            if (onOutcome !in turn.callers) onOutcome(RewardedOutcome.NoAd(network = false, granted = false))
            return
        }
        if (onOutcome in turn.callers) turn.callers -= onOutcome
        turn.callers += onOutcome
        turn.shows++
        port.recordOpportunity(declaration, BUSY, port.loadState(declaration))
        logger.info(TAG, "Rewarded ${declaration.code} busy: show again")
        // The ad already covers the screen: this caller's dialog closes at once.
        if (turn.onScreen && giveOnScreen) onOutcome(RewardedOutcome.OnScreen)
        port.showAgain(declaration, activity)
    }

    private fun show(
        declaration: AdPlacementDeclaration,
        activity: Activity?,
        policy: RewardedNoAdPolicy,
        onOutcome: (RewardedOutcome) -> Unit,
    ) {
        port.recordOpportunity(declaration, SHOWN, READY)
        // R-06: shown even when another full-screen ad holds the flag; then only that ad lowers it.
        val turn = Turn(raisedPresence = port.raisePresence(declaration, activity), policy = policy)
        turn.callers += onOutcome
        turns[declaration] = turn
        logger.info(TAG, "Rewarded ${declaration.code} shown")
        port.show(declaration, activity)
    }

    /** The ad covers the screen: every waiting dialog closes, and the tries with no ad start again (question 3). */
    fun onShowed(declaration: AdPlacementDeclaration) {
        val turn = turns[declaration] ?: return
        if (turn.onScreen) return
        turn.onScreen = true
        noAdInARow -= declaration
        turn.callers.toList().forEach { it(RewardedOutcome.OnScreen) }
    }

    /** R-02: the reward, at the SDK callback, once per turn, to the latest caller only (question 9). */
    fun onReward(declaration: AdPlacementDeclaration) {
        val turn = turns[declaration] ?: return
        if (turn.rewarded) return
        turn.rewarded = true
        port.recordReward(declaration)
        logger.info(TAG, "Rewarded ${declaration.code} reward")
        turn.callers.last()(RewardedOutcome.Granted)
    }

    /** The ad closed: the turn ends; closed before the reward gives nothing (R-04). */
    fun onDismissed(declaration: AdPlacementDeclaration) {
        val turn = turns.remove(declaration) ?: return
        if (turn.raisedPresence) port.lowerPresence()
        if (!turn.rewarded) logger.info(TAG, "Rewarded ${declaration.code} closed without reward")
    }

    /**
     * One show call failed (R-04). The turn ends only when every show call of it failed before the ad covered the
     * screen (question 5): the latest caller gets the policy, the others a blocked [RewardedOutcome.NoAd].
     */
    fun onShowFailed(declaration: AdPlacementDeclaration, code: Int) {
        val turn = turns[declaration] ?: return
        turn.showFailures++
        if (turn.onScreen || turn.showFailures < turn.shows) {
            logger.info(TAG, "Rewarded ${declaration.code} show failed: $code (turn goes on)")
            return
        }
        turns -= declaration
        if (turn.raisedPresence) port.lowerPresence()
        val granted = countNoAd(declaration, turn.policy)
        logger.info(TAG, "Rewarded ${declaration.code} skipped: show failed $code (granted $granted)")
        val latest = turn.callers.last()
        turn.callers.dropLast(1).forEach { it(RewardedOutcome.NoAd(network = false, granted = false)) }
        latest(RewardedOutcome.NoAd(network = false, granted = granted))
    }

    /**
     * Stuck recovery (F-03) or a lost host: every waiting turn ends as if closed, with no outcome. A decision still
     * running is not removed: it ends by itself within [DECISION_WAIT_MILLIS] (question 2 of plan T-047).
     */
    fun onStuck() {
        if (turns.isEmpty()) return
        turns.forEach { (declaration, turn) ->
            if (turn.raisedPresence) port.lowerPresence()
            logger.warn(TAG, "Rewarded ${declaration.code} stuck: turn ended")
        }
        turns.clear()
    }

    /** One more try with no ad in a row; back to 0 once the policy lets the action run. */
    private fun countNoAd(declaration: AdPlacementDeclaration, policy: RewardedNoAdPolicy): Boolean {
        val tries = (noAdInARow[declaration] ?: 0) + 1
        val granted = grantsWithoutAd(policy, tries)
        if (granted) noAdInARow -= declaration else noAdInARow[declaration] = tries
        return granted
    }

    private companion object {
        const val TAG = "Ads"
        const val SHOWN = "shown"
        const val READY = "ready"
        const val BUSY = "busy"
        const val NOT_READY = "not_ready"

        /** The longest wait of a decision, and of a load round for the switch (rules, section 11; plan T-047). */
        const val DECISION_WAIT_MILLIS = 5_000L
    }
}
