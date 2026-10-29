package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Activity
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** G-06: on every resume, the stuck flag is released before consent is gathered again, then the resume ad (T-025). */
@RunWith(AndroidJUnit4::class)
class ActivityResumeStepsTest {
    @Test
    fun unstickRunsBeforeConsentRegather() {
        val steps = mutableListOf<String>()
        val resume = ActivityResumeSteps(
            recoverStuck = { steps += "recoverStuck" },
            regatherConsent = { steps += "regatherConsent" },
            showResumeAd = { steps += "showResumeAd" },
            releaseLeaveMark = { steps += "releaseLeaveMark" },
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
        resume.onResumed(activity)
        assertEquals(listOf("recoverStuck", "regatherConsent", "showResumeAd", "releaseLeaveMark"), steps)
    }

    @Test
    fun resumeAdRunsAfterConsentRegather_beforeLeaveMarkRelease() {
        val steps = mutableListOf<String>()
        val shownOn = mutableListOf<Activity>()
        val resume = ActivityResumeSteps(
            recoverStuck = { steps += "recoverStuck" },
            regatherConsent = { steps += "regatherConsent" },
            showResumeAd = {
                steps += "showResumeAd"
                shownOn += it
            },
            releaseLeaveMark = { steps += "releaseLeaveMark" },
        )
        val activity = Robolectric.buildActivity(ComponentActivity::class.java).get()
        resume.onResumed(activity)
        // Stuck recovery and the consent regather run first; the leave mark is released after the resume ad (A-14).
        assertEquals(listOf("recoverStuck", "regatherConsent", "showResumeAd", "releaseLeaveMark"), steps)
        assertEquals(listOf<Activity>(activity), shownOn)
    }
}
