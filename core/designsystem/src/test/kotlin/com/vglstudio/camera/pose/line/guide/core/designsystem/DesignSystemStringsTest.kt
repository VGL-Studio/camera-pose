package com.vglstudio.camera.pose.line.guide.core.designsystem

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.Normalizer
import java.util.Locale
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text of the design system (brand tagline, network dialog) in the seven release languages (spec settings, KB-4;
 * plan T-051, section 3.1). English is the default language in values/; Vietnamese, the text table of the design, is in
 * values-vi/.
 */
@RunWith(AndroidJUnit4::class)
class DesignSystemStringsTest {
    private fun context(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(configuration)
    }

    @Test
    fun everyReleaseLanguage_isTranslated() {
        val english = context("en")
        listOf("hi", "pt-BR", "es-419", "id", "th", "vi").forEach { tag ->
            val context = context(tag)
            TRANSLATED.forEach { id ->
                val text = context.getString(id)
                val name = context.resources.getResourceEntryName(id)
                assertTrue("$tag $name is empty", text.isNotBlank())
                assertNotEquals("$tag $name is not translated", english.getString(id), text)
            }
        }
    }

    @Test
    fun defaultLanguage_hasNoVietnamese() {
        val english = context("en")
        TRANSLATED.forEach { id ->
            val text = english.getString(id)
            val name = english.resources.getResourceEntryName(id)
            assertFalse("$name is Vietnamese: $text", isVietnamese(text))
        }
    }

    /** Vietnamese letters, double marks included (ậ, ở, ừ…): any combining mark of Vietnamese after NFD, or đ. */
    private fun isVietnamese(text: String) =
        VIETNAMESE_MARK.containsMatchIn(Normalizer.normalize(text, Normalizer.Form.NFD))

    private companion object {
        /** Every string of values/ (5 strings, plan T-051 section 3.1). */
        val TRANSLATED = listOf(
            R.string.brand_tagline,
            R.string.network_dialog_title,
            R.string.network_dialog_message,
            R.string.network_dialog_retry,
            R.string.network_dialog_still_offline,
        )

        val VIETNAMESE_MARK = Regex("[\\u0300-\\u0323đĐ]")
    }
}
