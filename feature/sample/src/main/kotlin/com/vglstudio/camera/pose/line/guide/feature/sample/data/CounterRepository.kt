package com.vglstudio.camera.pose.line.guide.feature.sample.data

import kotlinx.coroutines.flow.Flow

/** Stores how many times the user tapped the sample button. */
interface CounterRepository {
    val count: Flow<Int>

    suspend fun increment()
}
