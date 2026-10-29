package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import javax.inject.Inject

/**
 * N-05: how many interstitials were shown since install (or since the data was cleared), kept in DataStore. Every
 * interstitial counts, the one of the loading screen too (T-024); T-033 sends `af_inters_displayed_{N}_times` from
 * the count [increment] returns.
 */
internal class InterDisplayCounter @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val logger: AppLogger,
) {
    /** Adds one display and returns the new count; null when the count could not be read or written. */
    suspend fun increment(): Int? = try {
        dataStore.edit { it[KEY] = (it[KEY] ?: 0) + 1 }[KEY]
    } catch (e: IOException) {
        logger.warn(TAG, "Interstitial display not counted", e)
        null
    }

    private companion object {
        const val TAG = "Ads"
        val KEY = intPreferencesKey("ads_inter_display_count")
    }
}
