package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.assets.AssetPath
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCatalog
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseCategory
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseGender
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType

internal object Fixtures {
    /** Text of a file in src/test/resources/catalog. */
    fun text(name: String): String {
        val stream = checkNotNull(javaClass.classLoader?.getResourceAsStream("catalog/$name")) { "No fixture $name" }
        return stream.bufferedReader().use { it.readText() }
    }

    fun pose(id: String, categoryId: String = "x", starter: Boolean = false): Pose = Pose(
        id = id,
        categoryId = categoryId,
        subjectType = SubjectType.SINGLE,
        gender = PoseGender.ANY,
        framing = Framing.FULL,
        tags = emptyList(),
        difficulty = 1,
        starter = starter,
        name = LocalizedText(mapOf("en" to "Pose $id")),
        steps = LocalizedList(mapOf("en" to listOf("Step"))),
        mistakes = LocalizedList(emptyMap()),
        keypoints = null,
        thumbnailPath = AssetPath("poses/img/$id/preview_540-1a2b3c4d.webp"),
        imagePath = AssetPath("poses/img/$id/preview_1200-1a2b3c4d.webp"),
        outlinePath = AssetPath("poses/img/$id/outline-1a2b3c4d.webp"),
    )

    /** Three poses in category x, three in y, and an empty category z. */
    fun sixPoseCatalog(): PoseCatalog = PoseCatalog(
        datasetVersion = "test",
        categories = listOf("x", "y", "z").map { PoseCategory(it, LocalizedText(mapOf("en" to it))) },
        poses = listOf(
            pose("p1", "x"),
            pose("p2", "x"),
            pose("p3", "x"),
            pose("p4", "y"),
            pose("p5", "y"),
            pose("p6", "y"),
        ),
    )
}
