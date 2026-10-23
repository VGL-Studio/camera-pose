package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.analytics.AnalyticsEvent
import com.vglstudio.camera.pose.line.guide.core.logging.LogLevel
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.PoseContent
import com.vglstudio.camera.pose.line.guide.core.poses.PoseFileResult
import com.vglstudio.camera.pose.line.guide.core.poses.PoseLoadFailure
import com.vglstudio.camera.pose.line.guide.core.poses.model.LibraryState
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFile
import com.vglstudio.camera.pose.line.guide.core.poses.model.SavedPose
import com.vglstudio.camera.pose.line.guide.core.poses.model.UsedPose
import com.vglstudio.camera.pose.line.guide.feature.camera.FakeAnalytics
import com.vglstudio.camera.pose.line.guide.feature.camera.MainDispatcherRule
import com.vglstudio.camera.pose.line.guide.feature.camera.pose.FakePoseCatalogRepository.Companion.fileOf
import java.io.IOException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The pose part of the camera (spec camera, KB-1, KB-2; plan T-014). Robolectric because ImageBitmap needs Bitmap;
 * native graphics, because the default mode cannot create the bitmaps of the fake decoder.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class PoseViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val library = FakePoseLibraryRepository()
    private val catalog = FakePoseCatalogRepository()
    private val history = FakePoseHistoryRepository()
    private val overlay = FakePoseOverlaySettingsRepository()
    private val decoder = FakePoseImageDecoder()
    private val activePose = ActivePoseHolder()
    private val network = FakeNetworkStatus()
    private val analytics = FakeAnalytics()
    private val logger = RecordingAppLogger()

    private fun viewModel(savedState: SavedStateHandle = SavedStateHandle()) = PoseViewModel(
        libraryRepository = library,
        catalogRepository = catalog,
        historyRepository = history,
        overlaySettingsRepository = overlay,
        decoder = decoder,
        activePose = activePose,
        networkStatus = network,
        analytics = analytics,
        logger = logger,
        savedStateHandle = savedState,
    )

    /** A view model over the fixture catalog with [saved] and [recent] applied. */
    private fun readyViewModel(
        saved: List<SavedPose> = emptyList(),
        recent: List<UsedPose> = emptyList(),
        savedState: SavedStateHandle = SavedStateHandle(),
    ): PoseViewModel {
        library.ready(PoseFixtures.library(saved, recent))
        return viewModel(savedState)
    }

    private fun PoseViewModel.ready(): PoseUiState.Ready {
        val value = uiState.value
        assertTrue("state $value", value is PoseUiState.Ready)
        return value as PoseUiState.Ready
    }

    private fun PoseViewModel.selected(): SelectedPose {
        val selected = ready().selected
        assertTrue("no pose selected", selected != null)
        return selected!!
    }

    private fun PoseViewModel.strip() = ready().strip.map { it.id }

    private fun PoseViewModel.thumb(id: String) = ready().strip.first { it.id == id }.image

    private fun failed(content: PoseContent, reason: LoadFailureReason) =
        PoseFileResult.Failed(PoseLoadFailure(content, reason))

    private fun selectedEvent(id: String, category: String, source: String) =
        AnalyticsEvent("pose_selected", mapOf("pose_id" to id, "category" to category, "source" to source))

    private fun loadFailedEvent(content: String, reason: String) =
        AnalyticsEvent("content_load_failed", mapOf("content" to content, "reason" to reason))

    // KB-1: pose on opening

    @Test
    fun firstOpen_selectsDefaultPose_withoutEventOrUse() {
        val viewModel = readyViewModel()
        val selected = viewModel.selected()
        assertEquals("w01", selected.id)
        assertEquals(0, selected.stepIndex)
        assertTrue(catalog.loads.contains("w01" to PoseFile.OUTLINE))
        assertTrue(analytics.events.isEmpty())
        assertTrue(history.used.isEmpty())
        assertEquals("w01", activePose.current.value?.id)
    }

    @Test
    fun lastUsedPose_isSelectedOnOpen_withOutline() {
        val viewModel = readyViewModel(recent = listOf(UsedPose("m02", 5)))
        val selected = viewModel.selected()
        assertEquals("m02", selected.id)
        assertEquals(PoseImage.Ready(decoder.bitmapOf(fileOf("m02", PoseFile.OUTLINE))), selected.outline)
    }

    @Test
    fun lastUsedMissingFromCatalog_fallsBackToDefault() {
        val viewModel = readyViewModel(recent = listOf(UsedPose("gone", 5)))
        assertEquals("w01", viewModel.selected().id)
    }

    // KB-1: chips and strip order

    @Test
    fun chips_areCategoriesWithPoses_inCatalogOrder() {
        val viewModel = readyViewModel()
        assertEquals(listOf(CategoryChip("women", "Nữ"), CategoryChip("men", "Nam")), viewModel.ready().categories)
        assertNull(viewModel.ready().selectedCategoryId)
    }

    @Test
    fun strip_all_savedThenRecentThenCatalog() {
        val viewModel = readyViewModel(
            saved = listOf(SavedPose("w03", 9)),
            recent = listOf(UsedPose("m02", 8), UsedPose("w02", 7)),
        )
        assertEquals(listOf("w03", "m02", "w02", "w01", "m01"), viewModel.strip())
        assertEquals(listOf("Pose w03", "Pose m02"), viewModel.ready().strip.take(2).map { it.label })
    }

    @Test
    fun strip_category_sameOrderWithinGroup() {
        val viewModel = readyViewModel(
            saved = listOf(SavedPose("w03", 9)),
            recent = listOf(UsedPose("m02", 8), UsedPose("w02", 7)),
        )
        viewModel.onCategorySelect("women")
        assertEquals("women", viewModel.ready().selectedCategoryId)
        assertEquals(listOf("w03", "w02", "w01"), viewModel.strip())
        assertEquals("m02", viewModel.selected().id)
        assertEquals(0, viewModel.ready().scrollRequest?.index)
    }

    @Test
    fun selectingPose_doesNotReorderStrip() {
        val viewModel = readyViewModel()
        val before = viewModel.strip()
        viewModel.onPoseSelect("m01")
        // markUsed makes the library emit again with m01 first.
        library.ready(PoseFixtures.library(recent = listOf(UsedPose("m01", 99))))
        assertEquals(before, viewModel.strip())
        assertEquals("m01", viewModel.selected().id)
    }

    // KB-1: tapping a cell

    @Test
    fun selectFromStrip_logsEvent_marksUsed_resetsStep() {
        val viewModel = readyViewModel()
        viewModel.onNextStep()
        assertEquals(1, viewModel.selected().stepIndex)

        viewModel.onPoseSelect("m01")
        assertEquals(listOf(selectedEvent("m01", "men", "strip")), analytics.events)
        assertEquals(listOf("m01"), history.used)
        val selected = viewModel.selected()
        assertEquals("m01", selected.id)
        assertEquals(0, selected.stepIndex)
        assertEquals(3, selected.steps.size)
        assertTrue(catalog.loads.contains("m01" to PoseFile.OUTLINE))
        assertEquals("m01", activePose.current.value?.id)
    }

    @Test
    fun selectSamePose_doesNothing() {
        val viewModel = readyViewModel()
        viewModel.onPoseSelect("w01")
        assertEquals("w01", viewModel.selected().id)
        assertTrue(analytics.events.isEmpty())
        assertTrue(history.used.isEmpty())
        assertEquals(1, catalog.loads.count { it == "w01" to PoseFile.OUTLINE })
    }

    @Test
    fun markUsedFails_keepsSelection_andLogsWarning() {
        history.failWrites = true
        val viewModel = readyViewModel()
        viewModel.onPoseSelect("m01")
        assertEquals("m01", viewModel.selected().id)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.error is IOException })
    }

    // KB-1: opened from the pose detail (T-022)

    @Test
    fun poseIdArgument_selectsThatPose_andScrollsToIt() {
        val viewModel = readyViewModel(savedState = SavedStateHandle(mapOf("poseId" to "w02")))
        val state = viewModel.ready()
        assertEquals("w02", viewModel.selected().id)
        assertNull(state.selectedCategoryId)
        assertEquals(state.strip.indexOfFirst { it.id == "w02" }, state.scrollRequest?.index)
        assertEquals(listOf("w02"), history.used)
        assertTrue(analytics.events.none { it.name == "pose_selected" })
    }

    @Test
    fun poseIdSetLater_switchesPose_andSwitchesChipToAll() {
        val savedState = SavedStateHandle()
        val viewModel = readyViewModel(savedState = savedState)
        viewModel.onCategorySelect("men")
        assertEquals("men", viewModel.ready().selectedCategoryId)

        savedState["poseId"] = "w03"
        assertEquals("w03", viewModel.selected().id)
        assertNull(viewModel.ready().selectedCategoryId)
        assertNull(savedState.get<String?>("poseId"))
        assertEquals(listOf("w03"), history.used)
        assertTrue(analytics.events.none { it.name == "pose_selected" })
    }

    @Test
    fun poseIdNotInCatalog_keepsCurrent_andLogsWarning() {
        val savedState = SavedStateHandle()
        val viewModel = readyViewModel(savedState = savedState)
        savedState["poseId"] = "gone"
        assertEquals("w01", viewModel.selected().id)
        assertTrue(
            logger.entries.any {
                it.level == LogLevel.WARN && it.message.contains("Requested pose is not in the catalog")
            },
        )
    }

    // KB-1: thumbnails

    @Test
    fun thumbnailLoading_doesNotBlockSelection() {
        catalog.pending["m01" to PoseFile.THUMBNAIL] = CompletableDeferred()
        val viewModel = readyViewModel()
        viewModel.onThumbShown("m01")
        assertEquals(PoseImage.Loading, viewModel.thumb("m01"))
        viewModel.onPoseSelect("m01")
        assertEquals("m01", viewModel.selected().id)
    }

    @Test
    fun thumbShown_loadsOnce() {
        val viewModel = readyViewModel()
        viewModel.onThumbShown("m01")
        viewModel.onThumbShown("m01")
        assertEquals(1, catalog.loads.count { it == "m01" to PoseFile.THUMBNAIL })
        assertEquals(PoseImage.Ready(decoder.bitmapOf(fileOf("m01", PoseFile.THUMBNAIL))), viewModel.thumb("m01"))
    }

    @Test
    fun thumbnailFails_marksFailed_logsEvent_andTapRetries() {
        catalog.results["m01" to PoseFile.THUMBNAIL] = failed(PoseContent.THUMBNAIL, LoadFailureReason.NETWORK)
        val viewModel = readyViewModel()
        viewModel.onThumbShown("m01")
        assertEquals(PoseImage.Failed, viewModel.thumb("m01"))
        assertEquals(listOf(loadFailedEvent("thumbnail", "network")), analytics.events)

        viewModel.onThumbShown("m01")
        assertEquals(1, catalog.loads.count { it == "m01" to PoseFile.THUMBNAIL })

        catalog.results.remove("m01" to PoseFile.THUMBNAIL)
        viewModel.onPoseSelect("m01")
        assertEquals(2, catalog.loads.count { it == "m01" to PoseFile.THUMBNAIL })
        assertTrue(viewModel.thumb("m01") is PoseImage.Ready)
        assertEquals("m01", viewModel.selected().id)
    }

    @Test
    fun undecodableThumbnail_isFailed_withReasonOther() {
        decoder.undecodable += fileOf("m01", PoseFile.THUMBNAIL)
        val viewModel = readyViewModel()
        viewModel.onThumbShown("m01")
        assertEquals(PoseImage.Failed, viewModel.thumb("m01"))
        assertEquals(listOf(loadFailedEvent("thumbnail", "other")), analytics.events)
    }

    // KB-1: no pose, catalog errors

    @Test
    fun emptyCatalog_readyWithoutPose() {
        library.ready(PoseFixtures.library(catalog = PoseFixtures.catalog(poses = emptyList())))
        val viewModel = viewModel()
        val state = viewModel.ready()
        assertNull(state.selected)
        assertEquals(emptyList<PoseThumbItem>(), state.strip)
        assertNull(activePose.current.value)
    }

    @Test
    fun libraryFailed_isUnavailable_andRetryCallsLibrary() {
        library.state.value = LibraryState.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.NETWORK))
        val viewModel = viewModel()
        assertEquals(PoseUiState.Unavailable, viewModel.uiState.value)
        viewModel.onRetryLibrary()
        assertEquals(1, library.retryCount)
    }

    @Test
    fun networkRecovered_reloadsFailedImagesAndLibrary() = runTest {
        catalog.results["m01" to PoseFile.THUMBNAIL] = failed(PoseContent.THUMBNAIL, LoadFailureReason.NETWORK)
        catalog.results["w01" to PoseFile.OUTLINE] = failed(PoseContent.OUTLINE, LoadFailureReason.NETWORK)
        val viewModel = readyViewModel()
        viewModel.onThumbShown("m01")
        assertEquals(PoseImage.Failed, viewModel.thumb("m01"))
        assertEquals(PoseImage.Failed, viewModel.selected().outline)

        catalog.results.clear()
        network.recovered.emit(Unit)
        assertTrue(viewModel.thumb("m01") is PoseImage.Ready)
        assertTrue(viewModel.selected().outline is PoseImage.Ready)
        assertEquals(0, library.retryCount)
    }

    @Test
    fun networkRecovered_whenUnavailable_retriesLibrary() = runTest {
        library.state.value = LibraryState.Failed(PoseLoadFailure(PoseContent.CATALOG, LoadFailureReason.NETWORK))
        val viewModel = viewModel()
        assertEquals(PoseUiState.Unavailable, viewModel.uiState.value)
        network.recovered.emit(Unit)
        assertEquals(1, library.retryCount)
    }

    // Spec section 7: outline

    @Test
    fun outlineFails_marksFailed_logsEvent_andRetryReloads() {
        catalog.results["w01" to PoseFile.OUTLINE] = failed(PoseContent.OUTLINE, LoadFailureReason.TIMEOUT)
        val viewModel = readyViewModel()
        assertEquals(PoseImage.Failed, viewModel.selected().outline)
        assertEquals(listOf(loadFailedEvent("outline", "timeout")), analytics.events)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.message.contains("Pose outline failed") })

        catalog.results.clear()
        viewModel.onRetryOutline()
        assertTrue(viewModel.selected().outline is PoseImage.Ready)
    }

    @Test
    fun changingPose_ignoresOldOutlineResult() {
        val oldOutline = CompletableDeferred<Unit>()
        catalog.pending["w01" to PoseFile.OUTLINE] = oldOutline
        val viewModel = readyViewModel()
        assertEquals(PoseImage.Loading, viewModel.selected().outline)

        viewModel.onPoseSelect("m01")
        oldOutline.complete(Unit)
        val selected = viewModel.selected()
        assertEquals("m01", selected.id)
        val outline = selected.outline
        assertTrue("outline $outline", outline is PoseImage.Ready)
        assertSame(decoder.bitmapOf(fileOf("m01", PoseFile.OUTLINE)), (outline as PoseImage.Ready).bitmap)
    }

    // KB-2: steps

    @Test
    fun steps_moveWithinBounds() {
        val viewModel = readyViewModel()
        viewModel.onPreviousStep()
        assertEquals(0, viewModel.selected().stepIndex)
        repeat(3) { viewModel.onNextStep() }
        assertEquals(3, viewModel.selected().stepIndex)
        viewModel.onNextStep()
        assertEquals(3, viewModel.selected().stepIndex)
        viewModel.onPreviousStep()
        assertEquals(2, viewModel.selected().stepIndex)
    }

    @Test
    fun hideSteps_isSaved_andKeptWhenPoseChanges() {
        val viewModel = readyViewModel()
        assertEquals(false, viewModel.ready().stepsHidden)
        viewModel.onHideSteps()
        assertEquals(true, viewModel.ready().stepsHidden)
        assertEquals(listOf(true), overlay.saves)

        viewModel.onPoseSelect("m01")
        assertEquals(true, viewModel.ready().stepsHidden)

        viewModel.onShowSteps()
        assertEquals(false, viewModel.ready().stepsHidden)
        assertEquals(listOf(true, false), overlay.saves)
    }

    @Test
    fun stepsHiddenStored_isHiddenOnOpen() {
        overlay.stored.value = true
        val viewModel = readyViewModel()
        assertEquals(true, viewModel.ready().stepsHidden)
    }

    @Test
    fun saveStepsFails_keepsChoice_andLogsWarning() {
        overlay.failWrites = true
        val viewModel = readyViewModel()
        viewModel.onHideSteps()
        assertEquals(true, viewModel.ready().stepsHidden)
        assertTrue(logger.entries.any { it.level == LogLevel.WARN && it.error is IOException })
    }

    // KB-6: next pose (for T-016)

    @Test
    fun nextPose_followsStrip_andWraps() {
        val viewModel = readyViewModel()
        viewModel.onCategorySelect("women")
        assertEquals(listOf("w01", "w02", "w03"), viewModel.strip())
        viewModel.onPoseSelect("w02")
        analytics.events.clear()

        viewModel.onNextPose()
        assertEquals("w03", viewModel.selected().id)
        assertEquals(listOf(selectedEvent("w03", "women", "next")), analytics.events)

        viewModel.onNextPose()
        assertEquals("w01", viewModel.selected().id)
    }
}
