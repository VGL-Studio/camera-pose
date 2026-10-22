package com.vglstudio.camera.pose.line.guide.feature.camera.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.mutablePreferencesOf
import androidx.datastore.preferences.core.stringPreferencesKey
import com.vglstudio.camera.pose.line.guide.feature.camera.model.CameraSettings
import com.vglstudio.camera.pose.line.guide.feature.camera.model.FlashMode
import com.vglstudio.camera.pose.line.guide.feature.camera.model.Lens
import com.vglstudio.camera.pose.line.guide.feature.camera.model.TimerSetting
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

class DataStoreCameraSettingsRepositoryTest {
    @Test
    fun nothingStored_returnsDefaults() = runTest {
        val repository = DataStoreCameraSettingsRepository(InMemoryDataStore())
        assertEquals(
            CameraSettings(FlashMode.OFF, TimerSetting.OFF, gridVisible = false, lens = Lens.BACK),
            repository.settings.first(),
        )
    }

    @Test
    fun eachSetter_isReadBack() = runTest {
        val repository = DataStoreCameraSettingsRepository(InMemoryDataStore())
        repository.setFlash(FlashMode.AUTO)
        repository.setTimer(TimerSetting.TEN)
        repository.setGridVisible(true)
        repository.setLens(Lens.FRONT)
        assertEquals(
            CameraSettings(FlashMode.AUTO, TimerSetting.TEN, gridVisible = true, lens = Lens.FRONT),
            repository.settings.first(),
        )
    }

    @Test
    fun unknownStoredValue_fallsBackToDefault() = runTest {
        val stored = mutablePreferencesOf(
            stringPreferencesKey("camera_flash") to "BLINK",
            intPreferencesKey("camera_timer") to 7,
            stringPreferencesKey("camera_lens") to "SIDE",
        )
        val repository = DataStoreCameraSettingsRepository(InMemoryDataStore(stored))
        assertEquals(CameraSettings(), repository.settings.first())
    }

    @Test
    fun readFailsOnce_recovers() = runTest {
        val stored = mutablePreferencesOf(intPreferencesKey("camera_timer") to 3)
        val repository = DataStoreCameraSettingsRepository(FlakyDataStore(failuresBeforeSuccess = 1, stored))
        assertEquals(
            listOf(CameraSettings(), CameraSettings(timer = TimerSetting.THREE)),
            repository.settings.take(2).toList(),
        )
    }
}

private class InMemoryDataStore(initial: Preferences = emptyPreferences()) : DataStore<Preferences> {
    private val state = MutableStateFlow(initial)
    override val data: Flow<Preferences> = state

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}

/** Fails to read [failuresBeforeSuccess] times, then serves [stored] (a transient I/O error). */
private class FlakyDataStore(private var failuresBeforeSuccess: Int, private val stored: Preferences) :
    DataStore<Preferences> {
    override val data: Flow<Preferences> = flow {
        if (failuresBeforeSuccess > 0) {
            failuresBeforeSuccess--
            throw IOException("transient read failure")
        }
        emit(stored)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences = transform(stored)
}
