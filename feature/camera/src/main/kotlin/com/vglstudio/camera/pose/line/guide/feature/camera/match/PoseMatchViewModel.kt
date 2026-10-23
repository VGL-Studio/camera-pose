package com.vglstudio.camera.pose.line.guide.feature.camera.match

import androidx.lifecycle.ViewModel
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.PoseMatchingSettings
import com.vglstudio.camera.pose.line.guide.core.logging.AppLogger
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.ActivePoseHolder
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** Compares the body in the camera with the pose on screen (spec camera, KB-5). */
@HiltViewModel
class PoseMatchViewModel @Inject constructor(
    private val frameSource: PoseFrameSource,
    private val activePose: ActivePoseHolder,
    private val featureFlags: FeatureFlags,
    private val settings: PoseMatchingSettings,
    private val matchHolder: PoseMatchHolder,
    private val logger: AppLogger,
) : ViewModel() {
    val uiState: StateFlow<MatchUiState> = MutableStateFlow(MatchUiState.Off)
}
