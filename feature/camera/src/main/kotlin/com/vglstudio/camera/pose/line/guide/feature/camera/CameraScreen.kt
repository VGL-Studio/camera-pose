package com.vglstudio.camera.pose.line.guide.feature.camera

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.produceState
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.repeatOnLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import com.vglstudio.camera.pose.line.guide.feature.camera.lastphoto.LastPhotoViewModel
import com.vglstudio.camera.pose.line.guide.feature.camera.layout.isUnfolded
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.OverlayToolColumn
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseActions
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseOverlay
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PosePicker
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseUiState
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseViewModel
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.toolColumnPlacement

/**
 * Stateful entry point used by navigation. Checks the camera permission each time the screen starts (also back from
 * the system settings), asks for it when the view model says so, and closes the camera when the screen stops.
 * The camera is always dark (spec camera).
 */
@Composable
internal fun CameraRoute(
    onPhotoCaptured: () -> Unit,
    onOpenPoses: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    onOpenLastPhoto: (String) -> Unit,
    viewModel: CameraViewModel = hiltViewModel(),
    poseViewModel: PoseViewModel = hiltViewModel(),
    lastPhotoViewModel: LastPhotoViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val poseState by poseViewModel.uiState.collectAsStateWithLifecycle()
    val poseActions = remember(poseViewModel) {
        PoseActions(
            onCategorySelect = poseViewModel::onCategorySelect,
            onPoseSelect = poseViewModel::onPoseSelect,
            onThumbShown = poseViewModel::onThumbShown,
            onPreviousStep = poseViewModel::onPreviousStep,
            onNextStep = poseViewModel::onNextStep,
            onHideSteps = poseViewModel::onHideSteps,
            onShowSteps = poseViewModel::onShowSteps,
            onRetryOutline = poseViewModel::onRetryOutline,
            onRetryLibrary = poseViewModel::onRetryLibrary,
            onOverlayTransform = poseViewModel::onOverlayTransform,
            onOverlayGestureEnd = poseViewModel::onOverlayGestureEnd,
            onOpacityChange = poseViewModel::onOpacityChange,
            onOpacityChangeFinished = poseViewModel::onOpacityChangeFinished,
            onScaleChange = poseViewModel::onScaleChange,
            onScaleChangeFinished = poseViewModel::onScaleChangeFinished,
            onTogglePanel = poseViewModel::onTogglePanel,
            onClosePanel = poseViewModel::onClosePanel,
            onToggleMirror = poseViewModel::onToggleMirror,
            onToggleLock = poseViewModel::onToggleLock,
            onToggleHidden = poseViewModel::onToggleHidden,
            onResetOverlay = poseViewModel::onResetOverlay,
        )
    }
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnPhotoCaptured by rememberUpdatedState(onPhotoCaptured)
    val currentOnOpenLastPhoto by rememberUpdatedState(onOpenLastPhoto)
    val lifecycleOwner = LocalLifecycleOwner.current
    val lastPhotoUri by lastPhotoViewModel.lastPhoto.collectAsStateWithLifecycle()
    val lastPhoto by produceState<ImageBitmap?>(null, lastPhotoUri) {
        value = lastPhotoUri?.let { lastPhotoViewModel.decoder.decode(it, LAST_PHOTO_MAX_SIDE_PX) }
    }
    val activity = LocalActivity.current
    // Read before the request; with the value read after it, tells whether the system dialog was shown (T-026).
    // No activity (previews only): count the dialog as shown.
    var rationaleBefore by remember { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        val dialogShown = activity == null ||
            permissionDialogShown(granted, rationaleBefore, activity.shouldShowRationale())
        viewModel.onPermissionResult(granted, dialogShown)
    }
    LifecycleStartEffect(viewModel) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onScreenStarted(granted)
        // Back from the review screen: the slot shows the photo just saved, and "Tư thế tiếp" selects the next pose.
        lastPhotoViewModel.onScreenStarted()
        if (lastPhotoViewModel.consumeNextPoseRequest()) poseViewModel.onNextPose()
        onStopOrDispose { viewModel.onScreenStopped() }
    }
    // Only while started: a photo taken just as the app went to the background opens the review screen on return.
    LaunchedEffect(viewModel, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            viewModel.events.collect { event ->
                when (event) {
                    CameraEvent.RequestPermission -> {
                        rationaleBefore = activity?.shouldShowRationale() ?: false
                        permissionLauncher.launch(Manifest.permission.CAMERA)
                    }
                    CameraEvent.PhotoCaptured -> currentOnPhotoCaptured()
                }
            }
        }
    }
    AppTheme(darkTheme = true) {
        CameraScreen(
            uiState = uiState,
            onFlashClick = viewModel::onFlashClick,
            onTimerClick = viewModel::onTimerClick,
            onGridClick = viewModel::onGridClick,
            onFlipClick = viewModel::onFlipClick,
            onZoomSelect = viewModel::onZoomSelect,
            onShutterClick = viewModel::onShutterClick,
            onCancelCountdown = viewModel::onCancelCountdown,
            onRetry = viewModel::onRetry,
            onOpenAppSettings = {
                val uri = Uri.fromParts("package", context.packageName, null)
                context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, uri))
            },
            onOpenPoses = onOpenPoses,
            onOpenSettings = onOpenSettings,
            preview = { modifier ->
                surfaceRequest?.let { CameraXViewfinder(surfaceRequest = it, modifier = modifier) }
            },
            poseState = poseState,
            poseActions = poseActions,
            lastPhoto = lastPhoto,
            onLastPhotoClick = lastPhotoUri?.let { uri -> { currentOnOpenLastPhoto(uri) } },
        )
    }
}

/**
 * Stateless screen: everything it shows comes from [uiState] and [poseState]. [lastPhoto] is the thumbnail of the
 * latest saved photo; the slot can be tapped only with [onLastPhotoClick] (T-016). Tested on the JVM and by
 * screenshots, without [preview], so the frame shows the camera surface colour. Layout from design/context/
 * (411 x 891): top bar at 32, the 3:4 preview below it (with the pose overlay when the camera is ready), the chip row
 * and pose strip, and the capture row.
 */
@Composable
internal fun CameraScreen(
    uiState: CameraUiState,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onFlipClick: () -> Unit,
    onZoomSelect: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPoses: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    modifier: Modifier = Modifier,
    preview: @Composable (Modifier) -> Unit = {},
    poseState: PoseUiState = PoseUiState.Loading,
    poseActions: PoseActions = PoseActions(),
    lastPhoto: ImageBitmap? = null,
    onLastPhotoClick: (() -> Unit)? = null,
) {
    // The scroll positions live above the choice of layout, so they survive a change of layout (plan T-020).
    val chipsState = rememberLazyListState()
    val stripState = rememberLazyListState()
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        if (isUnfolded(maxWidth)) {
            CameraUnfoldedLayout(
                uiState = uiState,
                onFlashClick = onFlashClick,
                onTimerClick = onTimerClick,
                onGridClick = onGridClick,
                onFlipClick = onFlipClick,
                onZoomSelect = onZoomSelect,
                onShutterClick = onShutterClick,
                onCancelCountdown = onCancelCountdown,
                onRetry = onRetry,
                onOpenAppSettings = onOpenAppSettings,
                onOpenPoses = onOpenPoses,
                onOpenSettings = onOpenSettings,
                preview = preview,
                poseState = poseState,
                poseActions = poseActions,
                lastPhoto = lastPhoto,
                onLastPhotoClick = onLastPhotoClick,
                chipsState = chipsState,
                stripState = stripState,
            )
        } else {
            CameraPhoneLayout(
                uiState = uiState,
                onFlashClick = onFlashClick,
                onTimerClick = onTimerClick,
                onGridClick = onGridClick,
                onFlipClick = onFlipClick,
                onZoomSelect = onZoomSelect,
                onShutterClick = onShutterClick,
                onCancelCountdown = onCancelCountdown,
                onRetry = onRetry,
                onOpenAppSettings = onOpenAppSettings,
                onOpenPoses = onOpenPoses,
                onOpenSettings = onOpenSettings,
                preview = preview,
                poseState = poseState,
                poseActions = poseActions,
                lastPhoto = lastPhoto,
                onLastPhotoClick = onLastPhotoClick,
                chipsState = chipsState,
                stripState = stripState,
            )
        }
    }
}

/** The phone layout: the whole of the screen before T-020, unchanged. */
@Composable
internal fun CameraPhoneLayout(
    uiState: CameraUiState,
    onFlashClick: () -> Unit,
    onTimerClick: () -> Unit,
    onGridClick: () -> Unit,
    onFlipClick: () -> Unit,
    onZoomSelect: (Float) -> Unit,
    onShutterClick: () -> Unit,
    onCancelCountdown: () -> Unit,
    onRetry: () -> Unit,
    onOpenAppSettings: () -> Unit,
    onOpenPoses: (() -> Unit)?,
    onOpenSettings: (() -> Unit)?,
    preview: @Composable (Modifier) -> Unit,
    poseState: PoseUiState,
    poseActions: PoseActions,
    lastPhoto: ImageBitmap?,
    onLastPhotoClick: (() -> Unit)?,
    chipsState: LazyListState,
    stripState: LazyListState,
) {
    val appColors = MaterialTheme.appColors
    val ready = uiState as? CameraUiState.Ready
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(appColors.cameraBackground)
            .testTag(CameraTestTags.SCREEN),
    ) {
        CameraTopBar(
            ready = ready,
            onFlashClick = onFlashClick,
            onTimerClick = onTimerClick,
            onGridClick = onGridClick,
            onOpenPoses = onOpenPoses,
            onOpenSettings = onOpenSettings,
        )
        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
        // The frame is 3:4 and as high as the room allows; the tools go in it or, on a short screen, in the free
        // margin beside it (plan T-015). Only a ready camera with a pose has tools.
        val overlay = (poseState as? PoseUiState.Ready)?.takeIf { it.selected != null }?.overlay
        val tools = if (ready != null) overlay else null
        var stepBottom by remember { mutableStateOf(DEFAULT_STEP_BOTTOM) }
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
            val frameWidth = minOf(maxWidth, maxHeight * PREVIEW_ASPECT_RATIO)
            val frameHeight = frameWidth / PREVIEW_ASPECT_RATIO
            val placement = toolColumnPlacement(maxWidth.value, frameWidth.value, frameHeight.value, stepBottom.value)
            val frameLeft = (maxWidth - frameWidth) / 2
            Box(
                modifier = Modifier
                    .aspectRatio(PREVIEW_ASPECT_RATIO, matchHeightConstraintsFirst = true)
                    .background(if (ready != null) appColors.cameraSurface else appColors.cameraBackground)
                    .testTag(CameraTestTags.PREVIEW),
                contentAlignment = Alignment.Center,
            ) {
                PreviewContent(
                    uiState = uiState,
                    onZoomSelect = onZoomSelect,
                    onRetry = onRetry,
                    onOpenAppSettings = onOpenAppSettings,
                    preview = preview,
                    poseOverlay = {
                        PoseOverlay(
                            state = poseState,
                            actions = poseActions,
                            hintBottomPadding = LOCK_HINT_ABOVE_ZOOM,
                            toolsOffset = if (tools != null && !placement.outsideFrame) {
                                DpOffset(placement.x.dp - frameLeft, placement.y.dp)
                            } else {
                                null
                            },
                            onStepBottomChange = { stepBottom = it },
                        )
                    },
                    zoomVisible = overlay?.panelOpen != true,
                )
            }
            if (tools != null && placement.outsideFrame) {
                OverlayToolColumn(
                    overlay = tools,
                    actions = poseActions,
                    modifier = Modifier.align(Alignment.TopStart).offset(x = placement.x.dp, y = placement.y.dp),
                )
            }
        }
        // Chip row and pose strip: 16 + 36 + 16 + 64.
        PosePicker(state = poseState, actions = poseActions, chipsState = chipsState, stripState = stripState)
        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
        CaptureRow(
            ready = ready,
            onShutterClick = onShutterClick,
            onCancelCountdown = onCancelCountdown,
            onFlipClick = onFlipClick,
            lastPhoto = lastPhoto,
            onLastPhotoClick = onLastPhotoClick,
        )
        Spacer(modifier = Modifier.height(Spacing.xl))
    }
}

internal object CameraTestTags {
    const val SCREEN = "camera_screen"
    const val PREVIEW = "camera_preview"
    const val POSES = "camera_poses"
    const val SETTINGS = "camera_settings"
    const val FLASH = "camera_flash"
    const val TIMER = "camera_timer"
    const val GRID_BUTTON = "camera_grid_button"
    const val GRID = "camera_grid"
    const val ZOOM = "camera_zoom"
    const val COUNTDOWN = "camera_countdown"
    const val SHUTTER = "camera_shutter"
    const val LAST_PHOTO = "camera_last_photo"
    const val FLIP = "camera_flip"
    const val CHIPS = "camera_chips"
    const val STRIP = "camera_strip"
    const val OUTLINE = "camera_outline"
    const val STEP_CARD = "camera_step_card"
    const val SHOW_STEPS = "camera_show_steps"
    const val NO_POSE_HINT = "camera_no_pose_hint"
    const val OVERLAY_GESTURES = "camera_overlay_gestures"
    const val OVERLAY_TOOLS = "camera_overlay_tools"
    const val TOOL_ADJUST = "camera_tool_adjust"
    const val TOOL_MIRROR = "camera_tool_mirror"
    const val TOOL_LOCK = "camera_tool_lock"
    const val TOOL_VISIBILITY = "camera_tool_visibility"
    const val ADJUST_PANEL = "camera_adjust_panel"
    const val OPACITY_SLIDER = "camera_opacity_slider"
    const val SIZE_SLIDER = "camera_size_slider"
    const val LOCK_HINT = "camera_lock_hint"

    /** Tag of the strip cell of a pose: `camera_pose_<id>`. */
    fun pose(id: String): String = "camera_pose_$id"
}

internal const val PREVIEW_ASPECT_RATIO = 3f / 4f

// Where the step card ends in the frame before it is measured: one row, 16 + 68 (design context).
internal val DEFAULT_STEP_BOTTOM = 84.dp

// The lock hint is 16 above the zoom control, which is 16 from the bottom of the frame and 48 high.
private val LOCK_HINT_ABOVE_ZOOM = Spacing.md + 48.dp + Spacing.md

/** Long side of the last photo thumbnail, in pixels (plan T-016). */
private const val LAST_PHOTO_MAX_SIDE_PX = 192

private fun Activity.shouldShowRationale(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
