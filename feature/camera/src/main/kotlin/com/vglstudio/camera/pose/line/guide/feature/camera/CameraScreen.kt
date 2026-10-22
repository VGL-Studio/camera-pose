package com.vglstudio.camera.pose.line.guide.feature.camera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.camera.compose.CameraXViewfinder
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors

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
    viewModel: CameraViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val surfaceRequest by viewModel.surfaceRequest.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val currentOnPhotoCaptured by rememberUpdatedState(onPhotoCaptured)
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
        viewModel.onPermissionResult(it)
    }
    LifecycleStartEffect(viewModel) {
        val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
            PackageManager.PERMISSION_GRANTED
        viewModel.onScreenStarted(granted)
        onStopOrDispose { viewModel.onScreenStopped() }
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                CameraEvent.RequestPermission -> permissionLauncher.launch(Manifest.permission.CAMERA)
                CameraEvent.PhotoCaptured -> currentOnPhotoCaptured()
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
        )
    }
}

/**
 * Stateless screen: everything it shows comes from [uiState]. Tested on the JVM and by screenshots, without [preview],
 * so the frame shows the camera surface colour. Layout from design/context/ (411 x 891): top bar at 32, the 3:4
 * preview below it, the space of the chip row and pose strip (empty until T-014), and the capture row.
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
) {
    val appColors = MaterialTheme.appColors
    val ready = uiState as? CameraUiState.Ready
    Column(
        modifier = modifier
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
        Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.TopCenter) {
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
                )
            }
        }
        // Chip row and pose strip (T-014): 16 + 36 + 16 + 64.
        Spacer(modifier = Modifier.height(POSE_AREA_HEIGHT))
        Spacer(modifier = Modifier.height(Spacing.sm + Spacing.xs))
        CaptureRow(
            ready = ready,
            onShutterClick = onShutterClick,
            onCancelCountdown = onCancelCountdown,
            onFlipClick = onFlipClick,
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
}

private const val PREVIEW_ASPECT_RATIO = 3f / 4f
