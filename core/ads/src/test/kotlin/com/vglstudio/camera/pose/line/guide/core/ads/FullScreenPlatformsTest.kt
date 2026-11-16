package com.vglstudio.camera.pose.line.guide.core.ads

import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.ADMOB
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlatform.MAX
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Idle
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Loading
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.Ready
import com.vglstudio.camera.pose.line.guide.core.ads.FullScreenLoadState.WaitingRetry
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** The platforms of an interstitial turn (plan T-069) and of a rewarded turn (plan T-106); KB-31, question 41. */
@OptIn(ExperimentalCoroutinesApi::class)
class FullScreenPlatformsTest {
    private val inApp = AdPlacement.GALLERY_INTERSTITIAL

    @Test
    fun interstitialPlatforms_followOrder_dropMissingUnits() {
        val both = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(ADMOB, MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX, ADMOB),
        )
        both.forEach { (order, expected) ->
            assertEquals(
                "$order",
                expected,
                interstitialPlatforms(inApp, order, hasAdmobUnit = true, hasMaxUnit = true),
            )
        }
        // A platform without its ad unit is dropped (like bannerPlatforms).
        assertEquals(listOf(ADMOB), interstitialPlatforms(inApp, MediationOrder.MAX_ADMOB, true, hasMaxUnit = false))
        assertEquals(listOf(MAX), interstitialPlatforms(inApp, MediationOrder.MAX_ADMOB, hasAdmobUnit = false, true))
        assertEquals(
            emptyList<AdPlatform>(),
            interstitialPlatforms(inApp, MediationOrder.MAX, true, hasMaxUnit = false),
        )
        assertEquals(emptyList<AdPlatform>(), interstitialPlatforms(inApp, MediationOrder.ADMOB, false, true))
    }

    @Test
    fun interstitialPlatforms_openAppInter_admobOnly_anyOrder() {
        MediationOrder.entries.forEach { order ->
            assertEquals(
                "$order",
                listOf(ADMOB),
                interstitialPlatforms(AdPlacement.OPEN_APP_INTER, order, hasAdmobUnit = true, hasMaxUnit = true),
            )
        }
        assertEquals(
            emptyList<AdPlatform>(),
            interstitialPlatforms(
                AdPlacement.OPEN_APP_INTER,
                MediationOrder.MAX,
                hasAdmobUnit = false,
                hasMaxUnit = true,
            ),
        )
    }

    @Test
    fun interstitialPlatforms_resumeInter_followsOrder() {
        assertEquals(
            listOf(MAX, ADMOB),
            interstitialPlatforms(
                AdPlacement.RESUME_INTER,
                MediationOrder.MAX_ADMOB,
                hasAdmobUnit = true,
                hasMaxUnit = true,
            ),
        )
        assertEquals(
            listOf(MAX),
            interstitialPlatforms(AdPlacement.RESUME_INTER, MediationOrder.MAX, hasAdmobUnit = true, hasMaxUnit = true),
        )
    }

    @Test
    fun readyPlatform_firstWithAd_elseNext_elseNull() {
        assertEquals(MAX, readyPlatform(listOf(MAX, ADMOB), admobReady = true, maxReady = true))
        assertEquals(ADMOB, readyPlatform(listOf(MAX, ADMOB), admobReady = true, maxReady = false))
        assertEquals(MAX, readyPlatform(listOf(ADMOB, MAX), admobReady = false, maxReady = true))
        assertEquals(ADMOB, readyPlatform(listOf(ADMOB, MAX), admobReady = true, maxReady = true))
        assertNull(readyPlatform(listOf(ADMOB, MAX), admobReady = false, maxReady = false))
        // A platform not in the list is never picked, even with an ad.
        assertNull(readyPlatform(listOf(ADMOB), admobReady = false, maxReady = true))
        assertNull(readyPlatform(emptyList(), admobReady = true, maxReady = true))
    }

    @Test
    fun turnLoadState_readyWhenAny_elseFirstPlatformState_idleWhenNone() {
        val retry = WaitingRetry(5_000)
        assertEquals(Ready, turnLoadState(listOf(MAX, ADMOB), admob = Ready, max = retry))
        assertEquals(Ready, turnLoadState(listOf(MAX, ADMOB), admob = Idle, max = Ready))
        assertEquals(retry, turnLoadState(listOf(MAX, ADMOB), admob = Loading, max = retry))
        assertEquals(Loading, turnLoadState(listOf(ADMOB, MAX), admob = Loading, max = retry))
        // Only the platforms of the turn count.
        assertEquals(Idle, turnLoadState(listOf(ADMOB), admob = Idle, max = Ready))
        assertEquals(Idle, turnLoadState(emptyList(), admob = Ready, max = Ready))
    }

    @Test
    fun preloadSettleState_loadingWhileAnyLoads() {
        assertEquals(Loading, preloadSettleState(listOf(MAX, ADMOB), admob = Loading, max = Ready))
        assertEquals(Loading, preloadSettleState(listOf(MAX, ADMOB), admob = Ready, max = Loading))
        assertEquals(Ready, preloadSettleState(listOf(MAX, ADMOB), admob = Ready, max = Idle))
        // A platform not in the turn does not hold the group.
        assertEquals(Ready, preloadSettleState(listOf(ADMOB), admob = Ready, max = Loading))
        assertEquals(Idle, preloadSettleState(emptyList(), admob = Loading, max = Loading))
    }

    @Test
    fun rewardedPlatforms_followOrder_dropMissingUnits() {
        val both = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(ADMOB, MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX, ADMOB),
        )
        val noMax = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to emptyList(),
            MediationOrder.ADMOB_MAX to listOf(ADMOB),
            MediationOrder.MAX_ADMOB to listOf(ADMOB),
        )
        val noAdmob = mapOf(
            MediationOrder.ADMOB to emptyList(),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX),
        )
        MediationOrder.entries.forEach { order ->
            assertEquals(
                "$order",
                both.getValue(order),
                rewardedPlatforms(order, hasAdmobUnit = true, hasMaxUnit = true),
            )
            assertEquals(
                "$order",
                noMax.getValue(order),
                rewardedPlatforms(order, hasAdmobUnit = true, hasMaxUnit = false),
            )
            assertEquals(
                "$order",
                noAdmob.getValue(order),
                rewardedPlatforms(order, hasAdmobUnit = false, hasMaxUnit = true),
            )
            assertEquals("$order", emptyList<AdPlatform>(), rewardedPlatforms(order, false, false))
        }
    }

    @Test
    fun maxRewardedWanted_maxNotInOrder_falseWithoutReadingSwitch() = runTest {
        var read = 0
        assertFalse(
            maxRewardedWanted(MediationOrder.ADMOB) {
                read++
                true
            },
        )
        assertEquals(0, read)
        // Control: MAX in the order, a switch on: the switch is read and the answer is yes.
        listOf(MediationOrder.MAX, MediationOrder.MAX_ADMOB, MediationOrder.ADMOB_MAX).forEach { order ->
            val wanted = maxRewardedWanted(order) {
                read++
                true
            }
            assertTrue("$order", wanted)
        }
        assertEquals(3, read)
        assertFalse(maxRewardedWanted(MediationOrder.MAX) { false })
    }

    @Test
    fun maxRewardedWanted_switchNotReadIn5s_false() = runTest {
        val never = CompletableDeferred<Boolean>()
        val timedOut = async { maxRewardedWanted(MediationOrder.MAX) { never.await() } }
        advanceTimeBy(4_999)
        runCurrent()
        assertFalse(timedOut.isCompleted)
        advanceTimeBy(1)
        runCurrent()
        assertTrue(timedOut.isCompleted)
        assertEquals(5_000, testScheduler.currentTime)
        assertFalse(timedOut.await())
        // Control: the switch answers yes just before the limit.
        val late = CompletableDeferred<Boolean>()
        val answered = async { maxRewardedWanted(MediationOrder.MAX) { late.await() } }
        advanceTimeBy(4_999)
        runCurrent()
        assertFalse(answered.isCompleted)
        late.complete(true)
        runCurrent()
        assertTrue(answered.await())
    }

    @Test
    fun appOpenPlatforms_resume_followsOrder_dropMissingUnits() {
        // Plan T-070 (KB-31, question 41): the resume app open ad follows ads_mediation_order.
        val both = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(ADMOB, MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX, ADMOB),
        )
        both.forEach { (order, expected) ->
            assertEquals(
                "$order",
                expected,
                appOpenPlatforms(AdPlacement.RESUME, order, hasAdmobUnit = true, hasMaxUnit = true),
            )
        }
        // A platform without its ad unit is dropped.
        val noMax = mapOf(
            MediationOrder.ADMOB to listOf(ADMOB),
            MediationOrder.MAX to emptyList(),
            MediationOrder.ADMOB_MAX to listOf(ADMOB),
            MediationOrder.MAX_ADMOB to listOf(ADMOB),
        )
        noMax.forEach { (order, expected) ->
            assertEquals(
                "$order no MAX unit",
                expected,
                appOpenPlatforms(AdPlacement.RESUME, order, hasAdmobUnit = true, hasMaxUnit = false),
            )
        }
        val noAdmob = mapOf(
            MediationOrder.ADMOB to emptyList(),
            MediationOrder.MAX to listOf(MAX),
            MediationOrder.ADMOB_MAX to listOf(MAX),
            MediationOrder.MAX_ADMOB to listOf(MAX),
        )
        noAdmob.forEach { (order, expected) ->
            assertEquals(
                "$order no AdMob unit",
                expected,
                appOpenPlatforms(AdPlacement.RESUME, order, hasAdmobUnit = false, hasMaxUnit = true),
            )
        }
        MediationOrder.entries.forEach { order ->
            assertEquals(
                "$order no unit",
                emptyList<AdPlatform>(),
                appOpenPlatforms(AdPlacement.RESUME, order, hasAdmobUnit = false, hasMaxUnit = false),
            )
        }
    }

    @Test
    fun appOpenPlatforms_openAppAndGallery_admobOnly_anyOrder() {
        // KB-31, criterion 3: the startup app open ad (and the staging gallery one) is AdMob only.
        listOf(AdPlacement.OPEN_APP, AdPlacement.GALLERY_APP_OPEN).forEach { placement ->
            MediationOrder.entries.forEach { order ->
                assertEquals(
                    "$placement $order",
                    listOf(ADMOB),
                    appOpenPlatforms(placement, order, hasAdmobUnit = true, hasMaxUnit = true),
                )
                assertEquals(
                    "$placement $order no AdMob unit",
                    emptyList<AdPlatform>(),
                    appOpenPlatforms(placement, order, hasAdmobUnit = false, hasMaxUnit = true),
                )
            }
        }
    }
}
