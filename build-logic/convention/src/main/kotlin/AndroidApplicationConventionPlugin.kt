import com.android.build.api.dsl.ApplicationExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.configure

/**
 * Convention for the :app module.
 * Two build flavors map to the two environments of ADR-0007: `staging` and `production`.
 */
class AndroidApplicationConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) {
        with(target) {
            pluginManager.apply("com.android.application")
            pluginManager.apply("org.jlleitschuh.gradle.ktlint")

            extensions.configure<ApplicationExtension> {
                configureKotlinAndroid(this)

                defaultConfig {
                    targetSdk = libs.versionInt("targetSdk")
                }

                flavorDimensions += "environment"
                productFlavors {
                    create("staging") {
                        dimension = "environment"
                        isDefault = true
                        applicationIdSuffix = ".staging"
                        versionNameSuffix = "-staging"
                    }
                    create("production") {
                        dimension = "environment"
                    }
                }

                buildTypes {
                    getByName("release") {
                        isMinifyEnabled = true
                        isShrinkResources = true
                        proguardFiles(
                            getDefaultProguardFile("proguard-android-optimize.txt"),
                            "proguard-rules.pro",
                        )
                    }
                }

                buildFeatures {
                    buildConfig = true
                }

                lint {
                    // Lint the whole app, including every module it depends on.
                    checkDependencies = true
                    abortOnError = true
                    warningsAsErrors = false
                    // Library versions are managed in gradle/libs.versions.toml and updated deliberately.
                    disable += setOf("GradleDependency", "AndroidGradlePluginVersion", "NewerVersionAvailable")
                }
            }
        }
    }
}
