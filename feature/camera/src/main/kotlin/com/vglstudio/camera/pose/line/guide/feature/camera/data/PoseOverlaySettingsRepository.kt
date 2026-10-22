package com.vglstudio.camera.pose.line.guide.feature.camera.data

import kotlinx.coroutines.flow.Flow

/** Stores how the pose overlay looks. T-015 adds opacity, size, mirror, lock and hide here. */
interface PoseOverlaySettingsRepository {
    /** Whether the step card is hidden (spec camera, KB-2). False when nothing is stored. */
    val stepsHidden: Flow<Boolean>

    /** Throws [java.io.IOException] when the value cannot be written. */
    suspend fun setStepsHidden(hidden: Boolean)
}
