package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.io.File

/*
 * Fixed theme of the tests of :core:ads (plan T-084, L-143): the values of `AppTheme` of the template at the time of
 * T-084, so the reference images and the size checks of this module do not change with the theme of an app.
 * :core:ads belongs to the template: an app does not edit this file. When the template changes `AppTheme`, this file
 * changes with it; AdsTestThemeMatchTest fails while the two differ. ADR-0008 (no hard-coded colors) is about UI code,
 * not about this fixed theme of the tests.
 */

private val AdsTestLightColors = lightColorScheme(
    primary = Color(0xFF147D68),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBF3EE),
    onPrimaryContainer = Color(0xFF0B4A3D),
    inversePrimary = Color(0xFF84DCC1),
    secondary = Color(0xFF4C635B),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCEE9DE),
    onSecondaryContainer = Color(0xFF082019),
    tertiary = Color(0xFF3F6374),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC3E8FB),
    onTertiaryContainer = Color(0xFF001F2A),
    error = Color(0xFFBA5148),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFF0DEDA),
    onErrorContainer = Color(0xFF5C1A14),
    background = Color(0xFFF7FAF8),
    onBackground = Color(0xFF17352F),
    surface = Color(0xFFF7FAF8),
    onSurface = Color(0xFF17352F),
    surfaceVariant = Color(0xFFDFEAE4),
    onSurfaceVariant = Color(0xFF6D8079),
    outline = Color(0xFF8A9C95),
    outlineVariant = Color(0xFFC3D1CA),
    surfaceTint = Color(0xFF147D68),
    inverseSurface = Color(0xFF2B3D38),
    inverseOnSurface = Color(0xFFEEF5F1),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F6F3),
    surfaceContainer = Color(0xFFEBF1EE),
    surfaceContainerHigh = Color(0xFFE5ECE8),
    surfaceContainerHighest = Color(0xFFDFEAE4),
    surfaceBright = Color(0xFFF7FAF8),
    surfaceDim = Color(0xFFD6E0DA),
)

private val AdsTestDarkColors = darkColorScheme(
    primary = Color(0xFF84DCC1),
    onPrimary = Color(0xFF101F1D),
    primaryContainer = Color(0xFF192E27),
    onPrimaryContainer = Color(0xFFA6F0D8),
    inversePrimary = Color(0xFF147D68),
    secondary = Color(0xFFB3CCC2),
    onSecondary = Color(0xFF1E352E),
    secondaryContainer = Color(0xFF344C44),
    onSecondaryContainer = Color(0xFFCEE9DE),
    tertiary = Color(0xFFA7CCE0),
    onTertiary = Color(0xFF0A3445),
    tertiaryContainer = Color(0xFF264B5B),
    onTertiaryContainer = Color(0xFFC3E8FB),
    error = Color(0xFFF49A90),
    onError = Color(0xFF5C1A14),
    errorContainer = Color(0xFF48332F),
    onErrorContainer = Color(0xFFF0DEDA),
    background = Color(0xFF101F1D),
    onBackground = Color(0xFFEFF8F4),
    surface = Color(0xFF101F1D),
    onSurface = Color(0xFFEFF8F4),
    surfaceVariant = Color(0xFF293D36),
    onSurfaceVariant = Color(0xFF96ADA5),
    outline = Color(0xFF6F857D),
    outlineVariant = Color(0xFF3F534C),
    surfaceTint = Color(0xFF84DCC1),
    inverseSurface = Color(0xFFEFF8F4),
    inverseOnSurface = Color(0xFF2B3D38),
    surfaceContainerLowest = Color(0xFF0B1816),
    surfaceContainerLow = Color(0xFF14241F),
    surfaceContainer = Color(0xFF182925),
    surfaceContainerHigh = Color(0xFF1F302B),
    surfaceContainerHighest = Color(0xFF293D36),
    surfaceBright = Color(0xFF2B3D38),
    surfaceDim = Color(0xFF101F1D),
)

/** Font files of the test font family: (file name without extension, weight, style), in the order of `InterFamily`. */
internal val AdsTestFontFiles: List<Triple<String, FontWeight, FontStyle>> = listOf(
    Triple("inter_regular", FontWeight.Normal, FontStyle.Normal),
    Triple("inter_medium", FontWeight.Medium, FontStyle.Normal),
    Triple("inter_semibold", FontWeight.SemiBold, FontStyle.Normal),
)

/**
 * Inter, read from copies in `src/test/resources/ads-test-fonts/` (Java resources of the tests, in no APK), so an app
 * that replaces the font of :core:designsystem does not change these tests. The folder must not be named `fonts/`:
 * that name hides the system fonts of Robolectric (plan T-084, decision 2).
 */
/** Class of this module, whose class loader sees the test resources. */
private object AdsTestFontLoader

internal val AdsTestFontFamily: FontFamily = FontFamily(
    AdsTestFontFiles.map { (name, weight, style) ->
        val url = checkNotNull(AdsTestFontLoader.javaClass.classLoader?.getResource("ads-test-fonts/$name.ttf")) {
            "Missing test font ads-test-fonts/$name.ttf"
        }
        Font(File(url.toURI()), weight, style)
    },
)

/** Material 3 type scale in Inter, without letter spacing, as in the design (ADR-0020). */
private val AdsTestTypography: Typography = Typography().run {
    Typography(
        displayLarge = displayLarge.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        displayMedium = displayMedium.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        displaySmall = displaySmall.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        headlineLarge = headlineLarge.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        headlineMedium = headlineMedium.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        headlineSmall = headlineSmall.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        titleLarge = titleLarge.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        // Error titles in the design are SemiBold (600).
        titleMedium = titleMedium.copy(
            fontFamily = AdsTestFontFamily,
            fontWeight = FontWeight.SemiBold,
            letterSpacing = 0.sp,
        ),
        titleSmall = titleSmall.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        bodyLarge = bodyLarge.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        bodyMedium = bodyMedium.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        bodySmall = bodySmall.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        labelLarge = labelLarge.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        labelMedium = labelMedium.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
        labelSmall = labelSmall.copy(fontFamily = AdsTestFontFamily, letterSpacing = 0.sp),
    )
}

/** Same as `AppTheme`: color scheme by [darkTheme], the type scale above, default shapes. */
@Composable
internal fun AdsTestTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = if (darkTheme) AdsTestDarkColors else AdsTestLightColors,
        typography = AdsTestTypography,
        content = content,
    )
}

/**
 * The "Next" button of an app in the tests of a native step: built like `PrimaryButton` of the template in its normal
 * state (52 dp, corner 16 dp), so the tests do not change when an app changes its own button (plan T-084, decision 3).
 */
@Composable
internal fun AdsTestNextButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Button(onClick = onClick, shape = RoundedCornerShape(16.dp), modifier = modifier.heightIn(min = 52.dp)) {
        Text(text = text, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}
