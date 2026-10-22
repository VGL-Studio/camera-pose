package com.vglstudio.camera.pose.line.guide.onboarding

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import javax.inject.Inject

/** Whether the user has passed the onboarding screen (spec onboarding, KB-3). */
internal interface OnboardingStatus {
    suspend fun isDone(): Boolean

    suspend fun markDone()
}

/** Skeleton (plan T-012, step 3). */
internal class DataStoreOnboardingStatus @Inject constructor(
    @Suppress("unused") private val dataStore: DataStore<Preferences>,
    @Suppress("unused") private val logger: AppLogger,
) : OnboardingStatus {
    override suspend fun isDone(): Boolean = false

    override suspend fun markDone() = Unit
}
