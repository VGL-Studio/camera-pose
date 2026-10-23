package com.vglstudio.camera.pose.line.guide.feature.poses.navigation

import androidx.compose.foundation.lazy.grid.LazyGridState
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.material3.Text
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performScrollToIndex
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.toRoute
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.feature.poses.PoseCardItem
import com.vglstudio.camera.pose.line.guide.feature.poses.PoseChip
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesScreen
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesTestTags
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesUiState
import com.vglstudio.camera.pose.line.guide.feature.poses.ThumbnailState
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Coming back from the pose detail keeps the scroll position of the library (spec poses, KB-2; plan T-022, section 4;
 * T-021 left this check to T-022). A real NavHost with the library screen and a plain detail destination. The library
 * keeps its grid state with rememberLazyGridState in the back stack entry, as PosesRoute does; the control keeps it
 * with a plain remember, which the entry does not save, so the check can fail.
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class PosesBackStackTest {
    @get:Rule
    val composeRule = createComposeRule()

    private var navController: NavHostController? = null
    private var gridState: LazyGridState? = null

    private fun setNavHost(savedGridState: Boolean) {
        composeRule.setContent {
            AppTheme(darkTheme = false) {
                val controller = rememberNavController().also { navController = it }
                NavHost(navController = controller, startDestination = PosesDestination) {
                    composable<PosesDestination> {
                        val state = if (savedGridState) rememberLazyGridState() else remember { LazyGridState() }
                        gridState = state
                        PosesScreen(
                            uiState = library(),
                            gridState = state,
                            onBack = {},
                            onOpenSettings = null,
                            onOpenPose = { controller.navigate(PoseDetailDestination(it)) },
                            onSelectFilter = {},
                            onShowAll = {},
                            onToggleSave = {},
                            onRetry = {},
                            onThumbnailNeeded = {},
                            onThumbnailReleased = {},
                            onRetryThumbnail = {},
                            onSaveFailedShown = {},
                        )
                    }
                    composable<PoseDetailDestination> { Text("detail") }
                }
            }
        }
    }

    private fun scrollToCard6AndOpenDetail() {
        composeRule.onNodeWithTag(PosesTestTags.GRID).performScrollToIndex(6)
        composeRule.runOnIdle { assertEquals(6, gridState!!.firstVisibleItemIndex) }
        composeRule.runOnUiThread { navController!!.navigate(PoseDetailDestination("p06")) }
        composeRule.waitForIdle()
        composeRule.runOnIdle {
            val current = navController!!.currentBackStackEntry
            assertEquals(PoseDetailDestination("p06"), current?.toRoute<PoseDetailDestination>())
        }
    }

    @Test
    fun backFromDetail_keepsGridScroll() {
        setNavHost(savedGridState = true)
        scrollToCard6AndOpenDetail()
        composeRule.runOnUiThread { navController!!.popBackStack() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(6, gridState!!.firstVisibleItemIndex) }
    }

    @Test
    fun backFromDetail_controlWithPlainRemember_losesGridScroll() {
        setNavHost(savedGridState = false)
        scrollToCard6AndOpenDetail()
        composeRule.runOnUiThread { navController!!.popBackStack() }
        composeRule.waitForIdle()
        composeRule.runOnIdle { assertEquals(0, gridState!!.firstVisibleItemIndex) }
    }

    private fun library(): PosesUiState.Ready {
        val image = ImageBitmap(8, 8)
        return PosesUiState.Ready(
            totalPoses = 12,
            chips = listOf(PoseChip.All, PoseChip.Saved),
            selected = PoseFilter.All,
            cards = (1..12).map { index ->
                PoseCardItem(
                    id = "p%02d".format(index),
                    categoryId = "women",
                    name = LocalizedText(mapOf("en" to "Pose $index")),
                    subjectType = SubjectType.SINGLE,
                    steps = LocalizedList(mapOf("en" to listOf("Step 1", "Step 2"))),
                    saved = false,
                    thumbnail = ThumbnailState.Ready(image),
                )
            },
        )
    }
}
