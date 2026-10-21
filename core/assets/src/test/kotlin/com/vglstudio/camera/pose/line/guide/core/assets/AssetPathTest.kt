package com.vglstudio.camera.pose.line.guide.core.assets

import org.junit.Assert.assertEquals
import org.junit.Test

class AssetPathTest {
    @Test
    fun versionedPaths_areAccepted() {
        listOf(
            "images/v2/hero.webp",
            "v1/data.json",
            "levels/v1.3/level-01.json",
            "images/hero.v2.webp",
            "images/hero-v12.png",
            "images/hero.3f2a9c1e.webp",
            "sounds/click_0123456789abcdef.ogg",
        ).forEach { assertEquals(it, AssetPath(it).value) }
    }

    @Test
    fun pathsThatCouldBeOverwritten_areRejected() {
        listOf(
            "images/hero.webp",
            "hero.webp",
            "images/v2",
            "images/hero.abc.webp",
            "images/latest/hero.webp",
        ).forEach { assertRejected(it) }
    }

    @Test
    fun pathsOutsideTheBase_areRejected() {
        listOf(
            "",
            "/images/v2/hero.webp",
            "images//v2/hero.webp",
            "images/v2/../hero.webp",
            "../v2/hero.webp",
            ".download/v2/hero.webp",
            "https://example.com/v2/hero.webp",
            "images/v2/hero.webp?x=1",
            "images/v2/hero image.webp",
        ).forEach { assertRejected(it) }
    }

    private fun assertRejected(path: String) {
        val accepted = runCatching { AssetPath(path) }.isSuccess
        assertEquals("\"$path\" should be rejected", false, accepted)
    }
}
