package com.vglstudio.camera.pose.line.guide.feature.camera

import androidx.camera.core.SurfaceRequest
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraCapabilities
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraController
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CameraOpenException
import com.vglstudio.camera.pose.line.guide.feature.camera.camera.CaptureFailedException
import com.vglstudio.camera.pose.line.guide.feature.camera.data.CameraSettingsRepository
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import java.io.File
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

internal class FakeCameraController : CameraController {
    val openCalls = mutableListOf<Lens>()
    val flashCalls = mutableListOf<FlashMode>()
    val zoomCalls = mutableListOf<Float>()
    var closeCount = 0
    var captureCount = 0
    var capabilities = CameraCapabilities(hasFlash = true, minZoomRatio = 1f, maxZoomRatio = 8f)
    var openError: Throwable? = null
    var captureError: Throwable? = null

    /** When set, open waits for it, so a test can tap during an open. */
    var pendingOpen: CompletableDeferred<Unit>? = null

    /** When set, takePicture waits for it, so a test can tap twice during one capture. */
    var pendingCapture: CompletableDeferred<Unit>? = null

    override val surfaceRequest: StateFlow<SurfaceRequest?> = MutableStateFlow(null)

    override suspend fun open(lens: Lens, flash: FlashMode): CameraCapabilities {
        openCalls += lens
        pendingOpen?.await()
        openError?.let { throw CameraOpenException(it) }
        return capabilities
    }

    override fun setFlash(mode: FlashMode) {
        flashCalls += mode
    }

    override fun setZoom(ratio: Float) {
        zoomCalls += ratio
    }

    override suspend fun takePicture(): File {
        captureCount++
        pendingCapture?.await()
        captureError?.let { throw CaptureFailedException(it) }
        return File("capture_1.jpg")
    }

    override fun close() {
        closeCount++
    }
}

internal class FakeCameraSettingsRepository : CameraSettingsRepository {
    val stored = MutableStateFlow(CameraSettings())
    val flashSaves = mutableListOf<FlashMode>()
    val timerSaves = mutableListOf<TimerSetting>()
    val gridSaves = mutableListOf<Boolean>()
    val lensSaves = mutableListOf<Lens>()
    var failWrites = false

    override val settings: Flow<CameraSettings> = stored

    override suspend fun setFlash(mode: FlashMode) = write { flashSaves += mode }

    override suspend fun setTimer(timer: TimerSetting) = write { timerSaves += timer }

    override suspend fun setGridVisible(visible: Boolean) = write { gridSaves += visible }

    override suspend fun setLens(lens: Lens) = write { lensSaves += lens }

    private fun write(save: () -> Unit) {
        if (failWrites) throw IOException("disk full")
        save()
    }
}

internal class FakeAnalytics : Analytics {
    val events = mutableListOf<AnalyticsEvent>()

    override fun log(event: AnalyticsEvent) {
        events += event
    }
}

internal class FakeCountdownInterruptions : CountdownInterruptions {
    val value = MutableStateFlow(false)

    override fun active(): Flow<Boolean> = value
}
