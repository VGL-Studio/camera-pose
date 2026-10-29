package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** load_attempt and latency_ms (docs/ops/ads-business-rules.md, section 8.2; plan T-023). */
class LoadAttemptCounterTest {
    private val counter = LoadAttemptCounter()

    @Test
    fun attemptCountsFailuresInARow() {
        counter.onRequestSent(SLOT, 0)
        assertEquals(1, counter.onFailed(SLOT, 10).attempt)
        counter.onRequestSent(SLOT, 20)
        assertEquals(2, counter.onFailed(SLOT, 30).attempt)
        assertEquals(2, counter.failuresInARow(SLOT))
        counter.onRequestSent(SLOT, 40)
        assertEquals(3, counter.onLoaded(SLOT, 50).attempt)
        assertEquals(0, counter.failuresInARow(SLOT))
        counter.onRequestSent(SLOT, 60)
        assertEquals(1, counter.onLoaded(SLOT, 70).attempt)
        // Another slot counts on its own.
        assertEquals(0, counter.failuresInARow(OTHER))
    }

    @Test
    fun latencyFromThisRequest() {
        counter.onRequestSent(SLOT, 1_000)

        assertEquals(250L, counter.onLoaded(SLOT, 1_250).latencyMs)
    }

    @Test
    fun callbackWithoutRequest_hasNoLatency() {
        // Control (L-055): a callback after a request has a latency.
        counter.onRequestSent(SLOT, 100)
        assertEquals(50L, counter.onLoaded(SLOT, 150).latencyMs)

        // The next callback has no request of its own (banner refresh).
        assertNull(counter.onLoaded(SLOT, 2_000).latencyMs)
        assertNull(counter.onFailed(SLOT, 3_000).latencyMs)
    }

    private companion object {
        const val SLOT = "preload:INTER"
        const val OTHER = "sample_native_small"
    }
}
