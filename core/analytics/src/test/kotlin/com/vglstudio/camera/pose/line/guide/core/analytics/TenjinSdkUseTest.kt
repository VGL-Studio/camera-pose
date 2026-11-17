package com.vglstudio.camera.pose.line.guide.core.analytics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The Tenjin SDK is called from one file only, TenjinSink.kt of :core:analytics (ADR-1010; plan T-099, step 2).
 * Scans the main sources of every module by import (L-062), like NoAppsFlyerSdkTest. The working directory of a unit
 * test is the module directory.
 */
class TenjinSdkUseTest {
    private val root = File("../..").canonicalFile

    private val mainSources: List<File> by lazy {
        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
    }

    @Test
    fun onlyTenjinLibSink_importsTheSdk() {
        // Control: the scan sees the main sources of this module, so the result below means something.
        assertTrue(
            "No main source of core/analytics found from ${root.path}",
            mainSources.any { "core/analytics/src/main/" in it.invariantSeparatorsPath },
        )
        val importing = mainSources.filter { file ->
            file.readLines().any { it.trimStart().startsWith("import com.tenjin") }
        }
        assertEquals(
            listOf("core/analytics/src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/analytics/TenjinSink.kt"),
            importing.map { it.relativeTo(root).invariantSeparatorsPath },
        )
    }

    private companion object {
        val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules")
    }
}
