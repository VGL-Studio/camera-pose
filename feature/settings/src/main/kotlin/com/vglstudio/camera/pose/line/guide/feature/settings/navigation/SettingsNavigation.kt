package com.vglstudio.camera.pose.line.guide.feature.settings.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.settings.SettingsRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the settings screen (spec settings, KB-1). */
@Serializable
data object SettingsDestination

/** [onBack] returns to the screen that opened settings (camera or library). */
fun NavGraphBuilder.settingsScreen(onBack: () -> Unit) {
    composable<SettingsDestination> {
        SettingsRoute(onBack = onBack)
    }
}
