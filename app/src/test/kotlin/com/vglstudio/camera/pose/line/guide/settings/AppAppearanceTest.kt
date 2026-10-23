package com.vglstudio.camera.pose.line.guide.settings

import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The look of the app follows the choice of the settings screen (spec settings, KB-2; plan T-025, section 4). */
class AppAppearanceTest {
    @Test
    fun system_followsDevice() {
        assertTrue(ThemeMode.SYSTEM.isDark(systemDark = true))
        assertFalse(ThemeMode.SYSTEM.isDark(systemDark = false))
    }

    @Test
    fun light_isNeverDark() {
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = true))
        assertFalse(ThemeMode.LIGHT.isDark(systemDark = false))
        // Control: the same device setting gives a dark app when the user chose Dark.
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }

    @Test
    fun dark_isAlwaysDark() {
        assertTrue(ThemeMode.DARK.isDark(systemDark = true))
        assertTrue(ThemeMode.DARK.isDark(systemDark = false))
    }
}
