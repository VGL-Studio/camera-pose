package com.vglstudio.camera.pose.line.guide.feature.camera.match

/** What the pose matching shows on the camera (spec camera, KB-5). */
sealed interface MatchUiState {
    /** Matching is not running: flag off, the pose has no keypoints, the outline is hidden, or the camera is closed. */
    data object Off : MatchUiState

    /** Detection runs but nobody is in the frame: no score, only the hint to step back. */
    data object NoPerson : MatchUiState

    data class Scoring(val score: Int, val hint: MatchHintText) : MatchUiState

    data class Matched(val score: Int) : MatchUiState
}

/** The body part to move, in the words of the user's own sides (labels of ML Kit, not of the screen). */
enum class MatchHintText {
    RAISE_LEFT_ARM,
    LOWER_LEFT_ARM,
    RAISE_RIGHT_ARM,
    LOWER_RIGHT_ARM,
    BEND_LEFT_ELBOW,
    STRAIGHTEN_LEFT_ARM,
    BEND_RIGHT_ELBOW,
    STRAIGHTEN_RIGHT_ARM,
    WIDEN_LEFT_LEG,
    CLOSE_LEFT_LEG,
    WIDEN_RIGHT_LEG,
    CLOSE_RIGHT_LEG,
    BEND_LEFT_KNEE,
    STRAIGHTEN_LEFT_LEG,
    BEND_RIGHT_KNEE,
    STRAIGHTEN_RIGHT_LEG,
}
