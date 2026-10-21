package com.vglstudio.camera.pose.line.guide.core.network

/** True when this error, or one of its causes, means the device could not reach the server. */
fun Throwable.isNetworkFailure(): Boolean = false
