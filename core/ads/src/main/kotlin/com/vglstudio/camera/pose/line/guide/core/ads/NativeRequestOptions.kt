package com.vglstudio.camera.pose.line.guide.core.ads

import com.google.android.gms.ads.MediaAspectRatio
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.nativead.NativeAdOptions

/**
 * What a native ad request asks the SDK for (O-06): video starts muted, no custom video controls, one image, the
 * aspect of the media ([MediaAspectRatio]), and the corner where the SDK pins AdChoices ([adChoicesCorner]). The
 * only place in the module that builds the SDK's NativeAdOptions, so every native request goes through it (L-072).
 */
internal data class NativeRequestOptions(
    val startMuted: Boolean,
    val customControls: Boolean,
    val multipleImages: Boolean,
    val mediaAspect: Int,
    val adChoicesCorner: Int = NativeAdOptions.ADCHOICES_TOP_RIGHT,
)

/**
 * Options of a native ad drawn in a screen: landscape media where the layout draws media, portrait for the card of a
 * grid (O-06); small draws none.
 */
internal fun nativeRequestOptions(size: NativeAdSize): NativeRequestOptions = NativeRequestOptions(
    startMuted = true,
    customControls = false,
    multipleImages = false,
    mediaAspect = when (size) {
        NativeAdSize.SMALL -> MediaAspectRatio.ANY
        // The 250 dp card of a flow step draws its media wide, like medium (plan T-032); AdChoices top right.
        NativeAdSize.MEDIUM, NativeAdSize.FULL, NativeAdSize.STEP -> MediaAspectRatio.LANDSCAPE
        NativeAdSize.CARD -> MediaAspectRatio.PORTRAIT
    },
)

/**
 * Options of the native full-screen ad: as every native ad, but any media aspect, so the tall media area does not
 * narrow the ads it can get (plan T-027, question 1).
 */
internal fun fullScreenNativeRequestOptions(): NativeRequestOptions = NativeRequestOptions(
    startMuted = true,
    customControls = false,
    multipleImages = false,
    mediaAspect = MediaAspectRatio.ANY,
)

/**
 * Options of the collapsible layer of a bottom bar: as medium, but AdChoices in the top left corner, since the collapse
 * button takes the top right one (design, plan T-030).
 */
internal fun collapsibleNativeRequestOptions(): NativeRequestOptions =
    nativeRequestOptions(NativeAdSize.MEDIUM).copy(adChoicesCorner = NativeAdOptions.ADCHOICES_TOP_LEFT)

/** The SDK options of this request; AdChoices in [NativeRequestOptions.adChoicesCorner], kept free by the layout. */
internal fun NativeRequestOptions.toNativeAdOptions(): NativeAdOptions = NativeAdOptions.Builder()
    .setAdChoicesPlacement(adChoicesCorner)
    .setMediaAspectRatio(mediaAspect)
    .setRequestMultipleImages(multipleImages)
    .setVideoOptions(
        VideoOptions.Builder()
            .setStartMuted(startMuted)
            .setCustomControlsRequested(customControls)
            .build(),
    )
    .build()
