package com.vglstudio.camera.pose.line.guide

import java.io.File
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Only the staging source set gives remote keys local values (ADR-0023): production builds read Firebase,
 * then the key's default. CI runs unit tests of the staging variant only, so this scans the sources of :app.
 * The working directory of a unit test is the module directory.
 */
class LocalRemoteValuesSourceSetTest {
    private val sources = File("src")

    // Staging may declare local values; test sources mention the qualifier to check it.
    private val excludedSourceSets = setOf("staging", "test", "testStaging")

    private fun File.kotlinFilesMentioningQualifier(): List<File> = walkTopDown()
        .filter { it.isFile && it.extension == "kt" && it.readText().contains(QUALIFIER) }
        .toList()

    @Test
    fun onlyStagingSourceSet_declaresLocalRemoteValues() {
        // Control: the staging source set does mention it, so the path is right and the scan reads files.
        assertTrue(
            "src/staging has no Kotlin file mentioning $QUALIFIER; is the path right?",
            File(sources, "staging").kotlinFilesMentioningQualifier().isNotEmpty(),
        )
        val offenders = sources.listFiles().orEmpty()
            .filter { it.isDirectory && it.name !in excludedSourceSets }
            .flatMap { it.kotlinFilesMentioningQualifier() }
        assertTrue("Only src/staging may declare local remote values, found: $offenders", offenders.isEmpty())
    }

    private companion object {
        const val QUALIFIER = "LocalRemoteValues"
    }
}
