package com.vglstudio.camera.pose.line.guide.core.config

import kotlinx.coroutines.flow.Flow

/**
 * Read access to feature flags. Feature modules use this interface, never Remote Config directly.
 * Values are flows: a value fetched remotely after start-up reaches the screen without restarting the app.
 */
interface FeatureFlags {
    fun enabled(flag: FeatureFlag): Flow<Boolean>
}
