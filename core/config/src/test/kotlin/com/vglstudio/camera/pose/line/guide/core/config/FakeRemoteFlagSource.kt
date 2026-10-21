package com.vglstudio.camera.pose.line.guide.core.config

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map

/** Remote values held in memory: a key missing from a map has no remote value. */
internal class FakeRemoteFlagSource : RemoteFlagSource {
    val booleans = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val longs = MutableStateFlow<Map<String, Long>>(emptyMap())

    override fun boolean(key: String): Flow<Boolean?> = booleans.map { it[key] }

    override fun long(key: String): Flow<Long?> = longs.map { it[key] }
}
