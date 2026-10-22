package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import javax.inject.Inject
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

internal class DataStoreCameraSettingsRepository @Inject constructor(private val dataStore: DataStore<Preferences>) :
    CameraSettingsRepository {
    override val settings: Flow<CameraSettings> = flowOf(CameraSettings())

    override suspend fun setFlash(mode: FlashMode) = Unit

    override suspend fun setTimer(timer: TimerSetting) = Unit

    override suspend fun setGridVisible(visible: Boolean) = Unit

    override suspend fun setLens(lens: Lens) = Unit
}
