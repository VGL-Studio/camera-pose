package com.vglstudio.camera.pose.line.guide.feature.loading

import android.os.Bundle
import android.os.Looper
import android.os.SystemClock
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.AdPlacement
import com.vglstudio.camera.pose.line.guide.core.ads.StartupAdTurn
import com.vglstudio.camera.pose.line.guide.core.analytics.Analytics
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlag
import com.vglstudio.camera.pose.line.guide.core.config.FeatureFlags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationRepository
import com.vglstudio.camera.pose.line.guide.feature.loading.data.PreparationTask
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.Shadows.shadowOf
import org.robolectric.android.controller.ActivityController
import org.robolectric.annotation.Config

/**
 * [LoadingRoute] in a real activity (plan T-074, pass 2; LoadingRouteTest of the template, T-024): the startup ad is
 * shown once, also when the activity is made again while it shows (L-061) or when the app went to the background while
 * waiting for it (L-091), and the end of the ad from a background thread still navigates on the main thread (L-051).
 * The [LoadingViewModel] is shared by every activity of a test, as the ViewModel store keeps it across a recreation.
 * No MainDispatcherRule: viewModelScope runs on the main looper of Robolectric, and [SystemClock] moves with it.
 */
@RunWith(AndroidJUnit4::class)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class LoadingRouteTest {
    @After
    fun tearDown() {
        RouteHost.reset()
    }

    private fun viewModel(startupAds: FakeStartupAds) {
        RouteHost.viewModel = LoadingViewModel(
            repository = InstantPreparationRepository(),
            analytics = NoAnalytics(),
            logger = RecordingAppLogger(),
            featureFlags = LoadingOn(),
            clock = { SystemClock.elapsedRealtime() },
            internet = { true },
            startupAds = startupAds,
        )
    }

    private fun open(turn: StartupAdTurn?): ActivityController<RouteTestActivity> {
        viewModel(FakeStartupAds().apply { result = CompletableDeferred(turn) })
        val controller = Robolectric.buildActivity(RouteTestActivity::class.java).setup()
        idle()
        return controller
    }

    private fun idle() {
        shadowOf(Looper.getMainLooper()).idleFor(3, TimeUnit.SECONDS)
    }

    @Test
    fun pendingAd_isShownOnce_thenNavigatesAfterFinish() {
        // Control: with no startup ad, nothing shows and the screen moves on by itself.
        open(turn = null)
        assertEquals(0, RouteHost.shown.size)
        assertEquals(1, RouteHost.navigations)

        RouteHost.reset()
        open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(listOf(AdPlacement.OPEN_APP), RouteHost.shown.map { it.placement })
        assertEquals(0, RouteHost.navigations)

        RouteHost.finishers.single()(false)
        idle()
        assertEquals(1, RouteHost.navigations)
        idle()
        assertEquals(listOf(AdPlacement.OPEN_APP), RouteHost.shown.map { it.placement })
    }

    @Test
    fun recreateWhileAdShows_doesNotShowAgain() {
        val controller = open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(1, RouteHost.shown.size)
        val first = controller.get()

        controller.recreate()
        idle()
        // Control: the activity was really made again.
        check(controller.get() !== first)
        assertEquals(1, RouteHost.shown.size)
        assertEquals(0, RouteHost.navigations)

        // The action kept from the first activity ends the turn on the new one.
        RouteHost.finishers.single()(false)
        idle()
        assertEquals(1, RouteHost.navigations)
        assertEquals(1, RouteHost.shown.size)
    }

    @Test
    fun finishFromBackgroundThread_navigatesOnMainThread() {
        open(turn = StartupAdTurn(AdPlacement.OPEN_APP))
        assertEquals(1, RouteHost.shown.size)

        val executor = Executors.newSingleThreadExecutor()
        try {
            executor.submit { RouteHost.finishers.single()(false) }.get(5, TimeUnit.SECONDS)
        } finally {
            executor.shutdown()
        }
        idle()
        assertEquals(1, RouteHost.navigations)
        assertSame(Looper.getMainLooper(), RouteHost.navigationLoopers.single())
    }

    @Test
    fun backgroundWhileWaiting_thenResumed_showsOnce() {
        val startupAds = FakeStartupAds().apply { result = CompletableDeferred() }
        viewModel(startupAds)
        val controller = Robolectric.buildActivity(RouteTestActivity::class.java).setup()
        idle()
        // Control: the screen is waiting for the ad, past its tasks and the minimum time.
        assertEquals(1, startupAds.calls)

        // To the background (stopped: CREATED) while waiting, then the ad is ready.
        controller.pause().stop()
        idle()
        startupAds.result.complete(StartupAdTurn(AdPlacement.OPEN_APP))
        idle()
        assertEquals("never shown while the app is in the background", 0, RouteHost.shown.size)

        controller.start().resume()
        idle()
        assertEquals(listOf(AdPlacement.OPEN_APP), RouteHost.shown.map { it.placement })
        assertEquals(0, RouteHost.navigations)

        RouteHost.finishers.single()(false)
        idle()
        assertEquals(1, RouteHost.navigations)
        assertEquals(1, RouteHost.shown.size)
    }
}

/** What the activities of [LoadingRouteTest] share; reset after each test. */
private object RouteHost {
    lateinit var viewModel: LoadingViewModel
    val shown = mutableListOf<StartupAdTurn>()
    val finishers = mutableListOf<(Boolean) -> Unit>()
    var navigations = 0
    val navigationLoopers = mutableListOf<Looper?>()

    fun showAd(turn: StartupAdTurn, onFinished: (Boolean) -> Unit) {
        shown += turn
        finishers += onFinished
    }

    fun onFinished() {
        navigations++
        navigationLoopers += Looper.myLooper()
    }

    fun reset() {
        shown.clear()
        finishers.clear()
        navigations = 0
        navigationLoopers.clear()
    }
}

/** Not in the manifest: Robolectric builds it all the same (as AdsActivityTrackerTest of :core:ads). */
class RouteTestActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AppTheme {
                LoadingRoute(
                    onFinished = RouteHost::onFinished,
                    viewModel = RouteHost.viewModel,
                    showAd = RouteHost::showAd,
                )
            }
        }
    }
}

private class InstantPreparationRepository : PreparationRepository {
    override suspend fun run(task: PreparationTask, onProgress: (done: Int, total: Int) -> Unit): Boolean = true
}

private class NoAnalytics : Analytics {
    override fun log(event: AnalyticsEvent) = Unit
}

private class LoadingOn : FeatureFlags {
    override fun enabled(flag: FeatureFlag): Flow<Boolean> = flowOf(true)
}
