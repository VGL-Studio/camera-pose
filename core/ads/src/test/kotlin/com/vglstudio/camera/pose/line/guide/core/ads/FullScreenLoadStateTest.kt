package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Idle
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Loading
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Ready
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.WaitingRetry
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** F-04: "load if needed" never loads while loading, ready, or waiting for a retry that is not due. */
class FullScreenLoadStateTest {
    @Test
    fun needsLoad_onlyWhenIdleOrRetryDue() {
        assertTrue(needsLoad(Idle, now = 0, gateOpen = true))
        assertFalse(needsLoad(Loading, now = 0, gateOpen = true))
        assertFalse(needsLoad(Ready, now = 0, gateOpen = true))
        assertFalse(needsLoad(WaitingRetry(10_000), now = 9_999, gateOpen = true))
        assertTrue(needsLoad(WaitingRetry(10_000), now = 10_000, gateOpen = true))
    }

    @Test
    fun retryDueWhileGateClosed_isNotWaiting() {
        // The retry fell due while the consent gate was closed: nothing may load then.
        assertFalse(needsLoad(WaitingRetry(10_000), now = 12_000, gateOpen = false))
        // The gate opens: the slot no longer counts as waiting for a retry.
        assertTrue(needsLoad(WaitingRetry(10_000), now = 12_000, gateOpen = true))
    }
}
