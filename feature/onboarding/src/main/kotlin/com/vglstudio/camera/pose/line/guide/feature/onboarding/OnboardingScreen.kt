package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
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
import androidx.compose.runtime.key
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
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.vglstudio.camera.pose.line.guide.core.ads.rememberAdHoldingLauncher
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Stroke

/**
 * Stateful entry point used by navigation. Asks for the camera permission when the view model says so and tells
 * whether the system dialog was really shown (T-026 rule, plan T-012 version 2). "Should show rationale" read before
 * the request is kept with rememberSaveable, so it survives an activity recreated while the dialog is open (plan
 * T-012, question 12; the camera uses remember). Back goes to the page before, from the second page (BackHandler in
 * [OnboardingScreen]); on the first page it closes the app. The theme follows the device.
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
    // The resume ad is held while the system dialog is up (spec ads, question 6; plan T-074, pass 2).
    val permissionLauncher = rememberAdHoldingLauncher(ActivityResultContracts.RequestPermission()) { granted ->
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
        onNext = viewModel::onNextClick,
        onBack = viewModel::onBackClick,
        onEnableCamera = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            viewModel.onEnableCameraClick(granted)
        },
    )
}

/**
 * Stateless screen: one page at a time, no pager (plan T-082, section 3.1). Layout from design/context/ (411 x 891):
 * the dots are pinned at the top; the page scrolls, with its [Spacing.lg] inner padding inside the scroll; the ad slot
 * of T-083, the permission note (last page) and the main button are pinned at the bottom, so the button keeps one
 * place on every page, with or without a card in the slot. On page 1 the illustration shrinks first (down to
 * [MinIllustrationHeight]), then the page scrolls (design README, section 6). Back goes to the page before, from the
 * second page; on the first page it closes the app.
 * [adSlot] draws the card of T-083 between the page and the bottom; nothing by default, with no padding of its own.
 */
@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEnableCamera: () -> Unit,
    modifier: Modifier = Modifier,
    adSlot: @Composable (page: OnboardingPage) -> Unit = {},
) {
    BackHandler(enabled = uiState is OnboardingUiState.Browsing && uiState.index > 0) { onBack() }
    val colors = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .testTag(OnboardingTestTags.SCREEN)
            .padding(start = Spacing.lg, end = Spacing.lg, top = Spacing.xl + Spacing.lg, bottom = Spacing.lg),
    ) {
        PageIndicator(count = uiState.pages.size, current = uiState.index)
        BoxWithConstraints(modifier = Modifier.weight(1f).fillMaxWidth().testTag(OnboardingTestTags.PAGE_AREA)) {
            // The inner padding scrolls with the page, so the illustration gets the area minus both paddings.
            val viewportHeight = maxHeight - Spacing.lg * 2
            key(uiState.page) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag(OnboardingTestTags.CONTENT)
                        .verticalScroll(rememberScrollState())
                        .padding(vertical = Spacing.lg),
                ) {
                    OnboardingPageContent(page = uiState.page, viewportHeight = viewportHeight)
                }
            }
        }
        Box(modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.AD_SLOT)) {
            adSlot(uiState.page)
        }
        if (uiState.isLastPage) {
            Spacer(modifier = Modifier.height(Spacing.lg))
            Text(
                text = stringResource(R.string.onboarding_note),
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.NOTE),
            )
        }
        Spacer(modifier = Modifier.height(Spacing.lg))
        if (uiState.isLastPage) {
            PrimaryButton(
                text = stringResource(R.string.onboarding_enable_camera),
                onClick = onEnableCamera,
                icon = painterResource(R.drawable.ic_onboarding_photo_camera),
                loading = uiState !is OnboardingUiState.Browsing,
                modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.ENABLE_CAMERA),
            )
        } else {
            PrimaryButton(
                text = stringResource(R.string.onboarding_next),
                onClick = onNext,
                modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.NEXT),
            )
        }
    }
}

/** One dot per page, the current one wider; screen readers read "Page 2 of 4" for the whole row. */
@Composable
private fun PageIndicator(count: Int, current: Int) {
    val description = stringResource(R.string.onboarding_page_indicator, current + 1, count)
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        modifier = Modifier
            .testTag(OnboardingTestTags.PAGE_INDICATOR)
            .semantics { contentDescription = description },
    ) {
        repeat(count) { index ->
            PageDot(selected = index == current, modifier = Modifier.testTag(OnboardingTestTags.pageDot(index + 1)))
        }
    }
}

@Composable
private fun PageDot(selected: Boolean, modifier: Modifier = Modifier) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .size(width = if (selected) Spacing.lg else Spacing.sm, height = Spacing.sm)
            .background(
                color = if (selected) colors.secondary else colors.outlineVariant,
                shape = RoundedCornerShape(Radius.full),
            ),
    )
}

/** The blocks of one page, [Spacing.lg] apart (design README, sections 2 and 3). */
@Composable
private fun OnboardingPageContent(page: OnboardingPage, viewportHeight: Dp) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.lg),
        modifier = Modifier.fillMaxWidth().testTag(page.testTag()),
    ) {
        when (page) {
            OnboardingPage.INTRO -> ShrinkFirstLayout(
                viewportHeight = viewportHeight,
                illustration = {
                    OnboardingIllustration(modifier = Modifier.fillMaxSize().testTag(OnboardingTestTags.ILLUSTRATION))
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                        PageTitle(R.string.onboarding_title)
                        PageBody(R.string.onboarding_body)
                    }
                },
            )
            OnboardingPage.POSES -> {
                IconBadge(R.drawable.ic_onboarding_accessibility_new)
                PageTitle(R.string.onboarding_poses_title)
                FeatureRows {
                    FeatureRow(
                        icon = R.drawable.ic_onboarding_accessibility_new,
                        text = R.string.onboarding_point_poses,
                        tag = OnboardingTestTags.FEATURE_POSES,
                    )
                    FeatureRow(
                        icon = R.drawable.ic_onboarding_bookmark,
                        text = R.string.onboarding_point_saved,
                        tag = OnboardingTestTags.FEATURE_SAVED,
                    )
                }
            }
            OnboardingPage.PREMIUM -> {
                IconBadge(R.drawable.ic_onboarding_workspace_premium)
                PageTitle(R.string.onboarding_premium_title)
                PageBody(R.string.onboarding_premium_body)
            }
            OnboardingPage.CAMERA -> {
                IconBadge(R.drawable.ic_onboarding_photo_camera)
                PageTitle(R.string.onboarding_camera_title)
                FeatureRows {
                    FeatureRow(
                        icon = R.drawable.ic_onboarding_tune,
                        text = R.string.onboarding_point_adjust,
                        tag = OnboardingTestTags.FEATURE_ADJUST,
                    )
                }
            }
        }
    }
}

private fun OnboardingPage.testTag(): String = when (this) {
    OnboardingPage.INTRO -> OnboardingTestTags.PAGE_INTRO
    OnboardingPage.POSES -> OnboardingTestTags.PAGE_POSES
    OnboardingPage.PREMIUM -> OnboardingTestTags.PAGE_PREMIUM
    OnboardingPage.CAMERA -> OnboardingTestTags.PAGE_CAMERA
}

@Composable
private fun PageTitle(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.onBackground,
        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.TITLE),
    )
}

@Composable
private fun PageBody(@StringRes text: Int) {
    Text(
        text = stringResource(text),
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.BODY),
    )
}

/** Round badge of pages 2, 3 and the last one (component IconBadge of the design). */
@Composable
private fun IconBadge(@DrawableRes icon: Int) {
    val colors = MaterialTheme.colorScheme
    val shape = RoundedCornerShape(Radius.full)
    Box(
        modifier = Modifier
            .size(PageBadgeSize)
            .background(colors.secondaryContainer, shape)
            .border(Stroke.sticker, colors.outline, shape)
            .testTag(OnboardingTestTags.BADGE),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            painter = painterResource(icon),
            contentDescription = null,
            tint = colors.onSecondaryContainer,
            modifier = Modifier.size(PageBadgeIconSize),
        )
    }
}

@Composable
private fun FeatureRows(content: @Composable () -> Unit) {
    Column(
        verticalArrangement = Arrangement.spacedBy(Spacing.md),
        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.FEATURES),
    ) {
        content()
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
    const val NEXT = "onboarding_next"
    const val PAGE_INDICATOR = "onboarding_page_indicator"
    const val PAGE_AREA = "onboarding_page_area"
    const val AD_SLOT = "onboarding_ad_slot"
    const val BADGE = "onboarding_badge"
    const val PAGE_INTRO = "onboarding_page_intro"
    const val PAGE_POSES = "onboarding_page_poses"
    const val PAGE_PREMIUM = "onboarding_page_premium"
    const val PAGE_CAMERA = "onboarding_page_camera"

    /** The dot of page [number], counted from 1. */
    fun pageDot(number: Int) = "onboarding_page_dot_$number"
}

/** Lowest height of the shrinking illustration (plan T-012, question 7). */
internal val MinIllustrationHeight = 160.dp
private val MaxIllustrationHeight = 280.dp
private val FeatureCircleSize = 40.dp
private val PageBadgeSize = 96.dp
private val PageBadgeIconSize = 48.dp
private val FeatureIconSize = 22.dp

private fun Activity.shouldShowRationale(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
