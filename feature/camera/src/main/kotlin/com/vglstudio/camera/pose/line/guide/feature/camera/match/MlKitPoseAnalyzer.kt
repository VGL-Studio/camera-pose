package com.vglstudio.camera.pose.line.guide.feature.camera.match

import android.os.SystemClock
import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.pose.PoseDetection
import com.google.mlkit.vision.pose.PoseDetector
import com.google.mlkit.vision.pose.defaults.PoseDetectorOptions
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/**
 * Thin layer over ML Kit pose detection, checked on devices (plan T-017, section 4): it copies what ML Kit returns into
 * [RawLandmark]s and hands them to [toBodyPose]. Frames are dropped at once while not [active]. The detector is made
 * on the first activation and closed when the camera is released.
 */
@Singleton
internal class MlKitPoseAnalyzer @Inject constructor(private val logger: AppLogger) :
    PoseFrameSource,
    ImageAnalysis.Analyzer {
    // No replay and no merging of equal frames; a slow collector loses the oldest frame, never blocks the analysis.
    private val shared = MutableSharedFlow<BodyPose?>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    override val frames: SharedFlow<BodyPose?> = shared

    @Volatile
    private var active = false

    /** Set by the camera controller when it opens the camera. */
    @Volatile
    var lens: Lens = Lens.BACK

    private var detector: PoseDetector? = null

    @Volatile
    private var framesInWindow = 0
    private var windowStart = 0L
    private var lastFailureLog = 0L

    /** Whether detection runs now: someone wants it and the detector is open. For the tests. */
    internal val running: Boolean get() = synchronized(this) { active && detector != null }

    @Synchronized
    override fun setActive(owner: Any, active: Boolean) {
        this.active = active
        if (active && detector == null) {
            detector = PoseDetection.getClient(
                PoseDetectorOptions.Builder().setDetectorMode(PoseDetectorOptions.STREAM_MODE).build(),
            )
        }
    }

    @OptIn(ExperimentalGetImage::class)
    override fun analyze(image: ImageProxy) {
        val detector = synchronized(this) { detector }
        val media = image.image
        if (!active || detector == null || media == null) {
            image.close()
            return
        }
        val rotation = image.imageInfo.rotationDegrees
        val width = image.width
        val height = image.height
        val mirrored = lens == Lens.FRONT
        detector.process(InputImage.fromMediaImage(media, rotation))
            .addOnSuccessListener { pose ->
                val points = pose.allPoseLandmarks.map {
                    RawLandmark(it.landmarkType, it.position.x, it.position.y, it.inFrameLikelihood)
                }
                shared.tryEmit(toBodyPose(points, width, height, rotation, mirrored))
                countFrame()
            }
            .addOnFailureListener { error ->
                val now = SystemClock.elapsedRealtime()
                if (now - lastFailureLog >= FAILURE_LOG_MILLIS) {
                    lastFailureLog = now
                    logger.warn(LOG_TAG, "Pose detection failed", error)
                }
                shared.tryEmit(null)
            }
            .addOnCompleteListener { image.close() }
    }

    /** Logs the frames analysed per second every five seconds, for the measure on a device. */
    private fun countFrame() {
        val now = SystemClock.elapsedRealtime()
        if (windowStart == 0L) windowStart = now
        framesInWindow++
        val elapsed = now - windowStart
        if (elapsed >= FPS_WINDOW_MILLIS) {
            logger.debug(LOG_TAG, "Pose analysis: ${framesInWindow * MILLIS_PER_SECOND / elapsed} fps")
            framesInWindow = 0
            windowStart = now
        }
    }

    /** Called when the camera closes: tells the collectors the frames stopped. */
    fun reset() {
        shared.tryEmit(null)
        framesInWindow = 0
        windowStart = 0L
    }

    override fun release(owner: Any) = release()

    /** Called when the camera is released for good. */
    @Synchronized
    fun release() {
        active = false
        detector?.close()
        detector = null
    }

    private companion object {
        const val LOG_TAG = "Camera"
        const val FPS_WINDOW_MILLIS = 5_000L
        const val FAILURE_LOG_MILLIS = 10_000L
        const val MILLIS_PER_SECOND = 1_000L
    }
}
