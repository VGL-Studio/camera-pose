package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

/** The sizes of the native pools (P-02, plan T-028). */
class AdFormatPoolTest {
    @Test
    fun poolCapacity_followsP02() {
        assertEquals(5, AdFormat.NATIVE_IN_COLLECTION.poolCapacity())
        assertEquals(3, AdFormat.NATIVE_MEDIUM.poolCapacity())
        assertEquals(3, AdFormat.NATIVE_STEP.poolCapacity())
        val pooled = setOf(AdFormat.NATIVE_IN_COLLECTION, AdFormat.NATIVE_MEDIUM, AdFormat.NATIVE_STEP)
        AdFormat.entries.filterNot { it in pooled }.forEach { assertNull(it.name, it.poolCapacity()) }
        assertEquals(pooled, AdFormat.entries.filter { it.usesPool }.toSet())
    }
}
