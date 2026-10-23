package com.vglstudio.camera.pose.line.guide.settings

import android.content.Context
import com.vglstudio.camera.pose.line.guide.core.data.UserPreferences
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

/** Skeleton for the tests written first (plan T-025, section 4). */
@Suppress("UNUSED_PARAMETER")
internal fun languageToApply(sdkInt: Int, storedTag: String): String? = null

/** Skeleton for the tests written first (plan T-025, section 4). */
@Suppress("UNUSED_PARAMETER")
internal fun Context.withAppLanguage(tag: String?): Context = this

/** Reads the stored choices before Hilt injects the activity (attachBaseContext). */
@EntryPoint
@InstallIn(SingletonComponent::class)
internal interface UserPreferencesEntryPoint {
    fun userPreferences(): UserPreferences
}

/** Skeleton for the tests written first (plan T-025, section 4). */
@Suppress("UNUSED_PARAMETER")
internal fun readStoredLanguage(context: Context): String = ""
