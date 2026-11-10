package com.vglstudio.camera.pose.line.guide

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * MainActivity places `FullScreenAdHost()` of :core:ads once, in onCreate, inside `AppTheme`, right after the
 * `NetworkDialogHost` call (HD-05; spec ads, question 18; ADR-1009; plan T-090, section 8, question 1), so the native
 * combo after an interstitial of a screen is drawn and can be closed.
 * MainActivity needs Hilt, which :app has no test setup for, so the call is read from the source, by symbol and in
 * code only: comments and string literals are left out (L-062). The working directory of a unit test is the module
 * directory.
 */
class MainActivityFullScreenAdHostWiringTest {
    @Test
    fun mainActivity_placesFullScreenAdHostOnce() {
        val code = codeOnly(File(MAIN_ACTIVITY).readText())
        // Control: the file read is the real MainActivity.
        assertTrue(Regex("""\bclass\s+MainActivity\b""").containsMatchIn(code))

        val calls = HOST_CALL.findAll(code).toList()
        assertEquals(1, calls.size)
        val call = calls.single().range.first
        val onCreate = checkNotNull(bodyOf(code, ON_CREATE)) { "no onCreate in MainActivity" }
        assertTrue("the call is in onCreate", call in onCreate)
        val appTheme = checkNotNull(bodyOf(code, APP_THEME)) { "no AppTheme(darkTheme = darkTheme) in MainActivity" }
        assertTrue("the call is inside AppTheme", call in appTheme)

        // Right after the NetworkDialogHost call: past the closing brace of its content lambda.
        val networkDialogHost = checkNotNull(NETWORK_DIALOG_HOST.find(code)) { "no NetworkDialogHost in MainActivity" }
        val argumentsEnd = closingParenOf(code, networkDialogHost.range.last)
        val content = checkNotNull(bracesFrom(code, argumentsEnd)) { "no content lambda after NetworkDialogHost(" }
        // Control: the NetworkDialogHost call sits inside AppTheme too, so "after it" means inside the same lambda.
        assertTrue(networkDialogHost.range.first in appTheme)
        assertTrue("the call comes after the content lambda of NetworkDialogHost", call > content.last)
    }

    private companion object {
        const val MAIN_ACTIVITY = "src/main/kotlin/com/vglstudio/camera/pose/line/guide/MainActivity.kt"

        val ON_CREATE = Regex("""\boverride\s+fun\s+onCreate\s*\(""")

        val APP_THEME = Regex("""\bAppTheme\(\s*darkTheme\s*=\s*darkTheme\s*\)""")

        val NETWORK_DIALOG_HOST = Regex("""\bNetworkDialogHost\(""")

        val HOST_CALL = Regex("""\bFullScreenAdHost\(\s*\)""")

        /** The range of the braces of the first function matching [header] in [code], or null. */
        fun bodyOf(code: String, header: Regex): IntRange? {
            val start = header.find(code) ?: return null
            return bracesFrom(code, start.range.last)
        }

        /** The range of the first `{` after [from] to its matching `}`, or null. */
        fun bracesFrom(code: String, from: Int): IntRange? {
            val open = code.indexOf('{', from)
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

        /** The index of the `)` that closes the `(` at [open]. */
        fun closingParenOf(code: String, open: Int): Int {
            check(code[open] == '(') { "no ( at $open" }
            var depth = 0
            for (i in open until code.length) {
                when (code[i]) {
                    '(' -> depth++
                    ')' -> if (--depth == 0) return i
                }
            }
            error("no ) closing the ( at $open")
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
