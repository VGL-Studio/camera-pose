package com.vglstudio.camera.pose.line.guide.feature.camera

import kotlinx.coroutines.flow.Flow

/** True while something covers the camera (the network dialog): a running countdown stops. */
fun interface CountdownInterruptions {
    fun active(): Flow<Boolean>
}
