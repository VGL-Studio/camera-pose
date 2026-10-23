package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import androidx.camera.core.CameraControl
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class CameraZoomTest {
    @Test
    fun applyZoom_setsTheRatioOnTheCameraControl() {
        val control = mockk<CameraControl>(relaxed = true)

        applyZoom(control, 2f, RecordingAppLogger())

        verify(exactly = 1) { control.setZoomRatio(2f) }
    }

    @Test
    fun applyZoom_logsAWarning_whenCameraXThrows() {
        val control = mockk<CameraControl>()
        every { control.setZoomRatio(any()) } throws IllegalStateException("closed")
        val logger = RecordingAppLogger()

        applyZoom(control, 2f, logger)

        // Counter-check (L-035): a call that succeeds logs nothing more.
        every { control.setZoomRatio(1f) } returns mockk(relaxed = true)
        applyZoom(control, 1f, logger)

        assertEquals(1, logger.entries.size)
        val entry = logger.entries.single()
        assertEquals(LogLevel.WARN, entry.level)
        assertEquals("Camera", entry.tag)
        assertEquals("Zoom could not be set", entry.message)
        assertTrue(entry.error is IllegalStateException)
    }
}
