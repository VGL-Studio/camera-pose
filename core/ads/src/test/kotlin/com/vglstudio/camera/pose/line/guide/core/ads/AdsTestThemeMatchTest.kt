package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.compose.ui.text.font.ResourceFont
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import java.io.File
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Keeps [AdsTestTheme] equal to `AppTheme` of the template (plan T-084, L-143): colors, type scale, shapes, the
 * structure of the font family and the font files themselves.
 *
 * Runs only in the template repository (it has `scripts/new-app.sh`, which `new-app.sh` deletes in an app): in an app
 * the whole class is reported as skipped, since an app changes its own theme on purpose. The class is still compiled
 * in an app, so an app keeps the name and signature `AppTheme(darkTheme, content)`.
 *
 * The test reads `scripts/new-app.sh` and `core/designsystem/src/main/res/font/` outside the inputs Gradle knows, so
 * Gradle may consider the test task up to date when only those files change: run it with `--rerun` when checking.
 */
@RunWith(AndroidJUnit4::class)
class AdsTestThemeMatchTest {
    @get:Rule
    val composeRule = createComposeRule()

    private lateinit var repoRoot: File

    @Before
    fun onlyInTemplate() {
        repoRoot = findRepoRoot()
        assumeTrue("Not the template repository", File(repoRoot, "scripts/new-app.sh").isFile)
    }

    @Test
    fun lightTheme_matchesAppTheme() = assertThemesMatch(darkTheme = false)

    @Test
    fun darkTheme_matchesAppTheme() = assertThemesMatch(darkTheme = true)

    @Test
    fun fontFamilies_matchAppTheme() {
        val (app, test) = captureThemes(darkTheme = false)
        val resources = ApplicationProvider.getApplicationContext<android.content.Context>().resources
        styles(app.typography).forEachIndexed { index, style ->
            val family = style.fontFamily
            assertTrue("AppTheme style $index: font family is not a font list: $family", family is FontListFontFamily)
            val fonts = (family as FontListFontFamily).fonts.map { font ->
                assertTrue("AppTheme style $index: font is not a resource font: $font", font is ResourceFont)
                Triple(resources.getResourceEntryName((font as ResourceFont).resId), font.weight, font.style)
            }
            assertEquals("AppTheme style $index: fonts", AdsTestFontFiles, fonts)
        }
        styles(test.typography).forEachIndexed { index, style ->
            assertSame("AdsTestTheme style $index: font family", AdsTestFontFamily, style.fontFamily)
        }
    }

    @Test
    fun fonts_matchDesignSystemFonts() {
        val fontDir = File(repoRoot, "core/designsystem/src/main/res/font")
        val files = fontDir.listFiles().orEmpty().filter { it.isFile }
        assertEquals(
            "Font files of :core:designsystem",
            AdsTestFontFiles.map { it.first }.toSet(),
            files.map { it.nameWithoutExtension }.toSet(),
        )
        assertTrue("No font file in $fontDir", files.isNotEmpty())
        files.forEach { file ->
            val copy = javaClass.classLoader!!.getResource("ads-test-fonts/${file.nameWithoutExtension}.ttf")
            assertNotNull("Missing test copy of ${file.name}", copy)
            assertArrayEquals("Test copy of ${file.name} differs", file.readBytes(), copy!!.readBytes())
        }
    }

    private fun assertThemesMatch(darkTheme: Boolean) {
        val (app, test) = captureThemes(darkTheme)
        assertEquals("Color scheme", app.colors.toString(), test.colors.toString())
        fixedColors(app.colors).zip(fixedColors(test.colors)).forEach { (expected, actual) ->
            assertEquals("Fixed color ${expected.first}", expected, actual)
        }
        styles(app.typography).zip(styles(test.typography)).forEachIndexed { index, (expected, actual) ->
            assertEquals("Text style $index", expected.copy(fontFamily = null), actual.copy(fontFamily = null))
        }
        assertEquals("Shapes", app.shapes, test.shapes)
    }

    private fun captureThemes(darkTheme: Boolean): Pair<ThemeValues, ThemeValues> {
        var app: ThemeValues? = null
        var test: ThemeValues? = null
        composeRule.setContent {
            AppTheme(darkTheme = darkTheme) { app = currentTheme() }
            AdsTestTheme(darkTheme = darkTheme) { test = currentTheme() }
        }
        composeRule.waitForIdle()
        return app!! to test!!
    }

    private data class ThemeValues(val colors: ColorScheme, val typography: Typography, val shapes: Shapes)

    @Composable
    private fun currentTheme() = ThemeValues(MaterialTheme.colorScheme, MaterialTheme.typography, MaterialTheme.shapes)

    /**
     * Roles of [ColorScheme] that `toString()` does not print, read by reflection so the test compiles with versions of
     * material3 that do not have them (plan T-084, decision 5).
     */
    private fun fixedColors(colors: ColorScheme): List<Pair<String, Any?>> = colors.javaClass.methods
        .filter { it.parameterCount == 0 && it.name.startsWith("get") && it.name.contains("Fixed") }
        .sortedBy { it.name }
        .map { it.name to it.invoke(colors) }

    private fun styles(t: Typography): List<TextStyle> = listOf(
        t.displayLarge, t.displayMedium, t.displaySmall,
        t.headlineLarge, t.headlineMedium, t.headlineSmall,
        t.titleLarge, t.titleMedium, t.titleSmall,
        t.bodyLarge, t.bodyMedium, t.bodySmall,
        t.labelLarge, t.labelMedium, t.labelSmall,
    )

    private fun findRepoRoot(): File {
        var dir: File? = File("").absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").isFile) dir = dir.parentFile
        return checkNotNull(dir) { "No settings.gradle.kts above ${File("").absolutePath}" }
    }
}
