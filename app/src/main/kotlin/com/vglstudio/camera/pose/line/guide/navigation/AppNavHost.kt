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
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.LoadingDestination
import com.vglstudio.camera.pose.line.guide.feature.loading.navigation.loadingScreen
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.SampleDestination
import com.vglstudio.camera.pose.line.guide.feature.sample.navigation.sampleScreen
import kotlinx.coroutines.launch

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [chooseMainScreen] picks the screen after loading (spec loading, KB-1); only :app knows every feature (ADR-0001).
 * [onPastLoadingChanged] tells whether the current destination is past the loading screen (spec network, section 2),
 * also after the system recreates the activity and restores the back stack.
 */
@Composable
internal fun AppNavHost(
    chooseMainScreen: suspend () -> MainScreen,
    modifier: Modifier = Modifier,
    onPastLoadingChanged: (Boolean) -> Unit = {},
) {
    val navController = rememberNavController()
    val scope = rememberCoroutineScope()
    val entry by navController.currentBackStackEntryAsState()
    val pastLoading = entry?.destination?.let { !it.hasRoute<LoadingDestination>() } ?: false
    val currentOnPastLoadingChanged by rememberUpdatedState(onPastLoadingChanged)
    LaunchedEffect(pastLoading) { currentOnPastLoadingChanged(pastLoading) }
    NavHost(
        navController = navController,
        startDestination = LoadingDestination,
        modifier = modifier.safeDrawingPadding(),
    ) {
        loadingScreen(
            onFinished = {
                scope.launch {
                    val screen = chooseMainScreen()
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
        sampleScreen()
    }
}

private fun MainScreen.destination(): Any = when (this) {
    // T-012 replaces this with OnboardingDestination.
    MainScreen.ONBOARDING -> SampleDestination
    // T-013 replaces this with CameraDestination.
    MainScreen.CAMERA -> SampleDestination
    MainScreen.SAMPLE -> SampleDestination
}
