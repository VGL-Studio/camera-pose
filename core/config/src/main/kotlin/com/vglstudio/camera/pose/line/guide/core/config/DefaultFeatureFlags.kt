package com.vglstudio.camera.pose.line.guide.core.config

import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map

/**
 * A flag is on when the current build enables it locally, or when its remote value is `true`.
 * Without a remote value, the flag takes its default value (off).
 */
internal class DefaultFeatureFlags @Inject constructor(
    @param:LocallyEnabledFlags private val locallyEnabled: Set<@JvmSuppressWildcards FeatureFlag>,
    private val remote: RemoteFlagSource,
) : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = if (flag in locallyEnabled) {
        flowOf(true)
    } else {
        remote.boolean(flag.remoteKey.name).map { it ?: flag.remoteKey.default }
    }
}
