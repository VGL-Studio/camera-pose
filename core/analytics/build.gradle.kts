plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.hilt)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.analytics"
}

dependencies {
    implementation(project(":core:config"))
    implementation(project(":core:logging"))

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.analytics)
    implementation(libs.appsflyer)
}
