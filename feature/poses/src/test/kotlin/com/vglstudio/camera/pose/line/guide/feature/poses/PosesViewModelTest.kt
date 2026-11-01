package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryContent
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryResult
import io.mockk.mockk
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/** The pose library (spec poses, KB-1, KB-2, KB-3, KB-5; plan T-021, section 4). Virtual time of runTest. */
@OptIn(ExperimentalCoroutinesApi::class)
class PosesViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakePosesLibrarySource()
    private val thumbnails = FakePoseThumbnailRepository { mockk<ImageBitmap>() }
    private val network = FakeNetworkStatus()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()
    private val flags = FakeFeatureFlags()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = PosesViewModel(
        librarySource = library,
        thumbnailRepository = thumbnails,
        networkStatus = network,
        analytics = analytics,
        logger = logger,
        featureFlags = flags,
        savedStateHandle = savedState,
    )

    private fun readyViewModel(
        content: LibraryContent = testContent(),
        savedState: SavedStateHandle = SavedStateHandle(),
    ): PosesViewModel {
        library.ready(content)
        return viewModel(savedState)
    }

    private fun PosesViewModel.ready(): PosesUiState.Ready {
        val value = uiState.value
        assertTrue("state $value", value is PosesUiState.Ready)
        return value as PosesUiState.Ready
    }

    private fun PosesViewModel.ids() = ready().cards.map { it.id }

    private fun PosesViewModel.card(id: String) = ready().cards.first { it.id == id }

    private fun savedEvent(id: String, saved: Boolean) =
        AnalyticsEvent("pose_saved", mapOf("pose_id" to id, "saved" to saved.toString()))

    private fun loadFailedEvent(content: String, reason: String) =
        AnalyticsEvent("content_load_failed", mapOf("content" to content, "reason" to reason))

    private fun loadFailedEvents() = analytics.events.filter { it.name == "content_load_failed" }

    // Flag (ADR-0003)

    @Test
    fun flagOff_isDisabled() {
        flags.value.value = false
        library.ready()
        assertEquals(PosesUiState.Disabled, viewModel().uiState.value)
    }

    @Test
    fun flagTurnsOffWhileReady_becomesDisabled() {
        val viewModel = readyViewModel()
        viewModel.ready()
        flags.value.value = false
        assertEquals(PosesUiState.Disabled, viewModel.uiState.value)
    }

    @Test
    fun libraryLoading_isLoading() {
        assertEquals(PosesUiState.Loading, viewModel().uiState.value)
    }

    // KB-8: premium poses (plan T-086)

    @Test
    fun lockedPose_cardIsLocked_othersNot() {
        val viewModel = readyViewModel(testContent(lockedIds = setOf("w03")))
        assertTrue(viewModel.card("w03").locked)
        assertEquals(listOf("w03"), viewModel.ready().cards.filter { it.locked }.map { it.id })
        // Control: once unlocked, the library has no locked id and the card follows.
        library.ready(testContent(lockedIds = emptySet()))
        assertFalse(viewModel.card("w03").locked)
    }

    // KB-1

    @Test
    fun ready_selectsAll_andCardsFollowLibraryOrder() {
        val content = testContent(savedIds = listOf("c01", "w02"))
        val viewModel = readyViewModel(content)
        assertEquals(PoseFilter.All, viewModel.ready().selected)
        assertEquals(content.posesFor(PoseFilter.All).map { it.id }, viewModel.ids())
        assertEquals(listOf("c01", "w02", "w01", "w03", "m01", "m02", "c02"), viewModel.ids())
    }

    @Test
    fun ready_totalAndCategoryCount() {
        val ready = readyViewModel().ready()
        assertEquals(7, ready.totalPoses)
        assertEquals(3, ready.chips.count { it is PoseChip.Category })
    }

    @Test
    fun ready_chipsAreAllSavedThenCategoriesInCatalogOrder() {
        val ready = readyViewModel().ready()
        assertEquals(
            listOf(
                PoseChip.All,
                PoseChip.Saved,
                PoseChip.Category("women", PosesFixtures.WOMEN.name),
                PoseChip.Category("men", PosesFixtures.MEN.name),
                PoseChip.Category("couples", PosesFixtures.COUPLES.name),
            ),
            ready.chips,
        )
    }

    @Test
    fun card_hasNameSubjectStepsAndSaved() {
        val viewModel = readyViewModel()
        val couple = viewModel.card("c01")
        assertEquals("couples", couple.categoryId)
        assertEquals(PosesFixtures.POSES.first { it.id == "c01" }.name, couple.name)
        assertEquals(SubjectType.COUPLE, couple.subjectType)
        assertEquals(PosesFixtures.POSES.first { it.id == "c01" }.steps, couple.steps)
        assertEquals(5, couple.steps.values.getValue("en").size)
        assertFalse(couple.saved)
        assertEquals(ThumbnailState.Loading, couple.thumbnail)
        assertTrue(viewModel.card("w02").saved)
        assertEquals(SubjectType.GROUP, viewModel.card("m02").subjectType)
    }

    @Test
    fun thumbnailNeeded_pending_isLoading() = runTest {
        thumbnails.delays["w01"] = 5_000
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        assertEquals(ThumbnailState.Loading, viewModel.card("w01").thumbnail)
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(ThumbnailState.Loading, viewModel.card("w01").thumbnail)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
    }

    // KB-2

    @Test
    fun selectCategory_showsOnlyThatCategory() {
        val content = testContent(savedIds = listOf("m02"))
        val viewModel = readyViewModel(content)
        viewModel.onSelectFilter(PoseFilter.Category("men"))
        val ready = viewModel.ready()
        assertEquals(PoseFilter.Category("men"), ready.selected)
        assertTrue(ready.cards.all { it.categoryId == "men" })
        assertEquals(content.posesFor(PoseFilter.Category("men")).map { it.id }, viewModel.ids())
        assertEquals(listOf("m02", "m01"), viewModel.ids())
    }

    @Test
    fun selectedFilter_restoredFromSavedStateHandle() {
        val savedState = SavedStateHandle()
        readyViewModel(savedState = savedState).onSelectFilter(PoseFilter.Category("men"))
        val restored = viewModel(savedState)
        assertEquals(PoseFilter.Category("men"), restored.ready().selected)
        assertEquals(listOf("m01", "m02"), restored.ids())
    }

    @Test
    fun restoredCategoryMissing_fallsBackToAll() {
        val viewModel = readyViewModel(savedState = SavedStateHandle(mapOf("poses_filter" to "category:kids")))
        assertEquals(PoseFilter.All, viewModel.ready().selected)
        assertEquals(7, viewModel.ready().cards.size)
    }

    // KB-3

    @Test
    fun toggleSave_savesAndLogsPoseSavedTrue() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.writeGate = gate
        val viewModel = readyViewModel()
        viewModel.onToggleSave("w01")
        // The button changes at once, before the write is done.
        assertTrue(viewModel.card("w01").saved)
        assertEquals(listOf("w01" to true), library.writes)
        assertTrue(analytics.events.none { it.name == "pose_saved" })
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.card("w01").saved)
        assertEquals(listOf(savedEvent("w01", true)), analytics.events)
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.message == "Pose saved: true" })
    }

    @Test
    fun toggleSaveOfSavedPose_unsavesAndLogsSavedFalse() = runTest {
        val viewModel = readyViewModel()
        viewModel.onToggleSave("w02")
        advanceUntilIdle()
        assertEquals(listOf("w02" to false), library.writes)
        assertFalse(viewModel.card("w02").saved)
        assertEquals(listOf(savedEvent("w02", false)), analytics.events)
    }

    @Test
    fun saveDoneBeforeLibraryHasIt_buttonKeepsNewState_untilLibraryAgrees() = runTest {
        // Blocking test (review of T-021): the override bridges the time between the write and the new library value.
        library.publishLater = true
        val viewModel = readyViewModel()
        viewModel.onToggleSave("w01")
        advanceUntilIdle()
        assertEquals(listOf("w01" to true), library.writes)
        assertFalse("library has not published yet", "w01" in testContent().savedIds)
        assertTrue(viewModel.card("w01").saved)
        library.publishPending()
        assertTrue(viewModel.card("w01").saved)
        // The override is gone: a later library value is shown as it is.
        library.ready(testContent(savedIds = listOf("w02")))
        assertFalse(viewModel.card("w01").saved)
    }

    @Test
    fun toggleSaveWhileWriting_isIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.writeGate = gate
        val viewModel = readyViewModel()
        viewModel.onToggleSave("w01")
        viewModel.onToggleSave("w01")
        assertEquals(1, library.writes.size)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, library.writes.size)
        assertTrue(viewModel.card("w01").saved)
        assertEquals(listOf(savedEvent("w01", true)), analytics.events)
    }

    @Test
    fun saveOnScreen_keepsOrder_untilFilterChanges() = runTest {
        val viewModel = readyViewModel()
        val before = viewModel.ids()
        assertEquals(listOf("w02", "w01", "w03", "m01", "m02", "c01", "c02"), before)
        viewModel.onToggleSave("m02")
        advanceUntilIdle()
        // The library now puts m02 first; the grid on screen keeps its order.
        assertEquals("m02", testContent(listOf("m02", "w02")).posesFor(PoseFilter.All).first().id)
        assertEquals(before, viewModel.ids())
        assertTrue(viewModel.card("m02").saved)
        viewModel.onSelectFilter(PoseFilter.Category("men"))
        viewModel.onSelectFilter(PoseFilter.All)
        assertEquals("m02", viewModel.ids().first())
    }

    @Test
    fun selectSaved_showsSavedInLibraryOrder() {
        val content = testContent(savedIds = listOf("c01", "w03", "w02"))
        val viewModel = readyViewModel(content)
        viewModel.onSelectFilter(PoseFilter.Saved)
        assertEquals(PoseFilter.Saved, viewModel.ready().selected)
        assertEquals(listOf("c01", "w03", "w02"), viewModel.ids())
        assertEquals(content.posesFor(PoseFilter.Saved).map { it.id }, viewModel.ids())
        assertTrue(viewModel.ready().cards.all { it.saved })
    }

    @Test
    fun selectSaved_nothingSaved_isEmpty_thenShowAllSelectsAll() {
        val viewModel = readyViewModel(testContent(savedIds = emptyList()))
        viewModel.onSelectFilter(PoseFilter.Saved)
        assertEquals(PoseFilter.Saved, viewModel.ready().selected)
        assertTrue(viewModel.ready().cards.isEmpty())
        viewModel.onShowAll()
        assertEquals(PoseFilter.All, viewModel.ready().selected)
        assertEquals(7, viewModel.ready().cards.size)
    }

    @Test
    fun saveFails_revertsButton_showsMessage_noEvent() = runTest {
        library.failWrites = true
        val viewModel = readyViewModel()
        viewModel.onToggleSave("w01")
        advanceUntilIdle()
        assertFalse(viewModel.card("w01").saved)
        assertTrue(viewModel.ready().saveFailed)
        assertTrue(analytics.events.none { it.name == "pose_saved" })
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Poses", warning.tag)
        assertTrue(warning.error is IOException)
        viewModel.onSaveFailedShown()
        assertFalse(viewModel.ready().saveFailed)
    }

    // KB-5: thumbnails

    @Test
    fun thumbnailAlreadyOnDevice_isReadyWithoutWaiting() = runTest {
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        runCurrent()
        assertEquals(0L, currentTime)
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
    }

    @Test
    fun thumbnailNeededTwice_loadsOnce() = runTest {
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        viewModel.onThumbnailNeeded("w01")
        advanceUntilIdle()
        assertEquals(1, thumbnails.loads["w01"])
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
        // Cards that were never needed are not loaded.
        assertEquals(setOf("w01"), thumbnails.loads.keys)
    }

    @Test
    fun thumbnailReleased_dropsImage_andNextNeedLoadsAgain() = runTest {
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        advanceUntilIdle()
        viewModel.onThumbnailReleased("w01")
        assertEquals(ThumbnailState.Loading, viewModel.card("w01").thumbnail)
        viewModel.onThumbnailNeeded("w01")
        advanceUntilIdle()
        assertEquals(2, thumbnails.loads["w01"])
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
        // A load still waiting when the card leaves the grid is cancelled: no image is kept.
        thumbnails.gates["w03"] = CompletableDeferred()
        viewModel.onThumbnailNeeded("w03")
        viewModel.onThumbnailReleased("w03")
        thumbnails.gates.getValue("w03").complete(Unit)
        advanceUntilIdle()
        assertEquals(ThumbnailState.Loading, viewModel.card("w03").thumbnail)
    }

    @Test
    fun atMostFourThumbnailLoadsAtOnce() = runTest {
        val viewModel = readyViewModel()
        val ids = viewModel.ids()
        ids.forEach { thumbnails.gates[it] = CompletableDeferred() }
        ids.forEach(viewModel::onThumbnailNeeded)
        runCurrent()
        assertEquals(4, thumbnails.running)
        ids.forEach { thumbnails.gates.getValue(it).complete(Unit) }
        advanceUntilIdle()
        assertEquals(4, thumbnails.maxRunning)
        assertTrue(viewModel.ready().cards.all { it.thumbnail is ThumbnailState.Ready })
    }

    @Test
    fun thumbnailFails_staysSkeleton_logsContentLoadFailedThumbnail() = runTest {
        thumbnails.fail("w01", LoadFailureReason.TIMEOUT)
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        advanceUntilIdle()
        assertEquals(ThumbnailState.Failed, viewModel.card("w01").thumbnail)
        assertEquals(listOf(loadFailedEvent("thumbnail", "timeout")), analytics.events)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Poses" })
    }

    @Test
    fun retryThumbnail_loadsAgain() = runTest {
        thumbnails.fail("w01", LoadFailureReason.NOT_FOUND)
        val viewModel = readyViewModel()
        viewModel.onThumbnailNeeded("w01")
        advanceUntilIdle()
        thumbnails.results.remove("w01")
        viewModel.onRetryThumbnail("w01")
        advanceUntilIdle()
        assertEquals(2, thumbnails.loads["w01"])
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
    }

    // KB-5: catalog

    @Test
    fun catalogFails_showsError_andLogsContentLoadFailedCatalog() {
        listOf(LoadFailureReason.OTHER, LoadFailureReason.NOT_FOUND, LoadFailureReason.TIMEOUT).forEach { reason ->
            val source = FakePosesLibrarySource().apply { fail(reason) }
            val events = FakeAnalytics()
            val viewModel = PosesViewModel(
                librarySource = source,
                thumbnailRepository = thumbnails,
                networkStatus = network,
                analytics = events,
                logger = logger,
                featureFlags = flags,
                savedStateHandle = SavedStateHandle(),
            )
            assertEquals(PosesUiState.Error(reason), viewModel.uiState.value)
            assertEquals(listOf(loadFailedEvent("catalog", reason.analyticsValue)), events.events)
        }
    }

    @Test
    fun catalogFailureRepeated_logsOnce() {
        library.fail(LoadFailureReason.OTHER)
        val viewModel = viewModel()
        library.emitAgain()
        assertEquals(PosesUiState.Error(LoadFailureReason.OTHER), viewModel.uiState.value)
        assertEquals(1, loadFailedEvents().size)
        assertEquals(1, logger.entries.count { it.level == LogLevel.WARN })
        // A new failure after loading again is logged again.
        library.state.value = LibraryResult.Loading
        library.fail(LoadFailureReason.OTHER)
        assertEquals(2, loadFailedEvents().size)
    }

    @Test
    fun retry_callsLibraryRetry_andLogs() = runTest {
        library.fail(LoadFailureReason.OTHER)
        val viewModel = viewModel()
        viewModel.onRetry()
        advanceUntilIdle()
        assertEquals(1, library.retryCount)
        assertTrue(
            logger.entries.any { it.level == LogLevel.INFO && it.tag == "Poses" && it.message == "Retrying catalog" },
        )
    }

    @Test
    fun catalogTimeout_showsError_notLoadingForever() {
        val viewModel = viewModel()
        assertEquals(PosesUiState.Loading, viewModel.uiState.value)
        library.fail(LoadFailureReason.TIMEOUT)
        assertEquals(PosesUiState.Error(LoadFailureReason.TIMEOUT), viewModel.uiState.value)
    }

    @Test
    fun catalogFailsNetwork_dialogShown_isLoadingNotError() {
        network.dialogShown.value = true
        library.fail(LoadFailureReason.NETWORK)
        val viewModel = viewModel()
        assertEquals(PosesUiState.Loading, viewModel.uiState.value)
        assertEquals(listOf(loadFailedEvent("catalog", "network")), analytics.events)
    }

    @Test
    fun catalogFailsNetwork_dialogNotShown_showsError() {
        library.fail(LoadFailureReason.NETWORK)
        val viewModel = viewModel()
        assertEquals(PosesUiState.Error(LoadFailureReason.NETWORK), viewModel.uiState.value)
        assertEquals(listOf(loadFailedEvent("catalog", "network")), analytics.events)
    }

    @Test
    fun networkRecovered_retriesCatalog_andFailedThumbnails() = runTest {
        network.dialogShown.value = true
        library.fail(LoadFailureReason.NETWORK)
        val viewModel = viewModel()
        network.recovered.emit(Unit)
        advanceUntilIdle()
        assertEquals(1, library.retryCount)

        library.ready()
        thumbnails.fail("w01", LoadFailureReason.NETWORK)
        thumbnails.fail("w03", LoadFailureReason.NETWORK)
        viewModel.onThumbnailNeeded("w01")
        viewModel.onThumbnailNeeded("w03")
        advanceUntilIdle()
        // w03 left the grid: it is not loaded again.
        viewModel.onThumbnailReleased("w03")
        thumbnails.results.clear()
        network.recovered.emit(Unit)
        advanceUntilIdle()
        assertEquals(2, thumbnails.loads["w01"])
        assertEquals(1, thumbnails.loads["w03"])
        assertEquals(ThumbnailState.Ready(thumbnails.imageOf("w01")), viewModel.card("w01").thumbnail)
        // The catalog is not failed any more: no second retry.
        assertEquals(1, library.retryCount)
    }

    // ADR-0019

    @Test
    fun logs_useTagPoses_andNoPoseIdsOrNames() = runTest {
        library.fail(LoadFailureReason.OTHER)
        val viewModel = viewModel()
        viewModel.onRetry()
        library.ready()
        viewModel.onSelectFilter(PoseFilter.Category("men"))
        viewModel.onSelectFilter(PoseFilter.Saved)
        viewModel.onShowAll()
        viewModel.onToggleSave("w01")
        thumbnails.fail("w03", LoadFailureReason.TIMEOUT)
        viewModel.onThumbnailNeeded("w03")
        advanceUntilIdle()
        library.failWrites = true
        viewModel.onToggleSave("c01")
        advanceUntilIdle()

        val entries = logger.entries
        assertTrue(entries.size >= 6)
        assertTrue(entries.all { it.tag == "Poses" })
        val forbidden = PosesFixtures.POSES.flatMap { listOf(it.id, it.name.values.getValue("vi")) } + "men"
        entries.forEach { entry ->
            forbidden.forEach { word -> assertFalse("'${entry.message}' has $word", entry.message.contains(word)) }
        }
    }
}
