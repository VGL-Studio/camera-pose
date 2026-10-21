package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Test

class LocalizedTextTest {
    private val text = LocalizedText(mapOf("pt" to "Olá", "pt-BR" to "Oi", "es-419" to "Hola", "en" to "Hello"))

    @Test
    fun exactTag_isUsed() {
        assertEquals("Oi", text.resolve(Locale.forLanguageTag("pt-BR")))
        assertEquals("Olá", text.resolve(Locale.forLanguageTag("pt")))
    }

    @Test
    fun languageOnly_matchesRegionalVariant() {
        // Mexican Spanish uses the Latin American entry; plain Portuguese uses the Brazilian one when it is the only one.
        assertEquals("Hola", text.resolve(Locale.forLanguageTag("es-MX")))
        val onlyBrazilian = LocalizedText(mapOf("en" to "Hello", "pt-BR" to "Oi"))
        assertEquals("Oi", onlyBrazilian.resolve(Locale.forLanguageTag("pt")))
    }

    @Test
    fun missingLanguage_fallsBackToEnglish() {
        assertEquals("Hello", text.resolve(Locale.forLanguageTag("ja")))
    }

    @Test
    fun list_isChosenLikeText() {
        val list = LocalizedList(mapOf("vi" to listOf("một", "hai"), "en" to listOf("one", "two")))
        assertEquals(listOf("một", "hai"), list.resolve(Locale.forLanguageTag("vi")))
        assertEquals(listOf("one", "two"), list.resolve(Locale.forLanguageTag("de")))
    }

    @Test
    fun indonesianLocale_matchesIdEntry() {
        val text = LocalizedText(mapOf("id" to "Halo", "en" to "Hello"))
        assertEquals("Halo", text.resolve(Locale("id", "ID")))
    }
}
