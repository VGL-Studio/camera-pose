package com.vglstudio.camera.pose.line.guide.core.network

import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

private const val MAX_CAUSE_DEPTH = 10

/**
 * True when this error, or one of its causes, means the device could not reach the server: unknown host, refused or
 * reset connection, no route, timeout. An HTTP error answered by the server is not one.
 */
fun Throwable.isNetworkFailure(): Boolean = generateSequence(this) { it.cause }
    .take(MAX_CAUSE_DEPTH)
    .any { it is UnknownHostException || it is SocketException || it is SocketTimeoutException }
