plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
    alias(libs.plugins.kotlin.serialization)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.poses"
}

dependencies {
    // Pose paths are AssetPath values, so features that read a Pose need the type on their compile classpath.
    api(project(":core:assets"))
    implementation(project(":core:config"))
    implementation(project(":core:data"))
    implementation(project(":core:logging"))

    implementation(libs.kotlinx.serialization.json)
}
