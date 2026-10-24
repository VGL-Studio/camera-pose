package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.PoseMatchingSettings
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Frames sent by hand. A MutableSharedFlow without replay, the same kind as the real source: it does not merge equal
 * frames and keeps nothing for a collector that is not there yet. A MutableStateFlow here would merge three equal
 * frames without a person into one and hide the "no person" counting (plan T-017, version 2).
 */
internal class FakePoseFrameSource : PoseFrameSource {
    private val shared = MutableSharedFlow<BodyPose?>(extraBufferCapacity = 64)
    override val frames: SharedFlow<BodyPose?> = shared

    /** Every call of [setActive], in order. */
    val activeCalls = mutableListOf<Boolean>()

    /** The last value given to [setActive]; false before any call. */
    val active: Boolean get() = activeCalls.lastOrNull() ?: false

    override fun setActive(active: Boolean) {
        activeCalls += active
    }

    /** Whether anyone collects the frames now; a frame sent while nobody does is lost, as with the real source. */
    val hasCollector: Boolean get() = shared.subscriptionCount.value > 0

    fun send(frame: BodyPose?) {
        shared.tryEmit(frame)
    }
}

/** POSE_MATCHING follows [value]; other flags are off. */
internal class FakeFeatureFlags(initial: Boolean = true) : FeatureFlags {
    val value = MutableStateFlow(initial)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> =
        if (flag == FeatureFlag.POSE_MATCHING) value else MutableStateFlow(false)
}

internal class FakePoseMatchingSettings(threshold: Int = 80, inFramePercent: Int = 50) : PoseMatchingSettings {
    val threshold = MutableStateFlow(threshold)
    val inFramePercent = MutableStateFlow(inFramePercent)

    override val scoreThreshold: Flow<Int> = this.threshold
    override val minInFramePercent: Flow<Int> = this.inFramePercent
}

/** A full body pose of the fixture catalog whose keypoints are [template]. */
internal fun poseWithKeypoints(
    id: String = "w01",
    template: TestBodyPoses.Shape = TestBodyPoses.standingArmsOut,
    framing: Framing = Framing.FULL,
): Pose = PoseFixtures.pose(id).copy(keypoints = template.keypoints(), framing = framing)
