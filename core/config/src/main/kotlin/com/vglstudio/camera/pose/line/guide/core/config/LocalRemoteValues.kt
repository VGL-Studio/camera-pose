package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Qualifier

/**
 * Qualifier for the set of values one build gives remote keys when Firebase has none (ADR-0023).
 * The app's `staging` source set contributes to this set; production contributes nothing and gets the empty set
 * declared in the config module. Order of precedence in [RemoteSettings]: the Firebase value, then the local value,
 * then [RemoteKey.default].
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class LocalRemoteValues

/**
 * A value one build gives a remote key when Firebase has none (ADR-0023).
 * Not generic, so Dagger sees one `Set<LocalRemoteValue>` key where the set is declared and where builds
 * contribute to it; [of] keeps the value of the key's type.
 */
class LocalRemoteValue private constructor(val key: RemoteKey<*>, val value: Any) {
    companion object {
        fun <T : Any> of(key: RemoteKey<T>, value: T): LocalRemoteValue = LocalRemoteValue(key, value)
    }
}
