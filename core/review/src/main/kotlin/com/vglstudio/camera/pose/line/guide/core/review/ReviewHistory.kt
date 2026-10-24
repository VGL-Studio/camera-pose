package com.vglstudio.camera.pose.line.guide.core.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import java.io.IOException
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** When the review dialog was last asked for. */
internal interface ReviewHistory {
    suspend fun lastAskedAt(): Long?

    suspend fun recordAsked(atMillis: Long)
}

/** One long in the shared preferences. A read or write error never reaches the caller. */
internal class DataStoreReviewHistory(private val dataStore: DataStore<Preferences>, private val logger: AppLogger) :
    ReviewHistory {
    override suspend fun lastAskedAt(): Long? = dataStore.data
        .catch { e ->
            if (e !is IOException) throw e
            logger.warn(LOG_TAG, "Review history unreadable", e)
            emit(emptyPreferences())
        }
        .map { it[LAST_ASKED_AT] }
        .first()

    override suspend fun recordAsked(atMillis: Long) {
        try {
            dataStore.edit { it[LAST_ASKED_AT] = atMillis }
        } catch (e: IOException) {
            logger.warn(LOG_TAG, "Review time not stored", e)
        }
    }

    private companion object {
        const val LOG_TAG = "InAppReview"
        val LAST_ASKED_AT = longPreferencesKey("in_app_review_last_asked_at")
    }
}
