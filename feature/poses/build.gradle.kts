plugins {
    alias(libs.plugins.template.android.feature)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.feature.poses"
}

dependencies {
    // Pose catalog, thumbnails and saved poses (plan T-009; plan T-021).
    implementation(project(":core:poses"))
    // A catalog that failed for lack of network waits under the network dialog (spec poses, KB-5).
    implementation(project(":core:network"))
}
