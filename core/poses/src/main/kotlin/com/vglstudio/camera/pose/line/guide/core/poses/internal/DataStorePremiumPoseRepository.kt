package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/** The unlocked poses are one list of [PoseListStore], newest first, with the unlock time (spec poses, section 6). */
internal class DataStorePremiumPoseRepository(
    dataStore: DataStore<Preferences>,
    private val flags: FeatureFlags,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PremiumPoseRepository {
    private val store = PoseListStore(dataStore, logger)

    override val access: Flow<PremiumAccess> = combine(
        flags.enabled(FeatureFlag.PREMIUM_POSES),
        store.read(UNLOCKED_KEY),
    ) { enabled, unlocked -> PremiumAccess(enabled, unlocked.mapTo(mutableSetOf()) { it.id }) }
        .distinctUntilChanged()

    override fun isUsable(pose: Pose): Flow<Boolean> = access.map { it.isUsable(pose) }.distinctUntilChanged()

    override suspend fun unlock(poseId: String): Boolean {
        // A decision at one moment (ADR-0003): with the flag off nothing is written (spec poses, question 10).
        if (!flags.enabled(FeatureFlag.PREMIUM_POSES).first()) {
            logger.info(LOG_TAG, "Unlock skipped: flag_off")
            return false
        }
        store.update(UNLOCKED_KEY) { list ->
            if (list.any { it.id == poseId }) list else listOf(StoredPose(poseId, clock.nowMillis())) + list
        }
        logger.info(LOG_TAG, "A premium pose was unlocked")
        return true
    }

    private companion object {
        val UNLOCKED_KEY = stringPreferencesKey("poses_unlocked")
    }
}
