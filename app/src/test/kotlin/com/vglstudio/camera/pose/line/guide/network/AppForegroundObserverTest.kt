package com.vglstudio.camera.pose.line.guide.network

import android.app.Application
import androidx.activity.ComponentActivity
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.annotation.Config

/** Starts and stops of the activity reach the network dialog, telling a configuration change apart (plan T-077). */
@RunWith(AndroidJUnit4::class)
@Config(application = Application::class)
class AppForegroundObserverTest {
    @Test
    fun reportsStarts_andStops_withConfigurationChange() {
        var started = 0
        val stops = mutableListOf<Boolean>()
        val controller = Robolectric.buildActivity(ComponentActivity::class.java).create()
        controller.get().observeAppForeground(onStarted = { started++ }, onStopped = { stops += it })

        controller.start().resume()
        assertEquals(1, started)
        assertEquals(emptyList<Boolean>(), stops)

        // To the background and back (L-091).
        controller.pause().stop()
        assertEquals(listOf(false), stops)
        controller.start().resume()
        assertEquals(2, started)

        // Recreated (L-061): the old activity reports a configuration change.
        controller.recreate()
        assertEquals(listOf(false, true), stops)
    }
}
