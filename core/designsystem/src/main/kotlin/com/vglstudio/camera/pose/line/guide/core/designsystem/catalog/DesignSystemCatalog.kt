package com.vglstudio.camera.pose.line.guide.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/**
 * Shows every value of the design system, so designers can rebuild it in Claude Design or Figma (ADR-0020).
 * Screenshot tests keep these pages as reference images; docs/design-system/tokens.json has the same values.
 * Text here is a design reference, not user-facing app text, so it is not in strings.xml.
 */
@Composable
fun ColorCatalog(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val roles = listOf(
        Triple("primary", colors.primary, colors.onPrimary),
        Triple("primaryContainer", colors.primaryContainer, colors.onPrimaryContainer),
        Triple("secondary", colors.secondary, colors.onSecondary),
        Triple("secondaryContainer", colors.secondaryContainer, colors.onSecondaryContainer),
        Triple("tertiary", colors.tertiary, colors.onTertiary),
        Triple("tertiaryContainer", colors.tertiaryContainer, colors.onTertiaryContainer),
        Triple("error", colors.error, colors.onError),
        Triple("errorContainer", colors.errorContainer, colors.onErrorContainer),
        Triple("background", colors.background, colors.onBackground),
        Triple("surface", colors.surface, colors.onSurface),
        Triple("surfaceVariant", colors.surfaceVariant, colors.onSurfaceVariant),
        Triple("outline", colors.outline, colors.surface),
    )
    CatalogPage(title = "Colors", modifier = modifier) {
        roles.forEach { (name, color, onColor) -> ColorSwatch(name, color, onColor) }
    }
}

@Composable
fun TypographyCatalog(modifier: Modifier = Modifier) {
    val type = MaterialTheme.typography
    val styles = listOf(
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
    CatalogPage(title = "Typography", modifier = modifier) {
        styles.forEach { (name, style) -> TypeSample(name, style) }
    }
}

@Composable
fun SpacingAndComponentCatalog(modifier: Modifier = Modifier) {
    CatalogPage(title = "Spacing and components", modifier = modifier) {
        Spacing.scale.forEach { (name, value) -> SpacingSample(name, value) }
        PrimaryButton(text = "PrimaryButton", onClick = {})
        PrimaryButton(text = "PrimaryButton disabled", onClick = {}, enabled = false)
    }
}

@Composable
fun AppColorCatalog(modifier: Modifier = Modifier) {
    Box(modifier)
}

@Composable
fun ShapeCatalog(modifier: Modifier = Modifier) {
    Box(modifier)
}

@Composable
fun BrandCatalog(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    CatalogPage(title = "Brand", modifier = modifier) {
        BrandLogo()
        Text(
            text = stringResource(R.string.brand_tagline),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
        ProgressBar(progress = { CATALOG_PROGRESS })
        ProgressBar(progress = { CATALOG_PROGRESS }, color = colors.error, trackColor = colors.errorContainer)
        PrimaryButton(text = "PrimaryButton", onClick = {}, icon = painterResource(R.drawable.brand_mark))
    }
}

private const val CATALOG_PROGRESS = 0.68f

@Composable
private fun CatalogPage(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            Text(text = title, style = MaterialTheme.typography.titleLarge)
            content()
        }
    }
}

@Composable
private fun ColorSwatch(name: String, color: Color, onColor: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(color)
            .border(Dp.Hairline, MaterialTheme.colorScheme.outline)
            .padding(Spacing.md),
    ) {
        Text(text = name, color = onColor, style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
private fun TypeSample(name: String, style: TextStyle) {
    Text(text = "$name ${style.fontSize.value.toInt()}/${style.lineHeight.value.toInt()}", style = style, maxLines = 1)
}

@Composable
private fun SpacingSample(name: String, value: Dp) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Box(modifier = Modifier.width(value).height(Spacing.md).background(MaterialTheme.colorScheme.primary))
        Text(text = "$name ${value.value.toInt()} dp", style = MaterialTheme.typography.bodyMedium)
    }
}
