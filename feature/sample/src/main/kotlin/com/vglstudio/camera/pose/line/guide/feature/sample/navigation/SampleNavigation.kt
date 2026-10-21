package com.vglstudio.camera.pose.line.guide.feature.sample.navigation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import com.vglstudio.camera.pose.line.guide.feature.sample.SampleRoute
import kotlinx.serialization.Serializable

/** Navigation destination of the sample feature. The app module wires it into its NavHost. */
@Serializable
data object SampleDestination

fun NavGraphBuilder.sampleScreen() {
    composable<SampleDestination> {
        SampleRoute()
    }
}
