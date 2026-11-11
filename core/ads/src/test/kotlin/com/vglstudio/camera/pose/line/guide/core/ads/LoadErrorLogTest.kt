package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** A real message of a native full-screen load seen on the emulator (RUN-20261025-01). */
internal const val NATIVE_MESSAGE = "Incorrect native ad response. Click actions were not properly specified"

/** A message with the test ad unit of Google (allowed by scripts/secret-scan.py); masked to `No ad for <ad unit>.`. */
internal const val ADMOB_UNIT_MESSAGE = "No ad for ca-app-pub-3940256099942544/2247696110."

/** A message with an ad unit of MAX in its shape (16 hex characters); masked to `No fill for <ad unit>`. */
internal const val MAX_UNIT_MESSAGE = "No fill for 2d0ec3c0d7d4b5ad"

/** The SDK message in the load error line of staging (plan T-093, KB-28): format, masks, one line, cap. */
class LoadErrorLogTest {
    @Test
    fun staging_codeAndMessage() {
        assertEquals("3 \"No fill.\"", LoadErrorLog(true).text(3, "No fill."))
        assertEquals("3 \"No fill.\"", loadErrorText(3, "No fill."))
        assertEquals("3", loadErrorText(3, null))
    }

    @Test
    fun staging_noMessage_isJustTheCode() {
        val log = LoadErrorLog(true)
        // Control: a message is written, so the lines below are short for want of a message only.
        assertEquals("0 \"x\"", log.text(0, "x"))

        assertEquals("0", log.text(0, null))
        assertEquals("0", log.text(0, "   "))
        assertNull(log.messageOf("   "))
    }

    @Test
    fun codeOnly_isJustTheCode() {
        // Control: the same message is kept when the message is on.
        assertEquals(NATIVE_MESSAGE, LoadErrorLog(true).messageOf(NATIVE_MESSAGE))

        assertEquals("3", LoadErrorLog.CODE_ONLY.text(3, NATIVE_MESSAGE))
        assertNull(LoadErrorLog(false).messageOf(NATIVE_MESSAGE))
    }

    @Test
    fun logSafeMessage_masksAdMobIds() {
        val results = listOf(
            "No ad for <ad unit>." to logSafeMessage(ADMOB_UNIT_MESSAGE),
            "App <ad unit> mismatch" to logSafeMessage("App ca-app-pub-3940256099942544~3347511713 mismatch"),
            "Publisher <ad unit> blocked" to logSafeMessage("Publisher pub-3940256099942544 blocked"),
        )
        results.forEach { (expected, actual) ->
            assertEquals(expected, actual)
            assertFalse(actual, "ca-app-pub" in actual)
            assertFalse(actual, "3940256099942544" in actual)
        }
    }

    @Test
    fun logSafeMessage_masksMaxUnitIds() {
        assertEquals("No fill for <ad unit>", logSafeMessage(MAX_UNIT_MESSAGE))
        assertEquals("unit <ad unit> failed", logSafeMessage("unit 2D0EC3C0D7D4B5AD0011 failed"))
    }

    @Test
    fun logSafeMessage_keepsOtherText() {
        // Control: a unit of MAX in the same kind of text is masked.
        assertEquals("x <ad unit>", logSafeMessage("x 2d0ec3c0d7d4b5ad"))

        listOf(
            "Error code 204, see https://support.google.com/admob/answer/9905175",
            "token abcdef012345678 ok",
            "id abcdef0123456789xyz",
            NATIVE_MESSAGE,
        ).forEach { kept -> assertEquals(kept, logSafeMessage(kept)) }
    }

    @Test
    fun logSafeMessage_oneLine() {
        assertEquals("Line one line two", logSafeMessage("Line one\n  line two\t"))
    }

    @Test
    fun logSafeMessage_capped_afterMasking() {
        // Control: a message of exactly the cap is kept whole.
        assertEquals("x".repeat(300), logSafeMessage("x".repeat(300)))

        assertEquals("x".repeat(300) + "...", logSafeMessage("x".repeat(400)))

        val withUnit = "x".repeat(295) + " ca-app-pub-3940256099942544/2247696110"
        assertEquals(334, withUnit.length)
        val capped = logSafeMessage(withUnit)
        // Masked first: "<ad unit>" is cut, never half of the id (cutting first would leave " ca-a...").
        assertEquals("x".repeat(295) + " <ad " + "...", capped)
        assertEquals(303, capped.length)
        assertTrue(capped.endsWith("..."))
        assertFalse(capped, "ca-app" in capped)
        assertFalse(capped, "3940256099942544" in capped)
    }
}
