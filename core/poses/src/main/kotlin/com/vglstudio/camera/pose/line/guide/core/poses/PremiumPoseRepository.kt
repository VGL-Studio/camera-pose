package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import kotlinx.coroutines.flow.Flow

/** Premium poses and the ones unlocked on this device, kept until the app data is cleared (spec poses, KB-8). */
interface PremiumPoseRepository {
    /** Which poses are locked now; a new value when FeatureFlag.PREMIUM_POSES or the unlocked list changes. */
    val access: Flow<PremiumAccess>

    /** Whether [pose] can be used now: the flag is off, the pose is not premium, or it was unlocked. */
    fun isUsable(pose: Pose): Flow<Boolean>

    /**
     * Unlocks [poseId] for good on this device and returns true. Writes nothing and returns false when
     * FeatureFlag.PREMIUM_POSES is off (spec poses, question 10), so the caller logs an unlock only when one happened;
     * a second call keeps the first unlock time and returns true. Throws [java.io.IOException] when the list cannot be
     * written; the caller tells the user.
     */
    suspend fun unlock(poseId: String): Boolean
}
