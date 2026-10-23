package com.vglstudio.camera.pose.line.guide.settings

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import com.vglstudio.camera.pose.line.guide.core.data.AppLanguages
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull

// The language picker of the app is used below Android 13 only; from Android 13 the system keeps the app language
// (spec settings, KB-4; plan T-025, question 5). There is no AppCompat, so the activity applies it itself.

private const val LANGUAGE_READ_TIMEOUT_MILLIS = 500L

/** The language the activity applies: the stored release language below Android 13, otherwise none. */
internal fun languageToApply(sdkInt: Int, storedTag: String): String? = storedTag.takeIf {
    sdkInt < Build.VERSION_CODES.TIRAMISU && it in AppLanguages.RELEASE
}

/** This context with its resources in [tag], or this context itself when [tag] is null. */
internal fun Context.withAppLanguage(tag: String?): Context {
    if (tag == null) return this
    val configuration = Configuration(resources.configuration)
    configuration.setLocales(LocaleList.forLanguageTags(tag))
    return createConfigurationContext(configuration)
}

/** Reads the stored choices before Hilt injects the activity (attachBaseContext). */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface UserPreferencesEntryPoint {
    fun userPreferences(): UserPreferences
}

/**
 * The language stored by the picker, read while the activity is being created, below Android 13 only. Blocking: the
 * activity needs it before its first frame; DataStore reads one small file, and the wait is capped at 500 ms.
 */
internal fun readStoredLanguage(context: Context): String {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return AppLanguages.SYSTEM
    val preferences = EntryPointAccessors
        .fromApplication(context.applicationContext, UserPreferencesEntryPoint::class.java)
        .userPreferences()
    return runBlocking { withTimeoutOrNull(LANGUAGE_READ_TIMEOUT_MILLIS) { preferences.appLanguage.first() } }
        ?: AppLanguages.SYSTEM
}
