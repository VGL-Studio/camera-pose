package com.vglstudio.camera.pose.line.guide.core.analytics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppsFlyer is not in this app (spec ads, question 3; plan T-074, section 4): no main source imports its SDK, and the
 * sink that :core:analytics binds does nothing. Scans the main sources of every module by symbol (L-062), like
 * AppsFlyerSdkUseTest of the template, which this test replaces. The working directory of a unit test is the module
 * directory.
 */
class NoAppsFlyerSdkTest {
    private val root = File("../..")

    private val mainSources: List<File> by lazy {
        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
    }

    @Test
    fun noMainSource_importsTheSdk() {
        // Control: the scan sees the main sources of this module, so an empty result means something.
        assertTrue(
            "No main source of core/analytics found from ${root.absolutePath}",
            mainSources.any { "core/analytics/src/main/" in it.invariantSeparatorsPath },
        )
        val importing = mainSources.filter { file ->
            file.readLines().any { it.trimStart().startsWith("import com.appsflyer") }
        }
        assertEquals(emptyList<String>(), importing.map { it.invariantSeparatorsPath.substringAfter("/../../") })
    }

    @Test
    fun sink_isNoOp() {
        val module = mainSources.single {
            it.invariantSeparatorsPath.endsWith("core/analytics/di/AnalyticsModule.kt")
        }.readText()
        // Control: the module binds a sink at all, so the check below reads the right line.
        assertTrue("No bindAppsFlyerSink in AnalyticsModule", "fun bindAppsFlyerSink(" in module)
        assertTrue(
            "AnalyticsModule must bind NoOpAppsFlyerSink",
            "bindAppsFlyerSink(impl: NoOpAppsFlyerSink)" in module,
        )
        val sink: AppsFlyerSink = NoOpAppsFlyerSink()
        // None of the calls throws, and none can reach an SDK (there is none).
        sink.init("dev-key")
        sink.start()
        sink.logEvent("af_test", mapOf("a" to 1))
        sink.logEvent("af_test", null)
    }

    private companion object {
        val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules")
    }
}
