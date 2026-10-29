package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** The context a full-screen ad is loaded with: the resumed activity of the app when usable (question 4 of T-034). */
@RunWith(AndroidJUnit4::class)
class FullScreenLoadContextTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    @Test
    fun resumedActivity_isUsed() {
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()

        assertSame(activity, fullScreenLoadContext(activity, application))
    }

    @Test
    fun finishingDestroyedOrNull_fallsBackToApplication() {
        // Control (L-055): a usable activity is used, so the fallbacks below are told apart from it.
        val usable = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        assertSame(usable, fullScreenLoadContext(usable, application))

        val finishing = Robolectric.buildActivity(ComponentActivity::class.java).setup().get()
        finishing.finish()
        assertSame(application, fullScreenLoadContext(finishing, application))

        val controller = Robolectric.buildActivity(ComponentActivity::class.java).setup()
        controller.pause().stop().destroy()
        assertSame(application, fullScreenLoadContext(controller.get(), application))

        assertSame(application, fullScreenLoadContext(null, application))
    }
}
