import io.github.takahirom.roborazzi.RoborazziExtension

plugins {
    alias(libs.plugins.template.android.library)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.roborazzi)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide.core.designsystem"
}

// The design system catalog is checked by screenshot tests, like feature screens (ADR-0020).
extensions.configure<RoborazziExtension> {
    outputDir.set(layout.projectDirectory.dir("src/test/screenshots"))
}

// DesignTokensTest rewrites docs/design-system/tokens.json when run with -PdesignTokens.update=true.
tasks.withType<Test>().configureEach {
    systemProperty("designTokens.update", providers.gradleProperty("designTokens.update").getOrElse("false"))
    systemProperty("designTokens.file", rootProject.file("docs/design-system/tokens.json").absolutePath)
}

dependencies {
    testImplementation(libs.roborazzi)
    testImplementation(libs.roborazzi.compose)
    testImplementation(libs.roborazzi.junit.rule)
}
