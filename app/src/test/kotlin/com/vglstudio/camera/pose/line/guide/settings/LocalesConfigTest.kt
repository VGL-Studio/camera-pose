package com.vglstudio.camera.pose.line.guide.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.R
import com.vglstudio.camera.pose.line.guide.core.data.AppLanguages
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.xmlpull.v1.XmlPullParser

/**
 * The app language page of the device lists the languages of android:localeConfig, so they must be the release
 * languages (spec settings, KB-4; plan T-025, section 4).
 */
@RunWith(AndroidJUnit4::class)
class LocalesConfigTest {
    @Test
    fun listsExactlyReleaseLanguages() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val parser = context.resources.getXml(R.xml.locales_config)
        val locales = mutableListOf<String>()
        while (parser.next() != XmlPullParser.END_DOCUMENT) {
            if (parser.eventType == XmlPullParser.START_TAG && parser.name == "locale") {
                locales += parser.getAttributeValue(ANDROID_NAMESPACE, "name")
            }
        }
        assertEquals(AppLanguages.RELEASE.toSet(), locales.toSet())
        assertEquals(AppLanguages.RELEASE.size, locales.size)
    }

    private companion object {
        const val ANDROID_NAMESPACE = "http://schemas.android.com/apk/res/android"
    }
}
