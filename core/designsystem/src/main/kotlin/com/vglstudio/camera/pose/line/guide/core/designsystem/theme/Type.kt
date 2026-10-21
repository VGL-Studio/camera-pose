package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R

internal const val FONT_FAMILY_NAME = "Inter"

/** Brand typeface, Inter 4.1 (SIL OFL 1.1, see third_party/inter). Each app may replace it (ADR-0008). */
internal val InterFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

/** Material 3 type scale in Inter, without letter spacing, as in the design (ADR-0020). */
internal val AppTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        displayMedium = displayMedium.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        displaySmall = displaySmall.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        headlineLarge = headlineLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        headlineMedium = headlineMedium.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        headlineSmall = headlineSmall.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        titleLarge = titleLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        // Error titles in the design are SemiBold (600).
        titleMedium = titleMedium.copy(
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleSmall = titleSmall.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        bodyLarge = bodyLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        labelLarge = labelLarge.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        labelMedium = labelMedium.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
        labelSmall = labelSmall.copy(fontFamily = InterFamily, letterSpacing = 0.sp),
    )
}

internal val BricolageFamily: FontFamily = FontFamily.Default
