plugins {
    alias(libs.plugins.template.android.application)
    alias(libs.plugins.template.android.compose)
    alias(libs.plugins.template.hilt)
    // Firebase config: app/src/staging/google-services.json is a fake for builds without secrets;
    // a real file in src/staging/debug, src/staging/release or src/production takes precedence (ADR-0012).
    alias(libs.plugins.google.services)
    alias(libs.plugins.firebase.crashlytics)
}

android {
    namespace = "com.vglstudio.camera.pose.line.guide"

    defaultConfig {
        // Replaced by the app's real id when a new app is created from the template.
        applicationId = "com.vglstudio.camera.pose.line.guide"
        versionCode = 1
        versionName = "0.1.0"
    }

    // Staging builds sent to the team are signed with a fixed staging-only key, so each build installs over
    // the previous one (ADR-0016). staging.yml provides it; without it, stagingRelease stays unsigned.
    // The debug build type keeps the debug key.
    providers.gradleProperty("stagingKeystoreFile").orNull?.let { keystore ->
        signingConfigs.create("staging") {
            storeFile = file(keystore)
            storePassword = providers.gradleProperty("stagingKeystorePassword").get()
            keyAlias = providers.gradleProperty("stagingKeyAlias").get()
            keyPassword = storePassword
        }
    }

    // Production releases are signed with the upload key; Play App Signing re-signs them for users (ADR-0007).
    // Only scripts/release.sh provides it, from the release folder kept outside the repo (ADR-0017).
    providers.gradleProperty("uploadKeystoreFile").orNull?.let { keystore ->
        signingConfigs.create("upload") {
            storeFile = file(keystore)
            storePassword = providers.gradleProperty("uploadKeystorePassword").get()
            keyAlias = providers.gradleProperty("uploadKeyAlias").get()
            keyPassword = providers.gradleProperty("uploadKeyPassword").getOrElse(storePassword!!)
        }
    }

    // Configuration per environment (ADR-0007). Staging uses Google's public test app id and the staging folder
    // on R2; production values are given by the person building the release, never stored in the repo (ADR-0009):
    //   -PadmobAppId=<AdMob app id> -PadUnits="<placement key>=<ad unit>;<placement key>=<ad unit>"
    //   -PassetsBaseUrl=<https address of the production files on R2>
    productFlavors {
        named("staging") {
            manifestPlaceholders["admobAppId"] = "ca-app-pub-3940256099942544~3347511713"
            buildConfigField("String", "ASSETS_BASE_URL", "\"https://camera-pose.vgl.io.vn/\"")
            // Builds sent to the team by staging.yml carry the run number, so testers can tell them apart.
            providers.gradleProperty("stagingBuildNumber").orNull?.toInt()?.let { buildNumber ->
                versionCode = buildNumber
                versionNameSuffix = "-staging.$buildNumber"
            }
            signingConfigs.findByName("staging")?.let { signingConfig = it }
        }
        named("production") {
            manifestPlaceholders["admobAppId"] = providers.gradleProperty("admobAppId").getOrElse("")
            buildConfigField("String", "AD_UNITS", "\"${providers.gradleProperty("adUnits").getOrElse("")}\"")
            buildConfigField(
                "String",
                "ASSETS_BASE_URL",
                "\"${providers.gradleProperty("assetsBaseUrl").getOrElse("")}\"",
            )
            signingConfigs.findByName("upload")?.let { signingConfig = it }
        }
    }
}

// Building production without the real configuration fails at once, rather than shipping an app without ads
// or files. Checked when a production variant is built, not at configuration, so other builds and IDE sync work
// without it.
val missingProductionProperties = mapOf(
    "admobAppId" to "ADR-0013",
    "adUnits" to "ADR-0013",
    "assetsBaseUrl" to "ADR-0015",
).filterKeys { !providers.gradleProperty(it).isPresent }
tasks.matching { it.name.startsWith("preProduction") && it.name.endsWith("Build") }.configureEach {
    doFirst {
        if (missingProductionProperties.isNotEmpty()) {
            throw GradleException(
                "Build production cần tham số thật: " +
                    missingProductionProperties.entries.joinToString { "-P${it.key} (${it.value})" } + ".",
            )
        }
    }
}

// The google-services task picks its config file when it runs, and the build cache can return the output made
// from another file (seen: the fake config came back from cache after the real file was added). Never cache it.
tasks.matching { it.name.endsWith("GoogleServices") }.configureEach {
    outputs.doNotCacheIf("google-services.json is chosen at execution time") { true }
}

// RemoteConfigKeysTest rewrites the two generated key files when run with -PremoteConfigKeys.update=true (ADR-0023).
// The files are inputs, so editing one by hand runs the test again instead of leaving it UP-TO-DATE.
val remoteConfigKeysMarkdown = rootProject.file("docs/remote-config-keys.md")
val remoteConfigKeysTemplate = rootProject.file("docs/remote-config-template.json")
tasks.withType<Test>().configureEach {
    systemProperty("remoteConfigKeys.update", providers.gradleProperty("remoteConfigKeys.update").getOrElse("false"))
    systemProperty("remoteConfigKeys.markdown", remoteConfigKeysMarkdown.absolutePath)
    systemProperty("remoteConfigKeys.template", remoteConfigKeysTemplate.absolutePath)
    inputs.files(remoteConfigKeysMarkdown, remoteConfigKeysTemplate).withPropertyName("remoteConfigKeysDocs")
}

dependencies {
    implementation(project(":core:designsystem"))
    implementation(project(":core:data"))
    implementation(project(":core:config"))
    implementation(project(":core:analytics"))
    implementation(project(":core:ads"))
    implementation(project(":core:assets"))
    implementation(project(":core:logging"))
    implementation(project(":core:poses"))
    implementation(project(":core:review"))
    implementation(project(":core:network"))

    implementation(project(":feature:sample"))
    implementation(project(":feature:loading"))
    implementation(project(":feature:camera"))
    implementation(project(":feature:onboarding"))
    implementation(project(":feature:poses"))
    implementation(project(":feature:settings"))

    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.navigation.compose)

    implementation(platform(libs.firebase.bom))
    implementation(libs.firebase.crashlytics)

    androidTestImplementation(libs.androidx.compose.ui.test.junit4)
    androidTestImplementation(libs.androidx.test.ext.junit)
    androidTestImplementation(libs.androidx.test.runner)
}
