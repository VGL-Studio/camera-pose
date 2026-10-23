package com.vglstudio.camera.pose.line.guide.feature.camera.match

import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState.Matched
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState.NoPerson
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState.Off
import com.vglstudio.camera.pose.line.guide.feature.camera.match.MatchUiState.Scoring
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.ActivePoseHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.OutlineView
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.PoseFixtures
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The matching of the camera on the JVM (spec camera, KB-5; plan T-017, section 4). Frames are sent by hand through a
 * source of the same kind as the real one; the standing pose is the template, and [TestBodyPoses.withScore] makes a
 * body that scores exactly the number asked.
 */
class PoseMatchViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val source = FakePoseFrameSource()
    private val activePose = ActivePoseHolder()
    private val flags = FakeFeatureFlags()
    private val settings = FakePoseMatchingSettings()
    private val holder = PoseMatchHolder()
    private val logger = RecordingAppLogger()

    private fun viewModel() = PoseMatchViewModel(
        frameSource = source,
        activePose = activePose,
        featureFlags = flags,
        settings = settings,
        matchHolder = holder,
        logger = logger,
    )

    /** A view model with a pose that has keypoints, the flag on and the outline visible: the source is active. */
    private fun runningViewModel(): PoseMatchViewModel {
        activePose.set(poseWithKeypoints())
        return viewModel()
    }

    private fun send(shape: TestBodyPoses.Shape) {
        assertTrue("nobody collects the frames", source.hasCollector)
        source.send(shape.body())
    }

    private fun sendScore(score: Int) = send(TestBodyPoses.withScore(score))

    private fun sendNoPerson(frame: BodyPose = TestBodyPoses.empty) {
        assertTrue("nobody collects the frames", source.hasCollector)
        source.send(frame)
    }

    // Conditions

    @Test
    fun allOn_activatesSource() {
        val viewModel = runningViewModel()
        assertTrue(source.active)
        // Active but nothing seen yet: nothing is shown.
        assertEquals(Off, viewModel.uiState.value)
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    @Test
    fun flagOff_isOff_andSourceInactive() {
        flags.value.value = false
        activePose.set(poseWithKeypoints())
        val viewModel = viewModel()
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
        assertFalse(source.hasCollector)
        // The control: the same pose with the flag on runs.
        flags.value.value = true
        assertTrue(source.active)
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    @Test
    fun poseWithoutKeypoints_isOff() {
        activePose.set(PoseFixtures.pose("w01"))
        val viewModel = viewModel()
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
        activePose.set(null)
        assertFalse(source.active)
        // The control: a pose with keypoints runs.
        activePose.set(poseWithKeypoints())
        assertTrue(source.active)
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    @Test
    fun outlineHidden_isOff_andSourceInactive() {
        activePose.set(poseWithKeypoints())
        activePose.setOutline(OutlineView(visible = false))
        val viewModel = viewModel()
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
        // The control: showing the outline again runs it.
        activePose.setOutline(OutlineView(visible = true))
        assertTrue(source.active)
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    @Test
    fun conditionChanges_whileRunning_deactivateAndGoOff() {
        val viewModel = runningViewModel()
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)

        flags.value.value = false
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
        flags.value.value = true
        assertTrue(source.active)

        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
        activePose.setOutline(OutlineView(visible = false))
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
        activePose.setOutline(OutlineView(visible = true))
        assertTrue(source.active)

        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
        activePose.set(PoseFixtures.pose("w02"))
        assertFalse(source.active)
        assertEquals(Off, viewModel.uiState.value)
    }

    // No person

    @Test
    fun threeFramesWithoutPerson_isNoPerson() = runTest {
        val viewModel = runningViewModel()
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)

        // One object sent three times: equal frames, which a StateFlow would have merged into one.
        val empty = TestBodyPoses.empty
        sendNoPerson(empty)
        assertTrue(viewModel.uiState.value is Scoring)
        sendNoPerson(empty)
        assertTrue(viewModel.uiState.value is Scoring)
        sendNoPerson(empty)
        assertEquals(NoPerson, viewModel.uiState.value)
    }

    @Test
    fun twoFramesWithoutPerson_thenAPerson_staysScoring() = runTest {
        val viewModel = runningViewModel()
        sendScore(70)
        sendNoPerson()
        sendNoPerson()
        sendScore(70)
        sendNoPerson()
        sendNoPerson()
        // The count started again after the person came back.
        assertTrue(viewModel.uiState.value is Scoring)
        sendNoPerson()
        assertEquals(NoPerson, viewModel.uiState.value)
    }

    @Test
    fun lowConfidenceFrames_areNoPerson() {
        val viewModel = runningViewModel()
        sendScore(70)
        repeat(3) { sendNoPerson(TestBodyPoses.lowConfidence) }
        assertEquals(NoPerson, viewModel.uiState.value)
        repeat(3) { sendNoPerson(TestBodyPoses.objectsOnly()) }
        assertEquals(NoPerson, viewModel.uiState.value)
    }

    @Test
    fun personComesBack_afterNoPerson_scoresAgain() {
        val viewModel = runningViewModel()
        repeat(3) { sendNoPerson() }
        assertEquals(NoPerson, viewModel.uiState.value)
        sendScore(70)
        assertEquals(70, (viewModel.uiState.value as Scoring).score)
    }

    @Test
    fun remoteMinInFrame_isUsed() {
        settings.inFramePercent.value = 95
        val viewModel = runningViewModel()
        // The landmarks are at 0.9: not enough for 95 percent.
        repeat(3) { sendScore(70) }
        assertEquals(NoPerson, viewModel.uiState.value)
        settings.inFramePercent.value = 50
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    // Score and hint

    @Test
    fun personPresent_isScoring_withSmoothedScore() {
        // A threshold above both scores, so that the second frame is still Scoring.
        settings.threshold.value = 90
        val viewModel = runningViewModel()
        sendScore(70)
        assertEquals(70, (viewModel.uiState.value as Scoring).score)
        sendScore(90)
        // Half of the new score and half of the old smoothed one.
        assertEquals(80, (viewModel.uiState.value as Scoring).score)
    }

    @Test
    fun hint_followsTheWorstPart() {
        val viewModel = runningViewModel()
        send(TestBodyPoses.leftArmDown)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, (viewModel.uiState.value as Scoring).hint)
    }

    @Test
    fun hint_changesOnlyAfterThreeFrames() {
        val viewModel = runningViewModel()
        send(TestBodyPoses.leftArmDown)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, (viewModel.uiState.value as Scoring).hint)
        // Another part is the worst: the text waits for three frames in a row.
        send(TestBodyPoses.rightArmDown)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, (viewModel.uiState.value as Scoring).hint)
        send(TestBodyPoses.rightArmDown)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, (viewModel.uiState.value as Scoring).hint)
        send(TestBodyPoses.rightArmDown)
        assertEquals(MatchHintText.RAISE_RIGHT_ARM, (viewModel.uiState.value as Scoring).hint)
    }

    @Test
    fun hint_interruptedChange_startsAgain() {
        val viewModel = runningViewModel()
        send(TestBodyPoses.leftArmDown)
        send(TestBodyPoses.rightArmDown)
        send(TestBodyPoses.rightArmDown)
        // The old part is the worst again: the count of the other one is lost.
        send(TestBodyPoses.leftArmDown)
        send(TestBodyPoses.rightArmDown)
        send(TestBodyPoses.rightArmDown)
        assertEquals(MatchHintText.RAISE_LEFT_ARM, (viewModel.uiState.value as Scoring).hint)
        send(TestBodyPoses.rightArmDown)
        assertEquals(MatchHintText.RAISE_RIGHT_ARM, (viewModel.uiState.value as Scoring).hint)
    }

    // Matched

    @Test
    fun reachingThreshold_isMatched_andHolderTrue() {
        val viewModel = runningViewModel()
        sendScore(70)
        assertTrue(viewModel.uiState.value is Scoring)
        assertFalse(holder.matched.value)
        // Smoothed: (95 + 70) / 2 = 82.5, rounded to 83.
        sendScore(95)
        assertEquals(Matched(83), viewModel.uiState.value)
        assertTrue(holder.matched.value)
    }

    @Test
    fun matched_leavesOnlyBelowThresholdMinus5() {
        val viewModel = runningViewModel()
        sendScore(90)
        assertEquals(Matched(90), viewModel.uiState.value)
        // 0.5 * 66 + 0.5 * 90 = 78: below the threshold of 80 but not below 75.
        sendScore(66)
        assertEquals(Matched(78), viewModel.uiState.value)
        assertTrue(holder.matched.value)
        // 0.5 * 70 + 0.5 * 78 = 74.
        sendScore(70)
        assertEquals(74, (viewModel.uiState.value as Scoring).score)
        assertFalse(holder.matched.value)
    }

    @Test
    fun scoringBetween75And80_doesNotBecomeMatched() {
        val viewModel = runningViewModel()
        sendScore(78)
        assertEquals(78, (viewModel.uiState.value as Scoring).score)
        assertFalse(holder.matched.value)
    }

    @Test
    fun remoteThreshold_isUsed() {
        settings.threshold.value = 60
        val viewModel = runningViewModel()
        sendScore(65)
        assertEquals(Matched(65), viewModel.uiState.value)
        assertTrue(holder.matched.value)
        // The threshold follows the setting while running.
        settings.threshold.value = 90
        sendScore(65)
        assertTrue(viewModel.uiState.value is Scoring)
        assertFalse(holder.matched.value)
    }

    // Camera closed and pose change

    @Test
    fun framesNull_isOff() {
        val viewModel = runningViewModel()
        sendScore(90)
        assertTrue(holder.matched.value)
        assertTrue(source.hasCollector)
        source.send(null)
        assertEquals(Off, viewModel.uiState.value)
        assertFalse(holder.matched.value)
        // Frames after the camera opened again are scored.
        sendScore(70)
        assertEquals(70, (viewModel.uiState.value as Scoring).score)
    }

    @Test
    fun poseChange_resetsSmoothing() {
        settings.threshold.value = 95
        val viewModel = runningViewModel()
        sendScore(90)
        assertEquals(90, (viewModel.uiState.value as Scoring).score)
        activePose.set(poseWithKeypoints(id = "w02"))
        assertTrue(source.active)
        // Without the reset this would be 80.
        sendScore(70)
        assertEquals(70, (viewModel.uiState.value as Scoring).score)
    }

    @Test
    fun poseChange_whileMatched_isNotMatchedAnyMore() {
        val viewModel = runningViewModel()
        sendScore(90)
        assertTrue(holder.matched.value)
        activePose.set(poseWithKeypoints(id = "w02"))
        assertEquals(Off, viewModel.uiState.value)
        assertFalse(holder.matched.value)
    }

    @Test
    fun flippedOutline_comparesTheSidesOnScreen() {
        // The outline has the left arm down; flipped, it is on the left of the screen, the right arm of the person.
        activePose.set(poseWithKeypoints(template = TestBodyPoses.leftArmDown))
        activePose.setOutline(OutlineView(visible = true, mirrored = true))
        val viewModel = viewModel()
        send(TestBodyPoses.rightArmDown)
        assertTrue(viewModel.uiState.value is Matched)
        activePose.setOutline(OutlineView(visible = true, mirrored = false))
        send(TestBodyPoses.rightArmDown)
        assertTrue(viewModel.uiState.value is Scoring)
    }

    @Test
    fun halfBodyPose_needsNoLegs() {
        activePose.set(poseWithKeypoints(framing = Framing.HALF))
        val viewModel = viewModel()
        send(TestBodyPoses.upperBodyOnly)
        assertTrue(viewModel.uiState.value is Matched)
    }

    // Log

    @Test
    fun logs_theChangesOfState_notEveryFrame() {
        val viewModel = runningViewModel()
        sendScore(90)
        sendScore(90)
        sendScore(90)
        assertEquals(1, logger.messages("Pose matched").size)
        assertTrue(logger.messages("Pose matching on").isNotEmpty())
        flags.value.value = false
        assertTrue(logger.messages("Pose matching off: flag").isNotEmpty())
        activePose.set(PoseFixtures.pose("w02"))
        assertTrue(viewModel.uiState.value is Off)
        flags.value.value = true
        assertTrue(logger.messages("Pose matching off: no keypoints").isNotEmpty())
        assertTrue(logger.entries.all { it.level != LogLevel.ERROR })
    }

    private fun RecordingAppLogger.messages(prefix: String) = entries.filter { it.message.startsWith(prefix) }
}
