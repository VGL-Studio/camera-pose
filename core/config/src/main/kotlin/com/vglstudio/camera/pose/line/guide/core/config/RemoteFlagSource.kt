package com.vglstudio.camera.pose.line.guide.core.config

import kotlinx.coroutines.flow.Flow

/**
 * Values fetched remotely (Firebase Remote Config in the app, a fake in tests).
 * Each function emits the value activated now, then every new value once it is activated.
 * It emits `null` while the key has no remote value, or when the remote value has another type.
 */
internal interface RemoteFlagSource {
    fun boolean(key: String): Flow<Boolean?>

    fun long(key: String): Flow<Long?>
}
