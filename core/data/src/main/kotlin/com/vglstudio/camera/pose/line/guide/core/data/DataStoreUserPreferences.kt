package com.vglstudio.camera.pose.line.guide.core.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Skeleton for the tests written first (plan T-025, section 4): defaults, nothing written. */
internal class DataStoreUserPreferences @Inject constructor(
    @Suppress("unused") private val dataStore: DataStore<Preferences>,
) : UserPreferences {
    override val themeMode: Flow<ThemeMode> = flowOf(ThemeMode.SYSTEM)

    override val shutterSoundEnabled: Flow<Boolean> = flowOf(true)

    override val appLanguage: Flow<String> = flowOf(AppLanguages.SYSTEM)

    override suspend fun setThemeMode(mode: ThemeMode) = Unit

    override suspend fun setShutterSoundEnabled(enabled: Boolean) = Unit

    override suspend fun setAppLanguage(tag: String) = Unit
}
