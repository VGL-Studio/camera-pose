package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/** The star row of a native ad (O-08): shown only for a real rating above 0, filled stars rounded, at most 5. */
class NativeStarCountTest {
    @Test
    fun missingOrNonPositiveRating_hasNoStars() {
        // Control: a real rating gives stars, so the zeros below are not a function that always returns 0.
        assertEquals(4, nativeStarCount(3.6))

        assertEquals(0, nativeStarCount(null))
        assertEquals(0, nativeStarCount(0.0))
        assertEquals(0, nativeStarCount(-1.0))
        assertEquals(0, nativeStarCount(Double.NaN))
    }

    @Test
    fun rating_isRoundedToWholeStars() {
        assertEquals(0, nativeStarCount(0.4))
        assertEquals(1, nativeStarCount(0.5))
        assertEquals(4, nativeStarCount(3.6))
        assertEquals(5, nativeStarCount(4.5))
    }

    @Test
    fun rating_isCappedAtFive() {
        assertEquals(5, nativeStarCount(7.0))
        assertEquals(5, nativeStarCount(Double.POSITIVE_INFINITY))
        // Control: just below the cap is not cut to 5.
        assertEquals(4, nativeStarCount(4.4))
    }
}
