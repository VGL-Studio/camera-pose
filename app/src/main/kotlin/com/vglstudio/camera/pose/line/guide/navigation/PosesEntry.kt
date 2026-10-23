package com.vglstudio.camera.pose.line.guide.navigation

/**
 * The library button of the camera: shown only while feature_poses_enabled is on (spec poses, section 8). Null hides
 * the button, as the camera already does for a missing action (T-013).
 */
internal fun posesOpener(enabled: Boolean, open: () -> Unit): (() -> Unit)? = if (enabled) open else null
