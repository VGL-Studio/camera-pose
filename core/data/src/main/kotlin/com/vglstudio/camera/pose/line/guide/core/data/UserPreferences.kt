package com.vglstudio.camera.pose.line.guide.core.data

import kotlinx.coroutines.flow.Flow

/** Light or dark look of the app chosen on the settings screen (spec settings, KB-2). */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** The release languages (spec settings, KB-4); "" follows the device. */
object AppLanguages {
    const val SYSTEM = ""
    val RELEASE: List<String> = listOf("en", "hi", "pt-BR", "es-419", "id", "th", "vi")
}

/**
 * Choices made on the settings screen, read by :app (theme, language) and the camera (shutter sound).
 * Each flow emits the stored value, or the default while nothing valid is stored. Setters throw IOException.
 */
interface UserPreferences {
    val themeMode: Flow<ThemeMode>

    val shutterSoundEnabled: Flow<Boolean>

    /** Used below Android 13 only; from Android 13 the system keeps the app language. */
    val appLanguage: Flow<String>

    suspend fun setThemeMode(mode: ThemeMode)

    suspend fun setShutterSoundEnabled(enabled: Boolean)

    suspend fun setAppLanguage(tag: String)
}
