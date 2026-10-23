package com.vglstudio.camera.pose.line.guide.feature.settings

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.data.AppLanguages
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text of the settings screen in the seven release languages (spec settings, KB-4; plan T-025, section 3.1). English
 * is the default language in values/. The language names of the picker are written once, each in its own language.
 */
@RunWith(AndroidJUnit4::class)
class SettingsStringsTest {
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
                if (tag to name !in SAME_AS_ENGLISH) {
                    assertNotEquals("$tag $name is not translated", english.getString(id), text)
                }
            }
        }
    }

    @Test
    fun currentLanguageName_matchesDialogName() {
        assertEquals(AppLanguages.RELEASE.toSet(), NAMES.keys)
        AppLanguages.RELEASE.forEach { tag ->
            val context = context(tag)
            assertEquals(
                tag,
                context.getString(NAMES.getValue(tag)),
                context.getString(R.string.settings_language_current),
            )
        }
    }

    private companion object {
        val TRANSLATED = listOf(
            R.string.settings_title,
            R.string.settings_back,
            R.string.settings_section_general,
            R.string.settings_theme,
            R.string.settings_theme_system,
            R.string.settings_theme_light,
            R.string.settings_theme_dark,
            R.string.settings_shutter_sound,
            R.string.settings_language,
            R.string.settings_language_system,
            R.string.settings_language_current,
            R.string.settings_section_privacy,
            R.string.settings_privacy_options,
            R.string.settings_privacy_policy,
            R.string.settings_section_about,
            R.string.settings_version,
            R.string.settings_cancel,
            R.string.settings_save_error,
        )

        /** The same word in both languages, as in the translation table of plan T-025 (section 3.1). */
        val SAME_AS_ENGLISH = setOf("es-419" to "settings_section_general")

        val NAMES = mapOf(
            "en" to R.string.settings_language_name_en,
            "hi" to R.string.settings_language_name_hi,
            "pt-BR" to R.string.settings_language_name_pt_br,
            "es-419" to R.string.settings_language_name_es_419,
            "id" to R.string.settings_language_name_id,
            "th" to R.string.settings_language_name_th,
            "vi" to R.string.settings_language_name_vi,
        )
    }
}
