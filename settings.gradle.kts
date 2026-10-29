pluginManagement {
    includeBuild("build-logic")
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        // Mediation SDKs hosted outside Google Maven and Maven Central (plan T-034).
        // Each repository serves only its group.
        maven("https://artifact.bytedance.com/repository/pangle/") {
            content { includeGroup("com.pangle.global") }
        }
        maven("https://dl-maven-android.mintegral.com/repository/mbridge_android_sdk_oversea") {
            content { includeGroupByRegex("com\\.mbridge.*") }
        }
    }
}

rootProject.name = "camera-pose"

include(":app")

include(":core:designsystem")
include(":core:data")
include(":core:config")
include(":core:analytics")
include(":core:ads")
include(":core:assets")
include(":core:logging")
include(":core:network")
include(":core:poses")
include(":core:review")

include(":feature:sample")
include(":feature:loading")
include(":feature:camera")
include(":feature:onboarding")
include(":feature:poses")
include(":feature:settings")
