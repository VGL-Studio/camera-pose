import io.github.takahirom.roborazzi.RoborazziExtension

plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.ads"
}

// The native ad components are checked by screenshot tests, like feature screens (ADR-0020).
extensions.configure<RoborazziExtension> {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

dependencies {
    implementation(project(":core:analytics"))
    implementation(project(":core:config"))
    implementation(project(":core:data"))
    implementation(project(":core:designsystem"))
    implementation(project(":core:logging"))
    implementation(project(":core:network"))

    implementation(libs.play.services.ads)
    implementation(libs.admob.mediation.applovin)
    implementation(libs.admob.mediation.meta)
    implementation(libs.admob.mediation.dtexchange)
    implementation(libs.admob.mediation.inmobi)
    implementation(libs.admob.mediation.ironsource)
    implementation(libs.admob.mediation.mintegral)
    implementation(libs.admob.mediation.moloco)
    implementation(libs.admob.mediation.pangle)
    implementation(libs.admob.mediation.unity)
    implementation(libs.unity.ads)
    implementation(libs.applovin.sdk)
    implementation(libs.max.mediation.google)
    implementation(libs.user.messaging.platform)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.runtime.compose)

    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
