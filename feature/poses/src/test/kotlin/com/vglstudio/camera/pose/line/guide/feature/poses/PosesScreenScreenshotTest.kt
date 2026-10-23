package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.PoseFilter
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Tests in the standard frame are named like the design images in
 * docs/features/poses/design/screens/ and compared by scripts/design-compare.sh (ADR-0020). Every test passes a
 * settings action, so the top bar looks like the design. The _small tests use the short phone frame (L-028).
 */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w411dp-h891dp-xxhdpi")
class PosesScreenScreenshotTest {
    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_default_light.png") { Screen(defaultState(), dark = false) }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/PosesScreen_default_dark.png") { Screen(defaultState(), dark = true) }
    }

    @Test
    fun loading_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_loading_light.png") { Screen(loadingState(), dark = false) }
    }

    @Test
    fun loading_dark() {
        captureRoboImage("src/test/screenshots/PosesScreen_loading_dark.png") { Screen(loadingState(), dark = true) }
    }

    @Test
    fun error_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_error_light.png") { Screen(ERROR, dark = false) }
    }

    @Test
    fun error_dark() {
        captureRoboImage("src/test/screenshots/PosesScreen_error_dark.png") { Screen(ERROR, dark = true) }
    }

    @Test
    fun savedEmpty_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_saved-empty_light.png") {
            Screen(savedEmptyState(), dark = false)
        }
    }

    @Test
    fun savedEmpty_dark() {
        captureRoboImage("src/test/screenshots/PosesScreen_saved-empty_dark.png") {
            Screen(savedEmptyState(), dark = true)
        }
    }

    // The most content: eight chips, six cards, one name on two lines.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun default_small() {
        captureRoboImage("src/test/screenshots/PosesScreen_default_small.png") {
            Screen(defaultState(longName = true), dark = false)
        }
    }

    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun savedEmpty_small() {
        captureRoboImage("src/test/screenshots/PosesScreen_saved-empty_small.png") {
            Screen(savedEmptyState(), dark = false)
        }
    }

    private companion object {
        const val SHORT_SCREEN = "w360dp-h640dp-xxhdpi"

        val ERROR = PosesUiState.Error(LoadFailureReason.OTHER)

        // Chips of the design (context/PosesScreen_default_light.md).
        val CHIPS = listOf(
            "women" to "Nữ",
            "men" to "Nam",
            "couples" to "Cặp đôi",
            "friends" to "Bạn bè",
            "family" to "Gia đình",
            "kids" to "Trẻ em",
            "travel" to "Du lịch",
            "cafe" to "Cà phê",
        )

        // Cards of the design, with the second line of the text table (plan T-021, question 11).
        data class SampleCard(
            val id: String,
            val category: String,
            val name: String,
            val type: SubjectType,
            val steps: Int,
        )

        val CARDS = listOf(
            SampleCard("w01", "women", "Tựa lan can", SubjectType.SINGLE, 4),
            SampleCard("m01", "men", "Nhìn qua vai", SubjectType.SINGLE, 3),
            SampleCard("c01", "couples", "Bước đi tự nhiên", SubjectType.COUPLE, 5),
            SampleCard("f01", "friends", "Ngồi bậc thềm", SubjectType.GROUP, 4),
            SampleCard("t01", "travel", "Tay chống hông", SubjectType.SINGLE, 3),
            SampleCard("k01", "cafe", "Cầm ly cà phê", SubjectType.SINGLE, 3),
        )

        const val LONG_NAME = "Tựa lan can nhìn về phía hoàng hôn"

        fun text(value: String) = LocalizedText(mapOf("en" to value))

        /** Design `default`: "Tất cả" selected, the second card saved, every image ready. */
        fun defaultState(longName: Boolean = false, ready: Int = CARDS.size): PosesUiState.Ready {
            val image = figure()
            return PosesUiState.Ready(
                totalPoses = 214,
                chips = listOf(PoseChip.All, PoseChip.Saved) +
                    CHIPS.map { (id, name) -> PoseChip.Category(id, text(name)) },
                selected = PoseFilter.All,
                cards = CARDS.mapIndexed { index, card ->
                    PoseCardItem(
                        id = card.id,
                        categoryId = card.category,
                        name = text(if (longName && index == 0) LONG_NAME else card.name),
                        subjectType = card.type,
                        stepCount = card.steps,
                        saved = index == 1,
                        thumbnail = if (index < ready) ThumbnailState.Ready(image) else ThumbnailState.Loading,
                    )
                },
            )
        }

        /** Design `loading`: the first two cards have their image, the others are skeletons. */
        fun loadingState() = defaultState(ready = 2)

        fun savedEmptyState() = defaultState().copy(selected = PoseFilter.Saved, cards = emptyList())

        /** A pose figure drawn with lines on a transparent background, so the category color shows, as on the design. */
        fun figure(): ImageBitmap {
            val width = 486
            val height = 630
            val image = ImageBitmap(width, height)
            val canvas = Canvas(image)
            val paint = Paint().apply {
                color = Color(0xFF111111)
                style = PaintingStyle.Stroke
                strokeWidth = 6f
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                isAntiAlias = true
            }
            // The figure box of the design: 80 x 160 in a 162 x 210 image.
            val boxWidth = width * 80f / 162f
            val boxHeight = boxWidth * 2
            val left = (width - boxWidth) / 2
            val top = (height - boxHeight) / 2
            fun point(x: Float, y: Float) = Offset(left + x * boxWidth, top + y * boxHeight)
            canvas.drawCircle(point(0.5f, 0.1f), boxWidth * 0.13f, paint)
            val lines = listOf(
                listOf(0.18f to 0.5f, 0.28f to 0.25f, 0.72f to 0.25f, 0.8f to 0.52f),
                listOf(0.3f to 0.25f, 0.34f to 0.55f, 0.66f to 0.55f, 0.7f to 0.25f),
                listOf(0.38f to 0.55f, 0.36f to 0.95f),
                listOf(0.62f to 0.55f, 0.64f to 0.95f),
            )
            lines.forEach { line ->
                val path = Path()
                line.forEachIndexed { index, (x, y) ->
                    val p = point(x, y)
                    if (index == 0) path.moveTo(p.x, p.y) else path.lineTo(p.x, p.y)
                }
                canvas.drawPath(path, paint)
            }
            return image
        }
    }
}

@Composable
private fun Screen(uiState: PosesUiState, dark: Boolean) {
    AppTheme(darkTheme = dark) {
        PosesScreen(
            uiState = uiState,
            gridState = rememberLazyGridState(),
            onBack = {},
            onOpenSettings = {},
            onOpenPose = {},
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
}
