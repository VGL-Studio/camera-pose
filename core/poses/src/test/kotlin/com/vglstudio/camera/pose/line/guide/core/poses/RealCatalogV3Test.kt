package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParseResult
import com.vglstudio.camera.pose.line.guide.core.poses.internal.CatalogParser
import com.vglstudio.camera.pose.line.guide.core.poses.internal.DEFAULT_CATALOG_PATH
import com.vglstudio.camera.pose.line.guide.core.poses.internal.buildPoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import java.security.MessageDigest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The real pose catalog v3 on the staging bucket, kept byte for byte as a fixture (plan T-071).
 * The parser must read all of its poses, and the default catalog path must name this file by its hash.
 */
class RealCatalogV3Test {
    private val fixtureName = "catalog-v3-512aa695.json"

    private fun parse(): CatalogParseResult.Valid {
        val result = CatalogParser(RecordingAppLogger()).parse(Fixtures.text(fixtureName))
        assertTrue("Expected Valid but was ${result::class.simpleName}", result is CatalogParseResult.Valid)
        return result as CatalogParseResult.Valid
    }

    private fun catalog(): PoseCatalog = parse().catalog

    @Test
    fun realCatalog_parsesAll350Poses_noneDropped_fiftyStarter() {
        val result = parse()

        assertEquals(0, result.droppedPoses)
        assertEquals(350, result.catalog.poses.size)
        assertEquals(50, result.catalog.poses.count { it.starter })
        assertEquals(
            listOf("women", "men", "couples", "friends", "family", "kids", "travel", "cafe"),
            result.catalog.categories.map { it.id },
        )
    }

    @Test
    fun realCatalog_libraryShowsOnlyCategoriesWithPoses() {
        val library = buildPoseLibrary(catalog(), emptyList(), emptyList())

        assertEquals(
            listOf("women", "men", "couples", "friends", "family", "travel", "cafe"),
            library.categories.map { it.id },
        )
    }

    @Test
    fun realCatalog_hasNoBorrowedAsset() {
        val poses = catalog().poses
        assertEquals(350, poses.size)

        val borrowed = poses.flatMap { pose ->
            listOf(pose.thumbnailPath, pose.imagePath, pose.outlinePath).map { it.value }
        }.filter { it.startsWith("borrowed/") }

        assertEquals(emptyList<String>(), borrowed)
    }

    @Test
    fun defaultCatalog_isTheRealCatalogV3() {
        val bytes = Fixtures.text(fixtureName).toByteArray(Charsets.UTF_8)
        val sha8 = MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) }
            .take(8)

        assertEquals("poses/catalog/v3/catalog-$sha8.json", DEFAULT_CATALOG_PATH)
    }
}
