package com.vglstudio.camera.pose.line.guide

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The developer's test devices exist only in the staging build (plan T-058, KB-28): no source set but staging reads
 * the build field, and production provides no test device and no per-network log. Production is not built on CI, so
 * this scans the sources of :app. The working directory of a unit test is the module directory.
 */
class AdsDebugOptionsSourceSetTest {
    private val sources = File("src")

    // Staging reads the field; test sources mention it to check it.
    private val excludedSourceSets = setOf("staging", "testStaging", "test")

    /** The code of [file], without comment lines. */
    private fun code(file: File): String = file.readLines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

    @Test
    fun onlyStaging_readsTestDeviceField() {
        // Control: the source sets are where this test expects them, and staging reads the field.
        assertTrue("No src/main in ${sources.absolutePath}", File(sources, "main").isDirectory)
        assertTrue(FIELD in code(File(sources, STAGING_MODULE)))

        val offenders = sources.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in excludedSourceSets }
            .flatMap { set -> set.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
            .filter { FIELD in code(it) }
        assertTrue("Only src/staging may read $FIELD, found: $offenders", offenders.isEmpty())
    }

    @Test
    fun production_providesNone() {
        val module = File(sources, PRODUCTION_MODULE)
        assertTrue("Missing ${module.path}", module.isFile)
        val text = code(module)

        assertTrue(text, "AdsDebugOptions.NONE" in text)
        assertFalse(text, "AdsDebugOptions.staging(" in text)
    }

    private companion object {
        const val FIELD = "ADS_TEST_DEVICE_IDS"
        const val STAGING_MODULE = "staging/kotlin/com/vglstudio/camera/pose/line/guide/di/StagingAdsModule.kt"
        const val PRODUCTION_MODULE = "production/kotlin/com/vglstudio/camera/pose/line/guide/di/ProductionAdsModule.kt"
    }
}
