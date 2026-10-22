package com.vglstudio.camera.pose.line.guide.feature.camera.data

import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import kotlinx.coroutines.flow.Flow

/** Stores the capture tools. Every setter throws [java.io.IOException] when the value cannot be written. */
interface CameraSettingsRepository {
    val settings: Flow<CameraSettings>

    suspend fun setFlash(mode: FlashMode)

    suspend fun setTimer(timer: TimerSetting)

    suspend fun setGridVisible(visible: Boolean)

    suspend fun setLens(lens: Lens)
}
