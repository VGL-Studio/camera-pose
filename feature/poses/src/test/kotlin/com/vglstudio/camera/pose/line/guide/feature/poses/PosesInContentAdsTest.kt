package com.vglstudio.camera.pose.line.guide.feature.poses

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The library declares its ad cells as in-content ads (MC-13; spec ads, question 22, point e; spec poses, KB-6; plan
 * T-102, section 4): [PosesRoute] wraps its bottom bar in `InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION))`, so
 * the bar has no floating layer while `is_show_poses_in_collection` is on. The behaviour of `InContentAds` is tested in
 * :core:ads (`InContentAdsTest`, `InContentAdsWiringTest`); a feature test has no ads controller, so this test reads
 * the source by symbol, in code only: comments, KDoc and string literals are left out (as `LeaveAppCallSitesTest` of
 * :app). The working directory of a unit test is the module directory.
 */
class PosesInContentAdsTest {
    @Test
    fun posesRoute_wrapsBottomBarInInContentAds() {
        val source = File(SOURCE)
        // Control: the source read is the real one.
        assertTrue("$SOURCE not found from ${File(".").absolutePath}", source.isFile)
        val code = codeOnly(source.readText())
        assertTrue(
            "PosesScreen.kt imports InContentAds",
            code.lines().any { it.trim() == "import $ADS_PACKAGE.InContentAds" },
        )
        val body = functionBody(code, "PosesRoute")
        assertNotNull("fun PosesRoute not found", body)
        // Control: the body read is the one of the route, with its bottom bar.
        assertTrue("PosesRoute has a bottom bar", BOTTOM_BAR_CALL.containsMatchIn(body!!))
        val call = callWithLambda(body, "InContentAds")
        assertNotNull("PosesRoute does not call InContentAds { ... }", call)
        val (arguments, lambda) = call!!
        assertTrue(
            "arguments: $arguments",
            Regex("""\bAdPlacement\.POSES_IN_COLLECTION\b""").containsMatchIn(arguments),
        )
        assertFalse("the bar's own native small is not listed: $arguments", arguments.contains("POSES_BOTTOM"))
        assertTrue("the bottom bar is inside InContentAds { ... }", BOTTOM_BAR_CALL.containsMatchIn(lambda))
    }

    @Test
    fun scanner_findsTheRealCall_butNotInCommentsOrStrings() {
        val real = """
            /** KDoc: [InContentAds] around the bar. */
            @Composable
            fun PosesRoute(onBack: () -> Unit, viewModel: PosesViewModel = hiltViewModel()) {
                val text = "}"
                InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) {
                    BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(padding) }
                }
            }
        """.trimIndent()
        val realCall = callWithLambda(functionBody(codeOnly(real), "PosesRoute")!!, "InContentAds")
        assertNotNull(realCall)
        assertTrue(realCall!!.first.contains("AdPlacement.POSES_IN_COLLECTION"))
        assertTrue(BOTTOM_BAR_CALL.containsMatchIn(realCall.second))

        val quiet = """
            // InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) {} }
            /** KDoc: InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { } */
            /* InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { } */
            @Composable
            fun PosesRoute() {
                val text = "InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) { }"
                BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(padding) }
            }
        """.trimIndent()
        val quietBody = functionBody(codeOnly(quiet), "PosesRoute")
        assertNotNull(quietBody)
        assertTrue(BOTTOM_BAR_CALL.containsMatchIn(quietBody!!))
        assertNull(callWithLambda(quietBody, "InContentAds"))

        val elsewhere = """
            @Composable
            fun PosesRoute() {
                BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(padding) }
            }

            @Composable
            fun OtherRoute() {
                InContentAds(listOf(AdPlacement.POSES_IN_COLLECTION)) {
                    BottomAdBarScaffold(AdPlacement.POSES_BOTTOM) { padding -> PosesScreen(padding) }
                }
            }
        """.trimIndent()
        val elsewhereCode = codeOnly(elsewhere)
        assertNull(callWithLambda(functionBody(elsewhereCode, "PosesRoute")!!, "InContentAds"))
        // Control: the call of the other function is found when that function is read.
        assertNotNull(callWithLambda(functionBody(elsewhereCode, "OtherRoute")!!, "InContentAds"))
        assertNull(functionBody(elsewhereCode, "MissingRoute"))
    }

    private companion object {
        const val SOURCE = "src/main/kotlin/com/vglstudio/camera/pose/line/guide/feature/poses/PosesScreen.kt"
        const val ADS_PACKAGE = "com.vglstudio.camera.pose.line.guide.core.ads"

        /** The bottom bar of the library: `BottomAdBarScaffold(AdPlacement.POSES_BOTTOM`. */
        val BOTTOM_BAR_CALL = Regex("""\bBottomAdBarScaffold\s*\(\s*AdPlacement\.POSES_BOTTOM\b""")

        /** The text between the braces of the body of `fun [name](...)` in [code], or null when there is none. */
        fun functionBody(code: String, name: String): String? {
            val start = Regex("""\bfun\s+$name\s*\(""").find(code) ?: return null
            val paramsEnd = matching(code, start.range.last, '(', ')') ?: return null
            val open = code.indexOf('{', paramsEnd)
            if (open < 0) return null
            val close = matching(code, open, '{', '}') ?: return null
            return code.substring(open + 1, close)
        }

        /**
         * The first call `[name](arguments) { lambda }` in [code]: its arguments and the text of its trailing lambda,
         * or null when there is no such call.
         */
        fun callWithLambda(code: String, name: String): Pair<String, String>? {
            val call = Regex("""\b$name\s*\(""").find(code) ?: return null
            val argumentsEnd = matching(code, call.range.last, '(', ')') ?: return null
            var open = argumentsEnd + 1
            while (open < code.length && code[open].isWhitespace()) open++
            if (code.getOrNull(open) != '{') return null
            val close = matching(code, open, '{', '}') ?: return null
            return code.substring(call.range.last + 1, argumentsEnd) to code.substring(open + 1, close)
        }

        /** The index of the [closing] that matches the [opening] at [from] in [code], or null. */
        fun matching(code: String, from: Int, opening: Char, closing: Char): Int? {
            var depth = 0
            for (i in from until code.length) {
                when (code[i]) {
                    opening -> depth++
                    closing -> if (--depth == 0) return i
                }
            }
            return null
        }

        /**
         * [source] with its comments and string literals blanked out, keeping the line breaks (as `LeaveAppCallSitesTest`
         * of :app). Enough for the source read here: no raw string holds a quote that ends it early.
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
