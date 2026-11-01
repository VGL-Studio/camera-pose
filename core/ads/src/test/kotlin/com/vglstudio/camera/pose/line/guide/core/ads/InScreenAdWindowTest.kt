package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The window of the in-screen ads (KB-27, ADR-0036 decision 1, plan T-054): the one condition, the window of the
 * process set on the calling stack, and the window of one banner.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class InScreenAdWindowTest {
    private val logger = RecordingAppLogger()

    private fun messages() = logger.entries.map { it.message }

    @Test
    fun mayRun_table() {
        assertTrue(inScreenAdsMayRun(shown = true, covered = false))
        assertFalse(inScreenAdsMayRun(shown = true, covered = true))
        assertFalse(inScreenAdsMayRun(shown = false, covered = false))
        assertFalse(inScreenAdsMayRun(shown = false, covered = true))
    }

    @Test
    fun window_followsForegroundAndCover_synchronously() {
        val window = InScreenAdWindow(logger)
        assertFalse(window.open.value)

        // Read with no dispatcher run in between: the window is set on the calling stack.
        window.onForeground(true)
        assertTrue(window.open.value)
        window.onCovered(true)
        assertFalse(window.open.value)
        window.onCovered(false)
        assertTrue(window.open.value)
        assertEquals(listOf(PAUSED, RESUMED), messages())

        // L-091: to background while covered, then the cover goes: still closed until foreground again.
        window.onCovered(true)
        window.onForeground(false)
        window.onCovered(false)
        assertFalse(window.open.value)
        window.onForeground(true)
        assertTrue(window.open.value)

        // No log when only foreground moves or the same cover value comes again.
        window.onForeground(false)
        window.onForeground(true)
        window.onCovered(false)
        window.onCovered(true)
        window.onCovered(true)
        assertEquals(listOf(PAUSED, RESUMED, PAUSED, RESUMED, PAUSED), messages())
        assertTrue(logger.entries.all { it.tag == "Ads" })
    }

    @Test
    fun flowWindow_combinesVisibleAndCover_distinct() = runTest {
        val visible = MutableSharedFlow<Boolean>(replay = 1).apply { tryEmit(true) }
        val covered = MutableStateFlow(false)
        val values = mutableListOf<Boolean>()
        val job = launch(UnconfinedTestDispatcher(testScheduler)) {
            inScreenAdWindowOf(visible, covered).collect { values += it }
        }

        covered.value = true
        // The screen reports the same value again: no repeat.
        visible.emit(true)
        covered.value = false
        visible.emit(true)
        job.cancel()

        assertEquals(listOf(true, false, true), values)
    }

    private companion object {
        const val PAUSED = "In-screen ads paused (full-screen ad on screen)"
        const val RESUMED = "In-screen ads resumed (full-screen ad closed)"
    }
}
