package com.vglstudio.camera.pose.line.guide.core.poses.model

import java.util.Locale

/** Text in several languages, keyed by language tag (`en`, `vi`, `pt-BR`, `es-419`). English is required. */
data class LocalizedText(val values: Map<String, String>) {
    /** Text in the language of [locale]: exact tag, then language, then English. */
    fun resolve(locale: Locale = Locale.getDefault()): String = values.values.firstOrNull().orEmpty()
}

/** A list of texts in several languages, chosen the same way as [LocalizedText]. */
data class LocalizedList(val values: Map<String, List<String>>) {
    fun resolve(locale: Locale = Locale.getDefault()): List<String> = emptyList()
}
