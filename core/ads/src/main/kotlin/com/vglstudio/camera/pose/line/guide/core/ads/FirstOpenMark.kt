package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import javax.inject.Inject
import kotlinx.coroutines.flow.first

/**
 * The mark "the app was opened once" (A-01), kept in DataStore. [AdsController] consumes it once per process, so
 * every read in the process sees the same answer (question 6 of plan T-024).
 */
internal class FirstOpenMark @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) {
    /**
     * True when this is the first open since install or since the data was cleared; then writes the mark. A read
     * error counts as a later open; a write error keeps this open the first one (the next may be one again).
     */
    suspend fun consume(): Boolean {
        val done = try {
            dataStore.data.first()[KEY] == true
        } catch (e: IOException) {
            logger.warn(TAG, "First-open mark not read: later open", e)
            return false
        }
        if (done) return false
        try {
            dataStore.edit { it[KEY] = true }
        } catch (e: IOException) {
            logger.warn(TAG, "First-open mark not written", e)
        }
        logger.info(TAG, "First open since install")
        return true
    }

    private companion object {
        const val TAG = "Ads"
        val KEY = booleanPreferencesKey("ads_first_open_done")
    }
}
