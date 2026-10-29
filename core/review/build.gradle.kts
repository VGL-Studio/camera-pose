plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.review"
}

dependencies {
    implementation(project(":core:ads"))
    implementation(project(":core:config"))
    implementation(project(":core:data"))
    implementation(project(":core:logging"))

    implementation(libs.play.review.ktx)
    implementation(libs.androidx.activity.compose)
}
