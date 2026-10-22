package com.vglstudio.camera.pose.line.guide.core.network

import java.io.FileNotFoundException
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.SocketException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NetworkErrorsTest {
    @Test
    fun connectionErrors_areNetworkFailures() {
        listOf(
            UnknownHostException("assets.example.com"),
            ConnectException("refused"),
            NoRouteToHostException("no route"),
            SocketTimeoutException("timeout"),
            SocketException("reset"),
        ).forEach { assertTrue("$it", it.isNetworkFailure()) }
    }

    @Test
    fun otherErrors_areNotNetworkFailures() {
        listOf(
            IOException("HTTP 404 for https://assets.example.com/a.json"),
            FileNotFoundException("a.json"),
            IOException("Could not move a.json into the cache"),
        ).forEach { assertFalse("$it", it.isNetworkFailure()) }
    }

    @Test
    fun networkErrorAsCause_isNetworkFailure() {
        assertTrue(IOException("download failed", UnknownHostException("assets.example.com")).isNetworkFailure())
    }
}
