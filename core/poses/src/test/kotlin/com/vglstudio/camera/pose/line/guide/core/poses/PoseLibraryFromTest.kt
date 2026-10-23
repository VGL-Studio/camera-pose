package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.internal.buildPoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseLibrary
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import org.junit.Assert.assertEquals
import org.junit.Test

/** The public builder gives the same library as the repository's own ordering (plan T-014). */
class PoseLibraryFromTest {
    private val catalog = Fixtures.sixPoseCatalog()
    private val saved = listOf(SavedPose("p3", 100), SavedPose("p5", 200))
    private val recent = listOf(UsedPose("p2", 50), UsedPose("p6", 70))

    private fun PoseLibrary.ids(filter: PoseFilter) = posesFor(filter).map { it.id }

    @Test
    fun from_matchesLibraryOrder() {
        val expected = buildPoseLibrary(catalog, saved, recent)
        val actual = PoseLibrary.from(catalog, saved, recent)

        assertEquals(expected.ids(PoseFilter.All), actual.ids(PoseFilter.All))
        assertEquals(expected.ids(PoseFilter.Category("x")), actual.ids(PoseFilter.Category("x")))
        assertEquals(expected.categories, actual.categories)
        assertEquals(expected.defaultPose, actual.defaultPose)
        assertEquals(expected.lastUsedPose, actual.lastUsedPose)
        assertEquals(listOf("p5", "p3", "p6", "p2", "p1", "p4"), actual.ids(PoseFilter.All))
    }

    @Test
    fun from_defaultsToEmptyHistory() {
        val library = PoseLibrary.from(catalog)

        assertEquals(listOf("p1", "p2", "p3", "p4", "p5", "p6"), library.ids(PoseFilter.All))
        assertEquals("p1", library.defaultPose?.id)
        assertEquals(null, library.lastUsedPose)
        assertEquals(listOf("x", "y"), library.categories.map { it.id })
    }
}
