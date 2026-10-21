package com.vglstudio.camera.pose.line.guide.core.config

import app.cash.turbine.test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DefaultFeatureFlagsTest {
    private val remote = FakeRemoteFlagSource()

    private fun flags(locallyEnabled: Set<FeatureFlag> = emptySet()) = DefaultFeatureFlags(locallyEnabled, remote)

    @Test
    fun everyFlag_isOffByDefault() {
        // Rule (ADR-0003): a feature stays hidden in production until it is approved for release.
        FeatureFlag.entries.forEach { flag ->
            assertFalse("Flag ${flag.name} must default to off", flag.defaultValue)
        }
    }

    @Test
    fun withoutLocalOrRemoteValues_everyFlagIsOff() = runTest {
        val flags = flags()
        FeatureFlag.entries.forEach { flag -> assertFalse(flags.enabled(flag).first()) }
    }

    @Test
    fun locallyEnabledFlag_isOn() = runTest {
        assertTrue(flags(locallyEnabled = setOf(FeatureFlag.SAMPLE)).enabled(FeatureFlag.SAMPLE).first())
    }

    @Test
    fun locallyEnabledFlag_staysOnWhenRemoteSaysOff() = runTest {
        remote.booleans.value = mapOf(FeatureFlag.SAMPLE.key to false)
        assertTrue(flags(locallyEnabled = setOf(FeatureFlag.SAMPLE)).enabled(FeatureFlag.SAMPLE).first())
    }

    @Test
    fun remoteValueTrue_turnsFlagOn() = runTest {
        remote.booleans.value = mapOf(FeatureFlag.SAMPLE.key to true)
        assertTrue(flags().enabled(FeatureFlag.SAMPLE).first())
    }

    @Test
    fun remoteValueActivatedLater_reachesCollectors() = runTest {
        flags().enabled(FeatureFlag.SAMPLE).test {
            assertFalse(awaitItem())
            remote.booleans.value = mapOf(FeatureFlag.SAMPLE.key to true)
            assertTrue(awaitItem())
            remote.booleans.value = emptyMap()
            assertFalse(awaitItem())
        }
    }

    @Test
    fun everyFlagKey_followsNamingRule() {
        FeatureFlag.entries.forEach { flag ->
            assertTrue(
                "Flag key '${flag.key}' must match feature_<name>_enabled",
                Regex("^feature_[a-z0-9_]+_enabled$").matches(flag.key),
            )
        }
    }
}
