package com.vglstudio.camera.pose.line.guide.feature.camera.camera

import android.content.Context
import android.media.MediaActionSound
import android.view.OrientationEventListener
import android.view.Surface
import androidx.camera.core.Camera
import androidx.camera.core.CameraControl
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.core.SurfaceRequest
import androidx.camera.core.resolutionselector.AspectRatioStrategy
import androidx.camera.core.resolutionselector.ResolutionSelector
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.lifecycle.awaitInstance
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.CaptureClock
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import java.io.File
import java.lang.reflect.Method
import javax.inject.Inject
import kotlin.coroutines.cancellation.CancellationException
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext

/**
 * The camera through CameraX: a 3:4 preview and photo capture. Holds its own lifecycle, so the view model never holds
 * the lifecycle of the screen; the screen opens and closes it through the view model. A thin layer over the hardware,
 * checked on devices (plan T-013, section 4); the decisions live in CameraViewModel.
 */
@ViewModelScoped
internal class CameraXController @Inject constructor(
    @ApplicationContext private val context: Context,
    private val fileStore: CaptureFileStore,
    private val clock: CaptureClock,
    private val logger: AppLogger,
) : CameraController,
    LifecycleOwner {
    private val registry = LifecycleRegistry(this)
    override val lifecycle: Lifecycle get() = registry

    private val requests = MutableStateFlow<SurfaceRequest?>(null)
    override val surfaceRequest: StateFlow<SurfaceRequest?> = requests.asStateFlow()

    private var provider: ProcessCameraProvider? = null
    private var camera: Camera? = null
    private var imageCapture: ImageCapture? = null
    private var shutterSound: MediaActionSound? = null

    // The app is locked to portrait, so the photo rotation follows how the phone is held.
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
            provider.unbindAll()
            registry.currentState = Lifecycle.State.CREATED
            val bound = provider.bindToLifecycle(this@CameraXController, selector, preview, capture)
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

    override fun setFlash(mode: FlashMode) {
        imageCapture?.flashMode = mode.toCameraX()
    }

    override fun setZoom(ratio: Float) {
        val control = camera?.cameraControl ?: return
        // setZoomRatio returns a Guava ListenableFuture, which is on the runtime classpath of CameraX but not on the
        // compile classpath, and the plan adds no library (plan T-013, section 7, condition 4). The result is not
        // needed, so the call goes through reflection; consumer-rules.pro keeps the method in minified builds. If the
        // lookup still fails, only zoom stops working.
        runCatching { setZoomRatio.invoke(control, ratio) }
            .onFailure { logger.warn(LOG_TAG, "Zoom could not be set", it) }
    }

    override suspend fun takePicture(playShutterSound: Boolean): File = withContext(Dispatchers.Main) {
        val capture = imageCapture ?: throw CaptureFailedException(IllegalStateException("The camera is not open"))
        val file = fileStore.newFile(clock.nowMillis())
        val sound = shutterSound ?: MediaActionSound().also {
            it.load(MediaActionSound.SHUTTER_CLICK)
            shutterSound = it
        }
        suspendCancellableCoroutine { continuation ->
            capture.takePicture(
                ImageCapture.OutputFileOptions.Builder(file).build(),
                ContextCompat.getMainExecutor(context),
                object : ImageCapture.OnImageSavedCallback {
                    override fun onCaptureStarted() {
                        sound.play(MediaActionSound.SHUTTER_CLICK)
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
        orientationListener.disable()
        provider?.unbindAll()
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.CREATED
        camera = null
        imageCapture = null
        requests.value = null
        logger.debug(LOG_TAG, "Camera unbound")
    }

    override fun release() {
        close()
        // CameraX keeps a camera per lifecycle owner until it is destroyed. A camera never opened (for example while
        // the permission prompt shows) is still INITIALIZED, which cannot move to DESTROYED and holds nothing.
        if (registry.currentState.isAtLeast(Lifecycle.State.CREATED)) registry.currentState = Lifecycle.State.DESTROYED
        shutterSound?.release()
        shutterSound = null
    }

    private fun FlashMode.toCameraX(): Int = when (this) {
        FlashMode.OFF -> ImageCapture.FLASH_MODE_OFF
        FlashMode.ON -> ImageCapture.FLASH_MODE_ON
        FlashMode.AUTO -> ImageCapture.FLASH_MODE_AUTO
    }

    private companion object {
        const val LOG_TAG = "Camera"
        val setZoomRatio: Method by lazy {
            CameraControl::class.java.getMethod("setZoomRatio", Float::class.javaPrimitiveType)
        }
    }
}
