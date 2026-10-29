package com.vglstudio.camera.pose.line.guide.config

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.AdsSettings
import com.vglstudio.camera.pose.line.guide.core.ads.InterstitialBreakSettings
import com.vglstudio.camera.pose.line.guide.core.analytics.AppsFlyerSettings
import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.PoseMatchingSettings
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.network.NetworkSettings
import com.vglstudio.camera.pose.line.guide.core.poses.PosesRemoteKeys
import com.vglstudio.camera.pose.line.guide.core.review.ReviewSettings
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * docs/remote-config-keys.md and docs/remote-config-template.json list every Remote Config key of the app
 * (ADR-0023). They are generated from [AppRemoteKeys]; this test fails when they no longer match the code.
 * To update both files after adding or changing a key:
 * ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true
 */
class RemoteConfigKeysTest {
    private val sample: Map<String, List<RemoteKey<*>>> = linkedMapOf(
        ":core:x" to listOf(
            RemoteKey.string("x_languages", "vi,en", "Languages offered."),
            RemoteKey.double("x_ratio_percent", 0.5, "A ratio."),
            RemoteKey.boolean("x_enabled", true, "Turns x on."),
            RemoteKey.long("x_interval_seconds", 45L, "An interval."),
        ),
    )

    @Test
    fun keysFile_matchesCode() {
        assertFileMatches(
            property = "remoteConfigKeys.markdown",
            expected = keysMarkdown(AppRemoteKeys.byModule),
            name = "docs/remote-config-keys.md",
        )
    }

    @Test
    fun templateFile_matchesCode() {
        assertFileMatches(
            property = "remoteConfigKeys.template",
            expected = firebaseTemplateJson(AppRemoteKeys.byModule),
            name = "docs/remote-config-template.json",
        )
    }

    @Test
    fun markdown_formatsEveryType() {
        val lines = keysMarkdown(sample).lines()
        listOf(
            "| `x_enabled` | boolean | `true` | `:core:x` | Turns x on. |",
            "| `x_interval_seconds` | long | `45` | `:core:x` | An interval. |",
            "| `x_ratio_percent` | double | `0.5` | `:core:x` | A ratio. |",
            "| `x_languages` | string | `\"vi,en\"` | `:core:x` | Languages offered. |",
        ).forEach { row -> assertEquals("Row $row", 1, lines.count { it == row }) }
        assertTrue(lines.contains("| Khóa | Kiểu | Mặc định | Module | Mô tả |"))
    }

    @Test
    fun markdown_sortsKeysByNameWithinModule() {
        // The sample is declared out of order, so the sorted order comes from the generator.
        val names = keysMarkdown(sample).lines()
            .filter { it.startsWith("| `") }
            .map { it.substringAfter("| `").substringBefore('`') }
        assertEquals(listOf("x_enabled", "x_interval_seconds", "x_languages", "x_ratio_percent"), names)
    }

    @Test
    fun markdown_escapesPipeInDescription() {
        val markdown = keysMarkdown(mapOf(":core:x" to listOf(RemoteKey.boolean("x_on", false, "on | off"))))
        assertTrue(markdown, markdown.lines().contains("| `x_on` | boolean | `false` | `:core:x` | on \\| off |"))
    }

    @Test
    fun json_mapsTypes() {
        val expected = """
            {
              "conditions": [],
              "parameters": {
                "x_enabled": {
                  "defaultValue": { "value": "true" },
                  "description": "Turns x on.",
                  "valueType": "BOOLEAN"
                },
                "x_interval_seconds": {
                  "defaultValue": { "value": "45" },
                  "description": "An interval.",
                  "valueType": "NUMBER"
                },
                "x_languages": {
                  "defaultValue": { "value": "vi,en" },
                  "description": "Languages offered.",
                  "valueType": "STRING"
                },
                "x_ratio_percent": {
                  "defaultValue": { "value": "0.5" },
                  "description": "A ratio.",
                  "valueType": "NUMBER"
                }
              }
            }
        """.trimIndent() + "\n"
        assertEquals(expected, firebaseTemplateJson(sample))
    }

    @Test
    fun json_escapesQuoteAndBackslash() {
        val key = RemoteKey.boolean("x_on", false, "say \"hi\" \\ ok")
        val json = firebaseTemplateJson(mapOf(":core:x" to listOf(key)))
        assertTrue(json, json.contains("\"description\": \"say \\\"hi\\\" \\\\ ok\","))
    }

    @Test
    fun everyKeyName_isUniqueAcrossModules() {
        val names = AppRemoteKeys.byModule.values.flatten().map { it.name }
        assertTrue("No keys listed", names.isNotEmpty())
        assertEquals(names.groupBy { it }.filterValues { it.size > 1 }.keys, emptySet<String>())
    }

    /**
     * Ad keys keep the AdsKit names and state their unit in the description (ADR-0026, decision 9). Keys released
     * before ADR-0023 keep their name (docs/ops/remote-config.md, section 2; plan T-074, question 5).
     */
    @Test
    fun everyNumberKey_endsWithUnit() {
        val numberKeys = AppRemoteKeys.byModule.filterKeys { it != ":core:ads" }.values.flatten()
            .filter { it.type == RemoteKey.Type.LONG || it.type == RemoteKey.Type.DOUBLE }
        assertTrue("No number keys listed", numberKeys.isNotEmpty())
        // Control: every name of the exception set is a real number key, so the set cannot hide a removed key.
        val numberKeyNames = numberKeys.map { it.name }.toSet()
        RELEASED_BEFORE_ADR_0023.forEach { name ->
            assertTrue("'$name' is not a number key of AppRemoteKeys", name in numberKeyNames)
        }
        numberKeys.filterNot { it.name in RELEASED_BEFORE_ADR_0023 }.forEach { key ->
            assertTrue(
                "Number key '${key.name}' must end with _seconds, _count or _percent (ADR-0023)",
                Regex("_(seconds|count|percent)$").containsMatchIn(key.name),
            )
        }
    }

    @Test
    fun everyFeatureFlagAndPlacement_isListed() {
        val keys = AppRemoteKeys.byModule.values.flatten()
        FeatureFlag.entries.forEach { flag -> assertTrue("$flag is missing", flag.remoteKey in keys) }
        // App-wide placements are switched by the global keys and have no is_show_<code> (plan T-024, question 3).
        val switched = AdPlacement.entries.filterNot { it.appWide }
        // Control: the filter leaves out the app-wide placements only.
        assertTrue(AdPlacement.entries.any { it.appWide })
        assertTrue(switched.isNotEmpty())
        switched.forEach { placement ->
            assertTrue("$placement is missing", AdsSettings.placementEnabled(placement) in keys)
        }
    }

    @Test
    fun adsNumberKeys_stateUnitInDescription() {
        val numberKeys = AppRemoteKeys.byModule.getValue(":core:ads")
            .filter { it.type == RemoteKey.Type.LONG || it.type == RemoteKey.Type.DOUBLE }
        assertTrue("No number keys in :core:ads", numberKeys.isNotEmpty())
        numberKeys.forEach { key ->
            assertTrue(
                "Number key '${key.name}' must state its unit (seconds, items) in the description (ADR-0026)",
                Regex("\\b(seconds|items)\\b").containsMatchIn(key.description),
            )
        }
    }

    @Test
    fun analyticsKeys_areListed() {
        assertEquals(AppsFlyerSettings.all(), AppRemoteKeys.byModule[":core:analytics"])
        assertEquals(11, AppsFlyerSettings.all().size)
    }

    @Test
    fun everyPlacementShowKey_isInTableOff() {
        val lines = keysMarkdown(AppRemoteKeys.byModule).lines()
        // Control: app-wide placements exist and are left out, as they have no is_show_<code> (plan T-024).
        assertTrue(AdPlacement.entries.any { it.appWide })
        AdPlacement.entries.filter { it.appWide }.forEach { placement ->
            assertEquals(placement.key, 0, lines.count { it.startsWith("| `is_show_${placement.key}` |") })
        }
        AdPlacement.entries.filterNot { it.appWide }.forEach { placement ->
            val row = "| `is_show_${placement.key}` | boolean | `false` | `:core:ads` | "
            assertEquals(placement.key, 1, lines.count { it.startsWith(row) })
        }
    }

    @Test
    fun networkKeys_areListed() {
        assertEquals(NetworkSettings.all(), AppRemoteKeys.byModule[":core:network"])
    }

    @Test
    fun appModuleKeys_areListed() {
        assertEquals(PosesRemoteKeys.all(), AppRemoteKeys.byModule[":core:poses"])
        assertEquals(listOf("poses_catalog_path"), PosesRemoteKeys.all().map { it.name })
        assertEquals(ReviewSettings.all(), AppRemoteKeys.byModule[":core:review"])
        assertEquals(
            listOf("in_app_review_min_saved_photos", "in_app_review_min_days_between"),
            ReviewSettings.all().map { it.name },
        )
        val ads = AppRemoteKeys.byModule.getValue(":core:ads")
        assertEquals(2, InterstitialBreakSettings.all().size)
        InterstitialBreakSettings.all().forEach { key -> assertTrue("${key.name} is missing", key in ads) }
        val config = AppRemoteKeys.byModule.getValue(":core:config")
        (AppLinks.KEYS + PoseMatchingSettings.KEYS).forEach { key ->
            assertTrue("${key.name} is missing", key in config)
        }
    }

    private fun assertFileMatches(property: String, expected: String, name: String) {
        val file = File(System.getProperty(property) ?: error("$property is not set"))
        if (System.getProperty("remoteConfigKeys.update") == "true") file.writeText(expected)
        assertEquals(
            "$name đã cũ so với code. Chạy ./gradlew :app:testStagingDebugUnitTest " +
                "-PremoteConfigKeys.update=true rồi commit.",
            expected,
            if (file.exists()) file.readText() else "",
        )
    }

    private companion object {
        /** Number keys on Firebase before the naming rule of ADR-0023; a released key is never renamed. */
        val RELEASED_BEFORE_ADR_0023 = setOf(
            "in_app_review_min_saved_photos",
            "in_app_review_min_days_between",
            "pose_match_threshold",
        )
    }
}

private fun RemoteKey<*>.typeName(): String = type.name.lowercase()

/** The default as it is written on Firebase: strings are quoted in the table, numbers and booleans are not. */
private fun RemoteKey<*>.firebaseValue(): String = default.toString()

private fun sortedKeys(byModule: Map<String, List<RemoteKey<*>>>): List<Pair<String, RemoteKey<*>>> =
    byModule.flatMap { (module, keys) -> keys.sortedBy { it.name }.map { module to it } }

internal fun keysMarkdown(byModule: Map<String, List<RemoteKey<*>>>): String = buildString {
    appendLine("# BẢNG KHÓA REMOTE CONFIG")
    appendLine()
    appendLine(
        "<!-- Sinh từ code bởi RemoteConfigKeysTest (:app). Không sửa tay. " +
            "Sinh lại: ./gradlew :app:testStagingDebugUnitTest -PremoteConfigKeys.update=true -->",
    )
    appendLine()
    appendLine(
        "Mọi khóa Remote Config của bản app hiện tại (ADR-0023). " +
            "File mẫu để nhập vào Firebase: `remote-config-template.json`.",
    )
    appendLine()
    appendLine("| Khóa | Kiểu | Mặc định | Module | Mô tả |")
    appendLine("| --- | --- | --- | --- | --- |")
    sortedKeys(byModule).forEach { (module, key) ->
        val default = if (key.type == RemoteKey.Type.STRING) "\"${key.firebaseValue()}\"" else key.firebaseValue()
        appendLine(
            "| `${key.name}` | ${key.typeName()} | `${markdownCell(default)}` | `$module` | " +
                "${markdownCell(key.description)} |",
        )
    }
}

internal fun firebaseTemplateJson(byModule: Map<String, List<RemoteKey<*>>>): String = buildString {
    val keys = sortedKeys(byModule).map { it.second }
    appendLine("{")
    appendLine("  \"conditions\": [],")
    appendLine("  \"parameters\": {")
    keys.forEachIndexed { index, key ->
        val valueType = when (key.type) {
            RemoteKey.Type.BOOLEAN -> "BOOLEAN"
            RemoteKey.Type.LONG, RemoteKey.Type.DOUBLE -> "NUMBER"
            RemoteKey.Type.STRING -> "STRING"
        }
        appendLine("    \"${jsonString(key.name)}\": {")
        appendLine("      \"defaultValue\": { \"value\": \"${jsonString(key.firebaseValue())}\" },")
        appendLine("      \"description\": \"${jsonString(key.description)}\",")
        appendLine("      \"valueType\": \"$valueType\"")
        appendLine(if (index < keys.lastIndex) "    }," else "    }")
    }
    appendLine("  }")
    appendLine("}")
}

private fun jsonString(text: String): String = text.replace("\\", "\\\\").replace("\"", "\\\"")

private fun markdownCell(text: String): String = text.replace("|", "\\|")
