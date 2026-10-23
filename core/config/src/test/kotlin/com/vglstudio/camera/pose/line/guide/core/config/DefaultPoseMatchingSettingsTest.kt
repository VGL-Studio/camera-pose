package com.vglstudio.camera.pose.line.guide.core.config

import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The numbers of the pose matching read from Remote Config (spec camera, KB-5; plan T-017, section 4). */
class DefaultPoseMatchingSettingsTest {
    private val remote = FakeRemoteFlagSource()
    private val settings = DefaultPoseMatchingSettings(DefaultRemoteSettings(remote))

    @Test
    fun defaults_are80And50() = runTest {
        assertEquals(80, settings.scoreThreshold.first())
        assertEquals(50, settings.minInFramePercent.first())
        // The control: a value set remotely replaces the default, so the defaults above come from reading.
        remote.longs.value = mapOf("pose_match_threshold" to 70L, "pose_match_min_in_frame_percent" to 60L)
        assertEquals(70, settings.scoreThreshold.first())
        assertEquals(60, settings.minInFramePercent.first())
    }

    @Test
    fun remoteValues_areUsed() = runTest {
        remote.longs.value = mapOf("pose_match_threshold" to 65L, "pose_match_min_in_frame_percent" to 75L)
        assertEquals(65, settings.scoreThreshold.first())
        assertEquals(75, settings.minInFramePercent.first())
        remote.longs.value = mapOf("pose_match_threshold" to 100L, "pose_match_min_in_frame_percent" to 99L)
        assertEquals(100, settings.scoreThreshold.first())
        assertEquals(99, settings.minInFramePercent.first())
        remote.longs.value = mapOf("pose_match_threshold" to 1L, "pose_match_min_in_frame_percent" to 1L)
        assertEquals(1, settings.scoreThreshold.first())
        assertEquals(1, settings.minInFramePercent.first())
    }

    @Test
    fun outOfRange_fallsBackToDefault() = runTest {
        listOf(0L, 101L, -5L).forEach { value ->
            remote.longs.value = mapOf("pose_match_threshold" to value)
            assertEquals("threshold $value", 80, settings.scoreThreshold.first())
        }
        listOf(0L, 100L, -5L).forEach { value ->
            remote.longs.value = mapOf("pose_match_min_in_frame_percent" to value)
            assertEquals("percent $value", 50, settings.minInFramePercent.first())
        }
        // The control: inside the range the value is used.
        remote.longs.value = mapOf("pose_match_threshold" to 90L, "pose_match_min_in_frame_percent" to 40L)
        assertEquals(90, settings.scoreThreshold.first())
        assertEquals(40, settings.minInFramePercent.first())
    }
}
