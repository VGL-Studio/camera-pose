package com.vglstudio.camera.pose.line.guide.feature.onboarding

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
 * Text of the onboarding screen in the seven release languages (spec onboarding, question 1). English is the default
 * language in values/; Vietnamese is the text table of the design (docs/features/onboarding/design/README.md,
 * section 2).
 */
@RunWith(AndroidJUnit4::class)
class OnboardingStringsTest {
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
    fun english_isDefault() {
        assertTexts("en", ENGLISH)
        // A language without its own folder shows the default language.
        assertTexts("fr", ENGLISH)
    }

    @Test
    fun vietnamese_matchesDesignTextTable() {
        assertTexts("vi", VIETNAMESE)
    }

    @Test
    fun everyReleaseLanguage_isTranslated() {
        val english = context("en")
        // "id" reads values-in.
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

    private companion object {
        val ENGLISH = mapOf(
            R.string.onboarding_title to "Strike a great pose, no guesswork",
            R.string.onboarding_body to "Pick a pose, step into the outline, then tap to shoot. " +
                "Photos save to your gallery without the outline.",
            R.string.onboarding_point_poses to "200+ poses: solo, couples, friends",
            R.string.onboarding_point_adjust to "Adjust outline opacity, size and position",
            R.string.onboarding_point_saved to "Save favorite poses to shoot again later",
            R.string.onboarding_enable_camera to "Turn on camera",
            R.string.onboarding_note to
                "The app needs camera access to show the preview. Photos are stored only on your device.",
            R.string.onboarding_next to "Next",
            R.string.onboarding_poses_title to "A pose for every shot",
            R.string.onboarding_premium_title to "Premium poses",
            R.string.onboarding_premium_body to "Watch one ad to unlock a premium pose, then keep it for good.",
            R.string.onboarding_camera_title to "Ready to shoot",
            R.string.onboarding_page_indicator to "Page %1\$d of %2\$d",
        )

        val VIETNAMESE = mapOf(
            R.string.onboarding_title to "Chụp dáng đẹp, không cần đoán",
            R.string.onboarding_body to
                "Chọn một tư thế, đứng vào nét viền rồi bấm chụp. Ảnh lưu vào thư viện máy, không kèm nét viền.",
            R.string.onboarding_point_poses to "Hơn 200 tư thế: một người, cặp đôi, nhóm bạn",
            R.string.onboarding_point_adjust to "Chỉnh độ mờ, cỡ và vị trí nét viền",
            R.string.onboarding_point_saved to "Lưu tư thế yêu thích để chụp lại sau",
            R.string.onboarding_enable_camera to "Bật camera",
            R.string.onboarding_note to
                "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
            R.string.onboarding_next to "Tiếp",
            R.string.onboarding_poses_title to "Tư thế cho mọi kiểu ảnh",
            R.string.onboarding_premium_title to "Tư thế premium",
            R.string.onboarding_premium_body to "Xem một quảng cáo để mở một tư thế premium, rồi dùng mãi về sau.",
            R.string.onboarding_camera_title to "Sẵn sàng chụp",
            R.string.onboarding_page_indicator to "Trang %1\$d trên %2\$d",
        )
    }
}
