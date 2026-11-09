package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

/**
 * The art of [page] (design README, section 3): a WebP of res/drawable-nodpi, fitted and centered in the frame, the
 * same in both themes. Hidden from screen readers: it has no content description. On [OnboardingPage.GUIDE] the text
 * of the speech bubble of the art is drawn over it, centered on a point of the art and sized with the art, not with
 * the font size of the device; screen readers read it (plan T-091, question 4).
 */
@Composable
internal fun OnboardingIllustration(page: OnboardingPage, modifier: Modifier = Modifier) {
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        Image(
            painter = painterResource(page.art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.fillMaxSize(),
        )
        if (page == OnboardingPage.GUIDE) {
            // The frame of the art after ContentScale.Fit, centered in the box.
            val artHeight = min(maxHeight, maxWidth / GUIDE_ART_RATIO)
            val artWidth = artHeight * GUIDE_ART_RATIO
            BubbleText(
                centerX = (maxWidth - artWidth) / 2 + artWidth * BUBBLE_CENTER_X,
                centerY = (maxHeight - artHeight) / 2 + artHeight * BUBBLE_CENTER_Y,
                artHeight = artHeight,
            )
        }
    }
}

/** One line, centered on ([centerX], [centerY]); labelMedium at an art [BubbleArtHeight] high, scaled with the art. */
@Composable
private fun BubbleText(centerX: Dp, centerY: Dp, artHeight: Dp) {
    val label = MaterialTheme.typography.labelMedium
    val scale = artHeight / BubbleArtHeight
    // Sizes in dp of the art turned into sp, so the font scale of the device does not change them.
    val style = with(LocalDensity.current) {
        label.copy(
            fontSize = (label.fontSize.value.dp * scale).toSp(),
            lineHeight = (label.lineHeight.value.dp * scale).toSp(),
        )
    }
    Layout(
        content = {
            Text(
                text = stringResource(R.string.onboarding_guide_bubble),
                style = style,
                color = MaterialTheme.appColors.onCategory,
                textAlign = TextAlign.Center,
                softWrap = false,
                maxLines = 1,
                modifier = Modifier.testTag(OnboardingTestTags.BUBBLE),
            )
        },
        modifier = Modifier.fillMaxSize(),
    ) { measurables, constraints ->
        // Measured without a width limit: the text keeps one line and its own width.
        val text = measurables.single().measure(Constraints())
        layout(constraints.maxWidth, constraints.maxHeight) {
            text.place(centerX.roundToPx() - text.width / 2, centerY.roundToPx() - text.height / 2)
        }
    }
}

@get:DrawableRes
private val OnboardingPage.art: Int
    get() = when (this) {
        OnboardingPage.INTRO -> R.drawable.onboarding_art_intro
        OnboardingPage.POSES -> R.drawable.onboarding_art_poses
        OnboardingPage.GUIDE -> R.drawable.onboarding_art_guide
        OnboardingPage.CAMERA -> R.drawable.onboarding_art_camera
    }

/** Width to height of onboarding_art_guide (951 x 1080). */
private const val GUIDE_ART_RATIO = 951f / 1080f

/** Center of the bubble text, as a fraction of the art (design README, section 3). */
private const val BUBBLE_CENTER_X = 0.768f
private const val BUBBLE_CENTER_Y = 0.152f

/** Height of the art at which the bubble text is labelMedium (frames with a card, design README, section 3). */
private val BubbleArtHeight = 289.dp
