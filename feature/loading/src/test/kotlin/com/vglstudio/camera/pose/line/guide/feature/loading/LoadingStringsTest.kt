package com.vglstudio.camera.pose.line.guide.feature.loading

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Text of the loading screen in the seven release languages (spec onboarding, question 1). English is the default
 * language in values/; Vietnamese is the text table of the design (docs/features/loading/design/README.md, section 2).
 */
@RunWith(AndroidJUnit4::class)
class LoadingStringsTest {
    private fun context(languageTag: String): Context {
        val base = ApplicationProvider.getApplicationContext<Context>()
        val configuration = Configuration(base.resources.configuration)
        configuration.setLocale(Locale.forLanguageTag(languageTag))
        return base.createConfigurationContext(configuration)
    }

    private fun assertTexts(languageTag: String, expected: Map<Int, String>) {
        val context = context(languageTag)
        expected.forEach { (id, text) ->
            assertEquals("$languageTag ${context.resources.getResourceEntryName(id)}", text, context.getString(id))
        }
    }

    @Test
    fun vietnamese_matchesDesignTextTable() {
        assertTexts("vi", VIETNAMESE)
    }

    @Test
    fun english_isDefault() {
        assertTexts("en", ENGLISH)
        // A language without its own folder shows the default language.
        assertTexts("fr", ENGLISH)
    }

    @Test
    fun everyReleaseLanguage_isTranslated() {
        val english = context("en")
        listOf("hi", "pt-BR", "es-419", "es-MX", "id", "th", "vi").forEach { tag ->
            val context = context(tag)
            ENGLISH.keys.forEach { id ->
                val text = context.getString(id)
                val name = context.resources.getResourceEntryName(id)
                assertTrue("$tag $name is empty", text.isNotBlank())
                assertNotEquals("$tag $name is not translated", english.getString(id), text)
            }
        }
    }

    @Test
    fun percent_isSameInEveryLanguage() {
        listOf("en", "vi", "hi", "pt-BR", "es-419", "id", "th").forEach { tag ->
            assertEquals(tag, "68%", context(tag).getString(R.string.loading_percent, 68))
        }
    }

    private companion object {
        val ENGLISH = mapOf(
        R.string.loading_tagline to "Follow the outline, get a great shot.",
        R.string.loading_status to "Loading data",
        R.string.loading_guidance to "Preparing your poses…",
        R.string.loading_status_failed to "Progress stopped",
        R.string.loading_error_title to "Unable to load data",
        R.string.loading_error to "The connection was interrupted. Check your network and try again to continue.",
        R.string.loading_retry to "Try again",
        R.string.loading_footer to "Your photos stay on your device",
        )

        val VIETNAMESE = mapOf(
        R.string.loading_tagline to "Tạo dáng theo nét viền, chụp đẹp ngay.",
        R.string.loading_status to "Đang tải dữ liệu",
        R.string.loading_guidance to "Đang chuẩn bị bộ tư thế…",
        R.string.loading_status_failed to "Tiến độ đã dừng",
        R.string.loading_error_title to "Không thể tải dữ liệu",
        R.string.loading_error to "Kết nối đang gián đoạn. Vui lòng kiểm tra mạng và thử lại để tiếp tục.",
        R.string.loading_retry to "Thử lại",
        R.string.loading_footer to "Ảnh chỉ lưu trên máy của bạn",
        )
    }
}
