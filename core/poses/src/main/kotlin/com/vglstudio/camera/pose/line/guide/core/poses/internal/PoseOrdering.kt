package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose

/** Saved poses first (newest saved first), then recent ones (newest used first), then the rest in catalog order. */
internal fun orderPoses(poses: List<Pose>, saved: List<SavedPose>, recent: List<UsedPose>): List<Pose> = emptyList()

internal fun buildPoseLibrary(catalog: PoseCatalog, saved: List<SavedPose>, recent: List<UsedPose>): PoseLibrary =
    PoseLibrary(
        categories = emptyList(),
        totalPoses = 0,
        savedIds = emptySet(),
        defaultPose = null,
        lastUsedPose = null,
        all = emptyList(),
        saved = emptyList(),
        byCategory = emptyMap(),
    )
