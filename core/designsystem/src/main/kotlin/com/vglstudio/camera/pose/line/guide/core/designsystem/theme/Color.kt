package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color

// Brand palette. Each app cloned from the template replaces these values (ADR-0008).
// Roles used by the loading screen design come from docs/features/loading/design/context/;
// the other roles are matching greens approved in plan T-002, so no Material 3 default purple is left.
private val BrandPrimary = Color(0xFF147D68)
private val BrandPrimaryDark = Color(0xFF84DCC1)

internal val LightColors = lightColorScheme(
    primary = BrandPrimary,
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEBF3EE),
    onPrimaryContainer = Color(0xFF0B4A3D),
    inversePrimary = BrandPrimaryDark,
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
    surfaceTint = BrandPrimary,
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

internal val DarkColors = darkColorScheme(
    primary = BrandPrimaryDark,
    onPrimary = Color(0xFF101F1D),
    primaryContainer = Color(0xFF192E27),
    onPrimaryContainer = Color(0xFFA6F0D8),
    inversePrimary = BrandPrimary,
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
    surfaceTint = BrandPrimaryDark,
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
