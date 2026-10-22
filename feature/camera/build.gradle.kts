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
    implementation(libs.androidx.camera.core)
    implementation(libs.androidx.camera.camera2)
    implementation(libs.androidx.camera.lifecycle)
    implementation(libs.androidx.camera.compose)
}
