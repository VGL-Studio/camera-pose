package com.vglstudio.camera.pose.line.guide.core.poses

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow

/** Preferences in memory. [failWrites] makes every write throw; [readFailures] makes the next reads throw. */
internal class InMemoryDataStore : DataStore<Preferences> {
    private val state = MutableStateFlow<Preferences>(emptyPreferences())

    var failWrites = false
    var readFailures = 0

    override val data: Flow<Preferences> = flow {
        if (readFailures > 0) {
            readFailures--
            throw IOException("read failed")
        }
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        if (failWrites) throw IOException("write failed")
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
