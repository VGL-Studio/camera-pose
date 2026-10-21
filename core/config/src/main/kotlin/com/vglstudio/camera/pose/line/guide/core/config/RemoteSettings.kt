package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * Business settings tuned remotely without a new release (ADR-0003), such as ad frequency.
 * Not feature flags: keys are declared by the core module that uses them. Feature modules do not use this.
 * Each flow emits the current value at once ([default] until a remote value is activated), then every change.
 */
interface RemoteSettings {
    fun boolean(key: String, default: Boolean): Flow<Boolean>

    fun long(key: String, default: Long): Flow<Long>

    fun string(key: String, default: String): Flow<String>
}

internal class DefaultRemoteSettings @Inject constructor(private val remote: RemoteFlagSource) : RemoteSettings {
    override fun boolean(key: String, default: Boolean): Flow<Boolean> = remote.boolean(key).map { it ?: default }

    override fun long(key: String, default: Long): Flow<Long> = remote.long(key).map { it ?: default }

    override fun string(key: String, default: String): Flow<String> = flowOf(default)
}
