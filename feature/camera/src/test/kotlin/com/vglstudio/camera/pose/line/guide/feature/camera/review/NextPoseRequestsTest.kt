package com.vglstudio.camera.pose.line.guide.feature.camera.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NextPoseRequestsTest {
    @Test
    fun consume_returnsOnceThenFalse() {
        val requests = NextPoseRequests()
        requests.request()
        assertTrue(requests.consume())
        assertFalse(requests.consume())
    }

    @Test
    fun consume_withoutRequest_isFalse() {
        val requests = NextPoseRequests()
        assertFalse(requests.consume())
        // Control: a request is then seen, so false came from the missing request.
        requests.request()
        assertTrue(requests.consume())
    }
}
