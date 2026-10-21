package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.Typography
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTypography
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.DarkColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.LightColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * docs/design-system/tokens.json lists every value of the design system for designers (ADR-0020).
 * This test fails when the file no longer matches the code. To update the file after an approved change:
 * ./gradlew :core:designsystem:testDebugUnitTest -PdesignTokens.update=true
 */
class DesignTokensTest {
    @Test
    fun tokensFile_matchesTheDesignSystem() {
        val file = File(System.getProperty("designTokens.file") ?: error("designTokens.file is not set"))
        val expected = tokensJson()
        if (System.getProperty("designTokens.update") == "true") file.writeText(expected)
        assertEquals(
            "docs/design-system/tokens.json is out of date. Run with -PdesignTokens.update=true and commit it.",
            expected,
            if (file.exists()) file.readText() else "",
        )
    }

    @Test
    fun colorRoles_listEveryRoleOfColorScheme() {
        // Color is a value class, so its getters return long and carry a mangled suffix, e.g. getPrimary-0d7_KjU.
        val schemeRoles = ColorScheme::class.java.methods
            .filter { method ->
                method.name.startsWith("get") &&
                    method.parameterCount == 0 &&
                    method.returnType == Long::class.javaPrimitiveType
            }
            .map { method ->
                method.name.removePrefix("get").substringBefore('-').replaceFirstChar { it.lowercaseChar() }
            }
            .filter { it != "scrim" }
            .toSet()
        assertEquals(
            "tokens.json must list every color role of ColorScheme except scrim (L-024).",
            schemeRoles.sorted(),
            colorRoles(LightColors).map { it.first }.sorted(),
        )
    }
}

internal fun tokensJson(): String = buildString {
    appendLine("{")
    appendLine("  \"fontFamily\": \"Inter\",")
    appendLine("  \"color\": {")
    appendLine("    \"light\": ${colors(LightColors)},")
    appendLine("    \"dark\": ${colors(DarkColors)}")
    appendLine("  },")
    val styles = typographyStyles(AppTypography)
    appendLine("  \"typography\": {")
    styles.forEachIndexed { index, (name, style) ->
        val comma = if (index < styles.lastIndex) "," else ""
        appendLine("    \"$name\": ${textStyle(style)}$comma")
    }
    appendLine("  },")
    val spacing = Spacing.scale.joinToString(", ") { (name, dp) -> "\"$name\": ${dp.value.toInt()}" }
    appendLine("  \"spacingDp\": {$spacing}")
    appendLine("}")
}

private fun colors(scheme: ColorScheme): String = colorRoles(scheme)
    .joinToString(prefix = "{", postfix = "}", separator = ", ") { (name, color) -> "\"$name\": \"${hex(color)}\"" }

internal fun colorRoles(scheme: ColorScheme): List<Pair<String, Color>> = listOf(
    "primary" to scheme.primary,
    "onPrimary" to scheme.onPrimary,
    "primaryContainer" to scheme.primaryContainer,
    "onPrimaryContainer" to scheme.onPrimaryContainer,
    "secondary" to scheme.secondary,
    "onSecondary" to scheme.onSecondary,
    "secondaryContainer" to scheme.secondaryContainer,
    "onSecondaryContainer" to scheme.onSecondaryContainer,
    "tertiary" to scheme.tertiary,
    "onTertiary" to scheme.onTertiary,
    "tertiaryContainer" to scheme.tertiaryContainer,
    "onTertiaryContainer" to scheme.onTertiaryContainer,
    "error" to scheme.error,
    "onError" to scheme.onError,
    "errorContainer" to scheme.errorContainer,
    "onErrorContainer" to scheme.onErrorContainer,
    "background" to scheme.background,
    "onBackground" to scheme.onBackground,
    "surface" to scheme.surface,
    "onSurface" to scheme.onSurface,
    "surfaceVariant" to scheme.surfaceVariant,
    "onSurfaceVariant" to scheme.onSurfaceVariant,
    "outline" to scheme.outline,
    "outlineVariant" to scheme.outlineVariant,
    "inversePrimary" to scheme.inversePrimary,
    "inverseSurface" to scheme.inverseSurface,
    "inverseOnSurface" to scheme.inverseOnSurface,
    "surfaceTint" to scheme.surfaceTint,
    "surfaceBright" to scheme.surfaceBright,
    "surfaceDim" to scheme.surfaceDim,
    "surfaceContainerLowest" to scheme.surfaceContainerLowest,
    "surfaceContainerLow" to scheme.surfaceContainerLow,
    "surfaceContainer" to scheme.surfaceContainer,
    "surfaceContainerHigh" to scheme.surfaceContainerHigh,
    "surfaceContainerHighest" to scheme.surfaceContainerHighest,
)

private fun hex(color: Color): String = "#%06X".format(color.toArgb() and 0xFFFFFF)

private fun typographyStyles(type: Typography): List<Pair<String, TextStyle>> = listOf(
    "displayLarge" to type.displayLarge,
    "displayMedium" to type.displayMedium,
    "displaySmall" to type.displaySmall,
    "headlineLarge" to type.headlineLarge,
    "headlineMedium" to type.headlineMedium,
    "headlineSmall" to type.headlineSmall,
    "titleLarge" to type.titleLarge,
    "titleMedium" to type.titleMedium,
    "titleSmall" to type.titleSmall,
    "bodyLarge" to type.bodyLarge,
    "bodyMedium" to type.bodyMedium,
    "bodySmall" to type.bodySmall,
    "labelLarge" to type.labelLarge,
    "labelMedium" to type.labelMedium,
    "labelSmall" to type.labelSmall,
)

private fun textStyle(style: TextStyle): String =
    "{\"fontSizeSp\": ${style.fontSize.value}, \"lineHeightSp\": ${style.lineHeight.value}, " +
        "\"fontWeight\": ${style.fontWeight?.weight}, \"letterSpacingSp\": ${style.letterSpacing.value}}"
