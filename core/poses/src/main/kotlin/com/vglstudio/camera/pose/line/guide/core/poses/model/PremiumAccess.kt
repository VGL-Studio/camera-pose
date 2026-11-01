package com.vglstudio.camera.pose.line.guide.core.poses.model

/** Which poses are locked at one moment (spec poses, KB-8). */
data class PremiumAccess(
    /** FeatureFlag.PREMIUM_POSES. Off: no pose is locked. */
    val enabled: Boolean,
    /** Poses unlocked on this device, also the ones no longer in the catalog (spec poses, question 9). */
    val unlockedIds: Set<String>,
) {
    /** True when premium poses are on, [pose] is premium and it was not unlocked. */
    fun isLocked(pose: Pose): Boolean = false

    fun isUsable(pose: Pose): Boolean = !isLocked(pose)
}
