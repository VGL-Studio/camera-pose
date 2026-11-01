package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Flags in memory: a flag is on while it is in [enabled]; change [enabled] to turn flags on or off mid-test. */
internal class FakeFeatureFlags(enabled: Set<FeatureFlag> = emptySet()) : FeatureFlags {
    val enabled = MutableStateFlow(enabled)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> = enabled.map { flag in it }
}
