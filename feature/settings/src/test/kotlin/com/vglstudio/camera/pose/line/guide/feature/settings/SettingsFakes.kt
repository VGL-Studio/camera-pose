package com.vglstudio.camera.pose.line.guide.feature.settings

import com.vglstudio.camera.pose.line.guide.core.config.AppLinks
import com.vglstudio.camera.pose.line.guide.core.data.ThemeMode
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import java.io.IOException
import kotlinx.coroutines.flow.MutableStateFlow

/** The settings choices held in memory. Records each save; [failWrites] makes every save throw IOException. */
internal class FakeUserPreferences : UserPreferences {
    override val themeMode = MutableStateFlow(ThemeMode.SYSTEM)
    override val shutterSoundEnabled = MutableStateFlow(true)
    override val appLanguage = MutableStateFlow("")

    val savedThemes = mutableListOf<ThemeMode>()
    val savedSounds = mutableListOf<Boolean>()
    val savedLanguages = mutableListOf<String>()
    var failWrites = false

    override suspend fun setThemeMode(mode: ThemeMode) {
        if (failWrites) throw IOException("disk write failed")
        savedThemes += mode
        themeMode.value = mode
    }

    override suspend fun setShutterSoundEnabled(enabled: Boolean) {
        if (failWrites) throw IOException("disk write failed")
        savedSounds += enabled
        shutterSoundEnabled.value = enabled
    }

    override suspend fun setAppLanguage(tag: String) {
        if (failWrites) throw IOException("disk write failed")
        savedLanguages += tag
        appLanguage.value = tag
    }
}

internal class FakeAppLinks : AppLinks {
    override val privacyPolicyUrl = MutableStateFlow("https://studio.vgl.io.vn/privacy-policy")
}
