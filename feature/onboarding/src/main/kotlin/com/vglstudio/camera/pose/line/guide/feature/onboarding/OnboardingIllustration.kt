package com.vglstudio.camera.pose.line.guide.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import kotlin.math.min

/**
 * Decorative illustration of the onboarding screen: three tilted pose cards and a camera badge (design README,
 * section 3). Drawn in the 363 x 280 frame of design/context/ and scaled down evenly to fit, centered. Hidden from
 * screen readers: it has no content description.
 */
@Composable
internal fun OnboardingIllustration(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val appColors = MaterialTheme.appColors
    BoxWithConstraints(modifier = modifier, contentAlignment = Alignment.Center) {
        val scale = min(maxWidth / FrameWidth, maxHeight / FrameHeight)
        Box(
            modifier = Modifier
                .requiredSize(FrameWidth, FrameHeight)
                .graphicsLayer {
                    scaleX = scale
                    scaleY = scale
                },
        ) {
            // Later cards are drawn over earlier ones. Positive rotation is clockwise.
            PoseCard(centerX = 116.dp, centerY = 158.5.dp, rotation = -CARD_TILT, color = appColors.categoryWomen)
            PoseCard(centerX = 181.dp, centerY = 130.dp, rotation = 0f, color = appColors.categoryCouple)
            PoseCard(centerX = 246.dp, centerY = 179.5.dp, rotation = CARD_TILT, color = appColors.categoryFriends)
            val badgeShape = RoundedCornerShape(Radius.full)
            Box(
                modifier = Modifier
                    .offset(x = BadgeCenterX - BadgeSize / 2, y = BadgeCenterY - BadgeSize / 2)
                    .size(BadgeSize)
                    .background(colors.secondaryContainer, badgeShape)
                    .border(Stroke.sticker, colors.outline, badgeShape),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(R.drawable.ic_onboarding_photo_camera),
                    contentDescription = null,
                    tint = colors.onSecondaryContainer,
                    modifier = Modifier.size(BadgeIconSize),
                )
            }
        }
    }
}

@Composable
private fun PoseCard(centerX: Dp, centerY: Dp, rotation: Float, color: Color) {
    val shape = RoundedCornerShape(Radius.card)
    Box(
        modifier = Modifier
            .offset(x = centerX - CardWidth / 2, y = centerY - CardHeight / 2)
            .size(CardWidth, CardHeight)
            .graphicsLayer { rotationZ = rotation }
            .background(color, shape)
            .border(Stroke.sticker, MaterialTheme.colorScheme.outline, shape),
    ) {
        // One outline for the three cards: the tilted frames of the design are this outline rotated.
        Image(
            painter = painterResource(R.drawable.onboarding_pose_outline),
            contentDescription = null,
            colorFilter = ColorFilter.tint(MaterialTheme.appColors.onCategory),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = OutlineTop)
                .size(OutlineWidth, OutlineHeight),
        )
    }
}

private val FrameWidth = 363.dp
private val FrameHeight = 280.dp
private val CardWidth = 150.dp
private val CardHeight = 200.dp
private const val CARD_TILT = 8f
private val OutlineTop = 30.dp
private val OutlineWidth = 70.dp
private val OutlineHeight = 140.dp
private val BadgeCenterX = 264.dp
private val BadgeCenterY = 42.dp
private val BadgeSize = 56.dp
private val BadgeIconSize = 28.dp
