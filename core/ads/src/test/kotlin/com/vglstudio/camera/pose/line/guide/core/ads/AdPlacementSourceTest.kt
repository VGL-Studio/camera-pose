package com.vglstudio.camera.pose.line.guide.core.ads

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * scripts/release.sh asks for a production ad unit for every placement it reads from AdPlacement.kt (plan T-019,
 * question 1). A placement line it cannot read would ship without an ad unit and silently show no ad. This guard
 * fails instead. The script reads one-line declarations, with or without attributes after the format.
 */
class AdPlacementSourceTest {
    // Copied from placement_keys() of scripts/release.sh (sed -nE, POSIX class [[:space:]] written as \s).
    private val releaseScriptLine = Regex("""^\s*[A-Z0-9_]+\("([a-z0-9_]+)", AdFormat\.[A-Z_]+(, .*)?\),?$""")

    private fun readByReleaseScript(lines: List<String>): List<String> = lines
        .mapNotNull { releaseScriptLine.matchEntire(it)?.groupValues?.get(1) }
        .filterNot { it.startsWith("gallery_") }

    @Test
    fun releaseScript_readsEveryShippedPlacement() {
        val source = File("src/main/kotlin/com/vglstudio/camera/pose/line/guide/core/ads/AdPlacement.kt")
        assertTrue("${source.absolutePath} not found", source.isFile)
        val read = readByReleaseScript(source.readLines())
        // Control: the script reads something from the real file.
        assertTrue("release.sh reads no placement", read.isNotEmpty())
        assertEquals(AdPlacement.entries.filterNot { it.stagingOnly }.map { it.key }.toSet(), read.toSet())
    }

    @Test
    fun releaseScript_readsLinesWithAttributes() {
        val lines = listOf(
            "    A_B(\"a_b\", AdFormat.NATIVE_SMALL),",
            "    X_Y(\"x_y\", AdFormat.INTERSTITIAL, intervalGroup = AdIntervalGroup.named(\"sounds\", 30)),",
            "    L_N(\"l_n\", AdFormat.NATIVE_MEDIUM, nativeEvery = 8),",
            "    GALLERY_X(\"gallery_x\", AdFormat.NATIVE_SMALL, nativeEvery = 8),",
        )
        assertEquals(listOf("a_b", "x_y", "l_n"), readByReleaseScript(lines))
    }
}
