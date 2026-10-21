package com.vglstudio.camera.pose.line.guide.core.designsystem.theme

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Spacing scale on a 4 dp grid. Features use these values instead of raw dp literals. */
object Spacing {
    val xs: Dp = 4.dp
    val sm: Dp = 8.dp
    val md: Dp = 16.dp
    val lg: Dp = 24.dp
    val xl: Dp = 32.dp

    /** Every step of the scale with its name, for the catalog and docs/design-system/tokens.json (ADR-0020). */
    val scale: List<Pair<String, Dp>> = listOf("xs" to xs, "sm" to sm, "md" to md, "lg" to lg, "xl" to xl)
}
