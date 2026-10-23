plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.feature.camera"
}

dependencies {
    implementation(project(":core:data"))
    // The countdown stops while the network dialog shows (plan T-013, option A).
    implementation(project(":core:network"))
    // Pose catalog, images and history (plan T-009, question 7; T-014).
    implementation(project(":core:poses"))
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
    // CameraX returns Guava ListenableFuture; Guava is on the runtime classpath only, through Firebase (plan T-031, L-007).
    implementation(libs.guava)
    // Pose detection on the device, for the pose matching (plan T-017; the library is chosen in spec question 9).
    implementation(libs.mlkit.pose.detection)
}
