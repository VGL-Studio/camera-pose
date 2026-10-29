package com.vglstudio.camera.pose.line.guide

import com.vglstudio.camera.pose.line.guide.di.AnalyticsConfigModule
import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * No AppsFlyer library in the build and no AppsFlyer key in the app (spec ads, question 3; plan T-074, section 4).
 * Reads the build files by symbol; the working directory of a unit test is the module directory.
 */
class NoAppsFlyerDependencyTest {
    private val root = File("..")

    @Test
    fun noBuildFile_namesAppsFlyer() {
        val catalog = File(root, "gradle/libs.versions.toml")
        assertTrue("Missing ${catalog.absolutePath}", catalog.isFile)
        val catalogText = catalog.readText()
        // Control: the catalog read is the real one, with the ads SDK of the app.
        assertTrue("play-services-ads" in catalogText)
        assertFalse("com.appsflyer in the version catalog", "group = \"com.appsflyer\"" in catalogText)
        val buildFiles = root.walkTopDown()
            .onEnter { it.name !in SKIPPED_DIRS }
            .filter { it.isFile && it.name == "build.gradle.kts" }
            .toList()
        // Control: the build files of the modules are found, :core:analytics among them.
        assertTrue(buildFiles.any { it.invariantSeparatorsPath.endsWith("core/analytics/build.gradle.kts") })
        val naming = buildFiles.filter { "libs.appsflyer" in it.readText() }
        assertEquals(emptyList<String>(), naming.map { it.invariantSeparatorsPath.substringAfter("/../") })
    }

    @Test
    fun appsFlyerConfig_isEmpty() {
        val config = AnalyticsConfigModule.appsFlyerConfig()
        assertFalse("AppsFlyer must have no key in this app: $config", config.enabled)
        assertEquals("", config.devKey)
    }

    private companion object {
        val SKIPPED_DIRS = setOf("build", ".gradle", ".git", ".idea", "node_modules")
    }
}
