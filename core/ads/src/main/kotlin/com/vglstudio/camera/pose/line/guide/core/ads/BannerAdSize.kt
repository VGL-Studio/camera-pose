package com.vglstudio.camera.pose.line.guide.core.ads

import android.content.Context
import com.google.android.gms.ads.AdSize

/** The size a banner format is drawn at; the one place that picks a banner's size (plan T-067). */
internal enum class BannerSizeKind {
    /** The anchored adaptive size of the current orientation, as JBase (B-01, ADR-0038). */
    ANCHORED,

    /** The large anchored adaptive size (B-03). */
    LARGE_ANCHORED,

    /** The fixed 300 x 250 dp size of [AdFormat.BANNER_MREC] (B-08). */
    MEDIUM_RECTANGLE,
}

/** Width of [BannerSizeKind.MEDIUM_RECTANGLE], the width of `AdSize.MEDIUM_RECTANGLE`. */
internal const val MEDIUM_RECTANGLE_WIDTH_DP = 300

/** Height of [BannerSizeKind.MEDIUM_RECTANGLE], the height of `AdSize.MEDIUM_RECTANGLE`. */
internal const val MEDIUM_RECTANGLE_HEIGHT_DP = 250

/**
 * Whether a banner of [kind] fits a slot [availableWidthDp] wide: the MREC needs its 300 dp (AdMob draws nothing in
 * less, plan T-062, question 2); the adaptive sizes fit any width.
 */
internal fun bannerFitsWidth(kind: BannerSizeKind, availableWidthDp: Int): Boolean = when (kind) {
    BannerSizeKind.MEDIUM_RECTANGLE -> availableWidthDp >= MEDIUM_RECTANGLE_WIDTH_DP
    BannerSizeKind.ANCHORED, BannerSizeKind.LARGE_ANCHORED -> true
}

/** The width of the ad in its frame: 300 dp for the MREC, centred; `null` fills the slot's width. */
internal fun bannerFrameWidthDp(kind: BannerSizeKind): Int? = when (kind) {
    BannerSizeKind.MEDIUM_RECTANGLE -> MEDIUM_RECTANGLE_WIDTH_DP
    BannerSizeKind.ANCHORED, BannerSizeKind.LARGE_ANCHORED -> null
}

/**
 * [AdFormat.BANNER] is drawn at the anchored adaptive size (B-01, ADR-0038), [AdFormat.BANNER_COLLAPSIBLE] at the
 * large one (B-03), [AdFormat.BANNER_MREC] at the fixed 300 x 250 dp (B-08); other formats throw.
 */
internal fun bannerSizeKind(format: AdFormat): BannerSizeKind = when (format) {
    AdFormat.BANNER -> BannerSizeKind.ANCHORED
    AdFormat.BANNER_COLLAPSIBLE -> BannerSizeKind.LARGE_ANCHORED
    AdFormat.BANNER_MREC -> BannerSizeKind.MEDIUM_RECTANGLE
    else -> throw IllegalArgumentException("$format is not a banner format")
}

/**
 * The SDK size of a banner of [format] in a slot [widthDp] wide, from [bannerSizeKind].
 *
 * The anchored size is the one JBase uses (`AdLayoutUtils.getAdaptiveBannerSize`). play-services-ads 25.5.0 marks
 * `getCurrentOrientationAnchoredAdaptiveBannerAdSize` deprecated with no replacement for this size: the replacement
 * Google names is the large size. If an SDK upgrade removes the function, B-01 has to be decided again.
 */
@Suppress("DEPRECATION")
internal fun bannerAdSize(context: Context, format: AdFormat, widthDp: Int): AdSize = when (bannerSizeKind(format)) {
    BannerSizeKind.ANCHORED -> AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(context, widthDp)
    BannerSizeKind.LARGE_ANCHORED -> AdSize.getLargeAnchoredAdaptiveBannerAdSize(context, widthDp)
    // Fixed size: the width of the slot does not matter ([bannerFitsWidth] decides whether it is drawn).
    BannerSizeKind.MEDIUM_RECTANGLE -> AdSize.MEDIUM_RECTANGLE
}
