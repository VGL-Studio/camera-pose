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
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.stickerShadow

private val MinHeight = 56.dp
private val IconSize = 20.dp
private val SpinnerStroke = 2.dp

/**
 * Main call-to-action button in the sticker style: filled with the secondary color, 2 dp outline and a hard
 * shadow while enabled. Minimum height 56 dp keeps the touch target above 48 dp.
 * An optional [icon] is drawn before the text, in the button's content color. While [loading], a spinner takes the
 * place of the icon, the button keeps its enabled look and clicks are ignored.
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
        onClick = { if (!loading) onClick() },
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
        ButtonContent(text = text, icon = icon, loading = loading)
    }
}

/** Icon (optional) or spinner, and text shared by [PrimaryButton] and [SecondaryButton]. */
@Composable
internal fun ButtonContent(text: String, icon: Painter?, loading: Boolean = false) {
    if (loading) {
        CircularProgressIndicator(
            modifier = Modifier.size(IconSize).testTag(PrimaryButtonTestTags.SPINNER),
            color = LocalContentColor.current,
            strokeWidth = SpinnerStroke,
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
    } else if (icon != null) {
        Icon(painter = icon, contentDescription = null, modifier = Modifier.size(IconSize))
        Spacer(modifier = Modifier.width(Spacing.sm))
    }
    Text(text = text, style = MaterialTheme.typography.titleMedium)
}

/** Tags for UI tests of screens that use [PrimaryButton]. */
object PrimaryButtonTestTags {
    const val SPINNER = "primary_button_spinner"
}
