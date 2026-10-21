package com.vglstudio.camera.pose.line.guide.navigation

import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
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

/**
 * Wires every feature's destinations together. Features never navigate to each other directly.
 * [onPastLoadingChanged] tells whether the current destination is past the loading screen (spec network, section 2),
 * also after the system recreates the activity and restores the back stack.
 */
@Composable
fun AppNavHost(modifier: Modifier = Modifier, onPastLoadingChanged: (Boolean) -> Unit = {}) {
    val navController = rememberNavController()
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
                // Loading leaves the back stack: Back on the start screen closes the app (spec loading, KB-1).
                navController.navigate(SampleDestination) {
                    popUpTo<LoadingDestination> { inclusive = true }
                }
            },
        )
        sampleScreen()
    }
}
