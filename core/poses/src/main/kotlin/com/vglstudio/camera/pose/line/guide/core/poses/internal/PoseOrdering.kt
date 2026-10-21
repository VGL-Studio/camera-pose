package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose

/**
 * Saved poses first (newest saved first), then recent ones (newest used first), then the rest in catalog order.
 * A pose that is in several parts appears once, in the first. Ids that are not in [poses] are ignored.
 */
internal fun orderPoses(poses: List<Pose>, saved: List<SavedPose>, recent: List<UsedPose>): List<Pose> {
    val byId = poses.associateBy { it.id }
    val savedPoses = savedPoses(byId, saved)
    val taken = savedPoses.mapTo(mutableSetOf()) { it.id }
    val recentPoses = recentPoses(byId, recent).filter { it.id !in taken }
    recentPoses.mapTo(taken) { it.id }
    return savedPoses + recentPoses + poses.filter { it.id !in taken }
}

internal fun buildPoseLibrary(catalog: PoseCatalog, saved: List<SavedPose>, recent: List<UsedPose>): PoseLibrary {
    val ordered = orderPoses(catalog.poses, saved, recent)
    val byId = catalog.poses.associateBy { it.id }
    val savedPoses = savedPoses(byId, saved)
    val byCategory = ordered.groupBy { it.categoryId }
    return PoseLibrary(
        categories = catalog.categories.filter { byCategory.containsKey(it.id) },
        totalPoses = catalog.poses.size,
        savedIds = savedPoses.mapTo(mutableSetOf()) { it.id },
        defaultPose = catalog.poses.firstOrNull(),
        lastUsedPose = recentPoses(byId, recent).firstOrNull(),
        all = ordered,
        saved = savedPoses,
        byCategory = byCategory,
    )
}

private fun savedPoses(byId: Map<String, Pose>, saved: List<SavedPose>): List<Pose> = saved
    .filter { it.id in byId }
    .sortedByDescending { it.savedAtMillis }
    .distinctBy { it.id }
    .map { byId.getValue(it.id) }

private fun recentPoses(byId: Map<String, Pose>, recent: List<UsedPose>): List<Pose> = recent
    .filter { it.id in byId }
    .sortedByDescending { it.usedAtMillis }
    .distinctBy { it.id }
    .map { byId.getValue(it.id) }
