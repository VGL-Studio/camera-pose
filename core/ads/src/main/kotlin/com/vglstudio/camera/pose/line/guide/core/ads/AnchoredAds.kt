package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.RememberObserver
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

private const val TAG = "Ads"

/**
 * One anchored ad of a screen (MC-12, plan T-091): a bottom bar, a fixed banner or a collapsible banner, [seq] its
 * place in the order it entered the composition. [allowed] is whether it may run now (consent, `is_show_<code>`, a
 * unit), null while its first value is not known.
 */
internal data class AnchoredAdMember(val seq: Int, val placement: AdPlacement, val allowed: Boolean?)

/**
 * The one rule of the turn (L-072, MC-12): [holder] keeps the turn while it is still among [members], even switched
 * off; otherwise the first member in order that is allowed takes it. A member whose first value is not known yet
 * makes every later one wait (null), so a later one never takes the turn before the keys of an earlier one arrive.
 */
internal fun anchoredAdHolder(members: List<AnchoredAdMember>, holder: Int?): Int? {
    if (holder != null && members.any { it.seq == holder }) return holder
    for (member in members.sortedBy { it.seq }) {
        when (member.allowed) {
            null -> return null
            true -> return member.seq
            false -> Unit
        }
    }
    return null
}

/** The warning of an anchored ad that waits because another one of the screen holds the turn (MC-12, ADR-0019). */
internal fun anchoredAdWaitLog(waiting: AdPlacement, holder: AdPlacement): String =
    "Anchored ad ${waiting.key} waits: ${holder.key} holds the anchored ad of this screen"

/** The log line of an anchored ad taking the turn of its screen (ADR-0019). */
internal fun anchoredAdTurnLog(placement: AdPlacement): String = "Anchored ad ${placement.key} takes the turn"

/** Whether a banner of [format] is an anchored ad (MC-12): the fixed and the collapsible banner, never the MREC. */
internal fun isAnchoredFormat(format: AdFormat): Boolean =
    format == AdFormat.BANNER || format == AdFormat.BANNER_COLLAPSIBLE

/**
 * The anchored ads of one screen and the one that holds the turn (MC-12). Members join in the order they enter the
 * composition, report whether they are allowed, and leave when they leave the composition; [holder] is recomputed by
 * [anchoredAdHolder] after each change. Logs a new holder (info) and, once per wait, a member allowed to run while
 * another one holds the turn (warning). Main thread only.
 */
internal class AnchoredAdScope(private val logger: AppLogger) {
    private class Entry(val seq: Int, val placement: AdPlacement) {
        var allowed: Boolean? by mutableStateOf(null)
        var allowedNow: () -> Boolean? = { allowed }
        var waitLogged = false
    }

    private var next = 0
    private val entries = mutableStateListOf<Entry>()
    private var holderSeq: Int? by mutableStateOf(null)

    /** The member holding the turn, null when none; read by Compose. */
    val holder: Int? get() = holderSeq

    /** Adds a member after every member already there; returns its place in the order. */
    fun join(placement: AdPlacement): Int {
        val seq = ++next
        entries += Entry(seq, placement)
        recompute()
        return seq
    }

    /** Member [seq] is [allowed] to run now, null while not known. */
    fun report(seq: Int, allowed: Boolean?) {
        val entry = entries.firstOrNull { it.seq == seq } ?: return
        if (entry.allowed != allowed) entry.allowed = allowed
        recompute()
    }

    /** Member [seq] left the composition: the turn passes on when it held it. */
    fun leave(seq: Int) {
        entries.removeAll { it.seq == seq }
        recompute()
    }

    /** How member [seq] reads whether it is allowed right now, from its state (not the value it last reported). */
    fun provideAllowedNow(seq: Int, allowedNow: () -> Boolean?) {
        entries.firstOrNull { it.seq == seq }?.allowedNow = allowedNow
    }

    /**
     * Whether member [seq], [allowed] now, would take the turn while nobody holds it: the other members by what they
     * read right now ([provideAllowedNow]), so two members turned on in the same frame never both take it.
     */
    fun wouldTake(seq: Int, allowed: Boolean?): Boolean {
        val members = entries.map {
            AnchoredAdMember(
                it.seq,
                it.placement,
                if (it.seq ==
                    seq
                ) {
                    allowed
                } else {
                    it.allowedNow()
                },
            )
        }
        return anchoredAdHolder(members, holder = null) == seq
    }

    private fun recompute() {
        val members = entries.map { AnchoredAdMember(it.seq, it.placement, it.allowed) }
        val newHolder = anchoredAdHolder(members, holderSeq)
        val holding = entries.firstOrNull { it.seq == newHolder }
        if (newHolder != holderSeq) {
            holderSeq = newHolder
            if (holding != null) {
                holding.waitLogged = false
                logger.info(TAG, anchoredAdTurnLog(holding.placement))
            }
        }
        if (holding == null) return
        entries.forEach { entry ->
            if (entry !== holding && entry.allowed == true && !entry.waitLogged) {
                entry.waitLogged = true
                logger.warn(TAG, anchoredAdWaitLog(entry.placement, holding.placement))
            }
        }
    }
}

/**
 * The anchored ad scopes of the screens of the app, one per [LifecycleOwner] (each destination of a `NavHost` is a
 * `NavBackStackEntry` of its own). A scope is kept until its owner is destroyed, never dropped when it is empty: a
 * screen that follows another one in the same owner uses the scope the other one just left.
 */
internal class AnchoredAdScopes(private val logger: AppLogger) {
    private val scopes = mutableMapOf<LifecycleOwner, AnchoredAdScope>()

    fun scopeFor(owner: LifecycleOwner): AnchoredAdScope {
        scopes[owner]?.let { return it }
        val scope = AnchoredAdScope(logger)
        if (owner.lifecycle.currentState == Lifecycle.State.DESTROYED) return scope
        scopes[owner] = scope
        owner.lifecycle.addObserver(
            LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_DESTROY) scopes.remove(owner)
            },
        )
        return scope
    }

    /** Tests only: whether [owner] has a scope. */
    fun containsForTest(owner: LifecycleOwner): Boolean = owner in scopes

    /** Tests only: the number of owners with a scope. */
    val sizeForTest: Int get() = scopes.size
}

/** The scope of the screen given by [AnchoredAdScreen]; none by default, the lifecycle owner's scope then. */
internal val LocalAnchoredAdScope = staticCompositionLocalOf<AnchoredAdScope?> { null }

/**
 * Makes [content] a screen of its own for the rule of one anchored ad per screen (MC-12): a bottom bar, a fixed
 * banner and a collapsible banner inside it take turns among themselves only. Use it only for a screen that is not a
 * destination of a `NavHost`, such as the pages of an activity of their own (the staging ad gallery): a destination
 * is already a screen. Never wrap tabs, pages of a pager or content that changes within a destination (they are one
 * screen, KB-35), nor the `NavHost` itself. Adds no layout node; without an ads controller it only draws [content].
 */
@Composable
fun AnchoredAdScreen(content: @Composable () -> Unit) {
    val ads = LocalAdsController.current
    if (ads == null) {
        content()
        return
    }
    val scope = remember(ads) { AnchoredAdScope(ads.logger) }
    CompositionLocalProvider(LocalAnchoredAdScope provides scope, content = content)
}

/**
 * Whether the anchored ad of [placement] holds the turn of its screen now (MC-12): the scope of [AnchoredAdScreen],
 * else the one of the lifecycle owner. [allowed] is whether it may run now, null until known; [allowedNow] reads the
 * same from state at the time of the call, for the other members turned on in the same frame. Computed in the
 * composition, so the ad builds in the same frame it becomes allowed; false in the first frame, before it joined.
 */
@Composable
internal fun rememberAnchoredAdTurn(
    ads: AdsController,
    placement: AdPlacement,
    allowed: Boolean?,
    allowedNow: () -> Boolean? = { allowed },
): Boolean {
    val owner = LocalLifecycleOwner.current
    val local = LocalAnchoredAdScope.current
    val scope = local ?: remember(ads, owner) { ads.anchoredScopes.scopeFor(owner) }
    val member = remember(scope, placement) { AnchoredAdTurnMember(scope, placement) }
    member.allowedNow = allowedNow
    val seq = member.seq
    SideEffect { member.seq?.let { scope.report(it, allowed) } }
    if (seq == null) return false
    val holder = scope.holder
    return holder == seq || (holder == null && scope.wouldTake(seq, allowed))
}

/**
 * A member of a scope for as long as it is in the composition: joins when remembered (in the order of the
 * composition, after the members that left in the same change), leaves when forgotten.
 */
private class AnchoredAdTurnMember(private val scope: AnchoredAdScope, private val placement: AdPlacement) :
    RememberObserver {
    var seq: Int? by mutableStateOf(null)
        private set

    /** Set in each composition: what the member reads now. */
    var allowedNow: () -> Boolean? = { null }

    override fun onRemembered() {
        val joined = scope.join(placement)
        scope.provideAllowedNow(joined) { allowedNow() }
        seq = joined
    }

    override fun onForgotten() {
        seq?.let { scope.leave(it) }
        seq = null
    }

    override fun onAbandoned() = Unit
}
