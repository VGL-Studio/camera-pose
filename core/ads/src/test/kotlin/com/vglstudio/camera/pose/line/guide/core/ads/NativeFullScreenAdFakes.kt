package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/** Fixed ads for the native full-screen tests (plan T-013, section 4). */
internal object FakeNativeAdAssets {
    /** The illustration texts of design/context/NativeFullScreenAdScreen_default_*.md; no icon. */
    val Design = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser",
        body = "Body text of the ad. Up to three lines; the ad decides the words, the app decides the style.",
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )

    /** The longest texts an ad may send: an 80-character headline and a 300-character body. */
    val Longest = NativeAdAssets(
        headline = "The longest headline an ad may send, eighty characters in all, cut on one line..",
        advertiser = "Advertiser with a rather long name",
        body = ("Body text of the ad that keeps going for a long while. ".repeat(6)).take(300),
        callToAction = "Install now",
        icon = null,
        hasMedia = true,
    )
}

/** Test tag of [FakeAdChoices]. */
internal const val FAKE_AD_CHOICES_TAG = "adChoices"

/** Stands in for the media of the ad: the "Media" label of the design on the media box. */
@Composable
internal fun BoxScope.FakeMedia() {
    Text(
        text = "Media",
        style = MaterialTheme.typography.labelMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = Spacing.md, top = NativeAdDimens.InnerPadding),
    )
}

/**
 * Stands in for the 16 dp AdChoices icon that the SDK draws in the top right corner. At font scale 2.0 the "i" is cut
 * (plan T-046): kept, as the SDK draws the real AdChoices and a fitted "i" would move every screenshot that draws this
 * box.
 */
@Composable
internal fun FakeAdChoices() {
    Box(
        modifier = Modifier
            .size(NativeAdDimens.AdChoicesSize)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag(FAKE_AD_CHOICES_TAG),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = "i",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
