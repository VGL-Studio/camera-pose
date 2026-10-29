package com.vglstudio.camera.pose.line.guide

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.BreakTicket
import com.vglstudio.camera.pose.line.guide.core.ads.InterstitialBreakGate
import com.vglstudio.camera.pose.line.guide.core.ads.LocalAdsController
import com.vglstudio.camera.pose.line.guide.core.ads.LocalInterstitialBreakGate
import com.vglstudio.camera.pose.line.guide.core.review.InAppReview
import com.vglstudio.camera.pose.line.guide.core.review.LocalInAppReview
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * The root of the app's UI that MainActivity sets on every create ([AppAdsRoot], plan T-074, section 4; L-061): the
 * content reads the break gate and the review entry, also in an activity made again (rotation, process restored).
 * The activity sets the root in onCreate as MainActivity does; with no controller (tests) [LocalAdsController] is null.
 */
@RunWith(AndroidJUnit4::class)
class AppAdsRootTest {
    private val application: Application = ApplicationProvider.getApplicationContext()
    private val gate = object : InterstitialBreakGate {
        override suspend fun onBreak(placement: AdPlacement): BreakTicket? = null

        override fun onShown(ticket: BreakTicket) = Unit
    }
    private val review = object : InAppReview {
        override fun requestIfDue(activity: Activity, savedPhotos: Int) = Unit
    }

    private class Read(val activity: Activity, val gate: InterstitialBreakGate?, val review: InAppReview?)

    private val reads = mutableListOf<Read>()

    private val setRootOnCreate = object : Application.ActivityLifecycleCallbacks {
        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            (activity as ComponentActivity).setContent {
                AppAdsRoot(ads = null, inAppReview = review, breakGate = gate) {
                    reads += Read(activity, LocalInterstitialBreakGate.current, LocalInAppReview.current)
                    // Control: the ads controller of the test is the one given, so the locals read are the root's.
                    assertNull(LocalAdsController.current)
                }
            }
        }

        override fun onActivityStarted(activity: Activity) = Unit

        override fun onActivityResumed(activity: Activity) = Unit

        override fun onActivityPaused(activity: Activity) = Unit

        override fun onActivityStopped(activity: Activity) = Unit

        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) = Unit
    }

    init {
        application.registerActivityLifecycleCallbacks(setRootOnCreate)
    }

    @After
    fun tearDown() {
        application.unregisterActivityLifecycleCallbacks(setRootOnCreate)
    }

    @Test
    fun providesBreakGateAndInAppReview_afterRecreate() {
        ActivityScenario.launch(ComponentActivity::class.java).use { scenario ->
            scenario.moveToState(Lifecycle.State.RESUMED)
            shadowOf(android.os.Looper.getMainLooper()).idle()
            val first = reads.last()
            assertSame(gate, first.gate)
            assertSame(review, first.review)

            scenario.recreate()
            shadowOf(android.os.Looper.getMainLooper()).idle()
            val second = reads.last()
            // Control: the read comes from the new activity, not from the first one again.
            assertNotSame(first.activity, second.activity)
            assertSame(gate, second.gate)
            assertSame(review, second.review)
            assertEquals(2, reads.map { it.activity }.distinct().size)
        }
    }
}
