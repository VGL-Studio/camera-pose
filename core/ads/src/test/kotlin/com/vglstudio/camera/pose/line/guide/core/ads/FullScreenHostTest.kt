package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * The pure rules of the activity that shows a full-screen ad (KB-26, criteria 3 and 4; plan T-055): the host is usable
 * only when it is not finishing, not destroyed and resumed; its destruction for a configuration change hands the turn
 * over, a real destruction forces the completion (N-04, HD-03).
 */
class FullScreenHostTest {
    @Test
    fun hostUsable_onlyWhenAliveAndResumed() {
        val usable = mutableListOf<Triple<Boolean, Boolean, Boolean>>()
        for (finishing in listOf(false, true)) {
            for (destroyed in listOf(false, true)) {
                for (resumed in listOf(false, true)) {
                    if (fullScreenHostUsable(finishing, destroyed, resumed)) {
                        usable += Triple(finishing, destroyed, resumed)
                    }
                }
            }
        }
        assertEquals(listOf(Triple(false, false, true)), usable)
    }

    @Test
    fun hostDestroyedAction_configChange_handsOver_realDestroy_forces() {
        val host = 41
        assertEquals(HostDestroyedAction.HAND_OVER, hostDestroyedAction(host, host, changingConfigurations = true))
        assertEquals(
            HostDestroyedAction.FORCE_COMPLETE,
            hostDestroyedAction(host, host, changingConfigurations = false),
        )
        // Another activity, or no turn with a host, is not the host.
        assertEquals(HostDestroyedAction.IGNORE, hostDestroyedAction(7, host, changingConfigurations = false))
        assertEquals(HostDestroyedAction.IGNORE, hostDestroyedAction(7, host, changingConfigurations = true))
        assertEquals(HostDestroyedAction.IGNORE, hostDestroyedAction(host, null, changingConfigurations = true))
        assertEquals(HostDestroyedAction.IGNORE, hostDestroyedAction(host, null, changingConfigurations = false))
    }
}
