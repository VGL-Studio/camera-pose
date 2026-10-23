package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint
import kotlin.math.cos
import kotlin.math.sin

/**
 * Synthetic bodies for the matching tests (plan T-017, section 4): 33 landmarks in MediaPipe order, coordinates 0 to 1
 * as the screen shows them (the person faces the camera, so the person's left is on the right of the screen). No
 * image and no ML Kit are needed. A [Shape] is the same body as a [BodyPose] and as the [Keypoint]s of a template.
 */
internal object TestBodyPoses {
    const val L_SHOULDER = 11
    const val R_SHOULDER = 12
    const val L_ELBOW = 13
    const val R_ELBOW = 14
    const val L_WRIST = 15
    const val R_WRIST = 16
    const val L_HIP = 23
    const val R_HIP = 24
    const val L_KNEE = 25
    const val R_KNEE = 26
    const val L_ANKLE = 27
    const val R_ANKLE = 28

    /** Landmark index to (x, y). */
    class Shape(val points: Map<Int, Pair<Float, Float>>) {
        fun with(vararg changes: Pair<Int, Pair<Float, Float>>) = Shape(points + changes)

        fun without(indexes: Iterable<Int>) = Shape(points.filterKeys { it !in indexes })

        /** Moves the shape by [dx], [dy] after scaling it by [scale] (about the origin). */
        fun movedAndScaled(scale: Float, dx: Float, dy: Float) =
            Shape(points.mapValues { (_, p) -> (p.first * scale + dx) to (p.second * scale + dy) })

        fun flippedX() = Shape(points.mapValues { (_, p) -> (1f - p.first) to p.second })

        /** [confidence] is the in-frame likelihood of every landmark. */
        fun body(confidence: Float = 0.9f, mirrored: Boolean = false) = BodyPose(
            landmarks = points.mapValues { (_, p) -> BodyLandmark(p.first, p.second, confidence) },
            mirrored = mirrored,
        )

        /** The 33 landmarks of a pose of the catalog; a landmark the shape lacks gets visibility 0. */
        fun keypoints(visibility: Float = 0.9f): List<Keypoint> = (0..32).map { index ->
            points[index]?.let { Keypoint(it.first, it.second, visibility) } ?: Keypoint(0f, 0f, 0f)
        }
    }

    private val STANDING_ARMS_OUT = mapOf(
        0 to (0.5f to 0.15f),
        1 to (0.52f to 0.13f), 2 to (0.53f to 0.13f), 3 to (0.54f to 0.13f),
        4 to (0.48f to 0.13f), 5 to (0.47f to 0.13f), 6 to (0.46f to 0.13f),
        7 to (0.56f to 0.15f), 8 to (0.44f to 0.15f),
        9 to (0.52f to 0.18f), 10 to (0.48f to 0.18f),
        L_SHOULDER to (0.6f to 0.3f), R_SHOULDER to (0.4f to 0.3f),
        L_ELBOW to (0.75f to 0.3f), R_ELBOW to (0.25f to 0.3f),
        L_WRIST to (0.9f to 0.3f), R_WRIST to (0.1f to 0.3f),
        17 to (0.93f to 0.3f), 18 to (0.07f to 0.3f),
        19 to (0.93f to 0.3f), 20 to (0.07f to 0.3f),
        21 to (0.93f to 0.3f), 22 to (0.07f to 0.3f),
        L_HIP to (0.55f to 0.55f), R_HIP to (0.45f to 0.55f),
        L_KNEE to (0.55f to 0.75f), R_KNEE to (0.45f to 0.75f),
        L_ANKLE to (0.55f to 0.95f), R_ANKLE to (0.45f to 0.95f),
        29 to (0.55f to 0.97f), 30 to (0.45f to 0.97f),
        31 to (0.58f to 0.98f), 32 to (0.42f to 0.98f),
    )

    /** Standing straight, both arms out to the sides. The person's left arm is on the right of the screen. */
    val standingArmsOut = Shape(STANDING_ARMS_OUT)

    /** As [standingArmsOut], the left arm pointing down. */
    val leftArmDown = standingArmsOut.with(
        L_ELBOW to (0.6f to 0.5f),
        L_WRIST to (0.6f to 0.7f),
    )

    val rightArmDown = standingArmsOut.with(
        R_ELBOW to (0.4f to 0.5f),
        R_WRIST to (0.4f to 0.7f),
    )

    val armsDown = leftArmDown.with(
        R_ELBOW to (0.4f to 0.5f),
        R_WRIST to (0.4f to 0.7f),
    )

    /** The left arm raised, half the way up. */
    val leftArmUp = standingArmsOut.with(
        L_ELBOW to (0.7f to 0.2f),
        L_WRIST to (0.8f to 0.1f),
    )

    /** The left upper arm out, the forearm down: the elbow is a right angle. */
    val leftElbowBent = standingArmsOut.with(L_WRIST to (0.75f to 0.5f))

    /** Both legs wide apart. */
    val legsApart = standingArmsOut.with(
        L_KNEE to (0.7f to 0.75f),
        L_ANKLE to (0.85f to 0.95f),
        R_KNEE to (0.3f to 0.75f),
        R_ANKLE to (0.15f to 0.95f),
    )

    /** Both legs close together. */
    val legsClose = standingArmsOut.with(
        L_KNEE to (0.5f to 0.75f),
        L_ANKLE to (0.5f to 0.95f),
        R_KNEE to (0.5f to 0.75f),
        R_ANKLE to (0.5f to 0.95f),
    )

    /** The left knee bent, the shin going out. */
    val leftKneeBent = standingArmsOut.with(L_ANKLE to (0.75f to 0.85f))

    /** Everything above the knees only: half of the body, as when the person stands too close. */
    val upperBodyOnly = standingArmsOut.without(listOf(25, 26, 27, 28, 29, 30, 31, 32))

    val upperBodyOnlyLeftArmDown = leftArmDown.without(listOf(25, 26, 27, 28, 29, 30, 31, 32))

    /** Everything but the ankles and feet. */
    val withoutAnkles = standingArmsOut.without(listOf(27, 28, 29, 30, 31, 32))

    /** A few stray landmarks with low confidence: what ML Kit tends to return for a wall, a ceiling or furniture. */
    fun objectsOnly(): BodyPose = BodyPose(
        landmarks = mapOf(
            0 to BodyLandmark(0.3f, 0.2f, 0.3f),
            L_SHOULDER to BodyLandmark(0.6f, 0.3f, 0.45f),
            R_SHOULDER to BodyLandmark(0.4f, 0.32f, 0.35f),
            L_HIP to BodyLandmark(0.55f, 0.6f, 0.4f),
            R_HIP to BodyLandmark(0.45f, 0.6f, 0.3f),
        ),
        mirrored = false,
    )

    /** A body with every landmark at the in-frame likelihood 0.2. */
    val lowConfidence: BodyPose get() = standingArmsOut.body(confidence = 0.2f)

    /** No landmark at all. One object, so that sending it twice sends equal frames. */
    val empty = BodyPose(emptyMap(), mirrored = false)

    /**
     * [standingArmsOut] with every arm and leg segment turned by the same angle about its first joint, so that the
     * score against [standingArmsOut] is exactly [score] (each segment is off by 60 * (1 - score / 100) degrees).
     */
    fun withScore(score: Int): Shape {
        val angle = Math.toRadians(60.0 * (1.0 - score / 100.0))
        val cos = cos(angle).toFloat()
        val sin = sin(angle).toFloat()
        val base = STANDING_ARMS_OUT
        fun turned(root: Int, vararg joints: Int): Array<Pair<Int, Pair<Float, Float>>> {
            val (rx, ry) = base.getValue(root)
            return joints.map { joint ->
                val (x, y) = base.getValue(joint)
                val dx = x - rx
                val dy = y - ry
                joint to ((rx + dx * cos - dy * sin) to (ry + dx * sin + dy * cos))
            }.toTypedArray()
        }
        return standingArmsOut.with(
            *turned(L_SHOULDER, L_ELBOW, L_WRIST),
            *turned(R_SHOULDER, R_ELBOW, R_WRIST),
            *turned(L_HIP, L_KNEE, L_ANKLE),
            *turned(R_HIP, R_KNEE, R_ANKLE),
        )
    }
}
