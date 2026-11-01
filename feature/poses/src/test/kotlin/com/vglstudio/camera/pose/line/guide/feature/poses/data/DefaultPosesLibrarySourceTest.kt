package com.vglstudio.camera.pose.line.guide.feature.poses.data

import app.cash.turbine.test
import com.vglstudio.camera.pose.line.guide.core.poses.PoseHistoryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLibraryRepository
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import com.vglstudio.camera.pose.line.guide.feature.poses.FakePremiumPoseRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesFixtures
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The library source over :core:poses: the locked poses follow the premium access of T-085 (spec poses, KB-8; plan
 * T-086, section 4). The catalog is the fixture one with w02 and c01 premium.
 */
class DefaultPosesLibrarySourceTest {
    private val library = FakeLibraryRepository()
    private val premium = FakePremiumPoseRepository()
    private val source = DefaultPosesLibrarySource(library, NoHistory, premium)

    private suspend fun content(): LibraryContent {
        val result = source.library.first { it is LibraryResult.Ready }
        return (result as LibraryResult.Ready).content
    }

    @Test
    fun flagOff_nothingLocked() = runTest {
        premium.access.value = PremiumAccess(enabled = false, unlockedIds = emptySet())
        val content = content()
        assertEquals(PosesFixtures.POSES.size, content.totalPoses)
        assertTrue(content.lockedIds.isEmpty())
    }

    @Test
    fun flagOn_premiumNotUnlocked_isLocked() = runTest {
        // Control of flagOff_nothingLocked: the same catalog, only the flag differs.
        premium.access.value = PremiumAccess(enabled = true, unlockedIds = emptySet())
        val content = content()
        assertEquals(setOf("w02", "c01"), content.lockedIds)
        assertFalse("w01" in content.lockedIds)
    }

    @Test
    fun oneUnlocked_isNotLocked_andFollowsAccess() = runTest {
        premium.access.value = PremiumAccess(enabled = true, unlockedIds = setOf("w02"))
        source.library.test {
            assertEquals(setOf("c01"), (awaitItem() as LibraryResult.Ready).content.lockedIds)
            premium.access.value = PremiumAccess(enabled = true, unlockedIds = setOf("w02", "c01"))
            assertEquals(emptySet<String>(), (awaitItem() as LibraryResult.Ready).content.lockedIds)
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun unlock_returnsRepositoryResult() = runTest {
        premium.unlockResult = true
        assertTrue(source.unlock("w02"))
        assertEquals(listOf("w02"), premium.unlocks)
        premium.unlockResult = false
        assertFalse(source.unlock("c01"))
        assertEquals(listOf("w02", "c01"), premium.unlocks)
    }

    private class FakeLibraryRepository : PoseLibraryRepository {
        val state = MutableStateFlow<LibraryState>(
            LibraryState.Ready(
                PoseLibrary.from(
                    PoseCatalog(
                        datasetVersion = "test",
                        categories = listOf(PosesFixtures.WOMEN, PosesFixtures.MEN, PosesFixtures.COUPLES),
                        poses = PosesFixtures.POSES.map { pose ->
                            if (pose.id in PREMIUM) pose.copy(premium = true) else pose
                        },
                    ),
                ),
            ),
        )

        override val library: Flow<LibraryState> = state

        override suspend fun retry() = Unit
    }

    private object NoHistory : PoseHistoryRepository {
        override val saved: Flow<List<SavedPose>> = MutableStateFlow(emptyList())
        override val recent: Flow<List<UsedPose>> = MutableStateFlow(emptyList())

        override suspend fun setSaved(poseId: String, saved: Boolean) = Unit

        override suspend fun markUsed(poseId: String) = Unit
    }

    private companion object {
        val PREMIUM = setOf("w02", "c01")
    }
}
