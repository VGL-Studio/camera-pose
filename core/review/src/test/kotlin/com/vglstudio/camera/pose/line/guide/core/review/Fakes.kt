package com.vglstudio.camera.pose.line.guide.core.review

import android.app.Activity
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.emptyPreferences
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.config.RemoteKey
import com.vglstudio.camera.pose.line.guide.core.config.RemoteSettings
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map

/** Counts the calls; [error] makes the next calls throw. */
internal class FakeReviewPrompt : ReviewPrompt {
    var calls = 0
    var error: Throwable? = null

    override suspend fun show(activity: Activity) {
        error?.let { throw it }
        calls++
    }
}

internal class FakeFeatureFlags(enabled: Set<FeatureFlag> = emptySet()) : FeatureFlags {
    val enabled = MutableStateFlow(enabled)

    override fun enabled(flag: FeatureFlag): Flow<Boolean> = enabled.map { flag in it }
}

/** A key missing from [longs] has no remote value, so the default is used. */
internal class FakeRemoteSettings : RemoteSettings {
    val longs = MutableStateFlow<Map<String, Long>>(emptyMap())

    // Safe cast: a LONG key is a RemoteKey<Long> (factories of RemoteKey).
    @Suppress("UNCHECKED_CAST")
    override fun <T : Any> value(key: RemoteKey<T>): Flow<T> = when (key.type) {
        RemoteKey.Type.LONG -> longs.map { (it[key.name] ?: key.default) as T }
        else -> MutableStateFlow(key.default)
    }
}

internal class FakeReviewHistory : ReviewHistory {
    var last: Long? = null

    override suspend fun lastAskedAt(): Long? = last

    override suspend fun recordAsked(atMillis: Long) {
        last = atMillis
    }
}

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
