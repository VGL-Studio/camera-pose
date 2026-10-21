plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.ads"
}

dependencies {
    implementation(project(":core:config"))

    implementation(libs.play.services.ads)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)
}
