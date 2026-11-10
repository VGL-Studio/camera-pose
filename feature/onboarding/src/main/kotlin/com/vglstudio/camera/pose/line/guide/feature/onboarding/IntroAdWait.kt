package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.delay

/** The longest page one holds Next while the step card waits for an ad (spec onboarding, KB-6). */
internal const val INTRO_AD_WAIT_MILLIS = 5_000L

/** The longest page three holds Next while the pool has no ad for page four (spec onboarding, KB-6, question 12). */
internal const val GUIDE_AD_WAIT_MILLIS = 3_000L

/** Why the hold of a page ended, as logged (plan T-083, plan T-092). */
internal enum class IntroAdWaitEnd(val logName: String) {
    AD("ad"),
    TIMEOUT("timeout"),
    NOT_ALLOWED("not allowed"),
    LEFT_PAGE("left page"),
    BACKGROUND("background"),
    DISPOSED("disposed"),
}

/**
 * Whether Next of page one is held now (spec onboarding, KB-6): at most [maxMillis] once per flow, from the first frame
 * page one shows ([onIntro]) with ads allowed ([allowed]) and no ad on the card ([hasAd]), in the foreground. The
 * hold ends for good when the ad comes, time runs out, ads stop being allowed, the page is left, the app goes to the
 * background, or the composition goes (activity built again); it never begins again. Leaving page one uses the hold
 * up even when it never began. [rememberPageAdWait] with the hold used up before the first visit: the flow starts
 * on page one, so not being on it means it was left.
 */
@Composable
internal fun rememberIntroAdWait(
    onIntro: Boolean,
    allowed: Boolean,
    hasAd: Boolean,
    onStarted: () -> Unit = {},
    onEnded: (IntroAdWaitEnd) -> Unit = {},
    maxMillis: Long = INTRO_AD_WAIT_MILLIS,
): Boolean = rememberPageAdWait(
    onPage = onIntro,
    allowed = allowed,
    hasAd = hasAd,
    maxMillis = maxMillis,
    usedUpBeforeFirstVisit = true,
    onStarted = onStarted,
    onEnded = onEnded,
)

/**
 * Whether Next of one page of the flow is held now (spec onboarding, KB-6; plan T-083, plan T-092): at most
 * [maxMillis] once per flow, from the first frame the page shows ([onPage]) with ads allowed ([allowed]) and no ad
 * yet ([hasAd]: the card of page one, the pool for page four), in the foreground. The hold ends for good when the ad
 * comes, time runs out, ads stop being allowed, the page is left, the app goes to the background, or the composition
 * goes (activity built again); it never begins again. Leaving the page uses the hold up, even one that never began;
 * so does the ad being there in the first frame on the page (the hold is for the first time the page shows). With
 * [usedUpBeforeFirstVisit], not being on the page uses it up too, before the page was ever visited (page one, where
 * the flow starts); without it, the pages before the first visit leave the hold whole (page three).
 */
@Composable
internal fun rememberPageAdWait(
    onPage: Boolean,
    allowed: Boolean,
    hasAd: Boolean,
    maxMillis: Long,
    usedUpBeforeFirstVisit: Boolean,
    onStarted: () -> Unit = {},
    onEnded: (IntroAdWaitEnd) -> Unit = {},
): Boolean {
    // Saved: a hold that began is not begun again after the activity is built again.
    var begun by rememberSaveable { mutableStateOf(false) }
    var visited by rememberSaveable { mutableStateOf(false) }
    var running by remember { mutableStateOf(false) }
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var foreground by remember { mutableStateOf(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) }
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnEnded by rememberUpdatedState(onEnded)
    val end: (IntroAdWaitEnd) -> Unit = { reason ->
        if (running) {
            running = false
            currentOnEnded(reason)
        }
    }
    // Lifecycle events, not a state read in composition: no frame is drawn in the background (L-091).
    LifecycleEventEffect(Lifecycle.Event.ON_START) { foreground = true }
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        foreground = false
        end(IntroAdWaitEnd.BACKGROUND)
    }
    // Built again (rotation, fold) or the screen left while holding: the hold ends with the composition.
    DisposableEffect(Unit) { onDispose { end(IntroAdWaitEnd.DISPOSED) } }
    // Leaving the page uses up the hold, even one that never began (ads allowed only later): back on the page, the
    // button is never held (plan T-083, review; Nhat 2026-11-01). Before the first visit only page one is used up.
    LaunchedEffect(onPage) {
        if (onPage) {
            visited = true
        } else if (visited || usedUpBeforeFirstVisit) {
            begun = true
        }
    }
    // Ready from the first frame also uses the wait up: after a rotation the new card has no ad yet (spec: "first
    // shown"), and the page must not hold then.
    LaunchedEffect(onPage, hasAd) { if (onPage && hasAd) begun = true }
    val canWait = onPage && allowed && !hasAd && foreground
    LaunchedEffect(canWait) {
        if (canWait && !begun) {
            begun = true
            running = true
            currentOnStarted()
            delay(maxMillis)
            end(IntroAdWaitEnd.TIMEOUT)
        } else if (!canWait) {
            end(
                when {
                    hasAd -> IntroAdWaitEnd.AD
                    !allowed -> IntroAdWaitEnd.NOT_ALLOWED
                    !onPage -> IntroAdWaitEnd.LEFT_PAGE
                    else -> IntroAdWaitEnd.BACKGROUND
                },
            )
        }
    }
    // Held from the first frame the hold may begin, so the button never shows enabled for one frame first.
    return canWait && (!begun || running)
}
