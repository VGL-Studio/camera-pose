# CameraXController calls CameraControl.setZoomRatio through reflection (plan T-013): keep it in minified builds.
-keep interface androidx.camera.core.CameraControl {
    *** setZoomRatio(float);
}
