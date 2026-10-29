package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import android.app.Application
import androidx.activity.ComponentActivity
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.example.fakesdk.FakeSdkActivity
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** An activity of the app, in the package of the test. */
class AppTestActivity : ComponentActivity()

/** The second activity of the app. */
class OtherAppTestActivity : ComponentActivity()

/** Lifecycle callbacks of Application, as the tracker gets them (plan T-020, section 3.1, point 4). */
@RunWith(AndroidJUnit4::class)
class AdsActivityTrackerTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()
    private val events = mutableListOf<String>()
    private val processEvents = mutableListOf<Boolean>()
    private val resumed = mutableListOf<Activity>()
    private val destroyed = mutableListOf<Pair<Activity, Boolean>>()
    private val registered = mutableListOf<AdsActivityTracker>()

    private val listener = object : AdsActivityTracker.Listener {
        override fun onForegroundChanged(foreground: Boolean) {
            events += "foreground:$foreground"
        }

        override fun onProcessForegroundChanged(foreground: Boolean) {
            processEvents += foreground
        }

        override fun onActivityResumed(activity: Activity) {
            resumed += activity
        }

        override fun onActivityDestroyed(activity: Activity) {
            destroyed += activity to activity.isChangingConfigurations
        }
    }

    private fun tracker(filter: AppActivityFilter = AppActivityFilter.inPackageOf(AppTestActivity::class.java)) =
        AdsActivityTracker(filter, listener).also {
            application.registerActivityLifecycleCallbacks(it)
            registered += it
        }

    @After
    fun unregister() {
        registered.forEach { application.unregisterActivityLifecycleCallbacks(it) }
    }

    @Test
    fun foreground_followsStartedActivities() {
        val tracker = tracker()
        val first = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        assertTrue(tracker.foreground)
        assertSame(first.get(), tracker.resumedActivity)
        first.pause()
        assertNull(tracker.resumedActivity)
        first.stop()
        assertFalse(tracker.foreground)

        // Two activities passing to each other: the new one starts before the old one stops.
        first.start().resume()
        first.pause()
        val second = Robolectric.buildActivity(OtherAppTestActivity::class.java).setup()
        first.stop()
        assertTrue(tracker.foreground)
        assertSame(second.get(), tracker.resumedActivity)
        assertEquals(listOf("foreground:true", "foreground:false", "foreground:true"), events)
    }

    @Test
    fun sdkActivity_isIgnored() {
        val tracker = tracker()
        val app = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        resumed.clear()
        // A full-screen ad of the SDK covers the app.
        val sdk = Robolectric.buildActivity(FakeSdkActivity::class.java).setup()
        app.pause().stop()
        assertFalse(tracker.foreground)
        assertNull(tracker.resumedActivity)
        assertTrue(resumed.isEmpty())
        sdk.pause().stop().destroy()
        app.start().resume()
        assertEquals(listOf<Activity>(app.get()), resumed)
        assertTrue(destroyed.none { it.first is FakeSdkActivity })
    }

    @Test
    fun sdkActivity_withFilterAcceptingAll_isTracked() {
        // Control (L-055): the same sequence with a filter that accepts every activity sees the SDK activity.
        val tracker = tracker(AppActivityFilter { true })
        val app = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        val sdk = Robolectric.buildActivity(FakeSdkActivity::class.java).setup()
        app.pause().stop()
        assertSame(sdk.get(), tracker.resumedActivity)
        assertTrue(tracker.foreground)
    }

    @Test
    fun appActivityFilter_matchesPackagePrefixOnly() {
        val filter = AppActivityFilter.inPackageOf(AppTestActivity::class.java)
        val app = Robolectric.buildActivity(AppTestActivity::class.java).get()
        val sdk = Robolectric.buildActivity(FakeSdkActivity::class.java).get()
        assertTrue(filter.isAppActivity(app))
        assertFalse(filter.isAppActivity(sdk))
        val appPackage = "com.vglstudio.camera.pose.line.guide.core.ads"
        assertTrue(isClassInPackage("com.vglstudio.camera.pose.line.guide.core.ads.AppTestActivity", appPackage))
        assertTrue(isClassInPackage("com.vglstudio.camera.pose.line.guide.core.ads.gallery.GalleryActivity", appPackage))
        assertFalse(isClassInPackage("com.vglstudio.camera.pose.line.guide.core.adsx.AdActivity", appPackage))
        assertFalse(isClassInPackage("com.google.android.gms.ads.AdActivity", appPackage))
    }

    @Test
    fun destroyedActivity_isReported() {
        tracker()
        val controller = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        val first = controller.get()
        controller.recreate()
        assertEquals(listOf(first to true), destroyed)
        val second = controller.get()
        controller.pause().stop().destroy()
        assertEquals(listOf(first to true, second to false), destroyed)
    }

    @Test
    fun processForeground_recreate_reportsNothing() {
        tracker()
        // ActivityController.recreate() of Robolectric.
        val controller = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        controller.recreate()
        controller.recreate()
        assertEquals(listOf(true), processEvents)
        // Control (L-055): a real stop of the recreated activity is a trip to background.
        controller.pause().stop()
        assertEquals(listOf(true, false), processEvents)
        controller.destroy()

        // ActivityScenario.recreate(), with an activity outside the app's package: the process counts every activity.
        processEvents.clear()
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.recreate()
            scenario.recreate()
            assertEquals(listOf(true), processEvents)
            scenario.moveToState(Lifecycle.State.CREATED)
            assertEquals(listOf(true, false), processEvents)
        }
    }

    @Test
    fun processForeground_backgroundAndBack_reportsFalseThenTrue() {
        tracker()
        val app = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        app.pause().stop()
        app.start().resume()
        assertEquals(listOf(true, false, true), processEvents)
        // The app's own foreground is unchanged.
        assertEquals(listOf("foreground:true", "foreground:false", "foreground:true"), events)
    }

    @Test
    fun processForeground_sdkActivityCoversApp_staysTrue() {
        val tracker = tracker()
        val app = Robolectric.buildActivity(AppTestActivity::class.java).setup()
        // A full-screen ad of the SDK opens over the app and closes; the app starts again before the SDK stops.
        val sdk = Robolectric.buildActivity(FakeSdkActivity::class.java).setup()
        app.pause().stop()
        assertFalse(tracker.foreground)
        sdk.pause()
        app.start().resume()
        sdk.stop().destroy()
        assertEquals(listOf(true), processEvents)
        // Control: the app's own foreground did go to false and back (SDK activities are not app activities).
        assertEquals(listOf("foreground:true", "foreground:false", "foreground:true"), events)
    }
}
