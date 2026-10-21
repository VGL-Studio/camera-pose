package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.internal.buildPoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PoseOrderingTest {
    private val catalog = Fixtures.sixPoseCatalog()

    private fun library(
        saved: List<SavedPose> = emptyList(),
        recent: List<UsedPose> = emptyList(),
        of: PoseCatalog = catalog,
    ): PoseLibrary = buildPoseLibrary(of, saved, recent)

    private fun PoseLibrary.ids(filter: PoseFilter) = posesFor(filter).map { it.id }

    // p5 and p3 are saved (p5 later); p2, p6 and p3 were used; p1 and p4 are the rest.
    private val saved = listOf(SavedPose("p3", 100), SavedPose("p5", 200))
    private val recent = listOf(UsedPose("p2", 50), UsedPose("p6", 70), UsedPose("p3", 999))

    @Test
    fun all_savedNewestFirst_thenRecentNewestFirst_thenRestInCatalogOrder() {
        assertEquals(
            listOf("p5", "p3", "p6", "p2", "p1", "p4"),
            library(saved, recent).ids(PoseFilter.All),
        )
    }

    @Test
    fun poseSavedAndRecent_appearsOnceInSavedPart() {
        val ids = library(saved, recent).ids(PoseFilter.All)
        assertEquals(1, ids.count { it == "p3" })
        assertEquals(1, ids.indexOf("p3"))
    }

    @Test
    fun category_appliesSameOrderWithinGroupOnly() {
        assertEquals(listOf("p5", "p6", "p4"), library(saved, recent).ids(PoseFilter.Category("y")))
        assertEquals(listOf("p3", "p2", "p1"), library(saved, recent).ids(PoseFilter.Category("x")))
    }

    @Test
    fun categories_hideEmptyGroups_keepCatalogOrder() {
        assertEquals(listOf("x", "y"), library().categories.map { it.id })
    }

    @Test
    fun totals_countEveryPoseAndOnlyVisibleCategories() {
        val library = library()
        assertEquals(6, library.totalPoses)
        assertEquals(2, library.categories.size)
    }

    @Test
    fun savedFilter_newestSavedFirst() {
        assertEquals(listOf("p5", "p3"), library(saved, recent).ids(PoseFilter.Saved))
    }

    @Test
    fun savedPoseMissingFromCatalog_isNotShown() {
        val library = library(saved = saved + SavedPose("ghost", 500))

        assertEquals(listOf("p5", "p3"), library.ids(PoseFilter.Saved))
        assertEquals(false, library.ids(PoseFilter.All).contains("ghost"))
        assertEquals(setOf("p3", "p5"), library.savedIds)
    }

    @Test
    fun recentPoseMissingFromCatalog_isIgnored() {
        val library = library(recent = listOf(UsedPose("ghost", 500), UsedPose("p4", 10)))
        assertEquals(listOf("p4", "p1", "p2", "p3", "p5", "p6"), library.ids(PoseFilter.All))
    }

    @Test
    fun defaultPose_isFirstPoseInCatalogOrder_evenWhenOthersSavedOrRecent() {
        assertEquals("p1", library(saved, recent).defaultPose?.id)
    }

    @Test
    fun lastUsedPose_isNewestRecentStillInCatalog() {
        val library = library(recent = listOf(UsedPose("ghost", 999), UsedPose("p2", 50), UsedPose("p6", 70)))
        assertEquals("p6", library.lastUsedPose?.id)
    }

    @Test
    fun lastUsedPose_noneInCatalog_isNull() {
        assertNull(library(recent = listOf(UsedPose("ghost", 999))).lastUsedPose)
        assertNull(library().lastUsedPose)
    }
}
