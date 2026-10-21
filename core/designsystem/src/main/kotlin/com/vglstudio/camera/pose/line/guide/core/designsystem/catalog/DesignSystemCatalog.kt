package com.vglstudio.camera.pose.line.guide.core.designsystem.catalog

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SecondaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.StickerShadow
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.stickerShadow

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
        SecondaryButton(text = "SecondaryButton", onClick = {})
        SecondaryButton(text = "SecondaryButton disabled", onClick = {}, enabled = false)
    }
}

@Composable
fun AppColorCatalog(modifier: Modifier = Modifier) {
    val app = MaterialTheme.appColors
    val swatches = listOf(
        Triple("success", app.success, app.onSuccess),
        Triple("successContainer", app.successContainer, app.onSuccessContainer),
        Triple("shadowSticker", app.shadowSticker, app.onCategory),
        Triple("category/women", app.categoryWomen, app.onCategory),
        Triple("category/men", app.categoryMen, app.onCategory),
        Triple("category/couple", app.categoryCouple, app.onCategory),
        Triple("category/friends", app.categoryFriends, app.onCategory),
        Triple("category/family", app.categoryFamily, app.onCategory),
        Triple("category/kids", app.categoryKids, app.onCategory),
        Triple("category/travel", app.categoryTravel, app.onCategory),
        Triple("category/cafe", app.categoryCafe, app.onCategory),
        Triple("camera/background", app.cameraBackground, app.onCamera),
        Triple("camera/surface", app.cameraSurface, app.onCamera),
        Triple("camera/control", app.cameraControl, app.cameraOnControl),
        Triple("camera/scrim", app.cameraScrim, app.onCamera),
    )
    CatalogPage(title = "App colors", modifier = modifier) {
        swatches.chunked(2).forEach { pair ->
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                pair.forEach { (name, color, onColor) ->
                    ColorSwatch(name, color, onColor, Modifier.weight(1f))
                }
                if (pair.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun ShapeCatalog(modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    CatalogPage(title = "Shapes", modifier = modifier) {
        Radius.scale.forEach { (name, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(
                    Modifier
                        .size(width = 64.dp, height = 40.dp)
                        .border(Stroke.sticker, colors.outline, RoundedCornerShape(value)),
                )
                Text(text = "$name ${value.value.toInt()} dp", style = MaterialTheme.typography.bodyMedium)
            }
        }
        Stroke.scale.forEach { (name, value) ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                Box(Modifier.size(width = 64.dp, height = 40.dp).border(value, colors.outline))
                Text(text = "$name ${value.value.toInt()} dp", style = MaterialTheme.typography.bodyMedium)
            }
        }
        val shape = RoundedCornerShape(Radius.card)
        Box(
            Modifier
                .padding(bottom = StickerShadow.offset)
                .size(112.dp)
                .stickerShadow(shape, MaterialTheme.appColors.shadowSticker)
                .background(colors.surface, shape)
                .border(Stroke.sticker, colors.outline, shape),
        )
    }
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
private fun ColorSwatch(name: String, color: Color, onColor: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
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
