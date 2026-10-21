plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.feature.sample"
}

dependencies {
    implementation(project(":core:data"))
}
