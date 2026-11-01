package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * AppLovin MAX starts only once AdMob is ready (plan T-068, section 2): the Google adapter of MAX calls
 * `MobileAds.disableMediationAdapterInitialization` before its own `MobileAds.initialize`, so MAX first would leave
 * the nine mediation adapters of AdMob uninitialized. The working directory of a unit test is the module directory.
 */
class AdsSdkStartTest {
    @Test
    fun maxStartsAfterAdmobReady() = runTest {
        val steps = mutableListOf<String>()
        val admobReady = CompletableDeferred<Unit>()
        val job = launch {
            startAdsSdks(
                initAdmob = { steps += "admob init" },
                awaitAdmobReady = {
                    admobReady.await()
                    steps += "admob ready"
                },
                startMax = { steps += "max" },
                preload = { steps += "preload" },
            )
        }
        runCurrent()
        // The call that starts AdMob has returned, but AdMob is not ready: MAX waits.
        assertEquals(listOf("admob init"), steps)
        admobReady.complete(Unit)
        job.join()
        assertEquals(listOf("admob init", "admob ready", "max", "preload"), steps)
    }

    @Test
    fun maxStartsAfterReadyCap() = runTest {
        val steps = mutableListOf<String>()
        launch {
            startAdsSdks(
                initAdmob = { steps += "admob init" },
                // S-21: AdMob counts as ready 35 s after it was started, even without its callback.
                awaitAdmobReady = { delay(35_000) },
                startMax = { steps += "max" },
                preload = { steps += "preload" },
            )
        }
        advanceTimeBy(34_999)
        runCurrent()
        assertFalse(steps.toString(), "max" in steps)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(steps.toString(), "max" in steps)
    }

    @Test
    fun startSdk_wiresAdmobThenMax() {
        val body = bodyOf(code(File(CONTROLLER)), "private fun startSdk()")
        // Control: the body found is the one that starts the SDK, not an empty or wrong part of the file.
        val initialize = body.indexOf("MobileAds.initialize(")
        assertTrue(body, initialize >= 0)
        assertTrue(body, "sdkStarted.value = true" in body)

        assertTrue("startSdk does not call startAdsSdks:\n$body", "startAdsSdks(" in body)
        val max = body.indexOf("onAdmobReady")
        assertTrue("startSdk does not start MAX through onAdmobReady:\n$body", max >= 0)
        assertTrue("MAX comes before MobileAds.initialize:\n$body", initialize < max)
        // The test devices of T-058 are still set before AdMob starts.
        val testDevices = body.indexOf("applyTestDevices(")
        assertTrue(body, testDevices >= 0)
        assertTrue("applyTestDevices comes after MobileAds.initialize:\n$body", testDevices < initialize)
    }

    private fun bodyOf(text: String, declaration: String): String {
        val start = text.indexOf(declaration)
        assertTrue("No $declaration", start >= 0)
        val open = text.indexOf('{', start)
        return text.substring(open, closingBracket(text, open) + 1)
    }

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

    /** The code of [file], without comment lines, so a name in a comment is not a call. */
    private fun code(file: File): String {
        assertTrue("Missing ${file.absolutePath}", file.isFile)
        return file.readLines()
            .filterNot { line ->
                val trimmed = line.trimStart()
                trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
            }
            .joinToString("\n")
    }

    private companion object {
        const val CONTROLLER = "src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdsController.kt"
    }
}
