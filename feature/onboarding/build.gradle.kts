plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.feature.onboarding"
}

dependencies {
    // LocalActivity: no longer on the compile classpath through other libraries since AGP 9 (template sync, ADR-0040).
    implementation(libs.androidx.activity.compose)
}
