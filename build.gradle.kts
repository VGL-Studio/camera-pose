// Root build. Plugins are declared here with `apply false` so that build-logic
// (which only has them as compileOnly) shares the same classloader.
plugins {
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.ksp) apply false
    alias(libs.plugins.hilt) apply false
    alias(libs.plugins.roborazzi) apply false
    alias(libs.plugins.ktlint) apply false
    alias(libs.plugins.google.services) apply false
    alias(libs.plugins.firebase.crashlytics) apply false
}

/**
 * Module dependency rule (ADR-0001), checked for every module except :app:
 * - no module depends on :app or on a :feature:* module (other than itself);
 * - a :core:* module depends on other :core:* modules only.
 * Run by scripts/check.sh and CI.
 */
tasks.register("checkModuleDependencies") {
    group = "verification"
    description = "Fails if a module breaks the module dependency rule (ADR-0001)."
    val violations = provider {
        subprojects
            .filter { it.path != ":app" }
            .flatMap { module ->
                val isCore = module.path.startsWith(":core:")
                module.configurations
                    .flatMap { configuration ->
                        configuration.dependencies
                            .withType(ProjectDependency::class.java)
                            .map { it.path }
                    }
                    .filter { it != module.path }
                    .filter { dep ->
                        dep == ":app" ||
                            dep.startsWith(":feature:") ||
                            (isCore && !dep.startsWith(":core:"))
                    }
                    .distinct()
                    .map { "${module.path} -> $it" }
            }
    }
    doLast {
        val found = violations.get()
        if (found.isNotEmpty()) {
            throw GradleException(
                "Module dependency rule broken (ADR-0001). Only :app may depend on features, " +
                    "features depend only on core, core depends only on core:\n" +
                    found.joinToString("\n") { "  $it" },
            )
        }
        logger.lifecycle("Module dependency rule OK.")
    }
}
