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

/** The sample catalog the developer uploads to R2 staging (plan T-009, section 3.3). */
class StagingSampleCatalogTest {
    private fun sample(): PoseCatalog {
        val result = CatalogParser(RecordingAppLogger()).parse(Fixtures.text("staging-sample.json"))
        assertTrue("Expected Valid but was $result", result is CatalogParseResult.Valid)
        result as CatalogParseResult.Valid
        assertEquals(0, result.droppedPoses)
        return result.catalog
    }

    @Test
    fun stagingSample_parsesSixPoses_fourStarter_andHidesEmptyCouples() {
        val catalog = sample()

        assertEquals(6, catalog.poses.size)
        assertEquals(listOf("w01", "w02", "m01", "m02"), catalog.poses.filter { it.starter }.map { it.id })
        assertEquals(listOf("women", "men", "couples"), catalog.categories.map { it.id })
        val library = buildPoseLibrary(catalog, emptyList(), emptyList())
        assertEquals(listOf("women", "men"), library.categories.map { it.id })
    }

    /**
     * Intent: the app's default catalog path names the catalog file the repo keeps a copy of, by the hash of its
     * content (ADR-0015). The name comes from T-009, when the default was the v1 staging sample; plan T-071 moved the
     * default to the real catalog v3, so the assertion now hashes that fixture (ADR-0031). The name is kept because
     * removing or renaming a `@Test` function of the main branch is blocked in review (L-095).
     */
    @Test
    fun defaultCatalogPath_isV1Folder_andEndsWithFirst8HexOfSha256OfStagingSample() {
        val bytes = Fixtures.text("catalog-v3-512aa695.json").toByteArray(Charsets.UTF_8)
        val sha = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }

        assertEquals("poses/catalog/v3/catalog-${sha.take(8)}.json", DEFAULT_CATALOG_PATH)
    }

    @Test
    fun stagingSample_everyImagePathIsAValidBorrowedV1AssetPath() {
        sample().poses.forEach { pose ->
            val folder = "borrowed/poses/img/v1/${pose.id}/"
            assertEquals(folder + "preview_540.webp", pose.thumbnailPath.value)
            assertEquals(folder + "preview_1200.webp", pose.imagePath.value)
            assertEquals(folder + "outline.webp", pose.outlinePath.value)
        }
    }
}
