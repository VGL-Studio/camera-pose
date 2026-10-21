package com.vglstudio.camera.pose.line.guide.core.poses.model

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath

enum class SubjectType { SINGLE, COUPLE, GROUP }

enum class PoseGender { FEMALE, MALE, MIXED, ANY }

enum class Framing { FULL, HALF, CLOSE }

/** One body landmark in the outline image: [x] and [y] from 0 to 1, [visibility] from 0 to 1. */
data class Keypoint(val x: Float, val y: Float, val visibility: Float)

/** The three images of a pose. */
enum class PoseFile { THUMBNAIL, IMAGE, OUTLINE }

data class PoseCategory(val id: String, val name: LocalizedText)

/** One pose of the catalog. The image paths are used by [PoseCatalogRepository.loadFile], not by features. */
data class Pose(
    val id: String,
    val categoryId: String,
    val subjectType: SubjectType,
    val gender: PoseGender,
    val framing: Framing,
    val tags: List<String>,
    val difficulty: Int,
    val starter: Boolean,
    val name: LocalizedText,
    val steps: LocalizedList,
    val mistakes: LocalizedList,
    val keypoints: List<Keypoint>?,
    internal val thumbnailPath: AssetPath,
    internal val imagePath: AssetPath,
    internal val outlinePath: AssetPath,
)
