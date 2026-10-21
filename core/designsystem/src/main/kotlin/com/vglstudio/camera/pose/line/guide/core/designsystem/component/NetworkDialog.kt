package com.vglstudio.camera.pose.line.guide.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.paneTitle
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.vglstudio.camera.pose.line.guide.core.designsystem.R
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

private val CardMaxWidth = 331.dp
private val IconCircleSize = 64.dp
private val IconSize = 32.dp
private const val SCRIM_ALPHA = 0.5f

/**
 * The network dialog card (spec network, design network): icon, title, message, "Retry" and, after a retry that
 * found no network, a line under the button. While [checking], the button shows a spinner and ignores clicks.
 */
@Composable
fun NetworkDialog(checking: Boolean, stillOffline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.sheet)
    val title = stringResource(R.string.network_dialog_title)
    Column(
        modifier = modifier
            .widthIn(max = CardMaxWidth)
            .fillMaxWidth()
            .background(colors.surface, shape)
            .border(Stroke.sticker, colors.outline, shape)
            // TalkBack announces the dialog by its title when it opens.
            .semantics { paneTitle = title }
            .testTag(NetworkDialogTestTags.CARD)
            .padding(Spacing.lg),
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .size(IconCircleSize)
                .background(colors.secondaryContainer, CircleShape)
                .border(Stroke.sticker, colors.outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(R.drawable.ic_wifi_off),
                contentDescription = null,
                modifier = Modifier.size(IconSize),
                tint = colors.onSecondaryContainer,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = colors.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = stringResource(R.string.network_dialog_message),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        PrimaryButton(
            text = stringResource(R.string.network_dialog_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().testTag(NetworkDialogTestTags.RETRY),
            icon = painterResource(R.drawable.ic_refresh),
            loading = checking,
        )
        if (stillOffline) {
            Text(
                text = stringResource(R.string.network_dialog_still_offline),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.testTag(NetworkDialogTestTags.STILL_OFFLINE),
            )
        }
    }
}

/**
 * The network dialog centered on a scrim that covers the whole screen and takes every touch, so nothing behind it
 * can be pressed and a touch outside the card closes nothing. Scrolls when the screen is too short.
 */
@Composable
fun NetworkDialogScreen(checking: Boolean, stillOffline: Boolean, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.cameraScrim.copy(alpha = SCRIM_ALPHA))
            .pointerInput(Unit) {
                awaitEachGesture {
                    do {
                        val event = awaitPointerEvent()
                        event.changes.forEach { it.consume() }
                    } while (event.changes.any { it.pressed })
                }
            }
            .testTag(NetworkDialogTestTags.SCREEN),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(Spacing.md),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            NetworkDialog(checking = checking, stillOffline = stillOffline, onRetry = onRetry)
        }
    }
}

object NetworkDialogTestTags {
    const val SCREEN = "network_dialog"
    const val CARD = "network_dialog_card"
    const val RETRY = "network_dialog_retry"
    const val SPINNER = PrimaryButtonTestTags.SPINNER
    const val STILL_OFFLINE = "network_dialog_still_offline"
}
