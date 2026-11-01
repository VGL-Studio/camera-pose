package com.vglstudio.camera.pose.line.guide.feature.poses

import androidx.compose.foundation.layout.PaddingValues
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
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarDefaults
import com.vglstudio.camera.pose.line.guide.core.ads.BottomAdBarLayout
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdAssets
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdCard
import com.vglstudio.camera.pose.line.guide.core.ads.NativeAdSize
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.poses.LoadFailureReason
import com.vglstudio.camera.pose.line.guide.core.poses.model.LocalizedList
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
@Config(qualifiers = "vi-w411dp-h891dp-xxhdpi")
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

    // Design `ad`: the native bottom bar over the grid; the cards scroll behind it (fixed ad: the real one is never
    // drawn in tests).
    @Test
    fun ad_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_ad_light.png") {
            Screen(defaultState(), dark = false, ad = true)
        }
    }

    @Test
    fun ad_dark() {
        captureRoboImage("src/test/screenshots/PosesScreen_ad_dark.png") {
            Screen(defaultState(), dark = true, ad = true)
        }
    }

    // The grid scrolls above the bottom bar.
    @Test
    @Config(qualifiers = SHORT_SCREEN)
    fun ad_small() {
        captureRoboImage("src/test/screenshots/PosesScreen_ad_small.png") {
            Screen(defaultState(longName = true), dark = false, ad = true)
        }
    }

    // Design `ad-collection` (KB-6, criterion 5; plan T-080): the ad card after the eighth pose, the grid scrolled to
    // card 7, no bottom bar (design README, section 3). A fixed ad: the real one is never drawn in tests.
    @Test
    fun adCollection_light() {
        captureRoboImage("src/test/screenshots/PosesScreen_ad-collection_light.png") {
            Screen(
                defaultState(ready = AD_COLLECTION_CARDS.size, cards = AD_COLLECTION_CARDS),
                dark = false,
                gridAds = PosesGridAds.Fixed(every = 8) { modifier, cardHeight ->
                    NativeAdCard(SCREENSHOT_CARD_AD, NativeAdSize.CARD, modifier, cardHeight = cardHeight)
                },
                firstVisibleItem = 6,
            )
        }
    }

    // Design `unfolded` (KB-7): three columns, the same data as `default` (six cards in two rows).
    @Test
    @Config(qualifiers = UNFOLDED)
    fun default_light_unfolded() {
        captureRoboImage("src/test/screenshots/PosesScreen_default_light_unfolded.png") {
            Screen(defaultState(), dark = false)
        }
    }

    @Test
    @Config(qualifiers = UNFOLDED)
    fun default_dark_unfolded() {
        captureRoboImage("src/test/screenshots/PosesScreen_default_dark_unfolded.png") {
            Screen(defaultState(), dark = true)
        }
    }

    private companion object {
        const val UNFOLDED = "vi-w673dp-h841dp-xxhdpi"
        const val SHORT_SCREEN = "vi-w360dp-h640dp-xxhdpi"

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

        // Twelve cards of design `ad-collection` (context/PosesScreen_ad-collection_light.md): cards 7 to 11 as drawn.
        val AD_COLLECTION_CARDS = CARDS + listOf(
            SampleCard("c02", "couples", "Ôm từ phía sau", SubjectType.COUPLE, 4),
            SampleCard("fa01", "family", "Cả nhà nắm tay", SubjectType.GROUP, 3),
            SampleCard("w02", "women", "Đứng tựa cửa sổ", SubjectType.SINGLE, 4),
            SampleCard("t02", "travel", "Ngồi bên hồ", SubjectType.SINGLE, 3),
            SampleCard("k02", "cafe", "Đọc sách bên cửa sổ", SubjectType.SINGLE, 3),
            SampleCard("f02", "friends", "Nhảy cùng nhau", SubjectType.GROUP, 4),
        )

        const val LONG_NAME = "Tựa lan can nhìn về phía hoàng hôn"

        fun text(value: String) = LocalizedText(mapOf("en" to value))

        /** Design `default`: "Tất cả" selected, the second card saved, every image ready. */
        fun defaultState(
            longName: Boolean = false,
            ready: Int = CARDS.size,
            cards: List<SampleCard> = CARDS,
        ): PosesUiState.Ready {
            val image = figure()
            return PosesUiState.Ready(
                totalPoses = 214,
                chips = listOf(PoseChip.All, PoseChip.Saved) +
                    CHIPS.map { (id, name) -> PoseChip.Category(id, text(name)) },
                selected = PoseFilter.All,
                cards = cards.mapIndexed { index, card ->
                    PoseCardItem(
                        id = card.id,
                        categoryId = card.category,
                        name = text(if (longName && index == 0) LONG_NAME else card.name),
                        subjectType = card.type,
                        steps = LocalizedList(mapOf("en" to (1..card.steps).map { "Step $it" })),
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
private fun Screen(
    uiState: PosesUiState,
    dark: Boolean,
    ad: Boolean = false,
    gridAds: PosesGridAds = PosesGridAds.None,
    firstVisibleItem: Int = 0,
) {
    AppTheme(darkTheme = dark) {
        val screen = @Composable { contentPadding: PaddingValues ->
            PosesScreen(
                uiState = uiState,
                gridState = rememberLazyGridState(initialFirstVisibleItemIndex = firstVisibleItem),
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
                contentPadding = contentPadding,
                gridAds = gridAds,
            )
        }
        if (ad) {
            // The bottom bar of the framework with a fixed native small ad, as the design (plan T-079, section 3.1).
            BottomAdBarLayout(
                reservation = BottomAdBarDefaults.nativeReservation,
                slot = { NativeAdCard(SCREENSHOT_AD, NativeAdSize.SMALL, it) },
                layer = null,
                content = screen,
            )
        } else {
            screen(PaddingValues())
        }
    }
}

/** The ad of the design (design/context/PosesScreen_ad_light.md): no icon, no media. */
private val SCREENSHOT_AD = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = null,
    callToAction = "Install",
    icon = null,
    hasMedia = false,
)

/** The ad card of design `ad-collection` (design/context/PosesScreen_ad-collection_light.md): icon and media. */
private val SCREENSHOT_CARD_AD = NativeAdAssets(
    headline = "Headline of the ad",
    advertiser = "Advertiser or short body",
    body = null,
    callToAction = "Install",
    icon = solidIcon(Color(0xFFFFC83D)),
    hasMedia = true,
)

/** A square icon of one [color], as the icon block of the design. */
private fun solidIcon(color: Color): ImageBitmap {
    val image = ImageBitmap(96, 96)
    Canvas(image).drawRect(0f, 0f, 96f, 96f, Paint().apply { this.color = color })
    return image
}
