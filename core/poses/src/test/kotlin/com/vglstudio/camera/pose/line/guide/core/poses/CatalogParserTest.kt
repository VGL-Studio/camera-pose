package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParseResult
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParser
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseGender
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CatalogParserTest {
    private val logger = RecordingAppLogger()
    private val parser = CatalogParser(logger)

    private fun valid(text: String): CatalogParseResult.Valid {
        val result = parser.parse(text)
        assertTrue("Expected Valid but was $result", result is CatalogParseResult.Valid)
        return result as CatalogParseResult.Valid
    }

    @Test
    fun validCatalog_parsesCategoriesAndPosesInCatalogOrder() {
        val result = valid(Fixtures.text("valid.json"))

        assertEquals("2026.10.1", result.catalog.datasetVersion)
        assertEquals(listOf("women", "men", "couples"), result.catalog.categories.map { it.id })
        assertEquals(listOf("w01", "w02", "w03", "m01", "m02", "m03"), result.catalog.poses.map { it.id })
        assertEquals(0, result.droppedPoses)
    }

    @Test
    fun validCatalog_readsEveryPoseField_includingKeypointsAndStarter() {
        val pose = valid(Fixtures.text("valid.json")).catalog.poses.first { it.id == "w01" }

        assertEquals("women", pose.categoryId)
        assertEquals(SubjectType.SINGLE, pose.subjectType)
        assertEquals(PoseGender.FEMALE, pose.gender)
        assertEquals(Framing.FULL, pose.framing)
        assertEquals(listOf("standing", "casual"), pose.tags)
        assertEquals(1, pose.difficulty)
        assertTrue(pose.starter)
        assertEquals("Easy stance", pose.name.resolve(Locale.ENGLISH))
        assertEquals("Đứng thoải mái", pose.name.resolve(Locale.forLanguageTag("vi")))
        assertEquals(listOf("Step one of Easy stance", "Step two of Easy stance"), pose.steps.resolve(Locale.ENGLISH))
        assertEquals(listOf("Common mistake in Easy stance"), pose.mistakes.resolve(Locale.ENGLISH))
        assertEquals("poses/img/w01/preview_540-1a2b3c01.webp", pose.thumbnailPath.value)
        assertEquals("poses/img/w01/preview_1200-1a2b3c01.webp", pose.imagePath.value)
        assertEquals("poses/img/w01/outline-1a2b3c01.webp", pose.outlinePath.value)
        assertEquals(33, pose.keypoints?.size)
        assertEquals(Keypoint(0.31f, 0.05f, 0.99f), pose.keypoints?.first())
    }

    @Test
    fun poseWithoutKeypoints_hasNullKeypoints() {
        val pose = valid(Fixtures.text("valid.json")).catalog.poses.first { it.id == "w02" }
        assertNull(pose.keypoints)
    }

    @Test
    fun keypointsNot33Points_dropsKeypointsKeepsPose() {
        val result = valid(catalogWith(poseJson(keypoints = "[[0.5, 0.5, 1.0], [0.4, 0.4, 1.0]]")))

        assertEquals(listOf("p1"), result.catalog.poses.map { it.id })
        assertNull(result.catalog.poses.single().keypoints)
    }

    @Test
    fun starterMissing_isFalse() {
        val pose = valid(Fixtures.text("valid.json")).catalog.poses.first { it.id == "w03" }
        assertFalse(pose.starter)
    }

    @Test
    fun unknownFields_areIgnored() {
        val result = valid(catalogWith(poseJson(extra = ", \"futureField\": {\"a\": 1}"), rootExtra = ", \"hint\": 5"))
        assertEquals(listOf("p1"), result.catalog.poses.map { it.id })
    }

    @Test
    fun malformedJson_returnsInvalid() {
        assertEquals(CatalogParseResult.Invalid, parser.parse(Fixtures.text("malformed.json")))
    }

    @Test
    fun minAppSchemaNewerThanApp_returnsUnsupportedSchema() {
        assertEquals(
            CatalogParseResult.UnsupportedSchema(minAppSchema = 2),
            parser.parse(Fixtures.text("newer-schema.json")),
        )
    }

    @Test
    fun partlyInvalid_dropsDuplicateUnknownCategoryUnknownEnumMissingEnglishAndInvalidPath_keepsValidPoses() {
        val result = valid(Fixtures.text("partly-invalid.json"))

        assertEquals(listOf("a01", "a02"), result.catalog.poses.map { it.id })
        // The first of two poses with the same id is kept.
        assertEquals("Valid one", result.catalog.poses.first().name.resolve(Locale.ENGLISH))
        assertEquals(6, result.droppedPoses)
        val warnings = logger.entries.filter { it.level == LogLevel.WARN }
        assertTrue(warnings.isNotEmpty())
        // The log counts dropped poses; it never names them.
        assertTrue(warnings.none { warning -> listOf("a01", "b01", "b05").any { it in warning.message } })
    }

    @Test
    fun premium_readsTrue_missingAndFalseAreFree() {
        val result = valid(
            catalogWithPoses(
                poseJson(id = "p1", extra = ", \"premium\": true"),
                poseJson(id = "p2", extra = ", \"premium\": false"),
                poseJson(id = "p3"),
            ),
        )

        assertEquals(0, result.droppedPoses)
        assertEquals(listOf("p1", "p2", "p3"), result.catalog.poses.map { it.id })
        assertEquals(listOf(true, false, false), result.catalog.poses.map { it.premium })
    }

    @Test
    fun premiumOnStarter_isIgnored_andLogsCountWithoutIds() {
        val result = valid(
            catalogWithPoses(
                poseJson(id = "s1", extra = ", \"starter\": true, \"premium\": true"),
                poseJson(id = "s2", extra = ", \"starter\": true"),
                poseJson(id = "p3", extra = ", \"premium\": true"),
            ),
        )

        assertEquals(listOf("s1", "s2", "p3"), result.catalog.poses.map { it.id })
        assertEquals(listOf(true, true, false), result.catalog.poses.map { it.starter })
        // Control in the same catalog: a premium pose that is not a starter stays premium.
        assertEquals(listOf(false, false, true), result.catalog.poses.map { it.premium })
        val warnings = logger.entries.filter { it.level == LogLevel.WARN && it.tag == "Poses" }
        assertEquals(1, warnings.count { "premium on 1 starter" in it.message })
        assertTrue(warnings.none { warning -> listOf("s1", "s2", "p3").any { it in warning.message } })
    }

    private fun poseJson(keypoints: String? = null, extra: String = "", id: String = "p1"): String {
        val keypointsPart = keypoints?.let { ", \"keypoints\": $it" }.orEmpty()
        return """{"id": "$id", "category": "c", "subjectType": "single", "gender": "any", "framing": "full",
            "name": {"en": "P"}, "steps": {"en": ["s"]},
            "preview": {"small": "poses/img/$id/preview_540-1a2b3c4d.webp",
                        "large": "poses/img/$id/preview_1200-1a2b3c4d.webp"},
            "outline": "poses/img/$id/outline-1a2b3c4d.webp"$keypointsPart$extra}"""
    }

    private fun catalogWith(pose: String, rootExtra: String = ""): String =
        """{"schema": 1, "minAppSchema": 1, "datasetVersion": "t",
            "categories": [{"id": "c", "name": {"en": "C"}}], "poses": [$pose]$rootExtra}"""

    private fun catalogWithPoses(vararg poses: String): String = catalogWith(poses.joinToString(", "))

    @Test
    fun malformedJson_logsNoThrowable() {
        parser.parse(Fixtures.text("malformed.json"))
        assertTrue(logger.entries.isNotEmpty())
        assertTrue(logger.entries.all { it.error == null })
    }
}
