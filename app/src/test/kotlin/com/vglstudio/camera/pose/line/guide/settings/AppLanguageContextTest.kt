package com.vglstudio.camera.pose.line.guide.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith

/** The app language below Android 13, without AppCompat (spec settings, KB-4; plan T-025, question 5). */
@RunWith(AndroidJUnit4::class)
class AppLanguageContextTest {
    @Test
    fun languageToApply_belowAndroid13_usesStoredTag() {
        assertEquals("vi", languageToApply(sdkInt = 32, storedTag = "vi"))
        assertEquals("es-419", languageToApply(sdkInt = 26, storedTag = "es-419"))
    }

    @Test
    fun languageToApply_fromAndroid13_isNull() {
        assertNull(languageToApply(sdkInt = 33, storedTag = "vi"))
        assertNull(languageToApply(sdkInt = 35, storedTag = "vi"))
        // Control: the same tag one version lower is applied by the app.
        assertEquals("vi", languageToApply(sdkInt = 32, storedTag = "vi"))
    }

    @Test
    fun languageToApply_systemOrUnknown_isNull() {
        assertNull(languageToApply(sdkInt = 32, storedTag = ""))
        assertNull(languageToApply(sdkInt = 32, storedTag = "fr"))
        // Control: a release language on the same version is applied.
        assertEquals("th", languageToApply(sdkInt = 32, storedTag = "th"))
    }

    @Test
    fun withAppLanguage_setsLocale() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("th", context.withAppLanguage("th").resources.configuration.locales[0].language)
    }

    @Test
    fun withAppLanguage_null_returnsSameContext() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertSame(context, context.withAppLanguage(null))
        // Control: a language gives another context.
        assertNotSame(context, context.withAppLanguage("vi"))
    }
}
