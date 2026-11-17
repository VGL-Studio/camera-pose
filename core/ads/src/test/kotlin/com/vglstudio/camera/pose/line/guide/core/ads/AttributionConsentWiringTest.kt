package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * [AdsController.start] tells the attribution SDKs when consent is known (plan T-099, step 7a; spec ads, question 21
 * i): the signal itself is tested on flows in AttributionStartTest, and this test checks the call is wired to the
 * consent round and the gate of the controller. Scans the source like AdNetworkLogWiringTest, after removing every
 * blank and line break, so the way ktlint wraps the call does not matter. The working directory of a unit test is
 * the module directory.
 */
class AttributionConsentWiringTest {
    private val controller: String by lazy {
        val file = File("src/main/kotlin").walkTopDown().first { it.isFile && it.name == "AdsController.kt" }
        code(file.readText())
    }

    @Test
    fun start_notifiesConsentKnown() {
        val body = bodyOf(controller, "fun start(")
        // Control: the body found is the one of start(), which also waits for AppsFlyer.
        assertTrue(body, "awaitAttributionStart(" in body)
        // Control: the check below fails on a start() without the call, and on a call outside a launch.
        assertFalse(notifiesInLaunch("{scope.launch{awaitAttributionStart(sdkStarted,a,b,c)}}"))
        assertFalse(notifiesInLaunch("{$CALL logger,TAG)scope.launch{}}"))
        assertTrue(notifiesInLaunch("{scope.launch{$CALL logger,TAG)}}"))

        assertTrue("start() does not call $CALL in a scope.launch:\n$body", notifiesInLaunch(body))
    }

    /** True when a `scope.launch { ... }` block of [body] (no blanks) calls [CALL]. */
    private fun notifiesInLaunch(body: String): Boolean {
        var from = 0
        while (true) {
            val launch = body.indexOf(LAUNCH, from)
            if (launch < 0) return false
            val open = launch + LAUNCH.length - 1
            val block = body.substring(open, closingBracket(body, open) + 1)
            if (CALL in block) return true
            from = open + 1
        }
    }

    /** The body of the function declared by [declaration], from its first `{` to the matching `}`, with no blanks. */
    private fun bodyOf(text: String, declaration: String): String {
        val start = text.indexOf(declaration)
        assertTrue("No $declaration", start >= 0)
        val open = text.indexOf('{', start)
        return text.substring(open, closingBracket(text, open) + 1).replace(Regex("""\s+"""), "")
    }

    /** Index of the bracket that closes the one at [open], skipping strings. */
    private fun closingBracket(text: String, open: Int): Int {
        var depth = 0
        var inString = false
        for (index in open until text.length) {
            val char = text[index]
            when {
                char == '"' && text[index - 1] != '\\' -> inString = !inString
                inString -> Unit
                char in "([{" -> depth++
                char in ")]}" -> {
                    depth--
                    if (depth == 0) return index
                }
            }
        }
        error("No closing bracket at $open")
    }

    /** [text] without comment lines, so a name in a KDoc is not a call. */
    private fun code(text: String): String = text.lines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

    private companion object {
        const val LAUNCH = "scope.launch{"
        const val CALL = "notifyAttributionConsentKnown(coordinator.finished,gate.state,attributionConsent,"
    }
}
