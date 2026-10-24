package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.hypot
import kotlin.math.max

/** The score of a detected body against a pose, 0 to 100, and the part that differs most (null: nothing compared). */
data class ScoreResult(val score: Int, val worst: MatchHintText?)

private const val NOSE = 0
private const val MIN_TEMPLATE_VISIBILITY = 0.5f

/** A segment is worth nothing from this many degrees of difference with the pose. */
private const val ZERO_SCORE_DEGREES = 60.0

/** Segments whose differences are closer than this are equally bad; the first one in the list is named. */
private const val TIE_DEGREES = 0.001

private enum class Side {
    LEFT,
    RIGHT,
    ;

    fun other() = if (this == LEFT) RIGHT else LEFT
}

private enum class Part { UPPER_ARM, FOREARM, THIGH, SHIN }

/** Landmark indexes (MediaPipe order) of the joints of one side. */
private fun shoulder(side: Side) = if (side == Side.LEFT) 11 else 12
private fun elbow(side: Side) = if (side == Side.LEFT) 13 else 14
private fun wrist(side: Side) = if (side == Side.LEFT) 15 else 16
private fun hip(side: Side) = if (side == Side.LEFT) 23 else 24
private fun knee(side: Side) = if (side == Side.LEFT) 25 else 26
private fun ankle(side: Side) = if (side == Side.LEFT) 27 else 28

private fun Part.from(side: Side) = when (this) {
    Part.UPPER_ARM -> shoulder(side)
    Part.FOREARM -> elbow(side)
    Part.THIGH -> hip(side)
    Part.SHIN -> knee(side)
}

private fun Part.to(side: Side) = when (this) {
    Part.UPPER_ARM -> elbow(side)
    Part.FOREARM -> wrist(side)
    Part.THIGH -> knee(side)
    Part.SHIN -> ankle(side)
}

/** The eight segments, in the order that settles ties: left arm, right arm, left leg, right leg. */
private val SEGMENTS: List<Pair<Side, Part>> = listOf(
    Side.LEFT to Part.UPPER_ARM,
    Side.LEFT to Part.FOREARM,
    Side.RIGHT to Part.UPPER_ARM,
    Side.RIGHT to Part.FOREARM,
    Side.LEFT to Part.THIGH,
    Side.LEFT to Part.SHIN,
    Side.RIGHT to Part.THIGH,
    Side.RIGHT to Part.SHIN,
)

private class Pt(val x: Double, val y: Double)

private class Vec(val x: Double, val y: Double) {
    val length get() = hypot(x, y)
}

private fun angleBetween(a: Vec, b: Vec): Double =
    Math.toDegrees(atan2(abs(a.x * b.y - a.y * b.x), a.x * b.x + a.y * b.y))

/** Whether the landmarks the framing of the pose needs are all in the frame. */
internal fun isPersonPresent(pose: BodyPose?, framing: Framing, minInFrame: Float): Boolean {
    if (pose == null) return false
    val required = when (framing) {
        Framing.FULL -> listOf(11, 12, 23, 24, 25, 26, 27, 28)
        Framing.HALF -> listOf(11, 12, 23, 24)
        Framing.CLOSE -> listOf(NOSE, 11, 12)
    }
    return required.all { (pose.landmarks[it]?.inFrame ?: 0f) >= minInFrame }
}

/**
 * How close the detected body is to the pose, by the angles of the eight limb segments (upper arm, forearm, thigh,
 * shin, each side). Angles only, so where the person stands and how big the outline is do not matter. Both bodies are
 * compared as the screen shows them: the outline is flipped when [templateMirrored], the detected body is already
 * flipped for the front camera. When exactly one of them is flipped, the sides of the template are swapped, so the
 * left arm of a person looking in a mirror is compared with the arm that is on the left of the screen in the outline.
 * The hint names the side of the person (the landmark labels of the detected body).
 */
internal fun score(
    template: List<Keypoint>,
    templateMirrored: Boolean,
    detected: BodyPose,
    minInFrame: Float,
): ScoreResult {
    val swap = templateMirrored != detected.mirrored
    fun templateSide(side: Side) = if (swap) side.other() else side

    fun detectedPoint(index: Int): Pt? =
        detected.landmarks[index]?.takeIf { it.inFrame >= minInFrame }?.let { Pt(it.x.toDouble(), it.y.toDouble()) }

    fun templatePoint(index: Int): Pt? = template.getOrNull(index)
        ?.takeIf { it.visibility >= MIN_TEMPLATE_VISIBILITY }
        ?.let { Pt(it.x.toDouble(), it.y.toDouble()) }

    var total = 0.0
    var counted = 0
    var worst: MatchHintText? = null
    var worstDegrees = -1.0
    val flip = if (templateMirrored) -1.0 else 1.0
    for ((side, part) in SEGMENTS) {
        val tSide = templateSide(side)
        val d0 = detectedPoint(part.from(side)) ?: continue
        val d1 = detectedPoint(part.to(side)) ?: continue
        val t0 = templatePoint(part.from(tSide)) ?: continue
        val t1 = templatePoint(part.to(tSide)) ?: continue
        val dv = Vec(d1.x - d0.x, d1.y - d0.y)
        val tv = Vec((t1.x - t0.x) * flip, t1.y - t0.y)
        if (dv.length == 0.0 || tv.length == 0.0) continue
        val degrees = angleBetween(dv, tv)
        total += max(0.0, 1.0 - degrees / ZERO_SCORE_DEGREES)
        counted++
        val hint = hintFor(side, part, dv, tv, ::detectedPoint) { joint -> templatePoint(joint(tSide)) } ?: continue
        if (degrees > worstDegrees + TIE_DEGREES) {
            worst = hint
            worstDegrees = degrees
        }
    }
    if (counted == 0) return ScoreResult(0, null)
    return ScoreResult(Math.round(total / counted * 100.0).toInt().coerceIn(0, 100), worst)
}

/**
 * The hint of a segment that differs, or null when a landmark the decision needs is missing. [detectedPoint] takes a
 * landmark index; [templatePoint] takes the joint function (such as `::elbow`) and finds it on the template side.
 */
private fun hintFor(
    side: Side,
    part: Part,
    detected: Vec,
    template: Vec,
    detectedPoint: (Int) -> Pt?,
    templatePoint: ((Side) -> Int) -> Pt?,
): MatchHintText? {
    val left = side == Side.LEFT
    return when (part) {
        Part.UPPER_ARM -> {
            // Pointing lower on the screen than the pose: raise the arm.
            val lower = detected.y / detected.length > template.y / template.length
            if (left) {
                if (lower) MatchHintText.RAISE_LEFT_ARM else MatchHintText.LOWER_LEFT_ARM
            } else {
                if (lower) MatchHintText.RAISE_RIGHT_ARM else MatchHintText.LOWER_RIGHT_ARM
            }
        }
        Part.FOREARM -> {
            val straighter = straighter(
                jointAngle(detectedPoint(shoulder(side)), detectedPoint(elbow(side)), detectedPoint(wrist(side))),
                jointAngle(templatePoint(::shoulder), templatePoint(::elbow), templatePoint(::wrist)),
            ) ?: return null
            if (left) {
                if (straighter) MatchHintText.BEND_LEFT_ELBOW else MatchHintText.STRAIGHTEN_LEFT_ARM
            } else {
                if (straighter) MatchHintText.BEND_RIGHT_ELBOW else MatchHintText.STRAIGHTEN_RIGHT_ARM
            }
        }
        Part.THIGH -> {
            // Away from the middle of the body, by the hips of the detected body (the screen side is the same).
            val own = detectedPoint(hip(side)) ?: return null
            val otherHip = detectedPoint(hip(side.other())) ?: return null
            val outward = if (own.x < otherHip.x) -1.0 else 1.0
            val wider = detected.x * outward / detected.length > template.x * outward / template.length
            if (left) {
                if (wider) MatchHintText.CLOSE_LEFT_LEG else MatchHintText.WIDEN_LEFT_LEG
            } else {
                if (wider) MatchHintText.CLOSE_RIGHT_LEG else MatchHintText.WIDEN_RIGHT_LEG
            }
        }
        Part.SHIN -> {
            val straighter = straighter(
                jointAngle(detectedPoint(hip(side)), detectedPoint(knee(side)), detectedPoint(ankle(side))),
                jointAngle(templatePoint(::hip), templatePoint(::knee), templatePoint(::ankle)),
            ) ?: return null
            if (left) {
                if (straighter) MatchHintText.BEND_LEFT_KNEE else MatchHintText.STRAIGHTEN_LEFT_LEG
            } else {
                if (straighter) MatchHintText.BEND_RIGHT_KNEE else MatchHintText.STRAIGHTEN_RIGHT_LEG
            }
        }
    }
}

/** Whether the detected joint is straighter than the one of the pose; null when either angle is unknown. */
private fun straighter(detected: Double?, template: Double?): Boolean? =
    if (detected == null || template == null) null else detected > template

/** The angle at [middle] between the directions to [first] and [last], in degrees; null when a point is missing. */
private fun jointAngle(first: Pt?, middle: Pt?, last: Pt?): Double? {
    if (first == null || middle == null || last == null) return null
    val a = Vec(first.x - middle.x, first.y - middle.y)
    val b = Vec(last.x - middle.x, last.y - middle.y)
    if (a.length == 0.0 || b.length == 0.0) return null
    return angleBetween(a, b)
}
