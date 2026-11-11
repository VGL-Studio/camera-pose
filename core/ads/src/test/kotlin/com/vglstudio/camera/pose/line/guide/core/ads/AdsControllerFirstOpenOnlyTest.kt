package com.vglstudio.camera.pose.line.guide.core.ads

import android.app.Application
import android.os.Looper
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.preferencesOf
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import java.time.Duration
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Shadows.shadowOf

/**
 * A placement declared first-open-only (OB-03, KB-20 criteria 6 and 7) through the real [AdsController]: on only in
 * the first open since install, the first-open mark read once per process, and a preload asked once the mark is read
 * and the placement is on. The template declares no such placement, so the tests set [AdsController.firstOpenOnlyOf].
 */
@RunWith(AndroidJUnit4::class)
class AdsControllerFirstOpenOnlyTest {
    private val application = ApplicationProvider.getApplicationContext<Application>()

    private fun controller(
        store: DataStore<Preferences>,
        vararg on: AdPlacement,
        logger: RecordingAppLogger = RecordingAppLogger(),
    ): AdsController {
        val settings = FakeRemoteSettings()
        on.forEach { settings.set(AdsSettings.placementEnabled(it), true) }
        return testAdsController(
            context = application,
            flags = DefaultAdsFlags(settings),
            logger = logger,
            firstOpenMark = FirstOpenMark(store, RecordingAppLogger()),
        )
    }

    private fun AdsController.enabledNow(placement: AdPlacement): Boolean = runBlocking { isEnabled(placement).first() }

    @Test
    fun laterOpen_firstOpenOnlyPlacementOff_otherPlacementOn() {
        val controller = controller(CountingPreferencesStore(marked()), STEP, MEDIUM)
        controller.firstOpenOnlyOf = { it == STEP }
        assertFalse(controller.enabledNow(STEP))
        // Control: a placement that is not first-open-only stays on at a later open.
        assertTrue(controller.enabledNow(MEDIUM))
    }

    @Test
    fun firstOpen_firstOpenOnlyPlacementOn() {
        // Control: the same placement at a later open is off.
        val later = controller(CountingPreferencesStore(marked()), STEP)
        later.firstOpenOnlyOf = { it == STEP }
        assertFalse(later.enabledNow(STEP))

        val first = controller(CountingPreferencesStore(), STEP)
        first.firstOpenOnlyOf = { it == STEP }
        assertTrue(first.enabledNow(STEP))

        // Control of the hook: not set, the placement is an ordinary one, on at a later open.
        assertTrue(controller(CountingPreferencesStore(marked()), STEP).enabledNow(STEP))
    }

    @Test
    fun firstOpenLastsForProcess_nextProcessIsLater() {
        val store = CountingPreferencesStore()
        val a = controller(store, STEP)
        a.firstOpenOnlyOf = { it == STEP }
        assertTrue(a.enabledNow(STEP))
        // The mark is written now, but this process stays the first open.
        assertTrue(a.enabledNow(STEP))
        assertEquals(1, store.reads)
        assertEquals(true, store.state.value[FIRST_OPEN_KEY])

        // The next process reads the mark again: a later open.
        val b = controller(store, STEP)
        b.firstOpenOnlyOf = { it == STEP }
        assertFalse(b.enabledNow(STEP))
        assertEquals(2, store.reads)
    }

    @Test
    fun markRead_asksPreloadOnlyWhenOn() {
        // First open: nothing asked before the mark is read, one ask after.
        val held = CompletableDeferred<Unit>()
        val firstLog = RecordingAppLogger()
        val first = controller(CountingPreferencesStore(held = held), STEP, logger = firstLog)
        first.firstOpenOnlyOf = { it == STEP }
        first.start(application, AppActivityFilter { true })
        idle()
        assertEquals(0, preloadAsks(firstLog))
        held.complete(Unit)
        waitForAsks(firstLog, 1)
        assertEquals(1, preloadAsks(firstLog))

        // A later open: the mark read, the placement off, nothing asked.
        val laterLog = RecordingAppLogger()
        val later = controller(CountingPreferencesStore(marked()), STEP, logger = laterLog)
        later.firstOpenOnlyOf = { it == STEP }
        later.start(application, AppActivityFilter { true })
        waitForAsks(laterLog, 1)
        assertEquals(0, preloadAsks(laterLog))

        // Control: not first-open-only, the same placement asks for a preload at a later open.
        val ordinaryLog = RecordingAppLogger()
        val ordinary = controller(CountingPreferencesStore(marked()), STEP, logger = ordinaryLog)
        ordinary.start(application, AppActivityFilter { true })
        waitForAsks(ordinaryLog, 1)
        assertEquals(1, preloadAsks(ordinaryLog))
    }

    @Test
    fun markRead_fullScreenStep_asksPreloadOnlyWhenOn() {
        val laterLog = RecordingAppLogger()
        val later = controller(CountingPreferencesStore(marked()), STEP_FULL, logger = laterLog)
        later.firstOpenOnlyOf = { it == STEP_FULL }
        later.start(application, AppActivityFilter { true })
        waitForAsks(laterLog, 1)
        assertEquals(0, preloadAsks(laterLog))

        val firstLog = RecordingAppLogger()
        val first = controller(CountingPreferencesStore(), STEP_FULL, logger = firstLog)
        first.firstOpenOnlyOf = { it == STEP_FULL }
        first.start(application, AppActivityFilter { true })
        waitForAsks(firstLog, 1)
        assertEquals(1, preloadAsks(firstLog))
    }

    private fun idle() = shadowOf(Looper.getMainLooper()).idle()

    private fun preloadAsks(log: RecordingAppLogger): Int =
        log.entries.count { it.message.startsWith("Preload PLACEMENT_ENABLED") }

    /** Runs the main looper until [count] asks are logged in [log], for at most two seconds of looper time. */
    private fun waitForAsks(log: RecordingAppLogger, count: Int) {
        repeat(20) {
            if (preloadAsks(log) >= count) return
            shadowOf(Looper.getMainLooper()).idleFor(Duration.ofMillis(100))
        }
    }

    private fun marked(): Preferences = preferencesOf(FIRST_OPEN_KEY to true)

    private companion object {
        val STEP = AdPlacement.GALLERY_NATIVE_STEP
        val STEP_FULL = AdPlacement.GALLERY_NATIVE_STEP_FULL
        val MEDIUM = AdPlacement.GALLERY_NATIVE_MEDIUM
    }
}

/** The DataStore key of [FirstOpenMark]. */
internal val FIRST_OPEN_KEY = booleanPreferencesKey("ads_first_open_done")

/**
 * A preferences store kept in memory that counts the reads of [data]; each read waits for [held] first, so a test
 * decides when the first-open mark is read.
 */
internal class CountingPreferencesStore(
    initial: Preferences = emptyPreferences(),
    private val held: CompletableDeferred<Unit>? = null,
) : DataStore<Preferences> {
    val state = MutableStateFlow(initial)
    var reads = 0
        private set

    override val data: Flow<Preferences> = flow {
        reads++
        held?.await()
        emitAll(state)
    }

    override suspend fun updateData(transform: suspend (t: Preferences) -> Preferences): Preferences {
        val updated = transform(state.value)
        state.value = updated
        return updated
    }
}
