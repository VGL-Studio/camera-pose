package com.vglstudio.camera.pose.line.guide.feature.loading

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.StartupAdTurn
import com.vglstudio.camera.pose.line.guide.core.ads.rememberShowStartupAd
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.BrandLogo
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.ProgressBar
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlin.math.roundToInt

/** Stateful entry point used by navigation. Calls [onFinished] once when the state becomes Finished. */
@Composable
internal fun LoadingRoute(
    onFinished: () -> Unit,
    viewModel: LoadingViewModel = hiltViewModel(),
    @Suppress("UNUSED_PARAMETER") showAd: (StartupAdTurn, (Boolean) -> Unit) -> Unit = rememberShowStartupAd(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    if (uiState == LoadingUiState.Finished) {
        // Draw nothing: with the flag off, the app opens straight on the start screen (spec loading, KB-4).
        LaunchedEffect(Unit) { currentOnFinished() }
        return
    }
    // The name of the running app (app_name of :app), so the feature neither depends on :app nor hardcodes it.
    val context = LocalContext.current
    val appName = remember(context) { context.applicationInfo.loadLabel(context.packageManager).toString() }
    LoadingScreen(uiState = uiState, appName = appName, onRetry = viewModel::onRetry)
}

/**
 * Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots.
 * Layout follows the Sticker design (docs/features/loading/design/README.md, section 3; numbers from design/context/):
 * the logo starts at 200 of 891 of the screen height in every state, so it does not move when the error block
 * appears below the bar, and the content is at most 318 wide, centred, on the theme background.
 * On short screens the top space shrinks, and the content and footer scroll together (section 6, T-005).
 */
@Composable
internal fun LoadingScreen(
    uiState: LoadingUiState,
    appName: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val progress = when (uiState) {
        is LoadingUiState.InProgress -> uiState.progress
        is LoadingUiState.Failed -> uiState.progress
        LoadingUiState.Finished -> 1f
    }
    val failed = uiState is LoadingUiState.Failed
    val cause = (uiState as? LoadingUiState.Failed)?.cause ?: LoadingFailure.OFFLINE
    Surface(
        modifier = modifier.fillMaxSize().testTag(LoadingTestTags.SCREEN),
        color = MaterialTheme.colorScheme.background,
    ) {
        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
            val screenHeight = maxHeight
            // Does not depend on the state, so the logo stays in place when the error block appears.
            val topSpace = if (screenHeight >= TALL_SCREEN_MIN_HEIGHT) {
                screenHeight * CONTENT_TOP_FRACTION
            } else {
                screenHeight * SHORT_SCREEN_TOP_FRACTION
            }
            Column(modifier = Modifier.fillMaxSize().verticalScroll(rememberScrollState())) {
                // At least the screen height, so the footer stays at the bottom while the content is short and
                // moves below the content when it is longer than the screen.
                Column(
                    modifier = Modifier.fillMaxWidth().heightIn(min = screenHeight),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    LoadingContent(
                        progress = progress,
                        failed = failed,
                        cause = cause,
                        appName = appName,
                        onRetry = onRetry,
                        modifier = Modifier
                            .padding(top = topSpace)
                            .padding(horizontal = Spacing.md)
                            .widthIn(max = CONTENT_MAX_WIDTH)
                            .fillMaxWidth(),
                    )
                    LoadingFooter(modifier = Modifier.padding(top = Spacing.lg, bottom = Spacing.xl))
                }
            }
        }
    }
}

/** Logo, app name, tagline, progress, and either the guidance line or the error block. */
@Composable
private fun LoadingContent(
    progress: Float,
    failed: Boolean,
    cause: LoadingFailure,
    appName: String,
    onRetry: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    val typography = MaterialTheme.typography
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        BrandLogo(modifier = Modifier.testTag(LoadingTestTags.LOGO))
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = appName,
            style = typography.headlineLarge,
            color = colors.onBackground,
            textAlign = TextAlign.Center,
            modifier = Modifier.testTag(LoadingTestTags.APP_NAME),
        )
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(R.string.loading_tagline),
            style = typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(Spacing.xl + Spacing.sm))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(if (failed) R.string.loading_status_failed else R.string.loading_status),
                style = typography.labelLarge,
                color = if (failed) colors.error else colors.onSurfaceVariant,
            )
            Text(
                text = stringResource(R.string.loading_percent, (progress * PERCENT).roundToInt()),
                style = typography.bodyMedium,
                color = if (failed) colors.error else colors.onSurface,
                modifier = Modifier.testTag(LoadingTestTags.PERCENT),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.md))
        if (failed) {
            ProgressBar(
                progress = { progress },
                modifier = Modifier.testTag(LoadingTestTags.PROGRESS),
                color = colors.error,
                trackColor = colors.errorContainer,
            )
            Spacer(modifier = Modifier.height(Spacing.lg))
            LoadingError(cause = cause, onRetry = onRetry)
        } else {
            ProgressBar(progress = { progress }, modifier = Modifier.testTag(LoadingTestTags.PROGRESS))
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.loading_guidance),
                style = typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

/**
 * Error block below the progress bar: title with an alert icon, guidance, and the retry button. The guidance
 * depends on [cause]: check the network, or try again later when the device is online (plan T-057).
 */
@Composable
private fun LoadingError(cause: LoadingFailure, onRetry: () -> Unit) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().testTag(LoadingTestTags.ERROR),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                painter = painterResource(R.drawable.ic_loading_alert),
                contentDescription = null,
                tint = colors.error,
                modifier = Modifier.size(ALERT_ICON_SIZE),
            )
            Spacer(modifier = Modifier.width(Spacing.sm))
            Text(
                text = stringResource(R.string.loading_error_title),
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface,
            )
        }
        Spacer(modifier = Modifier.height(Spacing.sm))
        Text(
            text = stringResource(
                if (cause == LoadingFailure.SERVER) R.string.loading_error_server else R.string.loading_error,
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        // Design: 8 + an empty 8 frame + 8 = 24.
        Spacer(modifier = Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.loading_retry),
            onClick = onRetry,
            modifier = Modifier.fillMaxWidth().testTag(LoadingTestTags.RETRY),
            icon = painterResource(R.drawable.ic_loading_retry),
        )
    }
}

/** Footer at the bottom of the screen, or below the content when it is longer: lock icon and privacy line. */
@Composable
private fun LoadingFooter(modifier: Modifier = Modifier) {
    val color = MaterialTheme.colorScheme.onSurfaceVariant
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically) {
        Icon(
            painter = painterResource(R.drawable.ic_loading_lock),
            contentDescription = null,
            tint = color,
            modifier = Modifier.size(LOCK_ICON_SIZE),
        )
        Spacer(modifier = Modifier.width(Spacing.sm))
        Text(
            text = stringResource(R.string.loading_footer),
            style = MaterialTheme.typography.labelSmall,
            color = color,
        )
    }
}

private const val PERCENT = 100

// The logo starts at 200 of 891 of the screen height (design/context/).
private const val CONTENT_TOP_FRACTION = 200f / 891f

// Below this height the quarter-height top space pushes the error block under the footer (FB-20261017-01), so
// the top space shrinks to SHORT_SCREEN_TOP_FRACTION (L-028, T-005).
private val TALL_SCREEN_MIN_HEIGHT = 800.dp
private const val SHORT_SCREEN_TOP_FRACTION = 0.08f

// Sizes from design/context/.
private val ALERT_ICON_SIZE = 20.dp
private val LOCK_ICON_SIZE = 14.dp
private val CONTENT_MAX_WIDTH = 318.dp

internal object LoadingTestTags {
    const val SCREEN = "loading_screen"
    const val LOGO = "loading_logo"
    const val PROGRESS = "loading_progress"
    const val PERCENT = "loading_percent"
    const val ERROR = "loading_error"
    const val APP_NAME = "loading_app_name"
    const val RETRY = "loading_retry"
}

// App name of the design, for previews only.
private const val PREVIEW_APP_NAME = "Camera Pose: Lines and Guides"

@Preview
@Composable
private fun LoadingScreenPreview() {
    AppTheme {
        LoadingScreen(uiState = LoadingUiState.InProgress(0.68f), appName = PREVIEW_APP_NAME, onRetry = {})
    }
}

@Preview
@Composable
private fun LoadingScreenFailedPreview() {
    AppTheme {
        LoadingScreen(uiState = LoadingUiState.Failed(0.68f), appName = PREVIEW_APP_NAME, onRetry = {})
    }
}

@Preview
@Composable
private fun LoadingScreenFailedServerPreview() {
    AppTheme {
        LoadingScreen(
            uiState = LoadingUiState.Failed(0.68f, LoadingFailure.SERVER),
            appName = PREVIEW_APP_NAME,
            onRetry = {},
        )
    }
}
