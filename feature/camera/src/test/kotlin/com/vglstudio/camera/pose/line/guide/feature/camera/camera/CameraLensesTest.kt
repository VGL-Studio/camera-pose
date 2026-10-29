package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import kotlin.coroutines.cancellation.CancellationException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

// Spec camera, KB-4 (plan T-064): which lenses the device has, as CameraX reports them.
class CameraLensesTest {
    @Test
    fun lensesOf_bothCameras() {
        assertEquals(setOf(Lens.BACK, Lens.FRONT), lensesOf { true })
    }

    @Test
    fun lensesOf_backOnly() {
        assertEquals(setOf(Lens.BACK), lensesOf { it == Lens.BACK })
    }

    @Test
    fun lensesOf_error_isUnknown() {
        assertEquals(emptySet<Lens>(), lensesOf { throw IllegalStateException("camera info unavailable") })
    }

    @Test(expected = CancellationException::class)
    fun lensesOf_cancellation_isRethrown() {
        lensesOf { throw CancellationException("cancelled") }
    }

    // Plan T-072: the caller hears of the error, so it can log it (ADR-0019).
    @Test
    fun lensesOf_error_isReportedToCaller() {
        val errors = mutableListOf<Exception>()

        val lenses = lensesOf(onError = { errors += it }) { throw IllegalStateException("camera info unavailable") }

        assertEquals(emptySet<Lens>(), lenses)
        assertEquals(1, errors.size)
        assertTrue(errors.single() is IllegalStateException)
    }
}
