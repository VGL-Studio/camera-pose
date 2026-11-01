import com.android.build.api.dsl.CommonExtension
import org.gradle.api.JavaVersion
import org.gradle.api.Project
import org.gradle.api.tasks.testing.Test
import org.gradle.kotlin.dsl.configure
import org.gradle.kotlin.dsl.dependencies
import org.gradle.kotlin.dsl.withType
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinAndroidProjectExtension

/** Settings shared by every Android module (application and library). */
internal fun Project.configureKotlinAndroid(commonExtension: CommonExtension) {
    commonExtension.apply {
        compileSdk = libs.versionInt("compileSdk")

        defaultConfig.apply {
            minSdk = libs.versionInt("minSdk")
            testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }

        compileOptions.apply {
            sourceCompatibility = JavaVersion.VERSION_17
            targetCompatibility = JavaVersion.VERSION_17
        }

        testOptions.apply {
            unitTests.isIncludeAndroidResources = true
            animationsDisabled = true
        }

        packaging.apply {
            resources.excludes += "/META-INF/{AL2.0,LGPL2.1}"
        }
    }

    extensions.configure<KotlinAndroidProjectExtension> {
        compilerOptions {
            jvmTarget.set(JvmTarget.JVM_17)
        }
    }

    // Gradle 9 fails a test task that finds no test, and Hilt gives a module without src/test generated test
    // classes. Only such a module may have no test; elsewhere an empty run still fails.
    if (!file("src/test").exists()) {
        tasks.withType<Test>().configureEach {
            failOnNoDiscoveredTests.set(false)
        }
    }

    dependencies {
        add("implementation", libs.lib("kotlinx-coroutines-android"))

        add("testImplementation", libs.lib("junit4"))
        add("testImplementation", libs.lib("kotlinx-coroutines-test"))
        add("testImplementation", libs.lib("turbine"))
        add("testImplementation", libs.lib("mockk"))
    }
}
