package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.StringRes
import androidx.compose.foundation.background
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.NativeStepAd
import com.vglstudio.camera.pose.line.guide.core.ads.rememberAdHoldingLauncher
import com.vglstudio.camera.pose.line.guide.core.ads.rememberNativeStepAdAllowed
import com.vglstudio.camera.pose.line.guide.core.ads.rememberNativeStepNextAdReady
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.PrimaryButton
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Radius
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing

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
    OnboardingAdContent(
        uiState = uiState,
        adsAllowed = rememberNativeStepAdAllowed(AdPlacement.NATIVE_ONBOARDING),
        onNext = viewModel::onNextClick,
        onBack = viewModel::onBackClick,
        onEnableCamera = {
            val granted = ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) ==
                PackageManager.PERMISSION_GRANTED
            viewModel.onEnableCameraClick(granted)
        },
        onIntroAdWaitStarted = viewModel::onIntroAdWaitStarted,
        onIntroAdWaitEnded = viewModel::onIntroAdWaitEnded,
        nextAdReady = { page, hasAd -> rememberNativeStepNextAdReady(AdPlacement.NATIVE_ONBOARDING, page, hasAd) },
        onGuideAdWaitStarted = viewModel::onGuideAdWaitStarted,
        onGuideAdWaitEnded = viewModel::onGuideAdWaitEnded,
    )
}

/**
 * Stateless screen: one page at a time, no pager (plan T-082, section 3.1). Layout from design/context/ (411 x 891):
 * the dots are pinned at the top; the page scrolls, with its [Spacing.lg] inner padding inside the scroll; the ad slot
 * of T-083, the permission note (last page) and the main button are pinned at the bottom, so the button keeps one
 * place on every page, with or without a card in the slot. On every page the illustration shrinks first (down to
 * [MinIllustrationHeight]), then the page scrolls (design README, section 6). Back goes to the page before, from the
 * second page; on the first page it closes the app.
 * [adSlot] draws the card of T-083 between the page and the bottom; nothing by default, with no padding of its own.
 * [nextEnabled] false holds "Next" while the card of page 1 waits for an ad (plan T-083) or while page 3 waits for
 * the ad of page 4 (plan T-092); "Turn on camera" never.
 */
@Composable
internal fun OnboardingScreen(
    uiState: OnboardingUiState,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEnableCamera: () -> Unit,
    modifier: Modifier = Modifier,
    adSlot: @Composable (page: OnboardingPage) -> Unit = {},
    nextEnabled: Boolean = true,
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
                enabled = nextEnabled,
                modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.NEXT),
            )
        }
    }
}

/**
 * The onboarding screen with the ad card of plan T-083: holds Next on page one while the card waits for an ad, and
 * on page three while the pool has no ad for page four, at most [GUIDE_AD_WAIT_MILLIS] (spec onboarding, KB-6,
 * question 12; plan T-092). [adsAllowed] is the gate of the card; [adSlot] draws the card and reports whether it has
 * an ad (the real card by default, a fake in tests); [nextAdReady] says whether the pool has an ad for the next page,
 * given the page on screen and whether its card has an ad ([rememberNativeStepNextAdReady] by default, a rule on a
 * fake count in tests). It is asked on page three only, so its reads of the pool run on that page alone.
 */
@Composable
internal fun OnboardingAdContent(
    uiState: OnboardingUiState,
    adsAllowed: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onEnableCamera: () -> Unit,
    modifier: Modifier = Modifier,
    onIntroAdWaitStarted: () -> Unit = {},
    onIntroAdWaitEnded: (IntroAdWaitEnd) -> Unit = {},
    nextAdReady: @Composable (page: OnboardingPage, currentStepHasAd: Boolean) -> Boolean = { _, _ -> false },
    onGuideAdWaitStarted: () -> Unit = {},
    onGuideAdWaitEnded: (IntroAdWaitEnd) -> Unit = {},
    adSlot: @Composable (page: OnboardingPage, onAdAvailabilityChange: (Boolean) -> Unit) -> Unit =
        { page, onChange -> OnboardingAdCard(page, onChange) },
) {
    var cardHasAd by remember { mutableStateOf(false) }
    // The page whose card has an ad, or null: the slot reports nothing while it loads, so the true of page two
    // would still read as true on page three (plan T-092).
    var cardAdPage by remember { mutableStateOf<OnboardingPage?>(null) }
    val browsing = uiState is OnboardingUiState.Browsing
    val introHeld = rememberIntroAdWait(
        onIntro = browsing && uiState.page == OnboardingPage.INTRO,
        allowed = adsAllowed,
        hasAd = cardHasAd,
        onStarted = onIntroAdWaitStarted,
        onEnded = onIntroAdWaitEnded,
    )
    val onGuide = browsing && uiState.page == OnboardingPage.GUIDE
    // Asked on page three only: other pages count as no signal, and the pool is not read on them.
    val guideAdReady = if (onGuide) nextAdReady(uiState.page, cardAdPage == uiState.page) else false
    val guideHeld = rememberPageAdWait(
        onPage = onGuide,
        allowed = adsAllowed,
        hasAd = guideAdReady,
        maxMillis = GUIDE_AD_WAIT_MILLIS,
        usedUpBeforeFirstVisit = false,
        onStarted = onGuideAdWaitStarted,
        onEnded = onGuideAdWaitEnded,
    )
    OnboardingScreen(
        uiState = uiState,
        onNext = onNext,
        onBack = onBack,
        onEnableCamera = onEnableCamera,
        modifier = modifier,
        nextEnabled = !introHeld && !guideHeld,
        adSlot = { page ->
            adSlot(page) { available ->
                cardHasAd = available
                cardAdPage = if (available) page else null
            }
        },
    )
}

/** The step card of [page] (spec onboarding, KB-6): no padding, so it leaves no space while it takes none. */
@Composable
internal fun OnboardingAdCard(page: OnboardingPage, onAdAvailabilityChange: (Boolean) -> Unit = {}) {
    NativeStepAd(
        placement = AdPlacement.NATIVE_ONBOARDING,
        step = page,
        modifier = Modifier.fillMaxWidth().testTag(OnboardingTestTags.AD_CARD),
        onAdAvailabilityChange = onAdAvailabilityChange,
    )
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

/**
 * One page: the art, the title and the body, [Spacing.lg] apart, the same on every page (design README, sections 2
 * and 3); the art shrinks first (ShrinkFirstLayout).
 */
@Composable
private fun OnboardingPageContent(page: OnboardingPage, viewportHeight: Dp) {
    Column(modifier = Modifier.fillMaxWidth().testTag(page.testTag())) {
        ShrinkFirstLayout(
            viewportHeight = viewportHeight,
            illustration = {
                OnboardingIllustration(page, Modifier.fillMaxSize().testTag(OnboardingTestTags.ILLUSTRATION))
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.lg)) {
                    PageTitle(page.title)
                    PageBody(page.body)
                }
            },
        )
    }
}

private fun OnboardingPage.testTag(): String = when (this) {
    OnboardingPage.INTRO -> OnboardingTestTags.PAGE_INTRO
    OnboardingPage.POSES -> OnboardingTestTags.PAGE_POSES
    OnboardingPage.GUIDE -> OnboardingTestTags.PAGE_GUIDE
    OnboardingPage.CAMERA -> OnboardingTestTags.PAGE_CAMERA
}

@get:StringRes
private val OnboardingPage.title: Int
    get() = when (this) {
        OnboardingPage.INTRO -> R.string.onboarding_title
        OnboardingPage.POSES -> R.string.onboarding_poses_title
        OnboardingPage.GUIDE -> R.string.onboarding_guide_title
        OnboardingPage.CAMERA -> R.string.onboarding_camera_title
    }

@get:StringRes
private val OnboardingPage.body: Int
    get() = when (this) {
        OnboardingPage.INTRO -> R.string.onboarding_body
        OnboardingPage.POSES -> R.string.onboarding_poses_body
        OnboardingPage.GUIDE -> R.string.onboarding_guide_body
        OnboardingPage.CAMERA -> R.string.onboarding_camera_body
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
    const val ENABLE_CAMERA = "onboarding_enable_camera"
    const val NOTE = "onboarding_note"
    const val NEXT = "onboarding_next"
    const val PAGE_INDICATOR = "onboarding_page_indicator"
    const val PAGE_AREA = "onboarding_page_area"
    const val AD_SLOT = "onboarding_ad_slot"
    const val AD_CARD = "onboarding_ad_card"
    const val BUBBLE = "onboarding_bubble"
    const val PAGE_INTRO = "onboarding_page_intro"
    const val PAGE_POSES = "onboarding_page_poses"
    const val PAGE_GUIDE = "onboarding_page_guide"
    const val PAGE_CAMERA = "onboarding_page_camera"

    /** The dot of page [number], counted from 1. */
    fun pageDot(number: Int) = "onboarding_page_dot_$number"
}

/** Lowest height of the shrinking illustration (plan T-012, question 7). */
internal val MinIllustrationHeight = 160.dp

/** Highest height of the illustration: its frame when there is room (design README, section 3). */
private val MaxIllustrationHeight = 360.dp

private fun Activity.shouldShowRationale(): Boolean =
    ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.CAMERA)
