package com.vglstudio.camera.pose.line.guide.core.poses.internal

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/** Poses stay raw JSON here, so one broken pose is dropped alone instead of failing the whole catalog. */
@Serializable
internal data class CatalogJson(
    val schema: Int = 1,
    val minAppSchema: Int = 1,
    val datasetVersion: String = "",
    val categories: List<CategoryJson> = emptyList(),
    val poses: List<JsonElement> = emptyList(),
)

@Serializable
internal data class CategoryJson(val id: String = "", val name: Map<String, String> = emptyMap())

@Serializable
internal data class PreviewJson(val small: String = "", val large: String = "")

@Serializable
internal data class PoseJson(
    val id: String = "",
    val category: String = "",
    val subjectType: String = "",
    val gender: String = "",
    val framing: String = "",
    val tags: List<String> = emptyList(),
    val difficulty: Int = 1,
    val starter: Boolean = false,
    // Catalogs made before premium poses (spec poses, question 6) have no such field: every pose is free.
    val premium: Boolean = false,
    val name: Map<String, String> = emptyMap(),
    val steps: Map<String, List<String>> = emptyMap(),
    val mistakes: Map<String, List<String>> = emptyMap(),
    val preview: PreviewJson = PreviewJson(),
    val outline: String = "",
    val keypoints: List<List<Float>>? = null,
)
