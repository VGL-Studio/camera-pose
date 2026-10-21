package com.vglstudio.camera.pose.line.guide.core.designsystem

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.text.font.FontListFontFamily
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTypography
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.BricolageFamily
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.appColors
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** The app colors follow the theme mode, and the brand typeface is applied to every text style. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class AppThemeTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun appColors_followTheme() {
        var light: Triple<Color, Color, Color>? = null
        var dark: Color? = null
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                val colors = MaterialTheme.appColors
                light = Triple(colors.success, colors.shadowSticker, colors.onSuccess)
            }
            AppTheme(darkTheme = true) { dark = MaterialTheme.appColors.success }
        }
        composeRule.waitForIdle()
        assertEquals(0xFF1F9D63.toInt(), light?.first?.toArgb())
        assertEquals(0xFFFFC83D.toInt(), light?.second?.toArgb())
        assertEquals(0xFFFFFFFF.toInt(), light?.third?.toArgb())
        assertEquals(0xFF3DDC97.toInt(), dark?.toArgb())
    }

    @Test
    fun bricolageFamily_hasFourWeights() {
        val weights = (BricolageFamily as? FontListFontFamily)?.fonts?.map { it.weight.weight }
        assertEquals(listOf(400, 500, 600, 800), weights)
    }

    @Test
    fun typography_everyStyleUsesBricolage() {
        val styles = with(AppTypography) {
            listOf(
                displayLarge, displayMedium, displaySmall, headlineLarge, headlineMedium, headlineSmall,
                titleLarge, titleMedium, titleSmall, bodyLarge, bodyMedium, bodySmall,
                labelLarge, labelMedium, labelSmall,
            )
        }
        assertEquals(15, styles.size)
        assertTrue(styles.all { it.fontFamily == BricolageFamily })
    }
}
