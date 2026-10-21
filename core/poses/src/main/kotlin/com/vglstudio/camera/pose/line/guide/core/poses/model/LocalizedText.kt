package com.vglstudio.camera.pose.line.guide.core.poses.model

import java.util.Locale

private const val FALLBACK_LANGUAGE = "en"

/** Text in several languages, keyed by language tag (`en`, `vi`, `pt-BR`, `es-419`). English is required. */
data class LocalizedText(val values: Map<String, String>) {
    /** Text in the language of [locale]: exact tag, then language, then English. */
    fun resolve(locale: Locale = Locale.getDefault()): String = values.pick(locale).orEmpty()
}

/** A list of texts in several languages, chosen the same way as [LocalizedText]. */
data class LocalizedList(val values: Map<String, List<String>>) {
    fun resolve(locale: Locale = Locale.getDefault()): List<String> = values.pick(locale).orEmpty()
}

/** Exact tag (`pt-BR`), then the bare language (`pt`), then any variant of it (`es-MX` takes `es-419`), then English. */
private fun <T> Map<String, T>.pick(locale: Locale): T? {
    this[locale.toLanguageTag()]?.let { return it }
    val language = locale.language
    this[language]?.let { return it }
    entries.firstOrNull { it.key.substringBefore('-') == language }?.let { return it.value }
    return this[FALLBACK_LANGUAGE]
}
