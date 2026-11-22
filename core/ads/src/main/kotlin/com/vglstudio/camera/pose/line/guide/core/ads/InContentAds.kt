package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf

private const val TAG = "Ads"

/** The formats of an ad inside the content of a screen (MC-13): never an anchored, floating or full-screen-on-action ad. */
private val IN_CONTENT_FORMATS = setOf(
    AdFormat.NATIVE_SMALL,
    AdFormat.NATIVE_MEDIUM,
    AdFormat.BANNER_MREC,
    AdFormat.NATIVE_IN_COLLECTION,
    AdFormat.NATIVE_STEP,
    AdFormat.NATIVE_FULL_SCREEN,
)

/**
 * Whether [placement] is an ad inside the content of a screen (MC-13, plan T-092): native small standing on its own,
 * medium, MREC, in a collection, the card and the page of a step; never a fixed or collapsible banner, a collapsible
 * native, an interstitial, an app open, a rewarded ad or an app-wide placement. By format only: the native small of a
 * bottom bar and a full-screen native opened by an action pass too (question 6 of the plan).
 */
internal fun isInContentPlacement(placement: AdPlacement): Boolean =
    !placement.appWide && placement.format in IN_CONTENT_FORMATS

/**
 * Whether the floating ads of the screen are blocked (L-072): once blocked in this entry, for the rest of it; else
 * whether an in-content placement is on ([anyOn]), null while not known.
 */
internal fun inContentBlock(anyOn: Boolean?, blockedThisEntry: Boolean): Boolean? =
    if (blockedThisEntry || anyOn == true) true else anyOn

/** The block of nested [InContentAds]: blocked when either one is, not known while one is not known. */
internal fun mergeInContentBlocks(outer: Boolean?, own: Boolean?): Boolean? = when {
    outer == true || own == true -> true
    outer == null || own == null -> null
    else -> false
}

/** Whether a banner of [format] floats over the screen (MC-13): the collapsible banner, outside a bottom bar. */
internal fun isFloatingBanner(format: AdFormat, inBottomBar: Boolean): Boolean =
    format == AdFormat.BANNER_COLLAPSIBLE && !inBottomBar

/** The log line of a screen whose floating ads are off for this entry (MC-13, ADR-0019). */
internal fun inContentBlockLog(placements: List<AdPlacement>): String =
    "Floating ads off on this screen: in-content ad on (${placements.joinToString { it.key }})"

/** Whether at least one of [placements] is on (`is_show_<code>`, [AdsController.isEnabled]); false for none. */
internal fun AdsController.inContentAdsOn(placements: List<AdPlacement>): Flow<Boolean> = if (placements.isEmpty()) {
    flowOf(false)
} else {
    combine(placements.map(::isEnabled)) { values -> values.any { on -> on } }.distinctUntilChanged()
}

/**
 * Whether the floating ads of the screen are blocked by its in-content ads (MC-13): true blocked, false not, null not
 * known yet. False by default, so a screen without [InContentAds] keeps its floating ads.
 */
internal val LocalInContentAdBlock = compositionLocalOf<Boolean?> { false }

/**
 * Declares the ads inside the content of a screen (MC-13, plan T-092): when at least one of [placements] is on
 * (`is_show_<code>`, whatever the consent or whether it has an ad), the screen has no floating ad until it is left:
 * the bottom bar ([BottomAdBarScaffold]) has its slot only, as with `is_show_native_collap` off, and the collapsible
 * banner ([AdSlot]) loads nothing, takes no space and does not hold the anchored turn of the screen (MC-12). Switched
 * on while the screen is open, the floating ad goes at once (its ad destroyed, as when leaving); every placement
 * switched off, it comes back at the next entry. Logs one line when the block starts.
 *
 * - Use it once, at the root of the screen, around its bottom bar and its collapsible banner; list every in-content
 *   placement of the screen: native small standing on its own, medium, MREC, in a collection, the card and the page
 *   of a step. A fixed or collapsible banner, an interstitial, an app open, a rewarded ad or an app-wide placement
 *   throws [IllegalArgumentException]. Never list the native small of the screen's own bottom bar: it does not
 *   throw, but its key on then blocks the layer of that bar.
 * - The list is fixed for a screen: a new list while the screen is open collects again from not known, the block
 *   goes back to not known and a floating ad showing is dropped.
 * - In a `NavHost`, put it inside the destination; on a screen with [AnchoredAdScreen], inside it. Leaving the
 *   composition ends the entry into the screen.
 * - Adds no layout node; without an ads controller, or with no placement, it only draws [content].
 */
@Composable
fun InContentAds(placements: List<AdPlacement>, content: @Composable () -> Unit) {
    require(placements.all(::isInContentPlacement)) {
        "Not in-content placements: ${placements.filterNot(::isInContentPlacement).joinToString { it.key }}"
    }
    val ads = LocalAdsController.current
    if (ads == null || placements.isEmpty()) {
        content()
        return
    }
    val anyOn by remember(placements) { ads.inContentAdsOn(placements) }
        .collectAsStateWithLifecycle(initialValue = null)
    var blockedThisEntry by remember { mutableStateOf(false) }
    val own = inContentBlock(anyOn, blockedThisEntry)
    SideEffect {
        if (own == true && !blockedThisEntry) {
            blockedThisEntry = true
            ads.logger.info(TAG, inContentBlockLog(placements))
        }
    }
    CompositionLocalProvider(
        LocalInContentAdBlock provides mergeInContentBlocks(LocalInContentAdBlock.current, own),
        content = content,
    )
}
