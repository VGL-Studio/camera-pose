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
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.CameraIconButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ChoiceDialog
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.GridOverlay
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseCardSkeleton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PoseThumb
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SecondaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SectionHeader
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SelectedBadge
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRow
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.SettingsRowTrailing
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StateMessage
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCard
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StepCardArrows
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.StickerChip
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ZoomControl
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

/** The camera components on the camera background (sticker.md, section 4). Shown in the dark theme. */
@Composable
fun CameraComponentCatalog(modifier: Modifier = Modifier) {
    val appColors = MaterialTheme.appColors
    val refresh = painterResource(R.drawable.ic_refresh)
    Column(
        modifier = modifier.fillMaxSize().background(appColors.cameraBackground).padding(Spacing.md),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
    ) {
        Text(text = "Camera components", style = MaterialTheme.typography.titleLarge, color = appColors.onCamera)
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            CameraIconButton(icon = refresh, onClick = {}, contentDescription = null)
            CameraIconButton(icon = refresh, onClick = {}, contentDescription = null, active = true)
            CameraIconButton(text = "3s", onClick = {}, contentDescription = null, active = true)
            CameraIconButton(icon = refresh, onClick = {}, contentDescription = null, enabled = false)
        }
        ZoomControl(levels = listOf("0.5", "1x", "2"), selectedIndex = 1, onSelect = {})
        ZoomControl(levels = listOf("1x", "2"), selectedIndex = 0, onSelect = {})
        Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
            Box(modifier = Modifier.size(200.dp, 267.dp).background(appColors.cameraSurface)) { GridOverlay() }
            StateMessage(
                icon = painterResource(R.drawable.ic_wifi_off),
                title = "Title",
                message = "Message of a state.",
                actionLabel = "Action",
                actionIcon = refresh,
                onAction = {},
            )
        }
    }
}

/**
 * The pose components (sticker.md, section 4): chips, badge, thumbnails (normal, selected, loading, failed) and step
 * cards (first step, middle step, with a text button). Thumbnails show a placeholder figure drawn here.
 */
@Composable
fun PoseComponentCatalog(modifier: Modifier = Modifier) {
    val figure = rememberPlaceholderFigure(MaterialTheme.colorScheme.onSurfaceVariant)
    val selectedFigure = rememberPlaceholderFigure(MaterialTheme.colorScheme.onSurface)
    CatalogPage(title = "Pose components", modifier = modifier) {
        // Room above the chips and thumbnails for the badge that sticks out.
        Row(
            modifier = Modifier.padding(top = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        ) {
            StickerChip(label = "All", selected = true, onClick = {})
            StickerChip(label = "Women", selected = false, onClick = {})
            StickerChip(label = "Couple", selected = false, onClick = {})
        }
        Row(
            modifier = Modifier.padding(top = Spacing.sm),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            SelectedBadge()
            Text(text = "SelectedBadge 22", style = MaterialTheme.typography.labelLarge)
        }
        Row(
            modifier = Modifier.padding(top = Spacing.md),
            horizontalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            PoseThumb(image = figure, selected = false, onClick = {}, contentDescription = "Normal")
            PoseThumb(image = selectedFigure, selected = true, onClick = {}, contentDescription = "Selected")
            PoseThumb(image = null, selected = false, onClick = {}, contentDescription = "Loading")
            PoseThumb(image = null, selected = false, onClick = {}, contentDescription = "Failed", failed = true)
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        StepCard(
            text = "Stand with your weight on the back leg.",
            badge = "1/4",
            modifier = Modifier.fillMaxWidth(),
            trailing = { CatalogArrows(previousEnabled = false) },
        )
        StepCard(
            text = "Turn your shoulders 45 degrees to the camera, chin slightly down.",
            badge = "2/4",
            modifier = Modifier.fillMaxWidth(),
            trailing = { CatalogArrows(previousEnabled = true) },
        )
        StepCard(
            text = "Turn your shoulders 45 degrees to the camera.",
            badge = "2/4",
            modifier = Modifier.width(220.dp),
            trailing = { CatalogArrows(previousEnabled = true) },
        )
        StepCard(
            text = "The outline could not load",
            modifier = Modifier.fillMaxWidth(),
            trailing = { TextButton(onClick = {}) { Text("Retry") } },
        )
    }
}

/**
 * The pose cards of the library (sticker.md, section 4; plan T-021): unsaved, saved, a long name on two lines, and the
 * skeleton shown while the image loads. Cards are 182 dp wide, as in the two-column grid of the design.
 */
@Composable
fun PoseCardCatalog(modifier: Modifier = Modifier) {
    val figure = BitmapPainter(rememberPlaceholderFigure(MaterialTheme.appColors.onCategory))
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier.padding(Spacing.md),
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
        ) {
            Text(text = "Pose cards", style = MaterialTheme.typography.titleLarge)
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PoseCard(
                    name = "Lean on the rail",
                    meta = "Single · 4 steps",
                    image = figure,
                    imageBackground = MaterialTheme.appColors.categoryWomen,
                    saved = false,
                    saveLabel = "Save pose",
                    onSaveClick = {},
                    modifier = Modifier.weight(1f),
                )
                PoseCard(
                    name = "Look over the shoulder",
                    meta = "Single · 3 steps",
                    image = figure,
                    imageBackground = MaterialTheme.appColors.categoryMen,
                    saved = true,
                    saveLabel = "Unsave pose",
                    onSaveClick = {},
                    modifier = Modifier.weight(1f),
                )
            }
            Row(horizontalArrangement = Arrangement.spacedBy(Spacing.md)) {
                PoseCard(
                    name = "Walk hand in hand along the beach at sunset",
                    meta = "Couple · 5 steps",
                    image = figure,
                    imageBackground = MaterialTheme.appColors.categoryCouple,
                    saved = false,
                    saveLabel = "Save pose",
                    onSaveClick = {},
                    modifier = Modifier.weight(1f),
                )
                PoseCardSkeleton(modifier = Modifier.weight(1f))
            }
        }
    }
}

/**
 * The settings components (plan T-025; design settings, section 3): a section header, the four kinds of SettingsRow
 * (the switch on and off) and a ChoiceDialog with three options, drawn in a 400 dp frame. Icons are stand-ins.
 */
@Composable
fun SettingsComponentCatalog(modifier: Modifier = Modifier) {
    val icon = painterResource(R.drawable.ic_check)
    Surface(modifier = modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column {
            Text(
                text = "Settings components",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(Spacing.md),
            )
            SectionHeader(title = "SectionHeader")
            SettingsRow(icon, "Value", SettingsRowTrailing.Chevron {}, value = "System default")
            SettingsRow(icon, "Switch on", SettingsRowTrailing.Switch(checked = true) {})
            SettingsRow(icon, "Switch off", SettingsRowTrailing.Switch(checked = false) {})
            SettingsRow(icon, "Link", SettingsRowTrailing.Link {})
            SettingsRow(icon, "Info", SettingsRowTrailing.None, value = "1.0.0")
            Box(modifier = Modifier.fillMaxWidth().height(400.dp)) {
                ChoiceDialog(
                    title = "ChoiceDialog",
                    options = listOf("System default", "Light", "Dark"),
                    selectedIndex = 0,
                    onSelect = {},
                    dismissLabel = "Cancel",
                    onDismiss = {},
                )
            }
        }
    }
}

@Composable
private fun CatalogArrows(previousEnabled: Boolean) {
    StepCardArrows(
        onPrevious = {},
        onNext = {},
        previousEnabled = previousEnabled,
        nextEnabled = true,
        previousDescription = "Previous step",
        nextDescription = "Next step",
    )
}

/** A figure drawn with lines, standing in for a pose thumbnail from R2. */
@Composable
private fun rememberPlaceholderFigure(color: Color): ImageBitmap = remember(color) {
    val size = 192
    val image = ImageBitmap(size, size)
    val canvas = Canvas(image)
    val paint = Paint().apply {
        this.color = color
        style = PaintingStyle.Stroke
        strokeWidth = 4.5f
        strokeCap = StrokeCap.Round
        isAntiAlias = true
    }
    canvas.drawCircle(Offset(96f, 44f), 10f, paint)
    val body = Path().apply {
        moveTo(66f, 110f)
        lineTo(72f, 62f)
        lineTo(120f, 62f)
        lineTo(126f, 110f)
        moveTo(80f, 62f)
        lineTo(82f, 108f)
        lineTo(110f, 108f)
        lineTo(112f, 62f)
        moveTo(86f, 108f)
        lineTo(82f, 166f)
        moveTo(106f, 108f)
        lineTo(112f, 166f)
    }
    canvas.drawPath(body, paint)
    image
}

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
