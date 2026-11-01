package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.lifecycle.Lifecycle
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map

/** What the slot of a banner draws (B-01, B-04, B-06). */
internal enum class BannerFrame {
    /** Nothing, no space, no ad view. */
    NONE,

    /** The frame with the ad view, sending requests. */
    ACTIVE,

    /** The frame of a banner that had an ad, kept at its height while consent is withdrawn; the ad is hidden. */
    KEPT_EMPTY,
}

/**
 * The frame of a banner slot: none when the placement is off (KB-7) or has no usable unit id (B-04, blank counts as
 * none); active while the consent gate is open (B-01); with the gate closed, kept empty if the slot [hadAd] (B-06),
 * none otherwise.
 */
internal fun bannerFrame(gateOpen: Boolean, enabled: Boolean, unitId: String?, hadAd: Boolean): BannerFrame = when {
    !enabled || unitId.isNullOrBlank() -> BannerFrame.NONE
    gateOpen -> BannerFrame.ACTIVE
    hadAd -> BannerFrame.KEPT_EMPTY
    else -> BannerFrame.NONE
}

/**
 * The height of a banner slot: none without a frame; [frameHeightDp] while the frame is kept (B-06) or active, before
 * the first ad too, so the screen does not jump. One exception (B-08, KB-29): an MREC whose frame failed to load
 * before it had any ad ([failedBeforeAd]) takes no space until an ad loads; fixed and collapsible banners keep theirs.
 */
internal fun bannerSlotHeightDp(
    frame: BannerFrame,
    kind: BannerSizeKind,
    frameHeightDp: Int,
    failedBeforeAd: Boolean,
): Int = when (frame) {
    BannerFrame.NONE -> 0
    BannerFrame.KEPT_EMPTY -> frameHeightDp
    BannerFrame.ACTIVE -> if (kind == BannerSizeKind.MEDIUM_RECTANGLE && failedBeforeAd) 0 else frameHeightDp
}

/** The ad view runs (resumed) only on a visible screen with an active frame; it is paused otherwise. */
internal fun bannerShouldRun(visible: Boolean, frame: BannerFrame): Boolean = visible && frame == BannerFrame.ACTIVE

/** True while this lifecycle is at least resumed: the screen holding the banner shows (KB-18, AR-03). */
internal fun Lifecycle.resumedFlow(): Flow<Boolean> =
    currentStateFlow.map { it.isAtLeast(Lifecycle.State.RESUMED) }.distinctUntilChanged()
