package com.vglstudio.camera.pose.line.guide.core.poses.internal

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.PremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

// Empty frame of the tests written first (T-085, step 3); the real code comes with the next commit.
@Suppress("UnusedPrivateProperty", "unused")
internal class DataStorePremiumPoseRepository(
    private val dataStore: DataStore<Preferences>,
    private val flags: FeatureFlags,
    private val clock: PosesClock,
    private val logger: AppLogger,
) : PremiumPoseRepository {
    override val access: Flow<PremiumAccess> = flowOf(PremiumAccess(false, emptySet()))

    override fun isUsable(pose: Pose): Flow<Boolean> = flowOf(true)

    override suspend fun unlock(poseId: String): Boolean = false
}
