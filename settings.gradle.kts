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

include(":feature:sample")
include(":feature:loading")
include(":feature:camera")
include(":feature:onboarding")
include(":feature:poses")
include(":feature:settings")
