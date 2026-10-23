package com.vglstudio.camera.pose.line.guide.feature.camera.data

import com.vglstudio.camera.pose.line.guide.feature.camera.pose.DEFAULT_OPACITY
import kotlinx.coroutines.flow.Flow

/** The kept part of the outline adjustment (spec camera, section 6). Values out of range are clamped on read. */
data class OverlaySettings(
    /** 0.2 to 1.0. */
    val opacity: Float = DEFAULT_OPACITY,
    /** 0.4 to 1.6. */
    val scale: Float = 1f,
    val mirrored: Boolean = false,
    val locked: Boolean = false,
    val hidden: Boolean = false,
)

/** Stores how the pose overlay looks: the step card choice and the outline adjustment. */
interface PoseOverlaySettingsRepository {
    /** Whether the step card is hidden (spec camera, KB-2). False when nothing is stored. */
    val stepsHidden: Flow<Boolean>

    /** Throws [java.io.IOException] when the value cannot be written. */
    suspend fun setStepsHidden(hidden: Boolean)

    /** The outline adjustment. [OverlaySettings] with its defaults when nothing is stored. */
    val overlay: Flow<OverlaySettings>

    /** Writes the five values at once. Throws [java.io.IOException] when they cannot be written. */
    suspend fun setOverlay(settings: OverlaySettings)
}
