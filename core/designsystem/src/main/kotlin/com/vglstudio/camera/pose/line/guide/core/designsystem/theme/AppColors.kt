package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/** App colors that Material 3 has no role for. Read them through [appColors]. */
@Immutable
data class AppColors(
    val success: Color,
    val onSuccess: Color,
    val successContainer: Color,
    val onSuccessContainer: Color,
    val shadowSticker: Color,
    val categoryWomen: Color,
    val categoryMen: Color,
    val categoryCouple: Color,
    val categoryFriends: Color,
    val categoryFamily: Color,
    val categoryKids: Color,
    val categoryTravel: Color,
    val categoryCafe: Color,
    val onCategory: Color,
    val cameraBackground: Color,
    val cameraSurface: Color,
    val onCamera: Color,
    val cameraOnSurfaceVariant: Color,
    val cameraControl: Color,
    val cameraOnControl: Color,
    val cameraOverlayStroke: Color,
    val cameraScrim: Color,
)

internal val LightAppColors = AppColors(
    success = Color(0xFF1F9D63),
    onSuccess = Color(0xFFFFFFFF),
    successContainer = Color(0xFF3DDC97),
    onSuccessContainer = Color(0xFF111111),
    shadowSticker = Color(0xFFFFC83D),
    categoryWomen = Color(0xFFFF5C8A),
    categoryMen = Color(0xFFFFC83D),
    categoryCouple = Color(0xFF3DDC97),
    categoryFriends = Color(0xFF4D8DFF),
    categoryFamily = Color(0xFF5ED3F3),
    categoryKids = Color(0xFFFFA8D0),
    categoryTravel = Color(0xFFFF8A3D),
    categoryCafe = Color(0xFFB8E986),
    onCategory = Color(0xFF111111),
    cameraBackground = Color(0xFF0A0A0B),
    cameraSurface = Color(0xFF1E1E1E),
    onCamera = Color(0xFFFFFFFF),
    cameraOnSurfaceVariant = Color(0xFFB5B0A8),
    cameraControl = Color(0xFFFFFFFF),
    cameraOnControl = Color(0xFF111111),
    cameraOverlayStroke = Color(0xFFFFFFFF),
    cameraScrim = Color(0xFF000000),
)

internal val DarkAppColors = AppColors(
    success = Color(0xFF3DDC97),
    onSuccess = Color(0xFF0B2E1E),
    successContainer = Color(0xFF3DDC97),
    onSuccessContainer = Color(0xFF111111),
    shadowSticker = Color(0xFFFFC83D),
    categoryWomen = Color(0xFFFF5C8A),
    categoryMen = Color(0xFFFFC83D),
    categoryCouple = Color(0xFF3DDC97),
    categoryFriends = Color(0xFF4D8DFF),
    categoryFamily = Color(0xFF5ED3F3),
    categoryKids = Color(0xFFFFA8D0),
    categoryTravel = Color(0xFFFF8A3D),
    categoryCafe = Color(0xFFB8E986),
    onCategory = Color(0xFF111111),
    cameraBackground = Color(0xFF0A0A0B),
    cameraSurface = Color(0xFF1E1E1E),
    onCamera = Color(0xFFFFFFFF),
    cameraOnSurfaceVariant = Color(0xFFB5B0A8),
    cameraControl = Color(0xFFFFFFFF),
    cameraOnControl = Color(0xFF111111),
    cameraOverlayStroke = Color(0xFFFFFFFF),
    cameraScrim = Color(0xFF000000),
)

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
