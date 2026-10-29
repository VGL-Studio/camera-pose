package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.testTag
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlinx.coroutines.flow.StateFlow

/**
 * The native full-screen turn on screen (plan T-026, HD-05), in the full-screen dialog of
 * [NativeFullScreenAdDialog]: the back button goes to [onBack], and [NativeFullScreenTurns] decides (K-08).
 */
@Composable
internal fun NativeFullScreenOverlay(view: NativeFullScreenView, onClose: () -> Unit, onBack: () -> Unit) {
    NativeFullScreenAdDialog(onClose = onBack) {
        NativeFullScreenOverlayContent(view, onClose, animated = true)
    }
}

/**
 * What the overlay draws, with no dialog, so screenshots and tests capture it in the activity: the ad with the
 * countdown or the close button, or the placeholder while the ad loads (K-03, K-04). [animated] off keeps the
 * placeholder still (L-048).
 */
@Composable
internal fun NativeFullScreenOverlayContent(view: NativeFullScreenView, onClose: () -> Unit, animated: Boolean) {
    val secondsLeft = if (view.closeEnabled) null else view.secondsLeft
    val ad = view.ad
    if (ad != null) {
        BoundNativeAd(ad, Modifier.fillMaxSize()) { binder ->
            NativeFullScreenAdLayout(
                assets = ad.assets,
                onClose = onClose,
                binder = binder,
                // The SDK draws AdChoices in the top right corner itself; the adChoices slot stays empty.
                media = { binder.Media(Modifier.fillMaxSize()) },
                secondsLeft = secondsLeft,
            )
        }
    } else {
        NativeFullScreenPlaceholder(secondsLeft, onClose, animated)
    }
}

/**
 * The native full-screen page while its ad loads: the frame of [NativeFullScreenAdLayout] (top bar with the countdown
 * or the close button, media, head of 48 dp, call to action of 52 dp) with loading blocks that pulse like
 * [NativeAdPlaceholder] when [animated].
 */
@Composable
private fun NativeFullScreenPlaceholder(secondsLeft: Int?, onClose: () -> Unit, animated: Boolean) {
    val alpha = placeholderAlpha(animated)
    NativeFullScreenFrame(Modifier.testTag(NativeFullScreenAdTestTags.PLACEHOLDER)) {
        NativeFullScreenTopBar(secondsLeft, onClose)
        val pulse = Modifier.graphicsLayer { this.alpha = alpha.value }
        PlaceholderBlock(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = NativeFullScreenAdDimens.MediaMinHeight)
                .then(pulse),
            MaterialTheme.shapes.large,
        )
        Row(
            modifier = Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.HeaderHeight).then(pulse),
            horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            PlaceholderBlock(Modifier.size(NativeFullScreenAdDimens.IconSize), MaterialTheme.shapes.small)
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
                PlaceholderBlock(Modifier.size(NativeAdDimens.LongLineWidth, NativeAdDimens.LineHeight), bar)
                PlaceholderBlock(Modifier.size(NativeAdDimens.ShortLineWidth, NativeAdDimens.LineHeight), bar)
            }
        }
        PlaceholderBlock(
            Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.CallToActionHeight).then(pulse),
            MaterialTheme.shapes.small,
        )
    }
}

/** One loading block, in the `surfaceContainer` of the media box of the design (question 4 of plan T-026). */
@Composable
private fun PlaceholderBlock(modifier: Modifier, shape: Shape) {
    Box(modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainer))
}

/**
 * Draws the turn of [view] when there is one, nothing else. Kept apart from [AdsController] so tests drive it with a
 * state of their own, across a recreated activity.
 */
@Composable
internal fun NativeFullScreenOverlayHost(
    view: StateFlow<NativeFullScreenView?>,
    onClose: () -> Unit,
    onBack: () -> Unit,
) {
    val current by view.collectAsStateWithLifecycle()
    current?.let { NativeFullScreenOverlay(it, onClose, onBack) }
}
