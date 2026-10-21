package com.vglstudio.camera.pose.line.guide.core.poses.internal

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCategory
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseGender
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import kotlinx.serialization.json.Json

/** Catalog schema this app version understands. */
internal const val APP_SCHEMA = 1

private const val ENGLISH = "en"
private const val KEYPOINT_COUNT = 33
private const val KEYPOINT_VALUES = 3

internal sealed interface CatalogParseResult {
    data class Valid(val catalog: PoseCatalog, val droppedPoses: Int) : CatalogParseResult

    data object Invalid : CatalogParseResult

    data class UnsupportedSchema(val minAppSchema: Int) : CatalogParseResult
}

/**
 * Reads the catalog JSON. A pose that cannot be used is dropped alone and the rest is kept; the log counts them
 * by reason and never names a pose.
 */
internal class CatalogParser(private val logger: AppLogger) {
    private val json = Json { ignoreUnknownKeys = true }

    fun parse(text: String): CatalogParseResult {
        val root = try {
            json.decodeFromString(CatalogJson.serializer(), text)
        } catch (e: IllegalArgumentException) {
            logger.warn(LOG_TAG, "The catalog is not valid JSON of the expected shape", e)
            return CatalogParseResult.Invalid
        }
        if (root.minAppSchema > APP_SCHEMA) {
            logger.warn(LOG_TAG, "The catalog needs a newer app: minAppSchema ${root.minAppSchema}, app $APP_SCHEMA")
            return CatalogParseResult.UnsupportedSchema(root.minAppSchema)
        }
        val categories = readCategories(root.categories)
        val dropped = mutableMapOf<String, Int>()
        val keptIds = mutableSetOf<String>()
        val poses = root.poses.mapNotNull { element ->
            val pose = try {
                json.decodeFromJsonElement(PoseJson.serializer(), element)
            } catch (e: IllegalArgumentException) {
                dropped.merge(REASON_MALFORMED, 1, Int::plus)
                return@mapNotNull null
            }
            when (val converted = convert(pose, categories.map { it.id }.toSet(), keptIds)) {
                is Converted.Kept -> converted.pose.also { keptIds += it.id }
                is Converted.Dropped -> {
                    dropped.merge(converted.reason, 1, Int::plus)
                    null
                }
            }
        }
        val droppedCount = dropped.values.sum()
        if (droppedCount > 0) {
            logger.warn(LOG_TAG, "Dropped $droppedCount poses of the catalog: ${dropped.entries.joinToString()}")
        }
        return CatalogParseResult.Valid(PoseCatalog(root.datasetVersion, categories, poses), droppedCount)
    }

    private fun readCategories(raw: List<CategoryJson>): List<PoseCategory> {
        val seen = mutableSetOf<String>()
        val categories = raw.filter { it.id.isNotBlank() && !it.name[ENGLISH].isNullOrBlank() && seen.add(it.id) }
            .map { PoseCategory(it.id, LocalizedText(it.name)) }
        if (categories.size < raw.size) {
            logger.warn(LOG_TAG, "Dropped ${raw.size - categories.size} categories of the catalog")
        }
        return categories
    }

    private sealed interface Converted {
        data class Kept(val pose: Pose) : Converted

        data class Dropped(val reason: String) : Converted
    }

    private fun convert(pose: PoseJson, categoryIds: Set<String>, keptIds: Set<String>): Converted {
        val subjectType = SubjectType.entries.firstOrNull { it.name.equals(pose.subjectType, ignoreCase = true) }
        val gender = PoseGender.entries.firstOrNull { it.name.equals(pose.gender, ignoreCase = true) }
        val framing = Framing.entries.firstOrNull { it.name.equals(pose.framing, ignoreCase = true) }
        val paths = listOf(pose.preview.small, pose.preview.large, pose.outline).map { toAssetPath(it) }
        return when {
            pose.id.isBlank() -> Converted.Dropped(REASON_MALFORMED)
            pose.id in keptIds -> Converted.Dropped(REASON_DUPLICATE)
            pose.category !in categoryIds -> Converted.Dropped(REASON_UNKNOWN_CATEGORY)
            subjectType == null || gender == null || framing == null -> Converted.Dropped(REASON_UNKNOWN_VALUE)
            pose.name[ENGLISH].isNullOrBlank() || pose.steps[ENGLISH].isNullOrEmpty() ->
                Converted.Dropped(REASON_MISSING_ENGLISH)
            paths.any { it == null } -> Converted.Dropped(REASON_INVALID_PATH)
            else -> Converted.Kept(
                Pose(
                    id = pose.id,
                    categoryId = pose.category,
                    subjectType = subjectType,
                    gender = gender,
                    framing = framing,
                    tags = pose.tags,
                    difficulty = pose.difficulty,
                    starter = pose.starter,
                    name = LocalizedText(pose.name),
                    steps = LocalizedList(pose.steps),
                    mistakes = LocalizedList(pose.mistakes),
                    keypoints = toKeypoints(pose.keypoints),
                    thumbnailPath = checkNotNull(paths[0]),
                    imagePath = checkNotNull(paths[1]),
                    outlinePath = checkNotNull(paths[2]),
                ),
            )
        }
    }

    private fun toAssetPath(value: String): AssetPath? = try {
        AssetPath(value)
    } catch (e: IllegalArgumentException) {
        null
    }

    /** Exactly 33 points of [x, y, visibility] with x and y inside the image; anything else means no keypoints. */
    private fun toKeypoints(raw: List<List<Float>>?): List<Keypoint>? {
        if (raw == null || raw.size != KEYPOINT_COUNT) return null
        val valid = raw.all { it.size == KEYPOINT_VALUES && it[0] in 0f..1f && it[1] in 0f..1f }
        return if (valid) raw.map { Keypoint(it[0], it[1], it[2]) } else null
    }

    private companion object {
        const val REASON_MALFORMED = "malformed"
        const val REASON_DUPLICATE = "duplicate"
        const val REASON_UNKNOWN_CATEGORY = "unknownCategory"
        const val REASON_UNKNOWN_VALUE = "unknownValue"
        const val REASON_MISSING_ENGLISH = "missingEnglish"
        const val REASON_INVALID_PATH = "invalidPath"
    }
}
