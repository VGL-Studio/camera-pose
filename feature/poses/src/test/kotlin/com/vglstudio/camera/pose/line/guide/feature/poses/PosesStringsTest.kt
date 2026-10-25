package com.vglstudio.camera.pose.line.guide.feature.poses

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
 * Text of the pose library and the pose detail in the seven release languages (spec settings, KB-4; plan T-051,
 * section 3.1). English is the default language in values/; Vietnamese, the text table of the design, is in
 * values-vi/.
 */
@RunWith(AndroidJUnit4::class)
class PosesStringsTest {
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
        /** Every string of values/ (30 strings, plan T-051 section 3.1). */
        val TRANSLATED = listOf(
            R.string.poses_title,
            R.string.poses_summary,
            R.string.poses_chip_all,
            R.string.poses_chip_saved,
            R.string.poses_card_meta,
            R.string.poses_subject_single,
            R.string.poses_subject_couple,
            R.string.poses_subject_group,
            R.string.poses_error_title,
            R.string.poses_error_message,
            R.string.poses_error_action,
            R.string.poses_saved_empty_title,
            R.string.poses_saved_empty_message,
            R.string.poses_saved_empty_action,
            R.string.poses_save_failed,
            R.string.poses_cd_back,
            R.string.poses_cd_settings,
            R.string.poses_cd_save,
            R.string.poses_cd_unsave,
            R.string.poses_cd_reload_image,
            R.string.poses_detail_meta,
            R.string.poses_detail_meta_no_group,
            R.string.poses_framing_full,
            R.string.poses_framing_half,
            R.string.poses_framing_close,
            R.string.poses_detail_steps_title,
            R.string.poses_detail_mistakes_title,
            R.string.poses_detail_shoot,
            R.string.poses_image_error_title,
            R.string.poses_image_error_message,
        )

        val VIETNAMESE_MARK = Regex("[\\u0300-\\u0323đĐ]")
    }
}
