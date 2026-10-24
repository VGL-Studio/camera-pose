package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.feature.camera.match.TestBodyPoses.Shape
import kotlin.math.abs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The scoring of a body against a pose, on synthetic bodies (spec camera, KB-5; plan T-017, section 4). Pure Kotlin:
 * no image, no ML Kit. The body is the same one the template is made of unless a test says otherwise.
 */
class PoseScorerTest {
    private val standing = TestBodyPoses.standingArmsOut

    private fun scoreOf(template: Shape, detected: BodyPose, templateMirrored: Boolean = false) =
        score(template.keypoints(), templateMirrored, detected, MIN_IN_FRAME)

    private fun present(pose: BodyPose?, framing: Framing) = isPersonPresent(pose, framing, MIN_IN_FRAME)

    // Is there a person

    @Test
    fun noLandmarks_isNotPresent() {
        assertFalse(present(TestBodyPoses.empty, Framing.FULL))
        assertFalse(present(null, Framing.FULL))
        // The same frame with a person in it is present.
        assertTrue(present(standing.body(), Framing.FULL))
    }

    @Test
    fun lowConfidence_isNotPresent() {
        assertFalse(present(TestBodyPoses.lowConfidence, Framing.FULL))
        assertFalse(present(TestBodyPoses.lowConfidence, Framing.CLOSE))
        assertTrue(present(standing.body(confidence = 0.5f), Framing.FULL))
        assertTrue(present(standing.body(confidence = 0.9f), Framing.FULL))
    }

    @Test
    fun fullBodyPose_withoutAnkles_isNotPresent() {
        val withoutAnkles = TestBodyPoses.withoutAnkles.body()
        assertFalse(present(withoutAnkles, Framing.FULL))
        // The same body is enough for a half body pose.
        assertTrue(present(withoutAnkles, Framing.HALF))
        assertTrue(present(standing.body(), Framing.FULL))
    }

    @Test
    fun halfBodyPose_withoutLegs_isPresent() {
        val upper = TestBodyPoses.upperBodyOnly.body()
        assertTrue(present(upper, Framing.HALF))
        assertFalse(present(upper, Framing.FULL))
        // Half body needs the hips as well.
        assertFalse(present(TestBodyPoses.upperBodyOnly.without(listOf(23, 24)).body(), Framing.HALF))
    }

    @Test
    fun closePose_needsNoseAndShoulders() {
        assertTrue(present(TestBodyPoses.upperBodyOnly.without(listOf(23, 24)).body(), Framing.CLOSE))
        assertFalse(present(standing.without(listOf(0)).body(), Framing.CLOSE))
        assertFalse(present(standing.without(listOf(TestBodyPoses.L_SHOULDER)).body(), Framing.CLOSE))
    }

    @Test
    fun objectsOnly_isNotPresent() {
        Framing.entries.forEach { framing ->
            assertFalse("$framing", present(TestBodyPoses.objectsOnly(), framing))
        }
        assertTrue(present(standing.body(), Framing.FULL))
    }

    // The score

    @Test
    fun samePose_scoresAtLeast95() {
        val result = scoreOf(standing, standing.body())
        assertTrue("score ${result.score}", result.score >= 95)
        assertNotNull(result.worst)
    }

    @Test
    fun samePose_movedAndScaled_scoresTheSame() {
        val plain = scoreOf(standing, standing.body())
        val moved = scoreOf(standing, standing.movedAndScaled(scale = 0.6f, dx = 0.2f, dy = 0.2f).body())
        assertTrue("plain ${plain.score}", plain.score >= 95)
        assertTrue("plain ${plain.score}, moved ${moved.score}", abs(plain.score - moved.score) <= 1)
    }

    @Test
    fun leftArmDown_scoresLower_andHintsRaiseLeftArm() {
        val result = scoreOf(standing, TestBodyPoses.leftArmDown.body())
        assertTrue("score ${result.score}", result.score in 60..90)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, result.worst)
        // The same body against its own pose is better (the control of the two numbers above).
        assertTrue(scoreOf(TestBodyPoses.leftArmDown, TestBodyPoses.leftArmDown.body()).score > result.score)
    }

    @Test
    fun rightArmDown_hintsRaiseRightArm() {
        val result = scoreOf(standing, TestBodyPoses.rightArmDown.body())
        assertTrue("score ${result.score}", result.score in 60..90)
        assertEquals(MatchHintText.RAISE_RIGHT_ARM, result.worst)
    }

    @Test
    fun leftArmUp_hintsLowerLeftArm() {
        val result = scoreOf(standing, TestBodyPoses.leftArmUp.body())
        assertTrue("score ${result.score}", result.score < 100)
        assertEquals(MatchHintText.LOWER_LEFT_ARM, result.worst)
    }

    @Test
    fun elbowMoreBentThanPose_hintsStraightenArm_andTheOtherWayBendElbow() {
        assertEquals(MatchHintText.STRAIGHTEN_LEFT_ARM, scoreOf(standing, TestBodyPoses.leftElbowBent.body()).worst)
        assertEquals(MatchHintText.BEND_LEFT_ELBOW, scoreOf(TestBodyPoses.leftElbowBent, standing.body()).worst)
    }

    @Test
    fun legsWiderThanPose_hintsCloseLeg_andTheOtherWayWidenLeg() {
        assertEquals(MatchHintText.CLOSE_LEFT_LEG, scoreOf(standing, TestBodyPoses.legsApart.body()).worst)
        assertEquals(MatchHintText.WIDEN_LEFT_LEG, scoreOf(TestBodyPoses.legsApart, standing.body()).worst)
        // Closer than the pose: the left leg first.
        assertEquals(
            MatchHintText.WIDEN_LEFT_LEG,
            scoreOf(TestBodyPoses.legsApart, TestBodyPoses.legsClose.body()).worst,
        )
    }

    @Test
    fun kneeMoreBentThanPose_hintsStraightenLeg_andTheOtherWayBendKnee() {
        assertEquals(MatchHintText.STRAIGHTEN_LEFT_LEG, scoreOf(standing, TestBodyPoses.leftKneeBent.body()).worst)
        assertEquals(MatchHintText.BEND_LEFT_KNEE, scoreOf(TestBodyPoses.leftKneeBent, standing.body()).worst)
    }

    @Test
    fun oppositeArms_scoresBelow50() {
        // Both arms turned the other way, and the legs apart as well.
        val result = scoreOf(TestBodyPoses.armsDown, TestBodyPoses.legsApart.body())
        assertTrue("score ${result.score}", result.score < 50)
        assertTrue(scoreOf(TestBodyPoses.armsDown, TestBodyPoses.armsDown.body()).score >= 95)
    }

    @Test
    fun score_isWithin0And100() {
        val shapes = listOf(
            standing,
            TestBodyPoses.leftArmDown,
            TestBodyPoses.rightArmDown,
            TestBodyPoses.armsDown,
            TestBodyPoses.leftArmUp,
            TestBodyPoses.leftElbowBent,
            TestBodyPoses.legsApart,
            TestBodyPoses.legsClose,
            TestBodyPoses.leftKneeBent,
            TestBodyPoses.upperBodyOnly,
        )
        var best = 0
        shapes.forEach { template ->
            shapes.forEach { detected ->
                val result = scoreOf(template, detected.body())
                assertTrue("score ${result.score}", result.score in 0..100)
                best = maxOf(best, result.score)
            }
        }
        // Not all zero.
        assertTrue(best >= 95)
    }

    @Test
    fun nothingToCompare_scoresZeroWithoutHint() {
        val result = scoreOf(standing, TestBodyPoses.empty)
        assertEquals(ScoreResult(0, null), result)
        // A template without visible limbs compares nothing either, however good the body is.
        val hidden = score(standing.keypoints(visibility = 0.1f), false, standing.body(), MIN_IN_FRAME)
        assertEquals(ScoreResult(0, null), hidden)
        assertTrue(scoreOf(standing, standing.body()).score > 0)
    }

    @Test
    fun landmarksBelowTheThreshold_areNotCompared() {
        // The left arm is not in the frame well enough: only the other six segments count, all of them perfect.
        val detected = BodyPose(
            standing.body().landmarks + mapOf(
                TestBodyPoses.L_ELBOW to BodyLandmark(0.6f, 0.5f, 0.2f),
                TestBodyPoses.L_WRIST to BodyLandmark(0.6f, 0.7f, 0.2f),
            ),
            mirrored = false,
        )
        val result = scoreOf(standing, detected)
        assertTrue("score ${result.score}", result.score >= 95)
        // With the arm well in the frame the same positions lower the score.
        assertTrue(scoreOf(standing, TestBodyPoses.leftArmDown.body()).score < 90)
    }

    // Sides

    @Test
    fun frontCamera_mirroredDetection_matchesAsInAMirror() {
        val back = scoreOf(standing, TestBodyPoses.leftArmDown.body())
        // The front camera shows the person as in a mirror: the same body, flipped, and marked so.
        val front = scoreOf(standing, TestBodyPoses.leftArmDown.flippedX().body(mirrored = true))
        assertTrue("back ${back.score}", back.score in 60..90)
        assertEquals(back.score, front.score)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, front.worst)
    }

    @Test
    fun flippedOutline_swapsSides() {
        // The outline is flipped on screen: its arm that is down is now on the left of the screen. The person's
        // right arm is on the left of the screen (back camera), so lowering the right arm matches it.
        val template = TestBodyPoses.leftArmDown
        val rightDown = scoreOf(template, TestBodyPoses.rightArmDown.body(), templateMirrored = true)
        assertTrue("score ${rightDown.score}", rightDown.score >= 95)
        // Without the swap of sides, or with the left arm down, the pose does not match.
        val leftDown = scoreOf(template, TestBodyPoses.leftArmDown.body(), templateMirrored = true)
        assertTrue("score ${leftDown.score}", leftDown.score < 90)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, leftDown.worst)
    }

    @Test
    fun bothFlipped_matchesDirectly() {
        // A flipped outline and a mirrored (front camera) body: the sides line up without a swap.
        val template = TestBodyPoses.leftArmDown
        val result = scoreOf(template, template.flippedX().body(mirrored = true), templateMirrored = true)
        assertTrue("score ${result.score}", result.score >= 95)
    }

    // Half body

    @Test
    fun upperBodyOnly_scoresArmsOnly() {
        val perfect = scoreOf(standing, TestBodyPoses.upperBodyOnly.body())
        assertTrue("score ${perfect.score}", perfect.score >= 95)
        val worse = scoreOf(standing, TestBodyPoses.upperBodyOnlyLeftArmDown.body())
        assertTrue("score ${worse.score}", worse.score in 40..60)
        // The hint is never about a leg.
        assertTrue(perfect.worst in ARM_HINTS)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, worse.worst)
    }

    private companion object {
        const val MIN_IN_FRAME = 0.5f
        val ARM_HINTS = setOf(
            MatchHintText.RAISE_LEFT_ARM,
            MatchHintText.LOWER_LEFT_ARM,
            MatchHintText.RAISE_RIGHT_ARM,
            MatchHintText.LOWER_RIGHT_ARM,
            MatchHintText.BEND_LEFT_ELBOW,
            MatchHintText.STRAIGHTEN_LEFT_ARM,
            MatchHintText.BEND_RIGHT_ELBOW,
            MatchHintText.STRAIGHTEN_RIGHT_ARM,
        )
    }
}
