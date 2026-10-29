package com.vglstudio.camera.pose.line.guide.core.analytics

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Where the AppsFlyer SDK is called (ADR-0027, decisions 1 and 3; section 8.3): one file of :core:analytics, the
 * plain event call only, TCF read before `init`, no user id, no debug log. Scans the main sources of every module by
 * symbol (L-062). The working directory of a unit test is the module directory.
 */
class AppsFlyerSdkUseTest {
    private val root = File("../..")

    private val mainSources: List<File> by lazy {
        root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.extension == "kt" && "/src/main/" in it.invariantSeparatorsPath }
            .toList()
    }

    @Test
    fun onlyAppsFlyerSink_importsTheSdk() {
        // Control: the scan sees the main sources of this module.
        assertTrue(
            "No main source found from ${root.absolutePath}",
            mainSources.any { "core/analytics/src/main/" in it.invariantSeparatorsPath },
        )
        val importing = mainSources.filter { file ->
            file.readLines().any { it.trimStart().startsWith("import com.appsflyer") }
        }
        assertEquals(listOf("AppsFlyerSink.kt"), importing.map { it.name })
    }

    @Test
    fun noAdRevenueApi() {
        assertTrue(mainSources.isNotEmpty())
        val offenders = mainSources.filter { file ->
            val text = file.readText()
            "logAdRevenue" in text || "import com.appsflyer.adrevenue" in text
        }
        assertTrue("Ad revenue API of AppsFlyer used in: $offenders", offenders.isEmpty())
    }

    @Test
    fun sink_noUserIdNoDebugLog_tcfBeforeInit() {
        val sink = mainSources.single { it.name == "AppsFlyerSink.kt" }.readText()
        assertFalse("setCustomerUserId(" in sink)
        assertFalse("setDebugLog(" in sink)
        val tcf = sink.indexOf("enableTCFDataCollection(true)")
        val init = sink.indexOf(".init(")
        // Control: both calls are there.
        assertTrue("enableTCFDataCollection(true) not called", tcf >= 0)
        assertTrue(".init( not called", init >= 0)
        assertTrue("TCF must be enabled before init", tcf < init)
    }

    private companion object {
        val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules")
    }
}
