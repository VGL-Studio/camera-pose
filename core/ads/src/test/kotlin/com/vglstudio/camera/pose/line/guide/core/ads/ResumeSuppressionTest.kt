package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** G-01 to G-05: what blocks the ads that show by themselves (resume ad, T-025), and what lowers it. */
class ResumeSuppressionTest {
    private val suppressor = ForegroundAdSuppressor()
    private val suppression = ResumeSuppression(suppressor, RecordingAppLogger())

    @Test
    fun holdRaisesAtOnce_releaseWaitsForShellReport() {
        val hold = suppressor.acquire("system_screen")
        // No report from the shell yet (G-02).
        assertTrue(suppression.resumeAdsSuppressed.value)
        hold.release()
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onShellReported(false)
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun leaveMark_releasedOnNextActivityResume() {
        suppression.markLeavingApp()
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onActivityResumed()
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun leaveMark_independentFromHolds() {
        val hold = suppressor.acquire("system_screen")
        suppression.markLeavingApp()
        suppression.onActivityResumed()
        // The mark is used up, the hold is not (G-01).
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onShellReported(true)
        assertTrue(suppression.resumeAdsSuppressed.value)
        hold.release()
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onShellReported(false)
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun failedOpen_resumeAdsWorkAgain() {
        val hold = suppressor.acquire("system_screen")
        // Control: the hold blocks.
        assertTrue(suppression.resumeAdsSuppressed.value)
        // Opening failed: the feature releases.
        hold.release()
        suppression.onShellReported(false)
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun leaveMark_neverOutlivesOneResume() {
        suppression.markLeavingApp()
        // Control: the mark blocks.
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onActivityResumed()
        assertFalse(suppression.resumeAdsSuppressed.value)
        suppression.onActivityResumed()
        assertFalse(suppression.resumeAdsSuppressed.value)
        // No negative release: a new mark blocks again.
        suppression.markLeavingApp()
        assertTrue(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun adClick_suppressesUntilNextActivityResume() {
        // Control: nothing blocks before the click.
        assertFalse(suppression.resumeAdsSuppressed.value)
        suppression.markAdClicked()
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onActivityResumed()
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun adClickTwice_keepsOneMark() {
        suppression.markAdClicked()
        suppression.markAdClicked()
        assertTrue(suppression.resumeAdsSuppressed.value)
        // A flag, not a counter: one resume releases both clicks.
        suppression.onActivityResumed()
        assertFalse(suppression.resumeAdsSuppressed.value)
    }

    @Test
    fun adClick_doesNotReleaseHolds() {
        val hold = suppression.hold(ResumeSuppression.SYSTEM_SCREEN)
        suppression.markAdClicked()
        suppression.onActivityResumed()
        // The click mark is used up, the hold is not (G-01).
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.onShellReported(true)
        assertTrue(suppression.resumeAdsSuppressed.value)
        suppression.release(hold)
        suppression.onShellReported(false)
        assertFalse(suppression.resumeAdsSuppressed.value)
        // Control (L-055): a click alone blocks, so the release above was the hold's and not a missing mark.
        suppression.markAdClicked()
        assertTrue(suppression.resumeAdsSuppressed.value)
    }
}
