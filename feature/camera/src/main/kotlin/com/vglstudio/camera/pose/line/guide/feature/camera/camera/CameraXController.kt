package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import android.content.Context
import android.media.MediaActionSound
import android.util.Size
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.UseCase
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.core.resolutionselector.ResolutionStrategy
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.CaptureClock
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MlKitPoseAnalyzer
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The camera through CameraX: a 3:4 preview and photo capture. Holds its own lifecycle, so the view model never holds
 * the lifecycle of the screen; the screen opens and closes it through the view model. With the pose matching flag on
 * it binds a third use case, an analysis of small frames for ML Kit (plan T-017). It unbinds only the use cases it bound
 * itself, never the whole process-wide provider: two camera screens can live at once during a screen change, and the
 * stale one must not unbind the camera of the new one (FB-20261026-01, plan T-058). A thin layer over the hardware,
 * checked on devices (plan T-013, section 4); the decisions live in CameraViewModel.
 */
@ViewModelScoped
internal class CameraXController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStore: CaptureFileStore,
    private val clock: CaptureClock,
    private val logger: AppLogger,
    private val poseAnalyzer: MlKitPoseAnalyzer,
    private val featureFlags: FeatureFlags,
) : CameraController,
    LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val requests = MutableStateFlow<SurfaceRequest?>(null)
    override val surfaceRequest: StateFlow<SurfaceRequest?> = requests.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var boundUseCases: List<UseCase> = emptyList()
    private var analysisExecutor: ExecutorService? = null
    private var shutterSound: MediaActionSound? = null

    // The photo rotation follows how the device is held, from the sensor's angle to the device's natural orientation.
    // Right for every orientation of the screen: portrait on a phone, any on a large screen (plan T-056).
    private val orientationListener = object : OrientationEventListener(context) {
        override fun onOrientationChanged(orientation: Int) {
            if (orientation == ORIENTATION_UNKNOWN) return
            imageCapture?.targetRotation = when (orientation) {
                in 45 until 135 -> Surface.ROTATION_270
                in 135 until 225 -> Surface.ROTATION_180
                in 225 until 315 -> Surface.ROTATION_90
                else -> Surface.ROTATION_0
            }
        }
    }

    override suspend fun open(lens: Lens, flash: FlashMode): CameraCapabilities = withContext(Dispatchers.Main) {
        try {
            val provider = ProcessCameraProvider.awaitInstance(context).also { provider = it }
            val resolution = ResolutionSelector.Builder()
                .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
                .build()
            val preview = Preview.Builder().setResolutionSelector(resolution).build()
            preview.setSurfaceProvider { request -> requests.value = request }
            val capture = ImageCapture.Builder()
                .setResolutionSelector(resolution)
                .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                .setFlashMode(flash.toCameraX())
                .build()
            val selector =
                if (lens == Lens.BACK) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA
            val analysis = if (featureFlags.enabled(FeatureFlag.POSE_MATCHING).first()) buildAnalysis() else null
            poseAnalyzer.lens = lens
            unbindOwn(provider)
            registry.currentState = Lifecycle.State.CREATED
            val bound = bindUseCases(provider, selector, preview, capture, analysis)
            registry.currentState = Lifecycle.State.RESUMED
            camera = bound
            imageCapture = capture
            orientationListener.enable()
            val zoom = bound.cameraInfo.zoomState.value
            CameraCapabilities(
                hasFlash = bound.cameraInfo.hasFlashUnit(),
                minZoomRatio = zoom?.minZoomRatio ?: 1f,
                maxZoomRatio = zoom?.maxZoomRatio ?: 1f,
            )
        } catch (e: CancellationException) {
            // Only a cancelled caller is a cancellation; a future CameraX cancelled on its own is a failed open.
            if (!currentCoroutineContext().isActive) throw e
            throw CameraOpenException(e)
        } catch (e: Exception) {
            throw CameraOpenException(e)
        }
    }

    /** Small frames, only the latest one kept while ML Kit is busy: the preview stays smooth on a modest phone. */
    private fun buildAnalysis(): ImageAnalysis {
        val selector = ResolutionSelector.Builder()
            .setAspectRatioStrategy(AspectRatioStrategy.RATIO_4_3_FALLBACK_AUTO_STRATEGY)
            .setResolutionStrategy(
                ResolutionStrategy(
                    Size(ANALYSIS_WIDTH, ANALYSIS_HEIGHT),
                    ResolutionStrategy.FALLBACK_RULE_CLOSEST_LOWER_THEN_HIGHER,
                ),
            )
            .build()
        val executor = analysisExecutor ?: Executors.newSingleThreadExecutor().also { analysisExecutor = it }
        return ImageAnalysis.Builder()
            .setResolutionSelector(selector)
            .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
            .build()
            .also { it.setAnalyzer(executor, poseAnalyzer) }
    }

    /** Some phones cannot run three use cases at once: then the camera works as before, without the matching. */
    private fun bindUseCases(
        provider: ProcessCameraProvider,
        selector: CameraSelector,
        preview: Preview,
        capture: ImageCapture,
        analysis: ImageAnalysis?,
    ): Camera {
        if (analysis != null) {
            try {
                return provider.bindToLifecycle(this, selector, preview, capture, analysis)
                    .also { boundUseCases = listOf(preview, capture, analysis) }
            } catch (e: IllegalArgumentException) {
                logger.warn(LOG_TAG, "Pose analysis not supported on this camera", e)
                provider.unbind(preview, capture, analysis)
            }
        }
        return provider.bindToLifecycle(this, selector, preview, capture)
            .also { boundUseCases = listOf(preview, capture) }
    }

    private fun unbindOwn(provider: ProcessCameraProvider?) {
        if (boundUseCases.isNotEmpty()) provider?.unbind(*boundUseCases.toTypedArray())
        boundUseCases = emptyList()
    }

    override suspend fun availableLenses(): Set<Lens> = withContext(Dispatchers.Main) {
        try {
            val provider = ProcessCameraProvider.awaitInstance(context).also { provider = it }
            lensesOf(onError = { logger.warn(LOG_TAG, "Camera list unavailable, the open decides", it) }) { lens ->
                provider.hasCamera(
                    if (lens == Lens.BACK) CameraSelector.DEFAULT_BACK_CAMERA else CameraSelector.DEFAULT_FRONT_CAMERA,
                )
            }
        } catch (e: CancellationException) {
            if (!currentCoroutineContext().isActive) throw e
            emptySet()
        } catch (e: Exception) {
            // Unknown: the open says whether the lens is there.
            logger.warn(LOG_TAG, "Camera provider unavailable, the open decides", e)
            emptySet()
        }
    }

    override fun setFlash(mode: FlashMode) {
        imageCapture?.flashMode = mode.toCameraX()
    }

    override fun setZoom(ratio: Float) {
        val control = camera?.cameraControl ?: return
        applyZoom(control, ratio, logger)
    }

    override suspend fun takePicture(playShutterSound: Boolean): File = withContext(Dispatchers.Main) {
        val capture = imageCapture ?: throw CaptureFailedException(IllegalStateException("The camera is not open"))
        val file = fileStore.newFile(clock.nowMillis())
        // Loaded only when the user keeps the shutter sound on (spec settings, KB-3).
        val sound = if (!playShutterSound) {
            null
        } else {
            shutterSound ?: MediaActionSound().also {
                it.load(MediaActionSound.SHUTTER_CLICK)
                shutterSound = it
            }
        }
        suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onCaptureStarted() {
                        sound?.play(MediaActionSound.SHUTTER_CLICK)
                    }

                    override fun onImageSaved(output: ImageCapture.OutputFileResults) {
                        continuation.resume(file)
                    }

                    override fun onError(exception: ImageCaptureException) {
                        continuation.resumeWithException(CaptureFailedException(exception))
                    }
                },
            )
        }
    }

    override fun close() {
        // Already closed (the screen stopped before it was destroyed): nothing to unbind, no second null frame.
        if (boundUseCases.isEmpty() && camera == null) return
        orientationListener.disable()
        unbindOwn(provider)
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.CREATED
        camera = null
        imageCapture = null
        requests.value = null
        poseAnalyzer.reset()
        logger.debug(LOG_TAG, "Camera unbound")
    }

    override fun release() {
        close()
        // CameraX keeps a camera per lifecycle owner until it is destroyed. A camera never opened (for example while
        // the permission prompt shows) is still INITIALIZED, which cannot move to DESTROYED and holds nothing.
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
        shutterSound?.release()
        shutterSound = null
        analysisExecutor?.shutdown()
        analysisExecutor = null
    }

    private fun FlashMode.toCameraX(): Int = when (this) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private companion object {
        const val LOG_TAG = "Camera"
        const val ANALYSIS_WIDTH = 640
        const val ANALYSIS_HEIGHT = 480
    }
}
