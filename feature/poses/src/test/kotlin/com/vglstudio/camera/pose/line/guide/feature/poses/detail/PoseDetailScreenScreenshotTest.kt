package com.vglstudio.camera.pose.line.guide.feature.poses.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.runComposeUiTest
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.github.takahirom.roborazzi.captureRoboImage
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.model.Framing
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedText
import com.vglstudio.camera.pose.line.guide.core.poses.model.SubjectType
import com.vglstudio.camera.pose.line.guide.feature.poses.PosesAdSeparator
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Screenshot tests (Roborazzi). Tests in the standard frame are named like the design images in
 * docs/features/poses/design/screens/ and compared by scripts/design-compare.sh (ADR-0020). The pose is the one of the
 * design (context/PoseDetailScreen_default_light.md). The _small tests use the short phone frame (L-028).
 *
 * The loading tests show a spinner that never stops: they stop the clock and capture at a fixed time, in this class
 * without a compose rule, as a rule would open a second window and Roborazzi would capture the whole screen (L-029).
 */
@OptIn(ExperimentalTestApi::class)
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
class PoseDetailScreenScreenshotTest {
    @Test
    fun default_light() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default_light.png") {
            Screen(readyState(), dark = false)
        }
    }

    @Test
    fun default_dark() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default_dark.png") {
            Screen(readyState(), dark = true)
        }
    }

    @Test
    fun error_light() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_error_light.png") {
            Screen(readyState(DetailImageState.Failed), dark = false)
        }
    }

    @Test
    fun error_dark() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_error_dark.png") {
            Screen(readyState(DetailImageState.Failed), dark = true)
        }
    }

    @Test
    fun loading_light() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Screen(readyState(DetailImageState.Loading), dark = false) }
        mainClock.advanceTimeBy(SPINNER_MS)
        onRoot().captureRoboImage("src/test/screenshots/PoseDetailScreen_loading_light.png")
    }

    @Test
    fun loading_dark() = runComposeUiTest {
        mainClock.autoAdvance = false
        setContent { Screen(readyState(DetailImageState.Loading), dark = true) }
        mainClock.advanceTimeBy(SPINNER_MS)
        onRoot().captureRoboImage("src/test/screenshots/PoseDetailScreen_loading_dark.png")
    }

    // The most content: five steps, two common mistakes, a name too long for the top bar.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun default_small() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default_small.png") {
            Screen(readyState(detail = longDetail()), dark = false)
        }
    }

    // The same in Portuguese, the longest main button and second line of the Latin-script languages (plan T-051).
    @Test
    @Config(qualifiers = "pt-rBR-w360dp-h640dp-xxhdpi")
    fun default_ptBr_small() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default-pt-br_small.png") {
            Screen(readyState(detail = longDetail()), dark = false)
        }
    }

    // The retry button and the main button are both on screen.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun error_small() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_error_small.png") {
            Screen(readyState(DetailImageState.Failed), dark = false)
        }
    }

    // Design `ad`: the bottom banner under the main button (a test banner: the real ad is never drawn in tests).
    @Test
    fun ad_light() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_ad_light.png") {
            Screen(readyState(), dark = false, banner = true)
        }
    }

    @Test
    fun ad_dark() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_ad_dark.png") {
            Screen(readyState(), dark = true, banner = true)
        }
    }

    // The most steps, with the banner: the main button and the banner are both on screen.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun ad_small() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_ad_small.png") {
            Screen(readyState(detail = longDetail()), dark = false, banner = true)
        }
    }

    // Design `unfolded` (KB-7): the image on the left, the text on the right, the button bar in the right column.
    @Test
    @Config(qualifiers = UNFOLDED)
    fun default_light_unfolded() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default_light_unfolded.png") {
            Screen(readyState(), dark = false)
        }
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun default_dark_unfolded() {
        captureRoboImage("src/test/screenshots/PoseDetailScreen_default_dark_unfolded.png") {
            Screen(readyState(), dark = true)
        }
    }

    private companion object {
        const val UNFOLDED = "vi-w673dp-h841dp-xxhdpi"
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"

        // A fixed point of the spinner animation where its arc is clearly drawn, as in the design.
        const val SPINNER_MS = 500L

        const val LONG_NAME = "Tựa lan can nhìn về phía hoàng hôn cùng người thương"

        fun text(value: String) = LocalizedText(mapOf("en" to value))

        fun list(vararg values: String) = LocalizedList(mapOf("en" to values.toList()))

        /** The pose of the design: "Tựa lan can", one person, full body, four steps, one common mistake. */
        fun detail(
            name: String = "Tựa lan can",
            subject: SubjectType = SubjectType.SINGLE,
            framing: Framing = Framing.FULL,
            steps: LocalizedList = list(
                "Đứng chéo 45° so với máy, chân trước hơi khuỵu.",
                "Tay trái chống hông, khuỷu tay mở ra sau.",
                "Tay phải thả lỏng dọc thân.",
                "Xoay vai về phía máy, cằm hơi hạ.",
            ),
            mistakes: LocalizedList = list("Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp."),
        ) = PoseDetail(
            id = "w01",
            categoryId = "women",
            name = text(name),
            categoryName = text("Nữ"),
            subjectType = subject,
            framing = framing,
            steps = steps,
            mistakes = mistakes,
        )

        fun longDetail() = detail(
            name = LONG_NAME,
            subject = SubjectType.COUPLE,
            framing = Framing.HALF,
            steps = list(
                "Đứng chéo 45° so với máy, chân trước hơi khuỵu.",
                "Tay trái chống hông, khuỷu tay mở ra sau.",
                "Tay phải thả lỏng dọc thân.",
                "Xoay vai về phía máy, cằm hơi hạ.",
                "Nhìn qua vai về phía ống kính, mắt mở tự nhiên và thả lỏng cổ.",
            ),
            mistakes = list(
                "Rút vai lên sát tai làm cổ ngắn lại. Thả vai xuống trước khi chụp.",
                "Hai người đứng cách xa nhau nên khung hình bị trống ở giữa.",
            ),
        )

        fun readyState(image: DetailImageState = DetailImageState.Ready(figure()), detail: PoseDetail = detail()) =
            PoseDetailUiState.Ready(pose = detail, saved = false, image = image)

        /** A figure drawn with lines on a transparent background, so the category color shows, as on the design. */
        fun figure(): ImageBitmap {
            val width = 1137
            val height = 1140
            val image = ImageBitmap(width, height)
            val canvas = Canvas(image)
            val paint = Paint().apply {
                color = Color(0xFF111111)
                style = PaintingStyle.Stroke
                strokeWidth = 9f
                strokeCap = StrokeCap.Round
                strokeJoin = StrokeJoin.Round
                isAntiAlias = true
            }
            // The figure box of the design: 150 x 300 in a 379 x 380 image.
            val boxWidth = width * 150f / 379f
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
private fun Screen(uiState: PoseDetailUiState, dark: Boolean, banner: Boolean = false) {
    AppTheme(darkTheme = dark) {
        PoseDetailScreen(
            uiState = uiState,
            onBack = {},
            onToggleSave = {},
            onRetryImage = {},
            onShoot = {},
            onSaveFailedShown = {},
            bottomBanner = if (banner) {
                { TestBanner() }
            } else {
                {}
            },
        )
    }
}

/** The banner of the design: the separator, then 56 dp of ad (58 dp in all, as the design's ad slot). */
@Composable
private fun TestBanner() {
    Column(Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surfaceContainerHigh)) {
        PosesAdSeparator()
        Box(Modifier.fillMaxWidth().height(56.dp))
    }
}
