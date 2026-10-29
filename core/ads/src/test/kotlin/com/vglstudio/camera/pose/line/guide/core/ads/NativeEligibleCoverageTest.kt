package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every place that draws a native ad reports `af_native_ad_eligible` (section 8.3; plan T-033), slots of later plans
 * included. Scans the main sources of :core:ads by symbol (L-062): the functions whose body calls
 * [ReportNativeEligible] are the reporting gates; every file that binds a native ad ([BoundNativeAd]) must call
 * [ReportNativeEligible] or one of those gates. The working directory of a unit test is the module directory.
 */
class NativeEligibleCoverageTest {
    private val sources: List<File> by lazy {
        File("src/main").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    /** The code of [file], without comment lines, so a name in a KDoc is not a call. */
    private fun code(file: File): String = file.readLines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

    /** Names of the functions whose body calls [ReportNativeEligible], read from the sources. */
    private fun reportingFunctions(): Set<String> = sources.flatMap { file ->
        val text = code(file)
        val declarations = FUNCTION.findAll(text).toList()
        declarations.mapIndexedNotNull { index, match ->
            val name = match.groupValues[1]
            val bodyEnd = declarations.getOrNull(index + 1)?.range?.first ?: text.length
            val body = text.substring(match.range.last + 1, bodyEnd)
            name.takeIf { it != REPORT && "$REPORT(" in body }
        }
    }.toSet()

    @Test
    fun everyFileDrawingANativeAd_reportsEligible() {
        val gates = reportingFunctions()
        // Control: the scan found gates and files that draw native ads.
        assertTrue("No function calls $REPORT: $gates", gates.isNotEmpty())
        val drawing = sources.filter { "$BOUND(" in code(it) && it.name !in EXCEPTIONS }
        assertTrue("No file calls $BOUND", drawing.isNotEmpty())

        val missing = drawing.filterNot { file ->
            val text = code(file)
            "$REPORT(" in text || gates.any { gate -> Regex("""(?<![\w.])$gate\(""").containsMatchIn(text) }
        }
        assertTrue(
            "Native ads drawn with no af_native_ad_eligible (call $REPORT or one of $gates): " +
                missing.map { it.name },
            missing.isEmpty(),
        )
    }

    private companion object {
        const val REPORT = "ReportNativeEligible"
        const val BOUND = "BoundNativeAd"
        val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""")

        /**
         * Files that draw a native ad with no slot of their own: the definition of [BoundNativeAd], and the
         * native full-screen turn (standalone and combo), whose eligible event is sent by [AdsController].
         */
        val EXCEPTIONS = setOf("NativeAdViewBinding.kt", "NativeFullScreenAdHost.kt", "NativeFullScreenOverlay.kt")
    }
}
