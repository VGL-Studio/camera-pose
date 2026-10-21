package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R

internal const val FONT_FAMILY_NAME = "Bricolage Grotesque"

/** Brand typeface, Bricolage Grotesque static cuts (SIL OFL 1.1, see third_party/bricolage-grotesque). */
internal val BricolageFamily: FontFamily = FontFamily(
    Font(R.font.bricolage_grotesque_regular, FontWeight.Normal),
    Font(R.font.bricolage_grotesque_medium, FontWeight.Medium),
    Font(R.font.bricolage_grotesque_semibold, FontWeight.SemiBold),
    Font(R.font.bricolage_grotesque_extrabold, FontWeight.ExtraBold),
)

/** Material 3 type scale in Bricolage Grotesque, without letter spacing, weights as in sticker.md (ADR-0020). */
internal val AppTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        displayMedium = displayMedium.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        displaySmall = displaySmall.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        headlineLarge = headlineLarge.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        headlineMedium = headlineMedium.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        headlineSmall = headlineSmall.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        titleLarge = titleLarge.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 0.sp,
        ),
        titleMedium = titleMedium.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleSmall = titleSmall.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        labelLarge = labelLarge.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        labelMedium = labelMedium.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        labelSmall = labelSmall.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.Medium,
            letterSpacing = 0.sp,
        ),
        bodyLarge = bodyLarge.copy(fontFamily = BricolageFamily, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(
            fontFamily = BricolageFamily,
            fontWeight = FontWeight.Normal,
            letterSpacing = 0.sp,
        ),
        bodySmall = bodySmall.copy(fontFamily = BricolageFamily, fontWeight = FontWeight.Normal, letterSpacing = 0.sp),
    )
}
