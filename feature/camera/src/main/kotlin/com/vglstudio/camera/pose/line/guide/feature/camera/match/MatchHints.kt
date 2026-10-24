package com.vglstudio.camera.pose.line.guide.feature.camera.match

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.vglstudio.camera.pose.line.guide.core.designsystem.R as DesignR
import com.vglstudio.camera.pose.line.guide.core.designsystem.component.MatchHint
import com.vglstudio.camera.pose.line.guide.feature.camera.CameraTestTags
import com.vglstudio.camera.pose.line.guide.feature.camera.R

@StringRes
internal fun MatchHintText.stringRes(): Int = when (this) {
    MatchHintText.RAISE_LEFT_ARM -> R.string.camera_match_raise_left_arm
    MatchHintText.LOWER_LEFT_ARM -> R.string.camera_match_lower_left_arm
    MatchHintText.RAISE_RIGHT_ARM -> R.string.camera_match_raise_right_arm
    MatchHintText.LOWER_RIGHT_ARM -> R.string.camera_match_lower_right_arm
    MatchHintText.BEND_LEFT_ELBOW -> R.string.camera_match_bend_left_elbow
    MatchHintText.STRAIGHTEN_LEFT_ARM -> R.string.camera_match_straighten_left_arm
    MatchHintText.BEND_RIGHT_ELBOW -> R.string.camera_match_bend_right_elbow
    MatchHintText.STRAIGHTEN_RIGHT_ARM -> R.string.camera_match_straighten_right_arm
    MatchHintText.WIDEN_LEFT_LEG -> R.string.camera_match_widen_left_leg
    MatchHintText.CLOSE_LEFT_LEG -> R.string.camera_match_close_left_leg
    MatchHintText.WIDEN_RIGHT_LEG -> R.string.camera_match_widen_right_leg
    MatchHintText.CLOSE_RIGHT_LEG -> R.string.camera_match_close_right_leg
    MatchHintText.BEND_LEFT_KNEE -> R.string.camera_match_bend_left_knee
    MatchHintText.STRAIGHTEN_LEFT_LEG -> R.string.camera_match_straighten_left_leg
    MatchHintText.BEND_RIGHT_KNEE -> R.string.camera_match_bend_right_knee
    MatchHintText.STRAIGHTEN_RIGHT_LEG -> R.string.camera_match_straighten_right_leg
}

/**
 * The matching hint of the camera for [state] (design README, section 3, "Điểm khớp"): the score and the hint of the
 * worst part, the message of a match, or only the call to step back. Nothing for [MatchUiState.Off]. A screen reader
 * gets only the text (see [MatchHint]).
 */
@Composable
internal fun MatchHintOverlay(state: MatchUiState, modifier: Modifier = Modifier) {
    val tagged = modifier.testTag(CameraTestTags.MATCH_HINT)
    when (state) {
        MatchUiState.Off -> Unit
        MatchUiState.NoPerson -> MatchHint(
            text = stringResource(R.string.camera_match_no_person),
            modifier = tagged,
            icon = painterResource(DesignR.drawable.ic_person_search),
        )
        is MatchUiState.Scoring -> MatchHint(
            text = stringResource(state.hint.stringRes()),
            modifier = tagged,
            score = state.score,
        )
        is MatchUiState.Matched -> MatchHint(
            text = stringResource(R.string.camera_match_matched),
            modifier = tagged,
            score = state.score,
            matched = true,
        )
    }
}
