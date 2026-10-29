package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

/**
 * Limits of one placement counted in natural breaks (plan T-018; spec ads, question 1): the first break that may
 * show an ad and the number of breaks between two ads. No time rule here: the only time gate is `ads_interval` of
 * :core:ads (plan T-074).
 */
internal data class BreakCap(val firstBreak: Int, val breaksBetween: Int) {
    companion object {
        /** No limit besides the full-screen gate of :core:ads. */
        val NONE = BreakCap(firstBreak = 1, breaksBetween = 1)
    }
}

/**
 * Counts the natural breaks of the session for each placement and remembers the last break that showed an ad, in
 * memory only (spec camera, section 6). Free of Android, so it runs in unit tests. Main thread only.
 */
internal class InterstitialBreaks {
    private class State {
        var breaks = 0
        var lastShownBreak: Int? = null
    }

    private val states = mutableMapOf<AdPlacement, State>()

    private fun stateOf(placement: AdPlacement) = states.getOrPut(placement) { State() }

    /** Counts one break of [placement], shown or not, and returns its number (the first is 1). */
    fun onBreak(placement: AdPlacement): Int = ++stateOf(placement).breaks

    fun allows(placement: AdPlacement, breakNumber: Int, cap: BreakCap): Boolean {
        if (breakNumber < cap.firstBreak) return false
        val lastBreak = stateOf(placement).lastShownBreak ?: return true
        return breakNumber - lastBreak >= cap.breaksBetween
    }

    fun onShown(placement: AdPlacement, breakNumber: Int) {
        stateOf(placement).lastShownBreak = breakNumber
    }
}

/** Remote keys of the break limits (spec ads, questions 1 and 2): names set by the app, not AdsKit keys. */
object InterstitialBreakSettings {
    val REVIEW_FIRST_BREAK: RemoteKey<Long> = RemoteKey.long(
        "ads_review_interstitial_first_break",
        2,
        "First saved photo of the session (counted in items) that may show review_interstitial. " +
            "Name set by the app, not an AdsKit key.",
    )
    val REVIEW_BREAKS_BETWEEN: RemoteKey<Long> = RemoteKey.long(
        "ads_review_interstitial_breaks_between",
        3,
        "Saved photos (items) between two review_interstitial ads. Name set by the app, not an AdsKit key.",
    )

    /** Every key of the break limits. */
    fun all(): List<RemoteKey<*>> = listOf(REVIEW_FIRST_BREAK, REVIEW_BREAKS_BETWEEN)

    /** The keys of the first break and of the breaks between two ads of [placement], or null: no limit. */
    fun capOf(placement: AdPlacement): Pair<RemoteKey<Long>, RemoteKey<Long>>? = when (placement) {
        AdPlacement.REVIEW_INTERSTITIAL -> REVIEW_FIRST_BREAK to REVIEW_BREAKS_BETWEEN
        else -> null
    }
}

/** One counted break that may show an ad. */
class BreakTicket internal constructor(val placement: AdPlacement, val breakNumber: Int)

/**
 * The break limits of :app in front of the full-screen gate of :core:ads (spec ads, question 1). Main thread only.
 */
interface InterstitialBreakGate {
    /**
     * Counts one natural break of [placement], shown or not, and returns a ticket when its limits let an ad show at
     * this break, null otherwise.
     */
    suspend fun onBreak(placement: AdPlacement): BreakTicket?

    /** The ad of [ticket] was shown: the next ones count from its break. */
    fun onShown(ticket: BreakTicket)
}

/**
 * Counts in memory for the session, reads both limits from Remote Config at each break (ADR-0003); a placement
 * without limits ([InterstitialBreakSettings.capOf] null) has [BreakCap.NONE].
 */
@Singleton
internal class DefaultInterstitialBreakGate @Inject constructor(private val settings: RemoteSettings) :
    InterstitialBreakGate {
    private val breaks = InterstitialBreaks()

    override suspend fun onBreak(placement: AdPlacement): BreakTicket? {
        // Every call is a break, counted even when no ad follows.
        val breakNumber = breaks.onBreak(placement)
        val cap = capOf(placement)
        return if (breaks.allows(placement, breakNumber, cap)) BreakTicket(placement, breakNumber) else null
    }

    override fun onShown(ticket: BreakTicket) {
        breaks.onShown(ticket.placement, ticket.breakNumber)
    }

    private suspend fun capOf(placement: AdPlacement): BreakCap {
        val (firstBreak, breaksBetween) = InterstitialBreakSettings.capOf(placement) ?: return BreakCap.NONE
        return BreakCap(
            firstBreak = settings.value(firstBreak).first().toIntOrMax(),
            breaksBetween = settings.value(breaksBetween).first().toIntOrMax(),
        )
    }

    private fun Long.toIntOrMax(): Int = coerceIn(0L, Int.MAX_VALUE.toLong()).toInt()
}
