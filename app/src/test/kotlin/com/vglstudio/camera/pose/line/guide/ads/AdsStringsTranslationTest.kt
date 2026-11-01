package com.vglstudio.camera.pose.line.guide.ads

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.R as AdsR
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * The buttons and the message of the rewarded dialog of :core:ads, translated by the app in its own values-<language>
 * (spec poses, KB-8; plan T-086, section 3.1). English keeps the text of the framework.
 */
@RunWith(AndroidJUnit4::class)
class AdsStringsTranslationTest {
    private fun context(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(configuration)
    }

    @Test
    fun vietnamese_usesTheAppTranslation() {
        val vietnamese = context("vi")
        assertEquals("Xem quảng cáo", vietnamese.getString(AdsR.string.ads_reward_watch))
        assertEquals("Để sau", vietnamese.getString(AdsR.string.ads_reward_no_thanks))
        assertEquals("Kiểm tra kết nối rồi thử lại", vietnamese.getString(AdsR.string.ads_reward_check_connection))
    }

    @Test
    fun english_keepsTheFrameworkText() {
        val english = context("en")
        assertEquals("Watch ad", english.getString(AdsR.string.ads_reward_watch))
        assertEquals("No, thanks", english.getString(AdsR.string.ads_reward_no_thanks))
        assertEquals(
            "Check your connection and try again",
            english.getString(AdsR.string.ads_reward_check_connection),
        )
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

    private companion object {
        val TRANSLATED = listOf(
            AdsR.string.ads_reward_watch,
            AdsR.string.ads_reward_no_thanks,
            AdsR.string.ads_reward_check_connection,
        )
    }
}
