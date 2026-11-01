package com.vglstudio.camera.pose.line.guide.feature.camera.pose

import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.ads.RewardedAdDialogTestTags
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.logging.RecordingAppLogger
import com.vglstudio.camera.pose.line.guide.core.poses.model.PremiumAccess
import com.vglstudio.camera.pose.line.guide.feature.camera.FakeAnalytics
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/**
 * The wiring of a locked cell of the camera strip (spec poses, KB-8; camera KB-1, criterion 8; plan T-086): with no ads
 * controller, as when `is_show_rw_premium_pose` is off or consent is closed, a tap unlocks the pose and selects it at
 * once, with no dialog. The real PoseViewModel over the fakes of PoseViewModelTest.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class LockedPoseUnlockTest {
    @get:Rule
    val composeRule = createComposeRule()

    private val library = FakePoseLibraryRepository()
    private val premium = FakePremiumPoseRepository(PremiumAccess(enabled = true, unlockedIds = emptySet()))

    private fun viewModel(): PoseViewModel {
        library.ready(PoseFixtures.library(catalog = PoseFixtures.premiumCatalog("m02")))
        return PoseViewModel(
            libraryRepository = library,
            catalogRepository = FakePoseCatalogRepository(),
            historyRepository = FakePoseHistoryRepository(),
            premiumRepository = premium,
            overlaySettingsRepository = FakePoseOverlaySettingsRepository(),
            decoder = FakePoseImageDecoder(),
            activePose = ActivePoseHolder(),
            networkStatus = FakeNetworkStatus(),
            analytics = FakeAnalytics(),
            logger = RecordingAppLogger(),
            savedStateHandle = SavedStateHandle(),
        )
    }

    private fun PoseViewModel.selectedId() = (uiState.value as PoseUiState.Ready).selected?.id

    @Test
    fun noAdsController_lockedCellTap_unlocksAndSelectsAtOnce() {
        val viewModel = viewModel()
        composeRule.setContent {
            AppTheme(darkTheme = true) {
                val unlock = rememberLockedPoseUnlock(viewModel)
                Button(onClick = { unlock.select("m02") }, modifier = Modifier.testTag(TAP)) { Text("m02") }
                LockedPoseUnlockDialog(unlock, poseName = null)
            }
        }
        assertEquals("w01", viewModel.selectedId())
        assertTrue((viewModel.uiState.value as PoseUiState.Ready).strip.single { it.id == "m02" }.locked)
        composeRule.onNodeWithTag(TAP).performClick()
        composeRule.waitForIdle()
        assertEquals(listOf("m02"), premium.unlocks)
        assertEquals("m02", viewModel.selectedId())
        composeRule.onNodeWithTag(RewardedAdDialogTestTags.CARD).assertDoesNotExist()
    }

    private companion object {
        const val TAP = "tap"
    }
}
