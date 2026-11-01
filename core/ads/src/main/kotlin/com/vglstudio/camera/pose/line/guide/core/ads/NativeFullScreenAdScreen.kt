package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/**
 * A native ad that covers the whole screen (KB-5), as in design/screens/NativeFullScreenAdScreen_default_*.png: a
 * 48 dp close button in the top left corner (KB-32, ADR-0038; the design images still draw 36 dp), AdChoices in the top right corner
 * ([adChoices]; the SDK draws it on real ads), a large [media] area, then the head of the ad, the body and a wide call
 * to action. The head is 48 dp at font scale 1.0 and grows with larger text, the media giving way (O-08, plan T-045).
 * While [secondsLeft] is above 0, a countdown of the same size takes the place of the close button and
 * takes no touch (K-03, plan T-026); null or 0 shows the close button. The app shows it with
 * [rememberShowFullScreenAd]; the gallery screen (T-014) and tests draw it with fixed content.
 */
@Composable
fun NativeFullScreenAdScreen(
    assets: NativeAdAssets,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
    secondsLeft: Int? = null,
) {
    NativeFullScreenAdLayout(assets, onClose, PreviewNativeAdBinder, modifier, media, adChoices, secondsLeft)
}

/** Sizes of the native full-screen ad, from design/context/NativeFullScreenAdScreen_default_light.md. */
internal object NativeFullScreenAdDimens {
    val CloseButtonSize = 48.dp
    val CloseIconSize = 24.dp

    /** The top bar of the page after an interstitial and of its placeholder: the 48 dp close button (ADR-0038). */
    val CloseBarHeight = 48.dp

    /** The top bar of the page of a flow step, which has no close button (question 2 of plan T-053). */
    val TopBarHeight = 36.dp
    val HeaderHeight = 48.dp
    val IconSize = 48.dp
    val CallToActionHeight = 52.dp

    /** The media shrinks first on short screens, never below the SDK's minimum. */
    val MediaMinHeight = 120.dp
}

internal object NativeFullScreenAdTestTags {
    const val CLOSE = "nativeFullScreenClose"
    const val MEDIA = "nativeFullScreenMedia"
    const val CTA = "nativeFullScreenCta"
    const val ICON = "nativeFullScreenIcon"
    const val COUNTDOWN = "nativeFullScreenCountdown"
    const val PLACEHOLDER = "nativeFullScreenPlaceholder"
    const val BODY = "nativeFullScreenBody"
}

private const val BODY_MAX_LINES = 3

/**
 * The layout of [NativeFullScreenAdScreen]. Each ad part goes through [binder], so a fake ad and a real ad bound to
 * the SDK share it; a real ad puts the SDK's MediaView in [media]. A null [onClose] is the page of a flow step
 * ([NativeFullScreenStep], variant `Flow step` of the design, plan T-032), laid out by [FlowStepPageLayout] (plan
 * T-041): no close button and no countdown ([secondsLeft] is ignored), the 36 dp top bar keeps only AdChoices, so the
 * media starts at y = 76 (12 dp above the page after an interstitial, whose close bar is 48 dp, plan T-053), no padding for the safe area of the window, and a layout chosen by the height the
 * app gives.
 */
@Composable
internal fun NativeFullScreenAdLayout(
    assets: NativeAdAssets,
    onClose: (() -> Unit)?,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
    secondsLeft: Int? = null,
) {
    if (onClose == null) {
        FlowStepPageLayout(assets, binder, modifier, media, adChoices)
        return
    }
    NativeFullScreenFrame(modifier) {
        NativeFullScreenTopBar(secondsLeft, onClose, adChoices)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .heightIn(min = NativeFullScreenAdDimens.MediaMinHeight)
                .clip(MaterialTheme.shapes.large)
                .background(MaterialTheme.colorScheme.surfaceContainer)
                .testTag(NativeFullScreenAdTestTags.MEDIA),
            content = media,
        )
        NativeFullScreenAdHeader(assets, binder)
        assets.body?.let { body ->
            NativeAdBody(body, binder, maxLines = BODY_MAX_LINES, modifier = Modifier.fillMaxWidth())
        }
        assets.callToAction?.let { NativeFullScreenCallToAction(it, binder) }
    }
}

/**
 * The page of the native full-screen ad after an interstitial and of its placeholder: background, the safe area of the
 * window, margins, 16 dp gaps. The page of a flow step has its own frame ([FlowStepPageFrame], plan T-041).
 */
@Composable
internal fun NativeFullScreenFrame(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .windowInsetsPadding(WindowInsets.safeDrawing)
            .padding(start = Spacing.md, top = Spacing.lg, end = Spacing.md, bottom = Spacing.xl),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        content = content,
    )
}

/** The 48 dp top bar: the countdown while [secondsLeft] is above 0, else the close button; AdChoices on the right. */
@Composable
internal fun NativeFullScreenTopBar(secondsLeft: Int?, onClose: () -> Unit, adChoices: @Composable () -> Unit = {}) {
    Row(
        modifier = Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.CloseBarHeight),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (secondsLeft != null && secondsLeft > 0) CountdownBadge(secondsLeft) else CloseButton(onClose)
        adChoices()
    }
}

/**
 * K-03: the seconds left in a 48 dp circle, where the close button comes once they are over; it takes no touch.
 * Same frame and colour as the close button (question 4 of plan T-026).
 */
@Composable
private fun CountdownBadge(secondsLeft: Int) {
    val description = stringResource(R.string.ads_close_countdown, secondsLeft)
    Box(
        modifier = Modifier
            .size(NativeFullScreenAdDimens.CloseButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .testTag(NativeFullScreenAdTestTags.COUNTDOWN)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "$secondsLeft",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

/**
 * A 48 dp round button, drawn, laid out and touched at 48 dp, so the touch area is the drawn frame (KB-32,
 * ADR-0038). A plain round Box, like the countdown it replaces, in the same frame.
 */
@Composable
private fun CloseButton(onClose: () -> Unit) {
    Box(
        modifier = Modifier
            .size(NativeFullScreenAdDimens.CloseButtonSize)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .clickable(role = Role.Button, onClick = onClose)
            .testTag(NativeFullScreenAdTestTags.CLOSE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_ads_close),
            contentDescription = stringResource(R.string.ads_close_full_screen),
            tint = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.size(NativeFullScreenAdDimens.CloseIconSize),
        )
    }
}

/**
 * Icon, headline, then the "Ad" label with the advertiser: 48 dp at least, taller with the text, so the headline and
 * the "Ad" label are never cut when the system text is large (O-08; plans T-041, T-045).
 */
@Composable
internal fun NativeFullScreenAdHeader(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxWidth().heightIn(min = NativeFullScreenAdDimens.HeaderHeight),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AdIcon(assets.icon, binder)
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.xs)) {
            binder.Asset(NativeAdAsset.HEADLINE, Modifier.fillMaxWidth()) {
                Text(
                    text = assets.headline,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
        }
    }
}

@Composable
private fun AdIcon(icon: ImageBitmap?, binder: NativeAdBinder) {
    val iconModifier = Modifier.size(NativeFullScreenAdDimens.IconSize).clip(MaterialTheme.shapes.small)
    binder.Asset(
        NativeAdAsset.ICON,
        Modifier.size(NativeFullScreenAdDimens.IconSize).testTag(NativeFullScreenAdTestTags.ICON),
    ) {
        if (icon != null) {
            Image(bitmap = icon, contentDescription = null, contentScale = ContentScale.Crop, modifier = iconModifier)
        } else {
            Box(modifier = iconModifier.background(MaterialTheme.colorScheme.secondaryContainer))
        }
    }
}

/** A plain box, not a Button, so a touch reaches the SDK's call-to-action view. */
@Composable
internal fun NativeFullScreenCallToAction(text: String, binder: NativeAdBinder) {
    binder.Asset(
        NativeAdAsset.CALL_TO_ACTION,
        Modifier
            .fillMaxWidth()
            .height(NativeFullScreenAdDimens.CallToActionHeight)
            .testTag(NativeFullScreenAdTestTags.CTA),
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(MaterialTheme.shapes.small)
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Preview
@Composable
private fun NativeFullScreenAdScreenPreview() {
    AppTheme {
        NativeFullScreenAdScreen(
            assets = NativeAdAssets(
                headline = "Headline of the ad",
                advertiser = "Advertiser",
                body = "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.",
                callToAction = "Install",
                icon = null,
                hasMedia = true,
            ),
            onClose = {},
        )
    }
}
