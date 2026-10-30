package com.vglstudio.camera.pose.line.guide.network

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainActivity reports its starts and stops to the network dialog from onCreate (spec network KB-3; plan T-077).
 * MainActivity needs Hilt, which :app has no test setup for, so the call is read from the source, by symbol and in
 * code only: comments and string literals are left out. The working directory of a unit test is the module directory.
 */
class MainActivityForegroundWiringTest {
    @Test
    fun mainActivity_observesAppForeground() {
        val code = codeOnly(File(MAIN_ACTIVITY).readText())
        // Control: the file read is the real MainActivity.
        assertTrue(Regex("""\bclass\s+MainActivity\b""").containsMatchIn(code))

        val calls = WIRING.findAll(code).toList()
        assertEquals(1, calls.size)
        val onCreate = checkNotNull(bodyOf(code, ON_CREATE)) { "no onCreate in MainActivity" }
        assertTrue("the call is in onCreate", calls.single().range.first in onCreate)
    }

    private companion object {
        const val MAIN_ACTIVITY = "src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt"

        val ON_CREATE = Regex("""\boverride\s+fun\s+onCreate\s*\(""")

        val WIRING = Regex(
            """\bobserveAppForeground\(\s*onStarted\s*=\s*networkDialog::onAppStarted\s*,\s*""" +
                """onStopped\s*=\s*networkDialog::onAppStopped\s*,?\s*\)""",
        )

        /** The range of the braces of the first function matching [header] in [code], or null. */
        fun bodyOf(code: String, header: Regex): IntRange? {
            val start = header.find(code) ?: return null
            val open = code.indexOf('{', start.range.last)
            if (open < 0) return null
            var depth = 0
            for (i in open until code.length) {
                when (code[i]) {
                    '{' -> depth++
                    '}' -> if (--depth == 0) return open..i
                }
            }
            return null
        }

        /** [source] with its comments and string literals blanked out, as in LeaveAppCallSitesTest. */
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
