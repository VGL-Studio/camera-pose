package com.vglstudio.camera.pose.line.guide.feature.camera

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.text.Normalizer
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text of the camera and review screens in the seven release languages (spec settings, KB-4; plan T-050, section 3.1).
 * English is the default language in values/; Vietnamese, the text table of the design, is in values-vi/. Every string
 * of the module is read through R.string (the R class of a module only has its own strings, nonTransitiveRClass).
 */
@RunWith(AndroidJUnit4::class)
class CameraStringsTest {
    private fun context(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(configuration)
    }

    private val strings: List<Pair<String, Int>> = R.string::class.java.fields
        .map { it.name to it.getInt(null) }
        .sortedBy { it.first }

    private val translated = strings.filter { it.first !in LANGUAGE_NEUTRAL }

    @Test
    fun english_isDefault() {
        listOf("en", "fr").forEach { tag ->
            val context = context(tag)
            strings.forEach { (name, id) ->
                assertFalse("$tag $name is Vietnamese", isVietnamese(context.getString(id)))
            }
        }
    }

    @Test
    fun everyReleaseLanguage_isTranslated() {
        val english = context("en")
        RELEASE_TRANSLATIONS.forEach { tag ->
            val context = context(tag)
            translated.forEach { (name, id) ->
                val text = context.getString(id)
                assertTrue("$tag $name is empty", text.isNotBlank())
                if (tag to name !in SAME_AS_ENGLISH) {
                    assertNotEquals("$tag $name is not translated", english.getString(id), text)
                }
            }
        }
    }

    @Test
    fun everyTranslation_keepsPlaceholders() {
        val english = context("en")
        RELEASE_TRANSLATIONS.forEach { tag ->
            val context = context(tag)
            strings.forEach { (name, id) ->
                assertEquals(
                    "$tag $name",
                    placeholders(english.getString(id)),
                    placeholders(context.getString(id)),
                )
            }
        }
    }

    /**
     * The slider shows how solid the outline is, 100% when it is fully drawn: a word for "transparency" would read the
     * other way round (independent review of T-050).
     */
    @Test
    fun opacityLabel_isNotTransparency() {
        TRANSPARENCY.forEach { (tag, word) ->
            val label = context(tag).getString(R.string.camera_adjust_opacity)
            val transparency = label.contains(word, ignoreCase = true)
            assertFalse("$tag camera_adjust_opacity says transparency: $label", transparency)
        }
    }

    private fun placeholders(text: String) = PLACEHOLDER.findAll(text).map { it.value }.sorted().toList()

    /** Vietnamese letters, double marks included (ậ, ở, ừ…): any combining mark of Vietnamese after NFD, or đ. */
    private fun isVietnamese(text: String) =
        VIETNAMESE_MARK.containsMatchIn(Normalizer.normalize(text, Normalizer.Form.NFD))

    private companion object {
        /** es-MX has no folder of its own and reads values-b+es+419. */
        val RELEASE_TRANSLATIONS = listOf("hi", "pt-BR", "es-419", "es-MX", "id", "th", "vi")

        /** Strings with no words, only a format, or a name that is the same in every language (translatable="false"). */
        val LANGUAGE_NEUTRAL = setOf(
            "camera_timer_label",
            "camera_zoom_selected",
            "camera_step_count",
            "camera_adjust_percent",
            "review_album_name",
        )

        /** (language, string) pairs kept in English, as the same word is in values-<language> of settings or onboarding. */
        val SAME_AS_ENGLISH = emptySet<Pair<String, String>>()

        /** The word for "transparency" in each language, as the onboarding uses it for the outline. */
        val TRANSPARENCY = listOf(
            "en" to "transparen",
            "hi" to "पारदर्शिता",
            "pt-BR" to "transparência",
            "es-419" to "transparencia",
            "id" to "transparansi",
            "th" to "โปร่งใส",
        )

        val PLACEHOLDER = Regex("%(\\d+\\$)?[a-z%]")
        val VIETNAMESE_MARK = Regex("[\\u0300-\\u0323đĐ]")
    }
}
