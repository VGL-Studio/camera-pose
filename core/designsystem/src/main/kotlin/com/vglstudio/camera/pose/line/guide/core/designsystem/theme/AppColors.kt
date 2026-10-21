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

private val Blank = Color.Transparent

internal val LightAppColors = AppColors(
    success = Blank, onSuccess = Blank, successContainer = Blank, onSuccessContainer = Blank,
    shadowSticker = Blank, categoryWomen = Blank, categoryMen = Blank, categoryCouple = Blank,
    categoryFriends = Blank, categoryFamily = Blank, categoryKids = Blank, categoryTravel = Blank,
    categoryCafe = Blank, onCategory = Blank, cameraBackground = Blank, cameraSurface = Blank,
    onCamera = Blank, cameraOnSurfaceVariant = Blank, cameraControl = Blank, cameraOnControl = Blank,
    cameraOverlayStroke = Blank, cameraScrim = Blank,
)

internal val DarkAppColors = LightAppColors

val LocalAppColors = staticCompositionLocalOf { LightAppColors }

val MaterialTheme.appColors: AppColors
    @Composable
    @ReadOnlyComposable
    get() = LocalAppColors.current
