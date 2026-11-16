package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The per-network log reaches every load of the SDK, and the test devices are set before the SDK starts (plan T-058,
 * KB-28, L-072). Scans the main sources of :core:ads by the types of AdMob, not by function names (L-062): listeners
 * of other SDKs (MAX) have functions of the same names. The working directory of a unit test is the module directory.
 * The load error line reaches every load failure of the SDK in the same way, through [LoadErrorLog] (plan T-093, KB-28).
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

    @Test
    fun everyAdMobLoadFailure_logsThroughLoadErrorLog() {
        // Controls: the bodies of today's callbacks are found, and a body without the log, or with the text of the
        // log beside a line that does not use it, is reported.
        assertEquals(4, loadFailureBodies(code(source("AdsController.kt"))).size)
        assertEquals(1, loadFailureBodies(code(source("BannerAdView.kt"))).size)
        assertEquals(1, loadFailureBodies(code(source("SdkNativeAdLoader.kt"))).size)
        val plain = "override fun onAdFailedToLoad(error: LoadAdError) { logger.warn(TAG, \"x \${error.code}\") }"
        assertEquals(1, loadFailureProblems(plain).size)
        val beside = "override fun onAdFailedToLoad(error: LoadAdError) { " +
            "val t = loadErrorLog.text(error.code, error.message); " +
            "logger.warn(TAG, \"x failed to load: \${error.code}\") }"
        assertEquals(1, loadFailureProblems(beside).size)

        val problems = sources.flatMap { file -> loadFailureProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("Load failures without the load error log:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test
    fun everyLoaderInMain_getsTheLoadErrorLog() {
        // Controls (L-055): a native loader without the log, or with another one, is reported.
        val noLog = "SdkNativeAdLoader(context, adRequest(), events, networkLog = networkLog, logKey = logKey)"
        assertEquals(1, nativeLoaderLogProblems(noLog).size)
        val other = "SdkNativeAdLoader(context, adRequest(), events, logKey = logKey, " +
            "loadErrorLog = LoadErrorLog.CODE_ONLY)"
        assertEquals(1, nativeLoaderLogProblems(other).size)

        val natives = sources.flatMap { file -> callsOf(code(file), SDK_LOADER) }
        assertTrue("No SdkNativeAdLoader is made in src/main", natives.isNotEmpty())
        val problems = sources.flatMap { file -> nativeLoaderLogProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("Native loaders without the load error log:\n${problems.joinToString("\n")}", problems.isEmpty())

        val slot = code(source("AdSlot.kt"))
        listOf("SdkBannerAdView", "MaxBannerAdView").forEach { name ->
            val calls = callsOf(slot, name)
            assertTrue("AdSlot.kt makes no $name", calls.isNotEmpty())
            calls.forEach { call -> assertTrue("$call", "loadErrorLog = ads.loadErrorLog" in call.arguments) }
        }
    }

    @Test
    fun everyNativeListener_getsTheLoadErrorLog() {
        // Control (L-055): a listener without the log is reported.
        val noLog = "NativeAdLoadListener(events, onLoadFailedResponse = { networkLog.failed(logKey, it) }, " +
            "onFailed = onFailed)"
        assertEquals(1, nativeListenerLogProblems(noLog).size)

        val calls = sources.flatMap { file -> callsOf(code(file), LISTENER).map { file.name to it } }
        assertTrue("No NativeAdLoadListener is made in src/main", calls.isNotEmpty())
        assertTrue("$calls", calls.any { (file, _) -> file == "SdkNativeAdLoader.kt" })
        val problems = sources.flatMap { file -> nativeListenerLogProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("Native listeners without the load error log:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test
    fun maxLoadFailure_passesMessage() {
        // Controls: a callback that drops the message, or the networks of the waterfall, does not match.
        assertFalse(
            MAX_FAILED.containsMatchIn(
                "onAdLoadFailed(adUnitId: String, error: MaxError) = " +
                    "onFailed(error.code, null, maxNetworkAttemptsOf(error.waterfall))",
            ),
        )
        assertFalse(
            MAX_FAILED.containsMatchIn(
                "onAdLoadFailed(adUnitId: String, error: MaxError) = onFailed(error.code, error.message, emptyList())",
            ),
        )

        val text = code(source("MaxBannerAdView.kt"))
        assertEquals(1, text.occurrences("onAdLoadFailed("))
        assertTrue(text, MAX_FAILED.containsMatchIn(text))
    }

    @Test
    fun everyMaxLoadCallback_handsTheWaterfall() {
        // Controls (L-055), both forms of a body: a callback without the call is reported, one with it is not.
        assertEquals(1, maxLoadProblems("override fun onAdLoaded(ad: MaxAd) = onLoaded(MaxAdInfo.EMPTY)").size)
        assertEquals(0, maxLoadProblems("override fun onAdLoaded(ad: MaxAd) = onLoaded(maxAdInfoOf(ad))").size)
        assertEquals(
            1,
            maxLoadProblems("override fun onAdLoaded(maxAd: MaxAd) {\n listener?.onLoaded(MaxAdInfo.EMPTY)\n}").size,
        )
        assertEquals(
            0,
            maxLoadProblems("override fun onAdLoaded(maxAd: MaxAd) {\n listener?.onLoaded(maxAdInfoOf(maxAd))\n}").size,
        )
        val failedPrefix = "override fun onAdLoadFailed(adUnitId: String, error: MaxError)"
        assertEquals(1, maxLoadProblems("$failedPrefix = onFailed(error.code, error.message, emptyList())").size)
        assertEquals(
            0,
            maxLoadProblems(
                "$failedPrefix =\n    onFailed(error.code, error.message, maxNetworkAttemptsOf(error.waterfall))",
            ).size,
        )
        assertEquals(1, maxLoadProblems("$failedPrefix {\n listener?.onLoadFailed(error.code, emptyList())\n}").size)
        assertEquals(
            0,
            maxLoadProblems(
                "$failedPrefix {\n listener?.onLoadFailed(error.code, maxNetworkAttemptsOf(error.waterfall))\n}",
            ).size,
        )
        // Control: a body of the expression form ends with its line; the next function is not part of it.
        assertEquals(
            1,
            maxLoadProblems(
                "override fun onAdLoaded(ad: MaxAd) = onLoaded(MaxAdInfo.EMPTY)\n" +
                    "override fun onAdClicked(ad: MaxAd) = onClicked(maxAdInfoOf(ad))",
            ).size,
        )
        // The callbacks of today (banner, interstitial, rewarded, app open): one load and one failure each.
        listOf("MaxBannerAdView.kt", "MaxInterstitial.kt", "MaxRewarded.kt", "MaxAppOpen.kt").forEach { name ->
            assertEquals(name, 1 to 1, maxCallbackCounts(code(source(name))))
        }

        val problems = sources.flatMap { file -> maxLoadProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("MAX load callbacks without the waterfall:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test
    fun everyMaxAdObject_hasRevenueListener() {
        // Controls (L-055).
        assertEquals(
            listOf("MaxInterstitialAd"),
            revenueListenerProblems("private val ad = MaxInterstitialAd(adUnitId, context)"),
        )
        val withLambda = "private val adView = MaxAdView(unitId, MaxAdFormat.BANNER, context)\n" +
            "adView.setRevenueListener { onRevenue(maxAdInfoOf(it)) }"
        assertEquals(emptyList<String>(), revenueListenerProblems(withLambda))
        val withCall = "private val ad = MaxRewardedAd.getInstance(adUnitId, context)\nad.setRevenueListener(listener)"
        assertEquals(emptyList<String>(), revenueListenerProblems(withCall))
        assertEquals(emptyList<String>(), maxObjectsBuilt("val v = MyMaxAdView(x)\nval w = x.MaxAdView(y)"))
        assertEquals(emptyList<String>(), maxObjectsBuilt(code("/** [MaxRewardedAd.getInstance] */")))
        assertEquals(listOf("MaxRewardedAd"), maxObjectsBuilt("val ad = MaxRewardedAd.getInstance(unit, context)"))

        val built = sources.flatMap { file -> maxObjectsBuilt(code(file)) }.toSet()
        assertEquals(
            "A type of MAX object is built nowhere in src/main: $built",
            setOf("MaxAdView", "MaxInterstitialAd", "MaxAppOpenAd", "MaxRewardedAd"),
            built,
        )
        val problems = sources.flatMap { file -> revenueListenerProblems(code(file)).map { "${file.name}: $it" } }
        assertTrue("MAX objects without a revenue listener:\n${problems.joinToString("\n")}", problems.isEmpty())
    }

    @Test
    fun everyMaxSlotAndBanner_getsTheNetworkLog() {
        // Control (L-055): a call without the log is reported.
        val noLog = callsOf("MaxBannerAdView(newSdkView = { x }, placement = p, logger = logger)", "MaxBannerAdView")
        assertTrue(noLog.isNotEmpty())
        assertFalse(noLog.single().arguments.any { it == "networkLog = ads.networkLog" })

        val banners = callsOf(code(source("AdSlot.kt")), "MaxBannerAdView")
        assertTrue("AdSlot.kt makes no MaxBannerAdView", banners.isNotEmpty())
        banners.forEach { call -> assertTrue("$call", "networkLog = ads.networkLog" in call.arguments) }

        val slots = callsOf(code(source("AdsController.kt")), "MaxInterstitialSlot")
        // Interstitial, rewarded, app open.
        assertEquals(slots.toString(), 3, slots.size)
        slots.forEach { call -> assertTrue("$call", "networkLog = networkLog" in call.arguments) }
    }

    /** The MAX load callbacks of [text] (loads, failures), counted by the types of MAX (L-062). */
    private fun maxCallbackCounts(text: String): Pair<Int, Int> =
        MAX_LOADED.findAll(text).count() to MAX_LOAD_FAILED.findAll(text).count()

    /**
     * The MAX load callbacks of [text] whose body does not hand the ad to [maxAdInfoOf] (loads) or the waterfall of the
     * error to [maxNetworkAttemptsOf] (failures).
     */
    private fun maxLoadProblems(text: String): List<String> {
        val loads = MAX_LOADED.findAll(text).mapNotNull { match ->
            val body = functionBody(text, match.range.last + 1)
            body.takeUnless { "maxAdInfoOf(${match.groupValues[1]})" in it }
        }
        val failures = MAX_LOAD_FAILED.findAll(text).mapNotNull { match ->
            val body = functionBody(text, match.range.last + 1)
            body.takeUnless { "maxNetworkAttemptsOf(${match.groupValues[1]}.waterfall)" in it }
        }
        return (loads + failures).toList()
    }

    /**
     * The body of the function whose parameter list ends right before [from]: a block from `{` to its matching `}`, or
     * an expression after `=` up to the end of its logical line (every bracket closed at a line break).
     */
    private fun functionBody(text: String, from: Int): String {
        var index = from
        while (index < text.length && text[index].isWhitespace()) index++
        // A return type, if any, up to the body.
        if (text.getOrNull(index) == ':') {
            while (index < text.length && text[index] != '{' && text[index] != '=') index++
        }
        return when (text.getOrNull(index)) {
            '{' -> text.substring(index, closingBracket(text, index) + 1)
            '=' -> {
                var depth = 0
                var end = index + 1
                var started = false
                while (end < text.length) {
                    val char = text[end]
                    if (char in "([{") depth++
                    if (char in ")]}") depth--
                    if (!char.isWhitespace()) started = true
                    if (char == '\n' && depth == 0 && started) break
                    end++
                }
                text.substring(index, end)
            }
            else -> error("No body at $from")
        }
    }

    /** The MAX objects [text] builds, by type: `MaxRewardedAd` for `MaxRewardedAd.getInstance(`. */
    private fun maxObjectsBuilt(text: String): List<String> =
        MAX_OBJECT.findAll(text).map { it.groupValues[1] }.toList() +
            MAX_REWARDED_OBJECT.findAll(text).map { "MaxRewardedAd" }.toList()

    /** The MAX objects of [text] in a source with no `setRevenueListener`. */
    private fun revenueListenerProblems(text: String): List<String> =
        if (REVENUE_LISTENER.containsMatchIn(text)) emptyList() else maxObjectsBuilt(text)

    /** The bodies of the AdMob load failure callbacks of [text], each with the name of its error. */
    private fun loadFailureBodies(text: String): List<Pair<String, String>> =
        Regex("""override fun onAdFailedToLoad\(\s*(\w+)\s*:\s*LoadAdError\s*\)""").findAll(text).map { match ->
            val open = text.indexOf('{', match.range.last)
            match.groupValues[1] to text.substring(open, closingBracket(text, open) + 1)
        }.toList()

    /**
     * The AdMob load failures of [text] whose body neither hands the message through [LoadErrorLog.messageOf] nor
     * logs the line `failed to load:` with [LoadErrorLog.text] of that error.
     */
    private fun loadFailureProblems(text: String): List<String> = loadFailureBodies(text).mapNotNull { (error, body) ->
        val handed = "loadErrorLog.messageOf($error.message)" in body
        val line = "failed to load: \${loadErrorLog.text($error.code, $error.message)}"
        val logged = callsOf(body, "logger.warn").any { call -> call.arguments.any { line in it } }
        body.takeUnless { handed || logged }
    }

    private fun nativeLoaderLogProblems(text: String): List<String> = callsOf(text, SDK_LOADER)
        .filterNot { "loadErrorLog = loadErrorLog" in it.arguments }
        .map { "$it: no loadErrorLog = loadErrorLog" }

    private fun nativeListenerLogProblems(text: String): List<String> = callsOf(text, LISTENER)
        .filterNot { "loadErrorLog = loadErrorLog" in it.arguments }
        .map { "$it: no loadErrorLog = loadErrorLog" }

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
    private fun code(file: File): String = code(file.readText())

    /** [text] without comment lines. */
    private fun code(text: String): String = text.lines()
        .filterNot { line ->
            val trimmed = line.trimStart()
            trimmed.startsWith("//") || trimmed.startsWith("*") || trimmed.startsWith("/*")
        }
        .joinToString("\n")

    private fun String.occurrences(part: String): Int = split(part).size - 1

    private companion object {
        const val SDK_LOADER = "SdkNativeAdLoader"
        const val NEW_LOADER = "newNativeLoader"
        const val LISTENER = "NativeAdLoadListener"
        val MAX_FAILED =
            Regex(
                """onAdLoadFailed\(\s*\w+\s*:\s*String\s*,\s*(\w+)\s*:\s*MaxError\s*\)\s*=\s*""" +
                    """onFailed\(\1\.code,\s*\1\.message,\s*maxNetworkAttemptsOf\(\1\.waterfall\)\)""",
            )
        val MAX_LOADED = Regex("""override fun onAdLoaded\(\s*(\w+)\s*:\s*MaxAd\s*\)""")
        val MAX_LOAD_FAILED =
            Regex("""override fun onAdLoadFailed\(\s*\w+\s*:\s*String\s*,\s*(\w+)\s*:\s*MaxError\s*\)""")
        val MAX_OBJECT = Regex("""(?<![\w.])(MaxAdView|MaxInterstitialAd|MaxAppOpenAd)\(""")
        val MAX_REWARDED_OBJECT = Regex("""(?<![\w.])MaxRewardedAd\.getInstance\(""")
        val REVENUE_LISTENER = Regex("""\.setRevenueListener\s*[({]""")
        val FUNCTION = Regex("""\bfun\s+(?:<[^>]*>\s*)?(?:[\w.]+\.)?(\w+)\s*\(""")
    }
}
