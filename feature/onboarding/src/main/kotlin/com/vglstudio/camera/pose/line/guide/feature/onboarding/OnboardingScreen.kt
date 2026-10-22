package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.DrawableRes
import androidx.annotation.StringRes
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

/**
 * Stateful entry point used by navigation. Asks for the camera permission when the view model says so and tells
 * whether the system dialog was really shown (T-026 rule, plan T-012 version 2). "Should show rationale" read before
 * the request is kept with rememberSaveable, so it survives an activity recreated while the dialog is open (plan
 * T-012, question 12; the camera uses remember). No BackHandler: Back closes the app. The theme follows the device.
 */
@Composable
internal fun OnboardingRoute(
    onStarted: () -> Unit,
    onFinished: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel(),
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = LocalActivity.current
    val currentOnStarted by rememberUpdatedState(onStarted)
    val currentOnFinished by rememberUpdatedState(onFinished)
    var rationaleBefore by rememberSaveable { mutableStateOf(false) }
    val permissionLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        // No activity (previews only): count the dialog as shown.
        val dialogShown = activity == null ||
            permissionDialogShown(granted, rationaleBefore, activity.shouldShowRationale())
        viewModel.onPermissionResult(granted, dialogShown)
    }
    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                OnboardingEvent.Started -> currentOnStarted()
                OnboardingEvent.RequestPermission -> {
                    rationaleBefore = activity?.shouldShowRationale() ?: false
                    permissionLauncher.launch(Manifest.permission.CAMERA)
                }
                OnboardingEvent.Finished -> currentOnFinished()
            }
        }
    }
    OnboardingScreen(
        uiState = uiState,
        onEnableCamera = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            viewModel.onEnableCameraClick(granted)
        },
    )
}

/**
 * Stateless screen. Layout from design/context/ (411 x 891): the illustration, text and three points scroll; the
 * button and the note are pinned at the bottom. On a short screen the illustration shrinks first (down to
 * [MinIllustrationHeight]), then the rest scrolls (design README, section 6).
 */
@Composable
internal fun OnboardingScreen(uiState: OnboardingUiState, onEnableCamera: () -> Unit, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(OnboardingTestTags.SCREEN)
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl + Spacing.lg, bottom = Spacing.lg),
    ) {
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth()) {
            val viewportHeight = maxHeight
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .testTag(OnboardingTestTags.CONTENT),
            ) {
                ShrinkFirstLayout(
                    viewportHeight = viewportHeight,
                    illustration = {
                        OnboardingIllustration(
                            modifier = Modifier.fillMaxSize().testTag(OnboardingTestTags.ILLUSTRATION),
                        )
                    },
                    text = { OnboardingText() },
                )
            }
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        PrimaryButton(
            text = stringResource(R.string.onboarding_enable_camera),
            onClick = onEnableCamera,
            icon = painterResource(R.drawable.ic_onboarding_photo_camera),
            loading = uiState !is OnboardingUiState.Ready,
            modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.ENABLE_CAMERA),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.onboarding_note),
            style = MaterialTheme.typography.bodySmall,
            color = colors.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.NOTE),
        )
    }
}

/** Title, introduction and the three points. */
@Composable
private fun OnboardingText() {
    val colors = MaterialTheme.colorScheme
    Column {
        Text(
            text = stringResource(R.string.onboarding_title),
            style = MaterialTheme.typography.headlineMedium,
            color = colors.onBackground,
            modifier = Modifier.testTag(OnboardingTestTags.TITLE),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Text(
            text = stringResource(R.string.onboarding_body),
            style = MaterialTheme.typography.bodyLarge,
            color = colors.onSurfaceVariant,
            modifier = Modifier.testTag(OnboardingTestTags.BODY),
        )
        Spacer(modifier = Modifier.height(Spacing.lg))
        Column(
            verticalArrangement = Arrangement.spacedBy(Spacing.md),
            modifier = Modifier.testTag(OnboardingTestTags.FEATURES),
        ) {
            FeatureRow(
                icon = R.drawable.ic_onboarding_accessibility_new,
                text = R.string.onboarding_point_poses,
                tag = OnboardingTestTags.FEATURE_POSES,
            )
            FeatureRow(
                icon = R.drawable.ic_onboarding_tune,
                text = R.string.onboarding_point_adjust,
                tag = OnboardingTestTags.FEATURE_ADJUST,
            )
            FeatureRow(
                icon = R.drawable.ic_onboarding_bookmark,
                text = R.string.onboarding_point_saved,
                tag = OnboardingTestTags.FEATURE_SAVED,
            )
        }
    }
}

@Composable
private fun FeatureRow(@DrawableRes icon: Int, @StringRes text: Int, tag: String) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.full)
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().testTag(tag)) {
        Box(
            modifier = Modifier
                .size(FeatureCircleSize)
                .background(colors.secondaryContainer, shape)
                .border(Stroke.sticker, colors.outline, shape),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                painter = painterResource(icon),
                contentDescription = null,
                tint = colors.onSecondaryContainer,
                modifier = Modifier.size(FeatureIconSize),
            )
        }
        Spacer(modifier = Modifier.width(Spacing.md))
        Text(
            text = stringResource(text),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.onSurface,
            modifier = Modifier.weight(1f),
        )
    }
}

/**
 * Places [illustration] above [text], [Spacing.lg] apart. The text is measured first at the full width; the
 * illustration takes what is left of [viewportHeight], between [MinIllustrationHeight] and [MaxIllustrationHeight].
 * When the total is taller than the viewport, the enclosing verticalScroll scrolls.
 */
@Composable
private fun ShrinkFirstLayout(viewportHeight: Dp, illustration: @Composable () -> Unit, text: @Composable () -> Unit) {
    Layout(
        content = {
            Box { illustration() }
            Box { text() }
        },
    ) { measurables, constraints ->
        val width = constraints.maxWidth
        val gap = Spacing.lg.roundToPx()
        val textPlaceable = measurables[1].measure(Constraints(minWidth = width, maxWidth = width))
        val illustrationHeight = (viewportHeight.roundToPx() - textPlaceable.height - gap)
            .coerceIn(MinIllustrationHeight.roundToPx(), MaxIllustrationHeight.roundToPx())
        val illustrationPlaceable = measurables[0].measure(Constraints.fixed(width, illustrationHeight))
        layout(width, illustrationHeight + gap + textPlaceable.height) {
            illustrationPlaceable.place(0, 0)
            textPlaceable.place(0, illustrationHeight + gap)
        }
    }
}

internal object OnboardingTestTags {
    const val SCREEN = "onboarding_screen"
    const val CONTENT = "onboarding_content"
    const val ILLUSTRATION = "onboarding_illustration"
    const val TITLE = "onboarding_title"
    const val BODY = "onboarding_body"
    const val FEATURES = "onboarding_features"
    const val FEATURE_POSES = "onboarding_feature_poses"
    const val FEATURE_ADJUST = "onboarding_feature_adjust"
    const val FEATURE_SAVED = "onboarding_feature_saved"
    const val ENABLE_CAMERA = "onboarding_enable_camera"
    const val NOTE = "onboarding_note"
}

/** Lowest height of the shrinking illustration (plan T-012, question 7). */
internal val MinIllustrationHeight = 160.dp
private val MaxIllustrationHeight = 280.dp
private val FeatureCircleSize = 40.dp
private val FeatureIconSize = 22.dp

private fun Activity.shouldShowRationale(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
