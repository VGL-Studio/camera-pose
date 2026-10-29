package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.LocalActivity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

/**
 * An action of the user held behind a rewarded ad (MC-05, KB-19), made by [rememberRewardedAction] and drawn by
 * [RewardedAdDialog]. Call [start] from the button of the action: with the placement switched off (or the consent
 * gate closed) the action runs at once; else the dialog asks to watch an ad. [watch] and [dismiss] are the two
 * buttons of the dialog; the back button and a touch outside are [dismiss].
 */
@Stable
class RewardedAction internal constructor(
    private val scope: CoroutineScope,
    private val phase: MutableState<RewardedDialogPhase>,
    /** The placement switch, waited for at each [start]; null runs the action at once (no controller). */
    private val enabled: Flow<Boolean>?,
    private val callbacks: State<RewardedActionCallbacks>,
) {
    private var waitingForSwitch = false

    /** The action ran since the dialog last opened: one opening runs it at most once (plan T-042). */
    private var grantedThisOpening = false

    /** True while the dialog shows: asking, or after "Watch ad" until the ad covers the screen. */
    val dialogVisible: Boolean get() = phase.value != RewardedDialogPhase.HIDDEN

    /**
     * The user asked for the action. Waits for the first value of the switch (never guesses it); off runs the
     * action, on opens the dialog and records `reward_dialog` `show`. Does nothing while the dialog shows or the
     * switch is awaited, so a double tap opens one dialog.
     */
    fun start() {
        if (dialogVisible || waitingForSwitch) return
        if (enabled == null) {
            callbacks.value.onGranted()
            return
        }
        waitingForSwitch = true
        scope.launch {
            val on = try {
                enabled.first()
            } finally {
                waitingForSwitch = false
            }
            if (!on) {
                callbacks.value.onGranted()
            } else if (!dialogVisible) {
                phase.value = RewardedDialogPhase.ASKING
                grantedThisOpening = false
                callbacks.value.onDialogEvent(DIALOG_SHOW)
            }
        }
    }

    /**
     * "Watch ad". The dialog stays until the ad covers the screen, so a second tap shows the ad again (R-06). The
     * result comes back through the controller's own scope, so a recreated screen does not lose it.
     */
    fun watch() {
        if (!dialogVisible) return
        phase.value = RewardedDialogPhase.WATCHING
        callbacks.value.start(onOutcome)
    }

    /**
     * "No, thanks", the back button or a touch outside: closes the dialog and runs nothing. After "Watch ad" it only
     * closes the dialog: the turn goes on, and its result still runs the action at most once.
     */
    fun dismiss() {
        when (phase.value) {
            RewardedDialogPhase.ASKING -> {
                phase.value = RewardedDialogPhase.HIDDEN
                callbacks.value.onDialogEvent(DIALOG_DISMISS)
            }
            RewardedDialogPhase.WATCHING -> phase.value = RewardedDialogPhase.HIDDEN
            RewardedDialogPhase.HIDDEN -> Unit
        }
    }

    /** One handler for every "Watch ad" of this action: the controller counts repeated taps as one caller. */
    private val onOutcome: (RewardedOutcome) -> Unit = { outcome ->
        when (outcome) {
            RewardedOutcome.OnScreen -> closeAfterWatch()
            RewardedOutcome.Granted -> {
                closeAfterWatch()
                grantOnce()
            }
            is RewardedOutcome.NoAd -> {
                closeAfterWatch()
                when {
                    outcome.granted -> grantOnce()
                    // R-03: only a network failure that blocks the action is reported (question 10).
                    outcome.network -> callbacks.value.notifyNoConnection()
                }
            }
        }
    }

    private fun grantOnce() {
        if (grantedThisOpening) return
        grantedThisOpening = true
        callbacks.value.onGranted()
    }

    private fun closeAfterWatch() {
        if (phase.value == RewardedDialogPhase.WATCHING) phase.value = RewardedDialogPhase.HIDDEN
    }
}

/** Where the dialog of a [RewardedAction] is. Only [ASKING] survives a recreated activity. */
internal enum class RewardedDialogPhase { HIDDEN, ASKING, WATCHING }

/** The latest callbacks of a [RewardedAction], from its last composition. */
internal class RewardedActionCallbacks(
    val start: (onOutcome: (RewardedOutcome) -> Unit) -> Unit,
    val onDialogEvent: (String) -> Unit,
    val onGranted: () -> Unit,
    val notifyNoConnection: () -> Unit,
)

/**
 * Holds [onGranted] behind the rewarded ad of [placement] (MC-05, KB-19; docs/ops/ads-guide.md, section 5). Call
 * `start()` of the result from the button of the action, and draw [RewardedAdDialog] with it:
 *
 * ```kotlin
 * val unlock = rememberRewardedAction(AdPlacement.RW_EXPORT, onGranted = viewModel::export)
 * Button(onClick = unlock::start) { Text(stringResource(R.string.export)) }
 * RewardedAdDialog(unlock, title = stringResource(R.string.export_reward_title)) { Text(...) }
 * ```
 *
 * [onGranted] runs at most once per opening of the dialog, on the main thread: at once when `is_show_ads` or `is_show_<code>` is off or
 * the consent gate is closed; when the SDK reports the reward, before the ad closes (R-02); or with no ad when the
 * policy of the placement allows it (MC-05). Closing the ad before the reward runs nothing (R-04). A network failure
 * that blocks the action shows a short message (R-03). Make it a function of the ViewModel, so the result is not lost
 * when the screen is recreated. With no ads controller (tests, previews), `start()` runs [onGranted] at once.
 *
 * [noAdPolicyOverride] is for the staging ad gallery only (plan T-036): non-null, it replaces the policy of the
 * placement for the next taps, and the placement must be staging only, else this throws [IllegalArgumentException]
 * when composed. A feature declares its policy on the placement line and leaves it null.
 */
@Composable
fun rememberRewardedAction(
    placement: AdPlacement,
    onGranted: () -> Unit,
    noAdPolicyOverride: RewardedNoAdPolicy? = null,
): RewardedAction = rememberRewardedActionOf(placement, placement.declaration, onGranted, noAdPolicyOverride)

/**
 * [rememberRewardedAction] with the declaration whose policy applies given apart from [placement], so tests can pass
 * a line of their own ([rewardedPolicyOf]).
 */
@Composable
internal fun rememberRewardedActionOf(
    placement: AdPlacement,
    declaration: AdPlacementDeclaration,
    onGranted: () -> Unit,
    noAdPolicyOverride: RewardedNoAdPolicy?,
): RewardedAction {
    require(placement.format == AdFormat.REWARDED) { "${placement.key} is not a rewarded placement" }
    val policy = rewardedPolicyOf(declaration, noAdPolicyOverride)
    val ads = LocalAdsController.current
    val activity = LocalActivity.current
    val context = LocalContext.current
    if (ads == null || activity == null) {
        return rememberRewardedActionWith(
            placement = placement,
            enabled = null,
            start = {},
            onDialogEvent = {},
            onGranted = onGranted,
            notify = {},
        )
    }
    val switch = remember(ads, placement) { ads.rewardedSwitch(placement) }
    return rememberRewardedActionWith(
        placement = placement,
        enabled = switch,
        start = { onOutcome -> ads.showRewarded(activity, placement, policy, onOutcome) },
        onDialogEvent = { ads.recordRewardDialog(placement, it) },
        onGranted = onGranted,
        notify = { showRewardedNotice(context, it) },
    )
}

/**
 * [rememberRewardedAction] with its parts given: the switch ([enabled], null to run at once), the call to the
 * controller ([start]), the recorder of `reward_dialog` ([onDialogEvent]) and the message of R-03 ([notify]).
 */
@Composable
internal fun rememberRewardedActionWith(
    placement: AdPlacement,
    enabled: Flow<Boolean>?,
    start: (onOutcome: (RewardedOutcome) -> Unit) -> Unit,
    onDialogEvent: (String) -> Unit,
    onGranted: () -> Unit,
    notify: (String) -> Unit,
): RewardedAction {
    val scope = rememberCoroutineScope()
    val phase = rememberSaveable(placement, saver = PhaseSaver) { mutableStateOf(RewardedDialogPhase.HIDDEN) }
    val noConnection = stringResource(R.string.ads_reward_check_connection)
    val callbacks = rememberUpdatedState(
        RewardedActionCallbacks(
            start = start,
            onDialogEvent = onDialogEvent,
            onGranted = onGranted,
            notifyNoConnection = { notify(noConnection) },
        ),
    )
    return remember(placement, enabled, scope) { RewardedAction(scope, phase, enabled, callbacks) }
}

/** Keeps only an asking dialog: after "Watch ad" the result goes to the screen that tapped it. */
private val PhaseSaver = Saver<MutableState<RewardedDialogPhase>, Boolean>(
    save = { it.value == RewardedDialogPhase.ASKING },
    restore = { asking -> mutableStateOf(if (asking) RewardedDialogPhase.ASKING else RewardedDialogPhase.HIDDEN) },
)

/** The message of R-03 when a network failure blocks the action: a short toast (question 2). */
internal fun showRewardedNotice(context: Context, text: String) {
    Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
}

/**
 * The dialog of [action] (R-01): [title] from the feature, the feature's own [content] (scrolls when long), then
 * "Watch ad" and "No, thanks", always below the content. Draws nothing while [RewardedAction.dialogVisible] is false.
 */
@Composable
fun RewardedAdDialog(
    action: RewardedAction,
    title: String,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    if (!action.dialogVisible) return
    Dialog(onDismissRequest = action::dismiss) {
        RewardedAdDialogCard(
            title = title,
            onWatch = action::watch,
            onNoThanks = action::dismiss,
            modifier = modifier,
            content = content,
        )
    }
}

// Width of a Material 3 basic dialog, as the network dialog (question 6 of plan T-031: no design yet).
private val DialogMaxWidth = 312.dp

// R-01: the "Watch ad" button of AdsKit is 56 dp high with corners of 28 dp (question 11 of plan T-031).
private val WatchButtonHeight = 56.dp
private val WatchButtonShape = RoundedCornerShape(28.dp)

/** The card of [RewardedAdDialog], without state. */
@Composable
internal fun RewardedAdDialogCard(
    title: String,
    onWatch: () -> Unit,
    onNoThanks: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit = {},
) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .widthIn(max = DialogMaxWidth)
            .fillMaxWidth()
            .background(colors.surfaceContainerHigh, MaterialTheme.shapes.extraLarge)
            // TalkBack announces the dialog by its title when it opens.
            .semantics { paneTitle = title }
            .testTag(RewardedAdDialogTestTags.CARD)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = title, style = MaterialTheme.typography.headlineSmall, color = colors.onSurface)
        Column(
            modifier = Modifier
                // Takes what is left above the buttons, so they are never pushed out of the window (L-028).
                .weight(1f, fill = false)
                .verticalScroll(rememberScrollState())
                .testTag(RewardedAdDialogTestTags.CONTENT),
        ) {
            content()
        }
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            Button(
                onClick = onWatch,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(WatchButtonHeight)
                    .testTag(RewardedAdDialogTestTags.WATCH),
                shape = WatchButtonShape,
            ) {
                Text(text = stringResource(R.string.ads_reward_watch), style = MaterialTheme.typography.labelLarge)
            }
            TextButton(
                onClick = onNoThanks,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag(RewardedAdDialogTestTags.NO_THANKS),
                // L-053: a text button on a colored card sets `primary` by itself; the card's own color is given.
                colors = ButtonDefaults.textButtonColors(contentColor = colors.onSurfaceVariant),
            ) {
                Text(text = stringResource(R.string.ads_reward_no_thanks), style = MaterialTheme.typography.labelLarge)
            }
        }
    }
}

/** Tags for UI tests of the rewarded dialog. */
object RewardedAdDialogTestTags {
    const val CARD = "rewarded_dialog_card"
    const val WATCH = "rewarded_dialog_watch"
    const val NO_THANKS = "rewarded_dialog_no_thanks"
    const val CONTENT = "rewarded_dialog_content"
}

/** `dialog_action` of `reward_dialog` (section 8.2, question 4 of plan T-031). */
internal const val DIALOG_SHOW = "show"
internal const val DIALOG_DISMISS = "dismiss"
