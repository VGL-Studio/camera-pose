package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

/**
 * The content of the unlock dialog of a premium pose (spec poses, KB-8; design PoseUnlockDialogScreen_default): the
 * pose name, then why to watch an ad, [Spacing.sm] apart. The content of RewardedAdDialog in the detail and the camera,
 * so the text is translated once.
 */
@Composable
fun PremiumUnlockContent(poseName: String, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        Text(text = poseName, style = MaterialTheme.typography.titleMedium, color = colors.onSurface)
        Text(
            text = stringResource(R.string.premium_unlock_message),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
        )
    }
}
