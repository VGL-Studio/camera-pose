plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.assets"
}

dependencies {
    implementation(project(":core:logging"))
    implementation(project(":core:network"))
}
