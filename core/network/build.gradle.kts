plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.network"
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:logging"))
}
