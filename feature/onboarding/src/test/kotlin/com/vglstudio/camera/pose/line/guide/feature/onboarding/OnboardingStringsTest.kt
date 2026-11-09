package com.vglstudio.camera.pose.line.guide.feature.onboarding

import android.content.Context
import android.content.res.Configuration
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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

    @Test
    fun noText_mentionsPremium() {
        // Control: the check below finds the word, in any case, inside a text.
        assertTrue(mentionsPremium("Unlock a Premium pose"))
        listOf("en", "hi", "pt-BR", "es-419", "id", "th", "vi").forEach { tag ->
            val context = context(tag)
            (ENGLISH.keys + VIETNAMESE.keys).forEach { id ->
                val name = context.resources.getResourceEntryName(id)
                assertFalse("$tag $name mentions premium", mentionsPremium(context.getString(id)))
            }
        }
    }

    private fun mentionsPremium(text: String) = text.contains("premium", ignoreCase = true)

    private companion object {
        val ENGLISH = mapOf(
            R.string.onboarding_title to "Great poses, no guessing",
            R.string.onboarding_body to
                "Not sure how to pose? The app suggests a pose and shows you where to stand.",
            R.string.onboarding_poses_title to "200+ poses ready to go",
            R.string.onboarding_poses_body to
                "Solo, couples or friends, it's all here. Save your favorites to shoot again later.",
            R.string.onboarding_guide_title to "Match the outline, shoot",
            R.string.onboarding_guide_body to "Adjust the outline's opacity, size and position to fit the frame. " +
                "Saved photos don't include the outline.",
            R.string.onboarding_guide_bubble to "Stand here!",
            R.string.onboarding_camera_title to "Ready to shoot!",
            R.string.onboarding_camera_body to "Pick a pose, step into the outline and take your first photo.",
            R.string.onboarding_enable_camera to "Turn on camera",
            R.string.onboarding_note to
                "The app needs camera access to show the preview. Photos are stored only on your device.",
            R.string.onboarding_next to "Next",
            R.string.onboarding_page_indicator to "Page %1\$d of %2\$d",
        )

        val VIETNAMESE = mapOf(
            R.string.onboarding_title to "Dáng đẹp, không cần đoán",
            R.string.onboarding_body to "Chưa biết tạo dáng thế nào? App gợi ý dáng và chỉ bạn đứng ra sao.",
            R.string.onboarding_poses_title to "Hơn 200 tư thế có sẵn",
            R.string.onboarding_poses_body to
                "Một người, cặp đôi hay nhóm bạn đều có. Lưu dáng yêu thích để chụp lại sau.",
            R.string.onboarding_guide_title to "Khớp nét viền, bấm chụp",
            R.string.onboarding_guide_body to
                "Chỉnh độ mờ, cỡ và vị trí nét viền cho vừa khung hình. Ảnh lưu ra không kèm nét viền.",
            R.string.onboarding_guide_bubble to "Đứng vào đây!",
            R.string.onboarding_camera_title to "Sẵn sàng chụp rồi!",
            R.string.onboarding_camera_body to "Chọn một dáng, đứng vào nét viền và chụp tấm ảnh đầu tiên của bạn.",
            R.string.onboarding_enable_camera to "Bật camera",
            R.string.onboarding_note to
                "App cần quyền camera để hiện khung xem trước. Ảnh chỉ lưu trên máy của bạn.",
            R.string.onboarding_next to "Tiếp",
            R.string.onboarding_page_indicator to "Trang %1\$d trên %2\$d",
        )
    }
}
