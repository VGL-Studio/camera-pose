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
import com.vglstudio.camera.pose.line.guide.feature.camera.navigation.cameraScreen
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.loadingScreen
import com.vglstudio.camera.pose.line.guide.feature.onboarding.navigation.OnboardingDestination
import com.vglstudio.camera.pose.line.guide.feature.onboarding.navigation.onboardingScreen
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.SampleDestination
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.sampleScreen
import kotlinx.coroutines.launch

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [chooseMainScreen] picks the screen after loading (spec loading, KB-1); only :app knows every feature (ADR-0001).
 * [markOnboardingDone] stores that the user tapped "Turn on camera" on onboarding (spec onboarding, KB-3).
 * [onPastLoadingChanged] tells whether the current destination is past the loading screen (spec network, section 2),
 * also after the system recreates the activity and restores the back stack.
 */
@Composable
internal fun AppNavHost(
    chooseMainScreen: suspend () -> MainScreen,
    markOnboardingDone: suspend () -> Unit,
    modifier: Modifier = Modifier,
    onPastLoadingChanged: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
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
                scope.launch {
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
            onStarted = { scope.launch { currentMarkOnboardingDone() } },
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
        // T-016 opens the review screen; T-021 and T-025 wire the library and settings buttons.
        cameraScreen(onPhotoCaptured = {})
    }
}

private fun MainScreen.destination(): Any = when (this) {
    MainScreen.ONBOARDING -> OnboardingDestination
    MainScreen.CAMERA -> CameraDestination()
    MainScreen.SAMPLE -> SampleDestination
}
