plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.feature.settings"
}

dependencies {
    // The choices of the screen are read by :app and the camera too (plan T-025, question 1).
    implementation(project(":core:data"))
}
