package com.vglstudio.camera.pose.line.guide.core.poses

import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import org.junit.Assert.assertEquals
import org.junit.Test

class PremiumAccessTest {
    @Test
    fun isLocked_onlyWhenEnabledPremiumAndNotUnlocked() {
        val cases = buildList {
            for (enabled in listOf(false, true)) {
                for (premium in listOf(false, true)) {
                    for (unlocked in listOf(false, true)) add(Triple(enabled, premium, unlocked))
                }
            }
        }
        assertEquals(8, cases.size)

        for ((enabled, premium, unlocked) in cases) {
            val access = PremiumAccess(enabled, if (unlocked) setOf("p1", "other") else setOf("other"))
            val pose = Fixtures.pose("p1", premium = premium)
            val expectedLocked = enabled && premium && !unlocked
            val case = "enabled=$enabled premium=$premium unlocked=$unlocked"

            assertEquals(case, expectedLocked, access.isLocked(pose))
            assertEquals(case, !expectedLocked, access.isUsable(pose))
        }
    }
}
