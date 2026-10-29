package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import java.util.Collections
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update

/**
 * [RemoteSettings] for unit tests, following the T-007 contract: a value set by key name, else the key's default.
 * Like the real one, the flow emits the current value at once, then again on every change of any key.
 * [readNames] records the name of every key the code asked for.
 */
internal class FakeRemoteSettings : RemoteSettings {
    private val values = MutableStateFlow<Map<String, Any>>(emptyMap())
    private val read = Collections.synchronizedSet(mutableSetOf<String>())

    /** Names of the keys read through [value] so far. */
    val readNames: Set<String> get() = synchronized(read) { read.toSet() }

    fun <T : Any> set(key: RemoteKey<T>, value: T) {
        values.update { it + (key.name to value) }
    }

    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> {
        read += key.name
        return values.map { (it[key.name] as T?) ?: key.default }
    }
}
