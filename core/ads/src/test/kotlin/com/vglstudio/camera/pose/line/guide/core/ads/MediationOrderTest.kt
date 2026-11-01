package com.vglstudio.camera.pose.line.guide.core.ads

import org.junit.Assert.assertEquals
import org.junit.Test

/** `ads_mediation_order` read as one of four orders (plan T-068, KB-31, ADR-0037). */
class MediationOrderTest {
    @Test
    fun of_readsEachValue() {
        assertEquals(MediationOrder.ADMOB, MediationOrder.of("admob"))
        assertEquals(MediationOrder.MAX, MediationOrder.of("max"))
        assertEquals(MediationOrder.ADMOB_MAX, MediationOrder.of("admob_max"))
        assertEquals(MediationOrder.MAX_ADMOB, MediationOrder.of("max_admob"))

        assertEquals(listOf(AdPlatform.MAX, AdPlatform.ADMOB), MediationOrder.MAX_ADMOB.platforms)
        assertEquals(listOf(AdPlatform.ADMOB, AdPlatform.MAX), MediationOrder.ADMOB_MAX.platforms)
        assertEquals(listOf(AdPlatform.MAX), MediationOrder.MAX.platforms)
        assertEquals(listOf(AdPlatform.ADMOB), MediationOrder.ADMOB.platforms)
    }

    @Test
    fun of_invalidIsDefault() {
        assertEquals(MediationOrder.MAX_ADMOB, MediationOrder.DEFAULT)
        listOf("", "MAX", " max", "max,admob", "applovin").forEach {
            assertEquals("'$it'", MediationOrder.MAX_ADMOB, MediationOrder.of(it))
        }
        // Control (L-055): a valid value other than the default is not read as the default.
        assertEquals(MediationOrder.ADMOB, MediationOrder.of("admob"))
    }

    @Test
    fun effective_withoutMaxIsAdmob() {
        MediationOrder.entries.forEach {
            assertEquals("$it without MAX", MediationOrder.ADMOB, MediationOrder.effective(it, maxAvailable = false))
            assertEquals("$it with MAX", it, MediationOrder.effective(it, maxAvailable = true))
        }
    }
}
