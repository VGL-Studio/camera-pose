package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Business settings tuned remotely without a new release (ADR-0003), such as ad frequency.
 * Not feature flags: keys are declared as [RemoteKey] by the core module that uses them (ADR-0023).
 * Feature modules do not use this.
 * The flow emits the current value at once, then every change. The value is the remote value of the key's type
 * once activated, else the value this build declares under [LocalRemoteValues] (staging only), else
 * [RemoteKey.default].
 */
interface RemoteSettings {
    fun <T : Any> value(key: RemoteKey<T>): Flow<T>
}

internal class DefaultRemoteSettings @Inject constructor(
    private val remote: RemoteFlagSource,
    @LocalRemoteValues localValues: Set<@JvmSuppressWildcards LocalRemoteValue>,
) : RemoteSettings {
    private val localValueByName: Map<String, Any> = localValues
        .groupBy { it.key.name }
        .mapValues { (name, values) ->
            require(values.size == 1) { "Remote Config key '$name' has more than one local value" }
            values.single().value
        }

    // Safe casts: the factories of RemoteKey tie each Type to one T (BOOLEAN to Boolean, LONG to Long, and so on),
    // the source function below is chosen by that Type, and LocalRemoteValue.of ties a local value to the T of its
    // key. Names are unique among local values, so the local value found has this key's type.
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> {
        val values: Flow<Any?> = when (key.type) {
            RemoteKey.Type.BOOLEAN -> remote.boolean(key.name)
            RemoteKey.Type.LONG -> remote.long(key.name)
            RemoteKey.Type.DOUBLE -> remote.double(key.name)
            RemoteKey.Type.STRING -> remote.string(key.name)
        }
        val fallback = (localValueByName[key.name] as T?) ?: key.default
        return values.map { (it as T?) ?: fallback }
    }
}
