package com.vglstudio.camera.pose.line.guide.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

/**
 * Whether the user has passed the onboarding screen (spec onboarding, KB-3). Kept by :app, not by the feature
 * (plan T-010, question 2): the feature only says when the user tapped "Turn on camera".
 */
internal interface OnboardingStatus {
    /** Throws [IOException] when the value cannot be read; the caller decides what to show. */
    suspend fun isDone(): Boolean

    /** Never throws [IOException]: a failed write is logged and onboarding shows again next time. */
    suspend fun markDone()
}

/**
 * Stores "onboarding done" in the shared preferences DataStore of :core:data. Clearing the app data removes it, so
 * onboarding shows again (KB-3).
 */
internal class DataStoreOnboardingStatus @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) : OnboardingStatus {
    override suspend fun isDone(): Boolean = dataStore.data.first()[KEY] ?: false

    override suspend fun markDone() {
        // Not cancelled if the activity is recreated right after the tap: the write must still land.
        withContext(NonCancellable) {
            try {
                dataStore.edit { preferences -> preferences[KEY] = true }
                logger.info(LOG_TAG, "Onboarding marked done")
            } catch (e: IOException) {
                logger.warn(LOG_TAG, "Could not save onboarding done", e)
            }
        }
    }

    private companion object {
        val KEY = booleanPreferencesKey("onboarding_done")
        const val LOG_TAG = "Onboarding"
    }
}
