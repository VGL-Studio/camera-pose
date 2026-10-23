package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.google.android.play.core.review.testing.FakeReviewManager
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric

/** [PlayReviewPrompt] asks for the review info, then launches the flow with it (plan T-019, section 4.1). */
@RunWith(AndroidJUnit4::class)
class PlayReviewPromptTest {
    private class RecordingManager(private val inner: ReviewManager) : ReviewManager {
        var requested = 0
        var launched: ReviewInfo? = null
        var issued: ReviewInfo? = null

        override fun requestReviewFlow(): Task<ReviewInfo> {
            requested++
            return inner.requestReviewFlow().also { task -> issued = task.result }
        }

        override fun launchReviewFlow(activity: Activity, info: ReviewInfo): Task<Void> {
            launched = info
            return inner.launchReviewFlow(activity, info)
        }
    }

    @Test
    fun fakeManager_completes() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val manager = RecordingManager(FakeReviewManager(context))
        val activity = Robolectric.buildActivity(Activity::class.java).get()
        PlayReviewPrompt(manager).show(activity)
        assertEquals(1, manager.requested)
        assertSame(manager.issued, manager.launched)
    }
}
