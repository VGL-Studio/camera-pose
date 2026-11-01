package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-network log reaches every load of the SDK, and the test devices are set before the SDK starts (plan T-058,
 * KB-28, L-072). Scans the main sources of :core:ads by the types of AdMob, not by function names (L-062): listeners
 * of other SDKs (MAX) have functions of the same names. The working directory of a unit test is the module directory.
 */
class AdNetworkLogWiringTest {
    private val sources: List<File> by lazy {
        File("src/main/kotlin").walkTopDown().filter { it.isFile && it.extension == "kt" }.toList()
    }

    private fun source(name: String): File = requireNotNull(sources.find { it.name == name }) { "No $name" }

    @Test
    fun startSdk_setsTestDevicesBeforeInitialize() {
        val body = bodyOf(code(source("AdsController.kt")), "private fun startSdk()")
        // Control: the body found is the one that starts the SDK.
        val initialize = body.indexOf("MobileAds.initialize(")
        assertTrue(body, initialize >= 0)

        val apply = body.indexOf("applyTestDevices(")
        assertTrue("startSdk does not call applyTestDevices:\n$body", apply >= 0)
        assertTrue("applyTestDevices comes after MobileAds.initialize:\n$body", apply < initialize)
    }

    @Test
    fun everySdkLoadCallback_logsNetworks() {
        // Control: the counts of today's callbacks (section 3.1 of the plan), and a listener of MAX is not counted.
        assertEquals(4 to 4, callbackCounts(code(source("AdsController.kt"))))
        assertEquals(1 to 1, callbackCounts(code(source("BannerAdView.kt"))))
        assertEquals(1 to 1, callbackCounts(code(source("SdkNativeAdLoader.kt"))))
        assertEquals(0 to 0, callbackCounts("override fun onAdLoaded(ad: MaxAd) {}"))

        val missing = sources.mapNotNull { file ->
            val text = code(file)
            val (loaded, failed) = callbackCounts(text)
            val logged = text.occurrences("networkLog.loaded(")
            val logFailed = text.occurrences("networkLog.failed(")
            "${file.name}: $loaded loads / $logged logged, $failed failures / $logFailed logged"
                .takeIf { loaded != logged || failed != logFailed }
        }
        assertTrue("Load callbacks without the network log:\n${missing.joinToString("\n")}", missing.isEmpty())
    }

    @Test
    fun everyLoaderInMain_getsTheNetworkLog() {
        // Controls (L-055): the scan reports a wrong key, a loader without the log, and skips a declaration.
        val wrongKey = loaderProblems(
            "nativeLoadOnce(newLoader = { e -> newNativeLoader(e, null, placement.key) }, placementKey = POOL_LOG_KEY)",
        )
        assertEquals(wrongKey.toString(), 1, wrongKey.size)
        assertTrue(wrongKey.toString(), "key" in wrongKey.single())
        val noLog = loaderProblems("SdkNativeAdLoader(context, adRequest(), events)")
        assertTrue(noLog.toString(), noLog.any { "networkLog" in it })
        val declaration = "internal class SdkNativeAdLoader(\n    private val events: NativeAdEvents?,\n" +
            "    private val logKey: String = \"\",\n) : NativeAdLoader"
        assertEquals(emptyList<String>(), loaderProblems(declaration))
        assertEquals(0, loaderCalls(declaration).size)

        val calls = sources.flatMap { file -> loaderCalls(code(file)) }
        assertTrue("No native loader is made in src/main", calls.isNotEmpty())
        assertTrue(
            "No pool load: $calls",
            calls.any { it.name == NEW_LOADER && it.arguments.getOrNull(2) == "POOL_LOG_KEY" },
        )

        val problems = sources.flatMap { file -> loaderProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("Native loaders without the right network log:\n${problems.joinToString("\n")}", problems.isEmpty())

        val banner = callsOf(code(source("AdSlot.kt")), "SdkBannerAdView")
        assertTrue("AdSlot.kt makes no SdkBannerAdView", banner.isNotEmpty())
        banner.forEach { call ->
            assertTrue("$call", call.arguments.any { it.startsWith("networkLog =") })
        }
    }

    /** One call of [name] in a source: its position and its arguments, split at the top level of brackets. */
    private data class Call(val name: String, val start: Int, val end: Int, val arguments: List<String>)

    private fun loaderCalls(text: String): List<Call> = callsOf(text, SDK_LOADER) + callsOf(text, NEW_LOADER)

    /**
     * What is wrong with the native loaders of [text]: (1) an SDK loader without `networkLog` and `logKey =`; (2) a
     * [NEW_LOADER] without three arguments; (3) a loader inside `nativeLoadOnce(` or `loadOneNativeAd(` whose key
     * is not the log key of that load; an SDK loader outside them only in the body of [NEW_LOADER], with its own key.
     */
    private fun loaderProblems(text: String): List<String> {
        val loads = callsOf(text, "nativeLoadOnce") + callsOf(text, "loadOneNativeAd")
        return loaderCalls(text).flatMap { call ->
            val problems = mutableListOf<String>()
            val key = when (call.name) {
                SDK_LOADER -> {
                    if (call.arguments.none { it.contains("networkLog") }) problems += "$call: no networkLog"
                    val logKey = call.arguments.find { it.startsWith("logKey =") }?.removePrefix("logKey =")?.trim()
                    if (logKey == null) problems += "$call: no logKey ="
                    logKey
                }
                else -> {
                    val third = call.arguments.getOrNull(2)
                    if (call.arguments.size != 3 || third.isNullOrBlank()) problems += "$call: not three arguments"
                    third
                }
            }
            val load = loads.filter { it.start < call.start && call.end <= it.end }.maxByOrNull { it.start }
            if (load != null) {
                val loadKey = if (load.name == "nativeLoadOnce") {
                    load.arguments.find { it.startsWith("placementKey =") }?.removePrefix("placementKey =")?.trim()
                } else {
                    load.arguments.lastOrNull()
                }
                if (key != null && key != loadKey) problems += "$call: key $key, the load logs $loadKey"
            } else if (call.name == SDK_LOADER) {
                val function = FUNCTION.findAll(text.substring(0, call.start)).lastOrNull()?.groupValues?.get(1)
                if (function != NEW_LOADER) problems += "$call: outside a load and outside $NEW_LOADER"
                if (key != null && key != "logKey") problems += "$call: key $key in $NEW_LOADER"
            }
            problems
        }
    }

    /** The calls of [name] in [text], declarations (`class`, `fun`) excluded. */
    private fun callsOf(text: String, name: String): List<Call> =
        Regex("""(?<![\w.])$name\(""").findAll(text).mapNotNull { match ->
            val before = text.substring(0, match.range.first).trimEnd()
            if (before.endsWith("class") || before.endsWith("fun")) return@mapNotNull null
            val open = match.range.last
            val close = closingBracket(text, open)
            Call(name, match.range.first, close, splitArguments(text.substring(open + 1, close)))
        }.toList()

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

    /** Arguments split at the commas of the top level only: `nativeSlotKey(placement, format)` is one argument. */
    private fun splitArguments(inside: String): List<String> {
        val arguments = mutableListOf<String>()
        var depth = 0
        var inString = false
        val current = StringBuilder()
        inside.forEachIndexed { index, char ->
            when {
                char == '"' && (index == 0 || inside[index - 1] != '\\') -> inString = !inString
                inString -> Unit
                char in "([{" -> depth++
                char in ")]}" -> depth--
                char == ',' && depth == 0 -> {
                    arguments += current.toString().trim()
                    current.clear()
                    return@forEachIndexed
                }
            }
            current.append(char)
        }
        current.toString().trim().takeIf { it.isNotEmpty() }?.let { arguments += it }
        return arguments.map { it.replace(Regex("""\s+"""), " ") }
    }

    /** The AdMob load callbacks of [text] (loads, failures), counted by the types of AdMob (L-062). */
    private fun callbackCounts(text: String): Pair<Int, Int> {
        val fullScreen = Regex("""override fun onAdLoaded\(\s*\w+\s*:\s*(InterstitialAd|AppOpenAd|RewardedAd)\s*\)""")
            .findAll(text).count()
        val banner = Regex("""override fun onAdLoaded\(\)""").findAll(text).count { match ->
            text.substring(0, match.range.first).substringAfterLast("object :").trimStart().startsWith("AdListener()")
        }
        val native = text.occurrences(".forNativeAd {")
        val failures = Regex("""override fun onAdFailedToLoad\(\s*\w+\s*:\s*LoadAdError\s*\)""").findAll(text).count()
        return (fullScreen + banner + native) to failures
    }

    /** The body of the function declared by [declaration], from its first `{` to the matching `}`. */
    private fun bodyOf(text: String, declaration: String): String {
        val start = text.indexOf(declaration)
        assertTrue("No $declaration", start >= 0)
        val open = text.indexOf('{', start)
        return text.substring(open, closingBracket(text, open) + 1)
    }

    /** The code of [file], without comment lines, so a name in a KDoc is not a call. */
    private fun code(file: File): String = file.readLines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

    private fun String.occurrences(part: String): Int = split(part).size - 1

    private companion object {
        const val SDK_LOADER = "SdkNativeAdLoader"
        const val NEW_LOADER = "newNativeLoader"
        val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""")
    }
}
