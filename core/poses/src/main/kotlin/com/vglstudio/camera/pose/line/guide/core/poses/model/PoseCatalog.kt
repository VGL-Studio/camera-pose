package com.vglstudio.camera.pose.line.guide.core.poses.model

/** The catalog as read from R2, after invalid poses are dropped. Categories and poses keep the catalog order. */
data class PoseCatalog(val datasetVersion: String, val categories: List<PoseCategory>, val poses: List<Pose>)
