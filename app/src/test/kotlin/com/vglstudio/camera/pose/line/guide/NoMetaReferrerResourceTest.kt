package com.vglstudio.camera.pose.line.guide

import android.app.Application
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/**
 * The Tenjin SDK reads the string resource `facebook_app_id` to measure installs from Meta; the app has none, so that
 * part never runs (spec ads, question 21 g; plan T-099). Reads the merged resources of the app.
 */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class NoMetaReferrerResourceTest {
    @Test
    fun mergedResources_haveNoFacebookAppId() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        // Control: a string of the app is found by the same lookup.
        assertNotEquals(0, context.resources.getIdentifier("app_name", "string", context.packageName))
        assertEquals(0, context.resources.getIdentifier("facebook_app_id", "string", context.packageName))
    }
}
