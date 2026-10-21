package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.stickerShadow

private val MinHeight = 56.dp
private val IconSize = 20.dp

/**
 * Main call-to-action button in the sticker style: filled with the secondary color, 2 dp outline and a hard
 * shadow while enabled. Minimum height 56 dp keeps the touch target above 48 dp.
 * An optional [icon] is drawn before the text, in the button's content color.
 */
@Composable
fun PrimaryButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: Painter? = null,
    loading: Boolean = false,
) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.button)
    Button(
        onClick = onClick,
        enabled = enabled,
        shape = shape,
        colors = ButtonDefaults.buttonColors(
            containerColor = colors.secondary,
            contentColor = colors.onSecondary,
        ),
        elevation = null,
        border = BorderStroke(Stroke.sticker, if (enabled) colors.outline else colors.outlineVariant),
        contentPadding = PaddingValues(horizontal = Spacing.lg),
        modifier = modifier
            .then(if (enabled) Modifier.stickerShadow(shape, MaterialTheme.appColors.shadowSticker) else Modifier)
            .heightIn(min = MinHeight),
    ) {
        ButtonContent(text = text, icon = icon)
    }
}

/** Icon (optional) and text shared by [PrimaryButton] and [SecondaryButton]. */
@Composable
internal fun ButtonContent(text: String, icon: Painter?) {
    if (icon != null) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(IconSize))
        Spacer(modifier = Modifier.width(Spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

object PrimaryButtonTestTags {
    const val SPINNER = "primary_button_spinner"
}
