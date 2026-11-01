package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.ui.graphics.ImageBitmap
import androidx.lifecycle.SavedStateHandle
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.feature.poses.FakeAnalytics
import com.vglstudio.camera.pose.line.guide.feature.poses.FakeFeatureFlags
import com.vglstudio.camera.pose.line.guide.feature.poses.FakeNetworkStatus
import com.vglstudio.camera.pose.line.guide.feature.poses.FakePoseImageRepository
import com.vglstudio.camera.pose.line.guide.feature.poses.FakePosesLibrarySource
import com.vglstudio.camera.pose.line.guide.feature.poses.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.poses.data.LibraryContent
import com.vglstudio.camera.pose.line.guide.feature.poses.testContent
import com.vglstudio.camera.pose.line.guide.feature.poses.testPose
import io.mockk.mockk
import java.io.IOException
import java.util.Locale
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

/**
 * The pose detail (spec poses, KB-3, KB-4, KB-5; plan T-022, section 4). The catalog has w01 (one person, full body,
 * 4 steps, 1 common mistake), w02 (3 steps), w05 (a couple, half body, 5 steps, 2 mistakes) and w06 (no mistakes).
 * Virtual time of runTest.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PoseDetailViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakePosesLibrarySource()
    private val images = FakePoseImageRepository { mockk<ImageBitmap>() }
    private val network = FakeNetworkStatus()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()
    private val flags = FakeFeatureFlags()

    private fun viewModel(
        poseId: String = "w01",
        savedState: SavedStateHandle = SavedStateHandle(mapOf("poseId" to poseId)),
    ) = PoseDetailViewModel(
        librarySource = library,
        imageRepository = images,
        networkStatus = network,
        analytics = analytics,
        logger = logger,
        featureFlags = flags,
        savedStateHandle = savedState,
    )

    private fun readyViewModel(
        poseId: String = "w01",
        content: LibraryContent = detailContent(),
        savedState: SavedStateHandle = SavedStateHandle(mapOf("poseId" to poseId)),
    ): PoseDetailViewModel {
        library.ready(content)
        return viewModel(poseId, savedState)
    }

    private fun PoseDetailViewModel.ready(): PoseDetailUiState.Ready {
        val value = uiState.value
        assertTrue("state $value", value is PoseDetailUiState.Ready)
        return value as PoseDetailUiState.Ready
    }

    private fun openedEvent(id: String = "w01", category: String = "women") =
        AnalyticsEvent("pose_detail_opened", mapOf("pose_id" to id, "category" to category))

    private fun savedEvent(id: String, saved: Boolean) =
        AnalyticsEvent("pose_saved", mapOf("pose_id" to id, "saved" to saved.toString()))

    private fun loadFailedEvent(reason: String) =
        AnalyticsEvent("content_load_failed", mapOf("content" to "image", "reason" to reason))

    private fun eventsNamed(name: String) = analytics.events.filter { it.name == name }

    // Flag (ADR-0003)

    @Test
    fun flagOff_isDisabled() {
        flags.value.value = false
        library.ready(detailContent())
        assertEquals(PoseDetailUiState.Disabled, viewModel().uiState.value)
        assertTrue(analytics.events.isEmpty())
    }

    // Catalog

    // Blocking test: it passes with the empty frame (plan T-022, section 4).
    @Test
    fun libraryLoading_isLoading() {
        assertEquals(PoseDetailUiState.Loading, viewModel().uiState.value)
    }

    @Test
    fun poseNotInCatalog_isMissing_andLogsWarn() {
        val viewModel = readyViewModel(poseId = "gone")
        assertEquals(PoseDetailUiState.Missing, viewModel.uiState.value)
        assertTrue(analytics.events.isEmpty())
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Poses", warning.tag)
        assertEquals("Pose detail: pose not in catalog", warning.message)
    }

    @Test
    fun libraryFailed_isMissing() {
        library.fail(LoadFailureReason.OTHER)
        assertEquals(PoseDetailUiState.Missing, viewModel().uiState.value)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.message == "Pose detail: library failed" })
    }

    // KB-4: content

    @Test
    fun ready_hasPoseSavedStateAndCategoryName() {
        // w01 is saved next to w02, which is not: a wrong lookup of the saved state fails one of the two checks.
        val content = detailContent(savedIds = listOf("w01"))
        val first = readyViewModel("w01", content).ready()
        assertEquals("w01", first.pose.id)
        assertEquals("women", first.pose.categoryId)
        assertEquals("Nữ", first.pose.categoryName?.resolve(Locale.forLanguageTag("vi")))
        assertEquals(SubjectType.SINGLE, first.pose.subjectType)
        assertEquals(Framing.FULL, first.pose.framing)
        assertEquals(4, first.pose.steps.values.getValue("en").size)
        assertEquals(1, first.pose.mistakes.values.getValue("en").size)
        assertTrue(first.saved)
        val second = viewModel("w02").ready()
        assertEquals("w02", second.pose.id)
        assertFalse(second.saved)
    }

    @Test
    fun couple_hasSubjectAndFraming() {
        val ready = readyViewModel("w05").ready()
        assertEquals(SubjectType.COUPLE, ready.pose.subjectType)
        assertEquals(Framing.HALF, ready.pose.framing)
        assertEquals(5, ready.pose.steps.values.getValue("en").size)
        assertEquals(2, ready.pose.mistakes.values.getValue("en").size)
    }

    @Test
    fun ready_logsPoseDetailOpenedOnce() {
        readyViewModel()
        // The library sends its value again: the event is not logged again.
        library.emitAgain()
        library.ready(detailContent(savedIds = listOf("w02", "w01")))
        assertEquals(listOf(openedEvent()), eventsNamed("pose_detail_opened"))
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.message == "Pose detail opened" })
    }

    @Test
    fun restoredViewModel_doesNotLogOpenedAgain() {
        val savedState = SavedStateHandle(mapOf("poseId" to "w01"))
        readyViewModel(savedState = savedState)
        assertEquals(1, eventsNamed("pose_detail_opened").size)
        // The same screen after a rotation: a new view model over the same saved state.
        val restored = viewModel(savedState = savedState)
        assertEquals("w01", restored.ready().pose.id)
        assertEquals(listOf(openedEvent()), eventsNamed("pose_detail_opened"))
    }

    // KB-4: shoot

    @Test
    fun shoot_logsPoseSelectedDetail_andReturnsId() {
        val viewModel = readyViewModel()
        assertEquals("w01", viewModel.onShoot())
        assertEquals(
            listOf(
                openedEvent(),
                AnalyticsEvent(
                    "pose_selected",
                    mapOf("pose_id" to "w01", "category" to "women", "source" to "detail"),
                ),
            ),
            analytics.events,
        )
    }

    // Blocking test: it passes with the empty frame (plan T-022, section 4).
    @Test
    fun shootBeforeReady_returnsNull_noEvent() {
        val viewModel = viewModel()
        assertNull(viewModel.onShoot())
        assertTrue(analytics.events.isEmpty())
    }

    // KB-3

    @Test
    fun toggleSave_savesOptimistically_andLogsPoseSaved() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.writeGate = gate
        val viewModel = readyViewModel()
        assertFalse(viewModel.ready().saved)
        viewModel.onToggleSave()
        // The button changes at once, before the write is done.
        assertTrue(viewModel.ready().saved)
        assertEquals(listOf("w01" to true), library.writes)
        assertTrue(eventsNamed("pose_saved").isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()
        assertTrue(viewModel.ready().saved)
        assertEquals(listOf(savedEvent("w01", true)), eventsNamed("pose_saved"))
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.message == "Pose saved: true" })
    }

    @Test
    fun toggleSaveOfSaved_unsaves_logsSavedFalse() = runTest {
        val viewModel = readyViewModel("w02")
        assertTrue(viewModel.ready().saved)
        viewModel.onToggleSave()
        advanceUntilIdle()
        assertEquals(listOf("w02" to false), library.writes)
        assertFalse(viewModel.ready().saved)
        assertEquals(listOf(savedEvent("w02", false)), eventsNamed("pose_saved"))
    }

    @Test
    fun saveDoneBeforeLibraryHasIt_buttonKeepsNewState_untilLibraryAgrees() = runTest {
        // Blocking test (review of T-021): the override bridges the time between the write and the new library value.
        library.publishLater = true
        val viewModel = readyViewModel()
        viewModel.onToggleSave()
        advanceUntilIdle()
        assertEquals(listOf("w01" to true), library.writes)
        assertTrue(viewModel.ready().saved)
        library.publishPending()
        assertTrue(viewModel.ready().saved)
        // The override is gone: a later library value is shown as it is.
        library.ready(testContent(savedIds = listOf("w02")))
        assertFalse(viewModel.ready().saved)
    }

    @Test
    fun toggleSaveWhileWriting_isIgnored() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.writeGate = gate
        val viewModel = readyViewModel()
        viewModel.onToggleSave()
        viewModel.onToggleSave()
        assertEquals(1, library.writes.size)
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(1, library.writes.size)
        assertTrue(viewModel.ready().saved)
        assertEquals(listOf(savedEvent("w01", true)), eventsNamed("pose_saved"))
    }

    @Test
    fun saveFails_reverts_showsMessage_noEvent() = runTest {
        library.failWrites = true
        val viewModel = readyViewModel()
        viewModel.onToggleSave()
        advanceUntilIdle()
        assertFalse(viewModel.ready().saved)
        assertTrue(viewModel.ready().saveFailed)
        assertTrue(eventsNamed("pose_saved").isEmpty())
        val warning = logger.entries.single { it.level == LogLevel.WARN }
        assertEquals("Poses", warning.tag)
        assertTrue(warning.error is IOException)
        viewModel.onSaveFailedShown()
        assertFalse(viewModel.ready().saveFailed)
    }

    // KB-5: the large image

    @Test
    fun imagePending_isLoading_thenReady() = runTest {
        images.delays["w01"] = 5_000
        val viewModel = readyViewModel()
        assertEquals(DetailImageState.Loading, viewModel.ready().image)
        advanceTimeBy(4_999)
        runCurrent()
        assertEquals(DetailImageState.Loading, viewModel.ready().image)
        advanceTimeBy(2)
        runCurrent()
        assertEquals(DetailImageState.Ready(images.imageOf("w01")), viewModel.ready().image)
        assertEquals(1, images.loads["w01"])
    }

    @Test
    fun imageFails_isFailed_andLogsContentLoadFailedImage() = runTest {
        listOf(LoadFailureReason.OTHER, LoadFailureReason.NOT_FOUND, LoadFailureReason.TIMEOUT).forEach { reason ->
            val failing = FakePoseImageRepository { mockk<ImageBitmap>() }.apply { fail("w01", reason) }
            val events = FakeAnalytics()
            val source = FakePosesLibrarySource().apply { ready(detailContent()) }
            val viewModel = PoseDetailViewModel(
                librarySource = source,
                imageRepository = failing,
                networkStatus = network,
                analytics = events,
                logger = logger,
                featureFlags = flags,
                savedStateHandle = SavedStateHandle(mapOf("poseId" to "w01")),
            )
            advanceUntilIdle()
            assertEquals(reason.name, DetailImageState.Failed, viewModel.ready().image)
            assertEquals(
                reason.name,
                listOf(loadFailedEvent(reason.analyticsValue)),
                events.events.filter { it.name == "content_load_failed" },
            )
        }
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.tag == "Poses" })
    }

    @Test
    fun imageTimeout_isFailed_notLoadingForever() = runTest {
        images.fail("w01", LoadFailureReason.TIMEOUT)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(DetailImageState.Failed, viewModel.ready().image)
    }

    @Test
    fun retryImage_loadsAgain_andLogs() = runTest {
        images.fail("w01", LoadFailureReason.NOT_FOUND)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(DetailImageState.Failed, viewModel.ready().image)
        images.results.remove("w01")
        viewModel.onRetryImage()
        advanceUntilIdle()
        assertEquals(2, images.loads["w01"])
        assertEquals(DetailImageState.Ready(images.imageOf("w01")), viewModel.ready().image)
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.message == "Retrying image" })
    }

    // KB-5: network

    @Test
    fun imageFailsNetwork_dialogShown_staysLoading() = runTest {
        network.dialogShown.value = true
        images.fail("w01", LoadFailureReason.NETWORK)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(1, images.loads["w01"])
        assertEquals(DetailImageState.Loading, viewModel.ready().image)
        assertEquals(listOf(loadFailedEvent("network")), eventsNamed("content_load_failed"))
    }

    @Test
    fun imageFailsNetwork_dialogNotShown_isFailed() = runTest {
        images.fail("w01", LoadFailureReason.NETWORK)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(1, images.loads["w01"])
        assertEquals(DetailImageState.Failed, viewModel.ready().image)
        assertEquals(listOf(loadFailedEvent("network")), eventsNamed("content_load_failed"))
    }

    @Test
    fun networkRecovered_reloadsFailedImage() = runTest {
        images.fail("w01", LoadFailureReason.OTHER)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(DetailImageState.Failed, viewModel.ready().image)
        images.results.remove("w01")
        network.recovered.emit(Unit)
        advanceUntilIdle()
        assertEquals(2, images.loads["w01"])
        assertEquals(DetailImageState.Ready(images.imageOf("w01")), viewModel.ready().image)
    }

    @Test
    fun networkRecovered_reloadsImageWaitingUnderDialog() = runTest {
        network.dialogShown.value = true
        images.fail("w01", LoadFailureReason.NETWORK)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(DetailImageState.Loading, viewModel.ready().image)
        images.results.remove("w01")
        network.recovered.emit(Unit)
        advanceUntilIdle()
        assertEquals(2, images.loads["w01"])
        assertEquals(DetailImageState.Ready(images.imageOf("w01")), viewModel.ready().image)
    }

    @Test
    fun networkRecovered_whileImageReady_doesNotLoadAgain() = runTest {
        val viewModel = readyViewModel()
        advanceUntilIdle()
        assertEquals(DetailImageState.Ready(images.imageOf("w01")), viewModel.ready().image)
        network.recovered.emit(Unit)
        advanceUntilIdle()
        assertEquals(1, images.loads["w01"])
    }

    // KB-8: premium poses (plan T-086)

    private fun unlockedEvent(id: String = "w01", category: String = "women") = AnalyticsEvent(
        "premium_pose_unlocked",
        mapOf("pose_id" to id, "category" to category, "source" to "detail"),
    )

    private fun selectedEvent(id: String = "w01", category: String = "women") =
        AnalyticsEvent("pose_selected", mapOf("pose_id" to id, "category" to category, "source" to "detail"))

    /** Every pose id that [viewModel] sends to open the camera, collected for the whole test. */
    private fun TestScope.openedCamera(viewModel: PoseDetailViewModel): List<String> {
        val opened = mutableListOf<String>()
        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) { viewModel.openCamera.toList(opened) }
        return opened
    }

    @Test
    fun lockedPose_isLocked_andSaveStillWorks() = runTest {
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        assertTrue(viewModel.ready().locked)
        viewModel.onToggleSave()
        advanceUntilIdle()
        assertEquals(listOf("w01" to true), library.writes)
        assertEquals(listOf(savedEvent("w01", true)), eventsNamed("pose_saved"))
        // Control: a pose that is not locked is not shown as locked.
        assertFalse(readyViewModel(poseId = "w02", content = detailContent(lockedIds = setOf("w01"))).ready().locked)
    }

    @Test
    fun unlockGranted_unlocks_logsEvent_thenOpensCamera() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.unlockGate = gate
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        val opened = openedCamera(viewModel)
        viewModel.onUnlockGranted()
        assertEquals(listOf("w01"), library.unlocks)
        // The unlock is logged once the write is done, and the camera opens after it.
        assertTrue(eventsNamed("premium_pose_unlocked").isEmpty())
        assertTrue(opened.isEmpty())
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf(openedEvent(), unlockedEvent(), selectedEvent()), analytics.events)
        assertEquals(listOf("w01"), opened)
    }

    @Test
    fun unlockReturnsFalse_noEvent_stillOpensCamera() = runTest {
        library.unlockResult = false
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        val opened = openedCamera(viewModel)
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        assertEquals(listOf("w01"), library.unlocks)
        assertTrue(eventsNamed("premium_pose_unlocked").isEmpty())
        assertEquals(listOf(selectedEvent()), eventsNamed("pose_selected"))
        assertEquals(listOf("w01"), opened)
        assertTrue(logger.entries.any { it.level == LogLevel.INFO && it.message == "Unlock skipped: premium off" })
        assertTrue(logger.entries.none { it.level == LogLevel.WARN })
    }

    @Test
    fun unlockGranted_twiceWhileWriting_writesOnce() = runTest {
        val gate = CompletableDeferred<Unit>()
        library.unlockGate = gate
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        val opened = openedCamera(viewModel)
        viewModel.onUnlockGranted()
        viewModel.onUnlockGranted()
        gate.complete(Unit)
        advanceUntilIdle()
        assertEquals(listOf("w01"), library.unlocks)
        assertEquals(listOf(unlockedEvent()), eventsNamed("premium_pose_unlocked"))
        assertEquals(listOf("w01"), opened)
    }

    @Test
    fun afterUnlock_libraryUpdate_clearsLocked() = runTest {
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        assertTrue(viewModel.ready().locked)
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        // The fake publishes the library without w01 in its locked ids, as :core:poses does.
        assertFalse(viewModel.ready().locked)
    }

    @Test
    fun noGrant_nothingWritten_noNavigation() = runTest {
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        val opened = openedCamera(viewModel)
        advanceUntilIdle()
        assertTrue(library.unlocks.isEmpty())
        assertTrue(eventsNamed("premium_pose_unlocked").isEmpty())
        assertTrue(opened.isEmpty())
        assertTrue(viewModel.ready().locked)
        // Control (L-055): the grant itself writes and opens the camera, so the checks above can fail.
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        assertEquals(listOf("w01"), library.unlocks)
        assertEquals(listOf("w01"), opened)
    }

    @Test
    fun notLocked_grant_opensCamera_withoutWriteOrEvent() = runTest {
        val viewModel = readyViewModel(content = detailContent(lockedIds = emptySet()))
        val opened = openedCamera(viewModel)
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        assertEquals(listOf("w01"), opened)
        assertTrue(library.unlocks.isEmpty())
        assertTrue(eventsNamed("premium_pose_unlocked").isEmpty())
        assertEquals(listOf(selectedEvent()), eventsNamed("pose_selected"))
    }

    @Test
    fun unlockWriteFails_staysLocked_showsMessage_noEvent() = runTest {
        library.failUnlock = true
        val viewModel = readyViewModel(content = detailContent(lockedIds = setOf("w01")))
        val opened = openedCamera(viewModel)
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        assertTrue(viewModel.ready().unlockFailed)
        assertTrue(viewModel.ready().locked)
        assertTrue(eventsNamed("premium_pose_unlocked").isEmpty())
        assertTrue(eventsNamed("pose_selected").isEmpty())
        assertTrue(opened.isEmpty())
        assertEquals(1, logger.entries.count { it.level == LogLevel.WARN })
        viewModel.onUnlockFailedShown()
        assertFalse(viewModel.ready().unlockFailed)
        // The user tries again once the write works.
        library.failUnlock = false
        viewModel.onUnlockGranted()
        advanceUntilIdle()
        assertEquals(listOf("w01", "w01"), library.unlocks)
        assertEquals(listOf(unlockedEvent()), eventsNamed("premium_pose_unlocked"))
        assertEquals(listOf("w01"), opened)
    }

    // ADR-0019

    @Test
    fun logs_useTagPoses_andNoPoseIdsOrNames() = runTest {
        images.fail("w01", LoadFailureReason.TIMEOUT)
        val viewModel = readyViewModel()
        advanceUntilIdle()
        viewModel.onRetryImage()
        advanceUntilIdle()
        viewModel.onToggleSave()
        advanceUntilIdle()
        viewModel.onShoot()
        library.failWrites = true
        viewModel.onToggleSave()
        advanceUntilIdle()
        readyViewModel(poseId = "gone")

        val entries = logger.entries
        assertTrue("entries ${entries.size}", entries.size >= 6)
        assertTrue(entries.all { it.tag == "Poses" })
        val forbidden = DETAIL_POSES.flatMap { pose ->
            listOf(pose.id, pose.name.values.getValue("vi"), pose.name.values.getValue("en"))
        } + listOf("women", "gone")
        entries.forEach { entry ->
            forbidden.forEach { word -> assertFalse("'${entry.message}' has $word", entry.message.contains(word)) }
        }
    }

    internal companion object {
        val DETAIL_POSES = listOf(
            testPose(
                "w01",
                "women",
                SubjectType.SINGLE,
                4,
                name = "Tựa lan can",
                englishName = "Lean on the rail",
                mistakes = listOf("Rút vai lên sát tai làm cổ ngắn lại."),
            ),
            testPose(
                "w02",
                "women",
                SubjectType.SINGLE,
                3,
                name = "Nhìn qua vai",
                englishName = "Look over the shoulder",
            ),
            testPose(
                "w05",
                "women",
                SubjectType.COUPLE,
                5,
                name = "Tựa vai nhau",
                englishName = "Lean on each other",
                framing = Framing.HALF,
                mistakes = listOf("Hai người đứng cách xa nhau.", "Vai trái bị che mất."),
            ),
            testPose("w06", "women", SubjectType.SINGLE, 4, name = "Tay chống hông", englishName = "Hand on hip"),
        )

        /** [savedIds] w02 by default; the first of the list is the one :core:poses puts first. [lockedIds] none. */
        fun detailContent(savedIds: List<String> = listOf("w02"), lockedIds: Set<String> = emptySet()): LibraryContent =
            testContent(savedIds = savedIds, poses = DETAIL_POSES, lockedIds = lockedIds)
    }
}
