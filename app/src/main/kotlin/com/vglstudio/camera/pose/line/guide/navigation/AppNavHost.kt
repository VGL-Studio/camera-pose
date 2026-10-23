package com.vglstudio.camera.pose.line.guide.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.CameraDestination
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.ReviewDestination
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.cameraScreen
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.reviewScreen
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.loadingScreen
import com.vglstudio.camera.pose.line.guide.feature.onboarding.navigation.OnboardingDestination
import com.vglstudio.camera.pose.line.guide.feature.onboarding.navigation.onboardingScreen
import com.vglstudio.camera.pose.line.guide.feature.poses.navigation.PosesDestination
import com.vglstudio.camera.pose.line.guide.feature.poses.navigation.posesScreen
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.SampleDestination
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.sampleScreen
import com.vglstudio.camera.pose.line.guide.feature.settings.navigation.SettingsDestination
import com.vglstudio.camera.pose.line.guide.feature.settings.navigation.settingsScreen
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [chooseMainScreen] picks the screen after loading (spec loading, KB-1); only :app knows every feature (ADR-0001).
 * [markOnboardingDone] stores that the user tapped "Turn on camera" on onboarding (spec onboarding, KB-3).
 * [onPastLoadingChanged] tells whether the current destination is past the loading screen (spec network, section 2),
 * also after the system recreates the activity and restores the back stack.
 * [posesEnabled] follows feature_poses_enabled: the library button of the camera shows only while it is on.
 * [settingsEnabled] follows feature_settings_enabled: the settings buttons of the camera and the library show only
 * while it is on.
 */
@Composable
internal fun AppNavHost(
    chooseMainScreen: suspend () -> MainScreen,
    markOnboardingDone: suspend () -> Unit,
    modifier: Modifier = Modifier,
    onPastLoadingChanged: (Boolean) -> Unit = {},
    posesEnabled: Boolean = false,
    settingsEnabled: Boolean = false,
) {
    val navController = rememberNavController()
    // Single top: a second tap on a gear does not stack a second settings screen (spec settings, KB-1).
    val openSettings: (() -> Unit)? = if (settingsEnabled) {
        { navController.navigate(SettingsDestination) { launchSingleTop = true } }
    } else {
        null
    }
    val scope = rememberCoroutineScope()
    val entry by navController.currentBackStackEntryAsState()
    val pastLoading = entry?.destination?.let { !it.hasRoute<LoadingDestination>() } ?: false
    val currentOnPastLoadingChanged by rememberUpdatedState(onPastLoadingChanged)
    val currentChooseMainScreen by rememberUpdatedState(chooseMainScreen)
    val currentMarkOnboardingDone by rememberUpdatedState(markOnboardingDone)
    LaunchedEffect(pastLoading) { currentOnPastLoadingChanged(pastLoading) }
    NavHost(
        navController = navController,
        startDestination = LoadingDestination,
        modifier = modifier.safeDrawingPadding(),
    ) {
        loadingScreen(
            onFinished = {
                // Main.immediate: chooseMainScreen reads DataStore, and navigate must resume on the main thread
                // whatever dispatcher the composition's scope uses (Compose tests resume it on a worker thread).
                scope.launch(Dispatchers.Main.immediate) {
                    val screen = currentChooseMainScreen()
                    // onFinished runs again after the activity is recreated: navigate only while still on loading.
                    if (navController.currentDestination?.hasRoute<LoadingDestination>() != true) return@launch
                    // Loading leaves the back stack: Back on the main screen closes the app (spec loading, KB-1).
                    navController.navigate(screen.destination()) {
                        popUpTo<LoadingDestination> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
        onboardingScreen(
            // Undispatched: markDone enters NonCancellable within the tap, even if the activity is recreated next.
            onStarted = { scope.launch(start = CoroutineStart.UNDISPATCHED) { currentMarkOnboardingDone() } },
            onFinished = {
                // Navigate once, only while still on onboarding (as loading above).
                if (navController.currentDestination?.hasRoute<OnboardingDestination>() == true) {
                    // The permission was just asked here: the camera does not ask again (spec camera, KB-7).
                    // Onboarding leaves the back stack: Back on the camera closes the app (spec onboarding, KB-2).
                    navController.navigate(CameraDestination(askPermission = false)) {
                        popUpTo<OnboardingDestination> { inclusive = true }
                        launchSingleTop = true
                    }
                }
            },
        )
        sampleScreen()
        cameraScreen(
            onPhotoCaptured = { navController.navigate(ReviewDestination()) { launchSingleTop = true } },
            onOpenPoses = posesOpener(posesEnabled) {
                navController.navigate(PosesDestination) { launchSingleTop = true }
            },
            onOpenSettings = openSettings,
            onOpenLastPhoto = { uri ->
                navController.navigate(ReviewDestination(savedPhotoUri = uri)) { launchSingleTop = true }
            },
        )
        // The review screen sits above the camera, which keeps its pose and tools (spec camera, KB-6).
        reviewScreen(
            onLeave = {
                // Only while the review screen is on top: never pop the camera too.
                if (navController.currentDestination?.hasRoute<ReviewDestination>() == true) {
                    navController.popBackStack()
                }
            },
        )
        // Back returns to the camera entry, which kept its pose (spec poses, KB-1). T-022 wires onOpenPose.
        posesScreen(
            onBack = {
                // Only while the library is on top: a second tap must not close the camera too.
                if (navController.currentDestination?.hasRoute<PosesDestination>() == true) {
                    navController.popBackStack()
                }
            },
            onOpenSettings = openSettings,
        )
        // Back returns to the screen that opened settings: the camera or the library (spec settings, KB-1).
        settingsScreen(
            onBack = {
                // Only while settings is on top: a second tap must not close the screen below too.
                if (navController.currentDestination?.hasRoute<SettingsDestination>() == true) {
                    navController.popBackStack()
                }
            },
        )
    }
}

private fun MainScreen.destination(): Any = when (this) {
    MainScreen.ONBOARDING -> OnboardingDestination
    MainScreen.CAMERA -> CameraDestination()
    MainScreen.SAMPLE -> SampleDestination
}
