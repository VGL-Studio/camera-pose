package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesBottomBanner
import com.vglstudio.camera.pose.line.guide.feature.poses.R
import com.vglstudio.camera.pose.line.guide.feature.poses.screenLocale

object PoseDetailTestTags {
    const val SCREEN = "pose_detail_screen"
    const val IMAGE = "pose_detail_image"
    const val BOTTOM_BAR = "pose_detail_bottom_bar"
}

@Composable
fun PoseDetailRoute(
    onBack: () -> Unit,
    onShootWithPose: (String) -> Unit,
    viewModel: PoseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    PoseDetailScreen(
        uiState = uiState,
        onBack = onBack,
        onToggleSave = viewModel::onToggleSave,
        onRetryImage = viewModel::onRetryImage,
        onShoot = { viewModel.onShoot()?.let(onShootWithPose) },
        onSaveFailedShown = viewModel::onSaveFailedShown,
        bottomBanner = { PosesBottomBanner(AdPlacement.POSES_DETAIL_BANNER) },
    )
}

/**
 * The pose detail (spec poses, KB-3, KB-4, KB-5, KB-6; design README). [bottomBanner] sits under the main button bar,
 * once the pose is shown.
 */
@Composable
fun PoseDetailScreen(
    uiState: PoseDetailUiState,
    onBack: () -> Unit,
    onToggleSave: () -> Unit,
    onRetryImage: () -> Unit,
    onShoot: () -> Unit,
    onSaveFailedShown: () -> Unit,
    modifier: Modifier = Modifier,
    bottomBanner: @Composable () -> Unit = {},
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val ready = uiState as? PoseDetailUiState.Ready
    val locale = screenLocale()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PoseDetailTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            PoseDetailTopBar(
                title = ready?.pose?.name?.resolve(locale),
                saved = ready?.saved,
                onBack = onBack,
                onToggleSave = onToggleSave,
            )
            when (uiState) {
                PoseDetailUiState.Loading -> Box(
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                }
                PoseDetailUiState.Disabled, PoseDetailUiState.Missing -> {
                    // The flag was turned off, or the pose is gone: back, once.
                    val currentOnBack by rememberUpdatedState(onBack)
                    LaunchedEffect(Unit) { currentOnBack() }
                }
                is PoseDetailUiState.Ready -> {
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = Spacing.md),
                        verticalArrangement = Arrangement.spacedBy(Spacing.md),
                    ) {
                        PoseDetailImage(uiState.pose.categoryId, uiState.image, onRetryImage)
                        PoseDetailMeta(uiState.pose)
                        PoseDetailSteps(uiState.pose.steps.resolve(locale), locale)
                        val mistakes = uiState.pose.mistakes.resolve(locale)
                        if (mistakes.isNotEmpty()) PoseDetailMistakes(mistakes)
                        Spacer(modifier = Modifier.height(Spacing.md))
                    }
                    PoseDetailBottomBar(onShoot = onShoot)
                    bottomBanner()
                }
            }
        }
        SnackbarHost(hostState = snackbarHostState, modifier = Modifier.align(Alignment.BottomCenter))
    }
    if (ready?.saveFailed == true) {
        val message = stringResource(R.string.poses_save_failed)
        val currentOnShown by rememberUpdatedState(onSaveFailedShown)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            currentOnShown()
        }
    }
}
