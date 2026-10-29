package com.vglstudio.camera.pose.line.guide.core.network

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Test

/** The shared Internet watch for other core modules replays the source of the network dialog (S-09, plan T-074). */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultInternetConnectivityTest {
    @Test
    fun connected_followsSource() = runTest {
        val source = FakeConnectivitySource(connected = false)
        val connectivity = DefaultInternetConnectivity(source)
        val values = mutableListOf<Boolean>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { connectivity.connected.toList(values) }
        source.connected.value = true
        source.connected.value = false
        assertEquals(listOf(false, true, false), values)
    }
}
