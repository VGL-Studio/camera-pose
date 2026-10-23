package com.vglstudio.camera.pose.line.guide.settings

import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode

/** Whether the app is drawn dark: the user's choice, or the device setting for "System default" (spec settings, KB-2). */
internal fun ThemeMode.isDark(systemDark: Boolean): Boolean = when (this) {
    ThemeMode.SYSTEM -> systemDark
    ThemeMode.LIGHT -> false
    ThemeMode.DARK -> true
}
