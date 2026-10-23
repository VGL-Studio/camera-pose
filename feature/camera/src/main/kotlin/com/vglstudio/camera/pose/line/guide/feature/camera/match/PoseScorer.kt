package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint

/** The score of a detected body against a pose, 0 to 100, and the part that differs most (null: nothing compared). */
data class ScoreResult(val score: Int, val worst: MatchHintText?)

/** Whether the landmarks the framing of the pose needs are all in the frame. */
@Suppress("UNUSED_PARAMETER")
internal fun isPersonPresent(pose: BodyPose?, framing: Framing, minInFrame: Float): Boolean = false

/** How close the detected body is to the pose, by the angles of the eight limb segments. */
@Suppress("UNUSED_PARAMETER")
internal fun score(
    template: List<Keypoint>,
    templateMirrored: Boolean,
    detected: BodyPose,
    minInFrame: Float,
): ScoreResult = ScoreResult(0, null)
