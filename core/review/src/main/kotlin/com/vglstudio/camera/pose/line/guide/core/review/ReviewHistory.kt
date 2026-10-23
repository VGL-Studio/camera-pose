package com.vglstudio.camera.pose.line.guide.core.review

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger

/** When the review dialog was last asked for. */
internal interface ReviewHistory {
    suspend fun lastAskedAt(): Long?

    suspend fun recordAsked(atMillis: Long)
}

/** Placeholder until the code step (plan T-019). */
@Suppress("UnusedPrivateProperty")
internal class DataStoreReviewHistory(private val dataStore: DataStore<Preferences>, private val logger: AppLogger) :
    ReviewHistory {
    override suspend fun lastAskedAt(): Long? = null

    override suspend fun recordAsked(atMillis: Long) = Unit
}
