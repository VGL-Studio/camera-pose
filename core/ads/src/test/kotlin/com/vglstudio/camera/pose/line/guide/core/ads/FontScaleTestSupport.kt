package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density

/**
 * Draws [content] at the system font scale [fontScale], as the font size setting of the device would (plan T-044).
 * Put it outside `AppTheme`, so the theme and everything below it read the scaled density. `Density(density,
 * fontScale)` keeps the screen density and goes through the non-linear font scale table of Android 14
 * (`FontScaleConverterFactory`), so the sizes match a device better than scaling the text by hand.
 */
@Composable
internal fun WithFontScale(fontScale: Float, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalDensity provides Density(LocalDensity.current.density, fontScale), content = content)
}
