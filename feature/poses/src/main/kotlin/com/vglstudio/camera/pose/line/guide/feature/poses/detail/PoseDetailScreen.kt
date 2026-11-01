package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
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
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarScaffold
import com.vglstudio.camera.pose.line.guide.core.ads.RewardedAdDialog
import com.vglstudio.camera.pose.line.guide.core.ads.rememberRewardedAction
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignR
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PremiumUnlockContent
import com.vglstudio.camera.pose.line.guide.feature.poses.R
import com.vglstudio.camera.pose.line.guide.feature.poses.isWide
import com.vglstudio.camera.pose.line.guide.feature.poses.screenLocale

object PoseDetailTestTags {
    const val SCREEN = "pose_detail_screen"
    const val IMAGE = "pose_detail_image"
    const val BOTTOM_BAR = "pose_detail_bottom_bar"
    const val UNLOCK = "pose_detail_unlock"
}

@Composable
fun PoseDetailRoute(
    onBack: () -> Unit,
    onShootWithPose: (String) -> Unit,
    viewModel: PoseDetailViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    // The interstitial before the camera (KB-4): the camera opens once the ad has closed, or at once without one.
    val shoot = rememberShootWithAd(AdPlacement.INTER_POSE_DETAIL, onShootWithPose)
    // The unlock of a premium pose (KB-8): the rewarded dialog of the framework, or the unlock at once when its key is
    // off or consent is closed. Once unlocked, the camera opens with the pose, with no interstitial (question 5).
    val unlock = rememberRewardedAction(AdPlacement.RW_PREMIUM_POSE, onGranted = viewModel::onUnlockGranted)
    val currentOnShootWithPose by rememberUpdatedState(onShootWithPose)
    // Collected in the composition, on the main thread, so navigating is safe (L-030).
    LaunchedEffect(viewModel) { viewModel.openCamera.collect { currentOnShootWithPose(it) } }
    val locale = screenLocale()
    RewardedAdDialog(unlock, title = stringResource(DesignR.string.premium_unlock_title)) {
        PremiumUnlockContent(poseName = (uiState as? PoseDetailUiState.Ready)?.pose?.name?.resolve(locale).orEmpty())
    }
    // The native bottom bar over the whole screen, below the main button bar (KB-6; spec ads, question 8). It can show
    // while the pose still loads (plan T-079, section 8, question 6).
    BottomAdBarScaffold(AdPlacement.POSE_DETAIL_BOTTOM) { padding ->
        PoseDetailScreen(
            uiState = uiState,
            onBack = onBack,
            onToggleSave = viewModel::onToggleSave,
            onRetryImage = viewModel::onRetryImage,
            onShoot = { shoot(viewModel::onShoot) },
            onSaveFailedShown = viewModel::onSaveFailedShown,
            contentPadding = padding,
            onUnlock = unlock::start,
            onUnlockFailedShown = viewModel::onUnlockFailedShown,
        )
    }
}

/**
 * The pose detail (spec poses, KB-3, KB-4, KB-5, KB-6; design README). Only the bottom of [contentPadding] is used: the
 * room the bottom ad bar keeps, so the main button bar and the snackbar stay above it (KB-6). A locked premium pose
 * shows the premium badge and the unlock button, which calls [onUnlock] (KB-8).
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
    contentPadding: PaddingValues = PaddingValues(),
    onUnlock: () -> Unit = {},
    onUnlockFailedShown: () -> Unit = {},
) {
    val bottomReserve = contentPadding.calculateBottomPadding()
    val snackbarHostState = remember { SnackbarHostState() }
    val ready = uiState as? PoseDetailUiState.Ready
    val locale = screenLocale()
    // One scroll state for both layouts: the reading position stays when the screen is folded or unfolded.
    val scrollState = rememberScrollState()
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .testTag(PoseDetailTestTags.SCREEN),
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(bottom = bottomReserve)) {
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
                is PoseDetailUiState.Ready -> BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isWide()) {
                        WideDetailLayout(uiState, locale, scrollState, onRetryImage, onShoot, onUnlock)
                    } else {
                        PhoneDetailLayout(uiState, locale, scrollState, onRetryImage, onShoot, onUnlock)
                    }
                }
            }
        }
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = bottomReserve),
        )
    }
    if (ready?.saveFailed == true) {
        val message = stringResource(R.string.poses_save_failed)
        val currentOnShown by rememberUpdatedState(onSaveFailedShown)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            currentOnShown()
        }
    }
    if (ready?.unlockFailed == true) {
        val message = stringResource(R.string.poses_unlock_failed)
        val currentOnShown by rememberUpdatedState(onUnlockFailedShown)
        LaunchedEffect(Unit) {
            snackbarHostState.showSnackbar(message)
            currentOnShown()
        }
    }
}
