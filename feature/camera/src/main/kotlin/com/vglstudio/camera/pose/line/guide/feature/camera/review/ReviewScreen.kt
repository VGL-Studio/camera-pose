package com.vglstudio.camera.pose.line.guide.feature.camera.review

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.core.app.ActivityCompat
import androidx.core.content.FileProvider
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.rememberShowInterstitial
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.core.review.rememberRequestInAppReview
import com.vglstudio.camera.pose.line.guide.feature.camera.R
import com.vglstudio.camera.pose.line.guide.feature.camera.layout.isUnfolded
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.STORAGE_PERMISSION
import com.vglstudio.camera.pose.line.guide.feature.camera.photo.storagePermissionBlocked
import com.vglstudio.camera.pose.line.guide.feature.camera.review.ReviewUiState.Ready
import java.io.File

/**
 * Stateful entry point used by navigation. Decodes the photo off the main thread, opens the share sheet, asks for the
 * storage permission on Android 9 and lower, and turns Back into the close button (spec camera, KB-6). Events are
 * collected only while the screen is started. Always dark, like the camera.
 */
@Composable
internal fun ReviewRoute(onLeave: () -> Unit, viewModel: ReviewViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val location = (uiState as? Ready)?.location
    val image by produceState<ImageBitmap?>(null, location) {
        value = location?.let { viewModel.decoder.decode(it, REVIEW_IMAGE_MAX_SIDE_PX) }
    }
    val context = LocalContext.current
    val activity = LocalActivity.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val currentOnLeave by rememberUpdatedState(onLeave)
    val requestReview = rememberRequestInAppReview()
    val savedCount by viewModel.savedCount.collectAsStateWithLifecycle()
    // Read when the exit ends, not when the route was composed: the last save may have just been counted.
    val currentSavedCount by rememberUpdatedState(savedCount)
    val currentRequestReview by rememberUpdatedState(requestReview)
    val askReview = { currentRequestReview(currentSavedCount) }
    val showAd = rememberShowInterstitial(AdPlacement.REVIEW_INTERSTITIAL) { shown ->
        afterReviewAd(shown, currentOnLeave, askReview)
    }
    val shareTitle = stringResource(R.string.review_share_title)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // No activity (previews only): count asking again as possible.
        val rationaleAfter = activity?.let {
            ActivityCompat.shouldShowRequestPermissionRationale(it, STORAGE_PERMISSION)
        } ?: true
        viewModel.onStoragePermissionResult(granted, storagePermissionBlocked(granted, rationaleAfter))
    }
    LifecycleStartEffect(viewModel) {
        viewModel.onScreenStarted()
        onStopOrDispose {}
    }
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    is ReviewEvent.Leave -> leaveReview(event.savedThisVisit, showAd, currentOnLeave, askReview)
                    is ReviewEvent.Share -> share(context, event.location, shareTitle, viewModel)
                    ReviewEvent.RequestStoragePermission -> permissionLauncher.launch(STORAGE_PERMISSION)
                    ReviewEvent.OpenAppSettings -> {
                        val uri = Uri.fromParts("package", context.packageName, null)
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
                    }
                }
            }
        }
    }
    val dialogOpen = (uiState as? Ready)?.pendingExit != null
    BackHandler {
        if (dialogOpen) viewModel.onDismissDialog() else viewModel.onCloseClick()
    }
    AppTheme(darkTheme = true) {
        ReviewScreen(
            uiState = uiState,
            image = image,
            onCloseClick = viewModel::onCloseClick,
            onSaveClick = viewModel::onSaveClick,
            onRetrySave = viewModel::onRetrySave,
            onOpenAppSettings = viewModel::onOpenAppSettings,
            onRetakeClick = viewModel::onRetakeClick,
            onShareClick = viewModel::onShareClick,
            onNextPoseClick = viewModel::onNextPoseClick,
            onDiscardConfirm = viewModel::onDiscardConfirm,
            onDiscardSave = viewModel::onDiscardSave,
            onDismissDialog = viewModel::onDismissDialog,
        )
    }
}

/** A cache file goes through the FileProvider of this module; a saved photo is shared by its MediaStore uri. */
private fun share(context: Context, location: String, title: String, viewModel: ReviewViewModel) {
    try {
        val uri = if (location.startsWith("content://")) {
            Uri.parse(location)
        } else {
            FileProvider.getUriForFile(context, "${context.packageName}$CAPTURES_AUTHORITY_SUFFIX", File(location))
        }
        context.startActivity(shareChooser(uri, title))
    } catch (e: ActivityNotFoundException) {
        viewModel.onShareFailed(e)
    } catch (e: IllegalArgumentException) {
        viewModel.onShareFailed(e)
    }
}

/**
 * Stateless screen. Layout from design/context/ReviewScreen_*.md (411 x 891), measured from the top of the frame: on a
 * phone the navigation host keeps it below the status bar. Top bar, the 3:4 photo with the three actions under it,
 * then the save error (if any) and the main button. The photo shrinks on short screens; the rest keeps its size.
 */
@Composable
internal fun ReviewScreen(
    uiState: ReviewUiState,
    image: ImageBitmap?,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit,
    onRetrySave: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetakeClick: () -> Unit,
    onShareClick: () -> Unit,
    onNextPoseClick: () -> Unit,
    onDiscardConfirm: () -> Unit,
    onDiscardSave: () -> Unit,
    onDismissDialog: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val ready = uiState as? Ready
    val saving = ready?.save == SaveStatus.SAVING
    val dialogOpen = ready?.pendingExit != null
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.appColors.cameraBackground)
            .testTag(ReviewTestTags.SCREEN),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                // While the dialog shows, screen readers only see the dialog (L-004).
                .then(if (dialogOpen) Modifier.clearAndSetSemantics {} else Modifier),
        ) {
            ReviewTopBar(enabled = !saving, onCloseClick = onCloseClick)
            if (ready != null) {
                BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    if (isUnfolded(maxWidth)) {
                        ReviewUnfoldedContent(
                            ready = ready,
                            image = image,
                            onSaveClick = onSaveClick,
                            onRetrySave = onRetrySave,
                            onOpenAppSettings = onOpenAppSettings,
                            onRetakeClick = onRetakeClick,
                            onShareClick = onShareClick,
                            onNextPoseClick = onNextPoseClick,
                        )
                    } else {
                        ReviewPhoneContent(
                            ready = ready,
                            image = image,
                            onSaveClick = onSaveClick,
                            onRetrySave = onRetrySave,
                            onOpenAppSettings = onOpenAppSettings,
                            onRetakeClick = onRetakeClick,
                            onShareClick = onShareClick,
                            onNextPoseClick = onNextPoseClick,
                        )
                    }
                }
            }
        }
        if (dialogOpen) {
            DiscardDialog(
                onDiscardConfirm = onDiscardConfirm,
                onDiscardSave = onDiscardSave,
                onDismissDialog = onDismissDialog,
            )
        }
    }
}

/** The phone layout of what is under the top bar: the photo, the actions, the save error and the main button. */
@Composable
internal fun ReviewPhoneContent(
    ready: Ready,
    image: ImageBitmap?,
    onSaveClick: () -> Unit,
    onRetrySave: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onRetakeClick: () -> Unit,
    onShareClick: () -> Unit,
    onNextPoseClick: () -> Unit,
) {
    val saving = ready.save == SaveStatus.SAVING
    Column(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = Spacing.md),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            ReviewPhoto(image = image, modifier = Modifier.weight(1f, fill = false))
            Spacer(modifier = Modifier.height(Spacing.lg))
            ReviewActions(
                enabled = !saving,
                onRetakeClick = onRetakeClick,
                onShareClick = onShareClick,
                onNextPoseClick = onNextPoseClick,
            )
            // Keeps the save error clear of the actions on short screens; on taller ones this is free space.
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        if (ready.save in ERROR_STATUSES) {
            SaveErrorMessage(
                save = ready.save,
                onRetrySave = onRetrySave,
                onOpenAppSettings = onOpenAppSettings,
                modifier = Modifier.padding(horizontal = Spacing.md),
            )
            Spacer(modifier = Modifier.height(Spacing.md))
        }
        PrimaryButton(
            text = stringResource(if (saving) R.string.review_saving else R.string.review_save),
            onClick = onSaveClick,
            modifier = Modifier
                .padding(horizontal = Spacing.md)
                .fillMaxWidth()
                .testTag(ReviewTestTags.SAVE),
            enabled = ready.save != SaveStatus.SAVED,
            icon = painterResource(R.drawable.ic_review_download),
            loading = saving,
        )
        Spacer(modifier = Modifier.height(Spacing.md + Spacing.xs))
    }
}

internal object ReviewTestTags {
    const val SCREEN = "review_screen"
    const val CLOSE = "review_close"
    const val PHOTO = "review_photo"
    const val ACTIONS = "review_actions"
    const val RETAKE = "review_retake"
    const val SHARE = "review_share"
    const val NEXT_POSE = "review_next_pose"
    const val SAVE_ERROR = "review_save_error"
    const val SAVE = "review_save"
    const val DISCARD_DIALOG = "review_discard_dialog"
}

/** Authority of the FileProvider in this module's manifest: `<applicationId>.captures`. */
private const val CAPTURES_AUTHORITY_SUFFIX = ".captures"

/** Long side of the photo on the review screen, in pixels (plan T-016). */
private const val REVIEW_IMAGE_MAX_SIDE_PX = 1600

internal val ERROR_STATUSES = setOf(
    SaveStatus.FAILED_STORAGE_FULL,
    SaveStatus.FAILED,
    SaveStatus.PERMISSION_DENIED,
    SaveStatus.PERMISSION_BLOCKED,
)
