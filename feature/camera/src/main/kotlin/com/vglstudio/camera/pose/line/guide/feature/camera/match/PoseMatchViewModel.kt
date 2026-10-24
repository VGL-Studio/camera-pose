package com.vglstudio.camera.pose.line.guide.feature.camera.match

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.PoseMatchingSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.model.Keypoint
import com.vglstudio.camera.pose.line.guide.core.poses.model.Pose
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.ActivePoseHolder
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.OutlineView
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlin.math.roundToInt
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/**
 * Compares the body in the camera with the pose on screen (spec camera, KB-5; plan T-017). It runs only while the flag
 * is on, the pose has keypoints and the outline is visible; then it takes the frames of the [PoseFrameSource], decides
 * whether a person is in the frame, scores the body, smooths the score and keeps the hint steady. The decisions are
 * here and in the pure functions of PoseScorer; ML Kit stays in the analyzer.
 */
@HiltViewModel
class PoseMatchViewModel @Inject constructor(
    private val frameSource: PoseFrameSource,
    private val activePose: ActivePoseHolder,
    private val featureFlags: FeatureFlags,
    private val settings: PoseMatchingSettings,
    private val matchHolder: PoseMatchHolder,
    private val logger: AppLogger,
) : ViewModel() {
    private val state = MutableStateFlow<MatchUiState>(MatchUiState.Off)
    val uiState: StateFlow<MatchUiState> = state.asStateFlow()

    private var threshold = DEFAULT_THRESHOLD
    private var minInFrame = DEFAULT_MIN_IN_FRAME

    // Kept for the run in progress; a new run (the conditions changed) starts them again.
    private var smoothed: Double? = null
    private var shownHint: MatchHintText? = null
    private var candidateHint: MatchHintText? = null
    private var candidateFrames = 0
    private var framesWithoutPerson = 0

    init {
        viewModelScope.launch { settings.scoreThreshold.collect { threshold = it } }
        viewModelScope.launch { settings.minInFramePercent.collect { minInFrame = it / PERCENT } }
        viewModelScope.launch {
            combine(
                featureFlags.enabled(FeatureFlag.POSE_MATCHING),
                activePose.current,
                activePose.outline,
            ) { flag, pose, outline -> Conditions(flag, pose, outline) }
                .distinctUntilChanged()
                .collectLatest { run(it) }
        }
    }

    private suspend fun run(conditions: Conditions) {
        resetRun()
        setState(MatchUiState.Off)
        val reason = conditions.inactiveReason()
        frameSource.setActive(reason == null)
        if (reason != null) {
            logger.info(LOG_TAG, "Pose matching off: $reason")
            return
        }
        logger.info(LOG_TAG, "Pose matching on")
        val pose = checkNotNull(conditions.pose)
        val template = checkNotNull(pose.keypoints)
        frameSource.frames.collect { frame -> onFrame(frame, pose, template, conditions.outline) }
    }

    private fun onFrame(frame: BodyPose?, pose: Pose, template: List<Keypoint>, outline: OutlineView) {
        if (frame == null) {
            resetRun()
            setState(MatchUiState.Off)
            return
        }
        if (!isPersonPresent(frame, pose.framing, minInFrame)) {
            onFrameWithoutPerson()
            return
        }
        val result = score(template, outline.mirrored, frame, minInFrame)
        // A score without a hint of its own keeps the hint shown. No part of the body to compare at all (a close pose
        // with the elbows out of the frame) is a call to step back.
        val worst = result.worst ?: shownHint?.takeIf { result.score > 0 } ?: run {
            onFrameWithoutPerson()
            return
        }
        framesWithoutPerson = 0
        val smoothedNow = smoothed?.let { SMOOTHING * result.score + (1 - SMOOTHING) * it } ?: result.score.toDouble()
        smoothed = smoothedNow
        val shownScore = smoothedNow.roundToInt()
        val hint = steadyHint(worst)
        val matched = state.value is MatchUiState.Matched
        val reached = if (matched) shownScore >= threshold - EXIT_MARGIN else shownScore >= threshold
        setState(if (reached) MatchUiState.Matched(shownScore) else MatchUiState.Scoring(shownScore, hint))
    }

    private fun onFrameWithoutPerson() {
        framesWithoutPerson++
        if (framesWithoutPerson >= NO_PERSON_FRAMES) {
            resetScoring()
            setState(MatchUiState.NoPerson)
        }
    }

    /** The hint changes only when another part has been the worst for three frames in a row. */
    private fun steadyHint(worst: MatchHintText): MatchHintText {
        val shown = shownHint
        if (shown == null || worst == shown) {
            shownHint = shown ?: worst
            candidateHint = null
            candidateFrames = 0
        } else {
            if (worst == candidateHint) {
                candidateFrames++
            } else {
                candidateHint = worst
                candidateFrames = 1
            }
            if (candidateFrames >= HINT_FRAMES) {
                shownHint = worst
                candidateHint = null
                candidateFrames = 0
            }
        }
        return checkNotNull(shownHint)
    }

    private fun setState(new: MatchUiState) {
        val wasMatched = state.value is MatchUiState.Matched
        state.value = new
        val matched = new is MatchUiState.Matched
        matchHolder.set(matched)
        if (matched && !wasMatched) logger.info(LOG_TAG, "Pose matched")
    }

    private fun resetScoring() {
        smoothed = null
        shownHint = null
        candidateHint = null
        candidateFrames = 0
    }

    private fun resetRun() {
        resetScoring()
        framesWithoutPerson = 0
    }

    override fun onCleared() {
        frameSource.setActive(false)
    }

    private data class Conditions(val flag: Boolean, val pose: Pose?, val outline: OutlineView) {
        fun inactiveReason(): String? = when {
            !flag -> "flag"
            pose?.keypoints == null -> "no keypoints"
            !outline.visible -> "outline hidden"
            else -> null
        }
    }

    private companion object {
        const val LOG_TAG = "Camera"
        const val DEFAULT_THRESHOLD = 80
        const val DEFAULT_MIN_IN_FRAME = 0.5f
        const val PERCENT = 100f
        const val NO_PERSON_FRAMES = 3
        const val HINT_FRAMES = 3
        const val SMOOTHING = 0.5
        const val EXIT_MARGIN = 5
    }
}
