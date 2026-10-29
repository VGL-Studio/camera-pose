package com.vglstudio.camera.pose.line.guide

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Every way a feature leaves the app on purpose goes through the helpers of :core:ads (plan T-074, pass 2; G-03,
 * G-05): no source of `feature/<name>/src/main` nor `core/review/src/main` calls `rememberLauncherForActivityResult`,
 * `registerForActivityResult`, `requestPermissions` or `openUri`, or starts an activity itself, so the resume ad never shows right after a permission dialog, the share sheet or a
 * system page. Read by symbol, in code only: comments and string literals are left out. The working directory of a
 * unit test is the module directory.
 */
class LeaveAppCallSitesTest {
    private val root = File("..")

    @Test
    fun noFeatureNorReviewSource_leavesTheAppDirectly() {
        val sources = scannedSources()
        val texts = sources.associateWith { codeOnly(it.readText()) }
        // Control: the sources read are the real ones.
        assertTrue(sources.any { it.invariantSeparatorsPath.endsWith("feature/camera/CameraScreen.kt") })

        val direct = texts.flatMap { (file, text) -> directCalls(text).map { "${relative(file)}:$it" } }
        assertEquals("Direct calls, use the helpers of :core:ads", emptyList<String>(), direct)
        // Control: the call sites of the plan go through the helpers, so the scan above did read them.
        assertTrue(texts.values.any { Regex("""\brememberAdHoldingLauncher\b""").containsMatchIn(it) })
        assertTrue(texts.values.any { Regex("""\brememberStartOutsideApp\b""").containsMatchIn(it) })
    }

    @Test
    fun scanner_findsARawCall_butNotInCommentsOrStrings() {
        val raw = """
            val launcher = rememberLauncherForActivityResult(RequestPermission()) { }
            fun open() { context.startActivity(intent) }
            fun other() { activity?.startActivityForResult(intent, 1) }
            fun link() { uriHandler.openUri(url) }
            val result = registerForActivityResult(StartActivityForResult()) { }
            fun ask() { ActivityCompat.requestPermissions(activity, permissions, 1) }
            val go = ::startActivity
        """.trimIndent()
        assertEquals(
            listOf(
                "1 rememberLauncherForActivityResult",
                "2 startActivity",
                "3 startActivityForResult",
                "4 openUri",
                "5 registerForActivityResult",
                "6 requestPermissions",
                "7 startActivity",
            ),
            directCalls(codeOnly(raw)),
        )

        val quiet = """
            // context.startActivity(intent) in a line comment
            /* rememberLauncherForActivityResult in a block comment */
            /** KDoc: [startActivity] */
            val text = "startActivity(intent)"
            val launcher = rememberAdHoldingLauncher(RequestPermission()) { }
            val start = rememberStartOutsideApp()
            fun startActivityLater() = Unit
        """.trimIndent()
        assertEquals(emptyList<String>(), directCalls(codeOnly(quiet)))
    }

    private fun scannedSources(): List<File> {
        val featureMains = File(root, "feature").listFiles().orEmpty()
            .map { File(it, "src/main") }
            .filter { it.isDirectory }
        val dirs = featureMains + File(root, "core/review/src/main")
        // Control: every feature module is read, not only one.
        assertTrue(featureMains.size >= MIN_FEATURE_MODULES)
        return dirs.flatMap { dir -> dir.walkTopDown().filter { it.isFile && it.extension == "kt" }.toList() }
    }

    private fun relative(file: File): String = file.invariantSeparatorsPath.substringAfter("../")

    private companion object {
        const val MIN_FEATURE_MODULES = 5

        /** The calls that leave the app or open a system screen without the helpers of :core:ads. */
        private const val NAMES = "rememberLauncherForActivityResult|registerForActivityResult|startActivity|" +
            "startActivityForResult|startActivities|startIntentSender|startIntentSenderForResult|openUri|" +
            "requestPermissions"

        /** A call (`name(`, `name {`, `name<`) or a function reference (`::name`) of one of [NAMES]. */
        val DIRECT_CALL = Regex("""\b($NAMES)\s*(?=[({<])|::($NAMES)\b""")

        /** "line symbol" of each direct call in [code], lines counted from 1. */
        fun directCalls(code: String): List<String> = code.lines().flatMapIndexed { index, line ->
            DIRECT_CALL.findAll(line).map { match ->
                "${index + 1} ${match.groupValues[1].ifEmpty { match.groupValues[2] }}"
            }.toList()
        }

        /**
         * [source] with its comments and string literals blanked out, keeping the line breaks so line numbers stay
         * right. Enough for the sources read here: no raw string holds a quote that ends it early.
         */
        fun codeOnly(source: String): String {
            val out = StringBuilder(source.length)
            var i = 0
            while (i < source.length) {
                val c = source[i]
                val next = source.getOrNull(i + 1)
                when {
                    c == '/' && next == '/' -> {
                        while (i < source.length && source[i] != '\n') i++
                    }
                    c == '/' && next == '*' -> {
                        val end = source.indexOf("*/", i + 2).let { if (it < 0) source.length else it + 2 }
                        source.substring(i, end).forEach { if (it == '\n') out.append('\n') }
                        i = end
                    }
                    source.startsWith("\"\"\"", i) -> {
                        val end = source.indexOf("\"\"\"", i + 3).let { if (it < 0) source.length else it + 3 }
                        source.substring(i, end).forEach { if (it == '\n') out.append('\n') }
                        out.append("\"\"")
                        i = end
                    }
                    c == '"' || c == '\'' -> {
                        var j = i + 1
                        while (j < source.length && source[j] != c && source[j] != '\n') {
                            if (source[j] == '\\') j++
                            j++
                        }
                        out.append(c).append(c)
                        i = (j + 1).coerceAtMost(source.length)
                    }
                    else -> {
                        out.append(c)
                        i++
                    }
                }
            }
            return out.toString()
        }
    }
}
