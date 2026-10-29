package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.max
import androidx.compose.ui.unit.min
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlin.math.ceil
import kotlin.math.floor

/**
 * The heights the page of a flow step is laid out by (plans T-041, T-049), at the font scale of [flowStepMetrics]: the
 * head of the ad ([headerHeight]), the least height of the column layout ([columnMinHeight], media at its 120 dp
 * floor and no body), the least height of the page ([minHeight], the compact row: 152 dp at font scale 1.0; the page
 * takes no ad below it), and one line of the body ([bodyLineHeight]).
 */
internal data class FlowStepMetrics(
    val headerHeight: Dp,
    val columnMinHeight: Dp,
    val minHeight: Dp,
    val bodyLineHeight: Dp,
)

/** Margins of the column, from design/context/NativeFullScreenAdScreen_default_light.md: 24 top, 32 bottom. */
private val ColumnTop = Spacing.lg
private val ColumnBottom = Spacing.xl

/** Margin of the compact row on its four sides, and the gap between all parts of the page. */
private val RowPadding = Spacing.md
private val PartGap = Spacing.md

/** Height of the media when the page has no height bound (in a scrolling area): the media of the full native ad. */
private val UnboundedMediaHeight = NativeAdDimens.FullMediaHeight

/** Width of the media in the compact row, as a part of the inner width; never under 120 dp. */
private const val ROW_MEDIA_WIDTH_FRACTION = 0.4f

private const val BODY_MAX_LINES = 3

/**
 * The [FlowStepMetrics] of the page at [density] (screen density and font scale, with the non-linear font table of
 * Android 14) and [typography]. The head of the ad holds the headline and the "Ad" line, 48 dp at least (the design's
 * number at font scale 1.0, where the page needs 360 dp for its column and 152 dp in all).
 */
internal fun flowStepMetrics(density: Density, typography: Typography): FlowStepMetrics = with(density) {
    val badgeLine = max(lineHeightOf(typography.labelSmall), lineHeightOf(typography.bodySmall))
    val textHeader = lineHeightOf(typography.titleMedium) + Spacing.xs + badgeLine
    val header = max(NativeFullScreenAdDimens.HeaderHeight, textHeader)
    val topBar = NativeFullScreenAdDimens.TopBarHeight
    val cta = NativeFullScreenAdDimens.CallToActionHeight
    val minMedia = NativeAdDimens.MinMediaSize
    FlowStepMetrics(
        headerHeight = header,
        columnMinHeight = ColumnTop + topBar + PartGap + minMedia + PartGap + header + PartGap + cta + ColumnBottom,
        minHeight = RowPadding + max(minMedia, rowTextColumnMin(header)) + RowPadding,
        bodyLineHeight = lineHeightOf(typography.bodyMedium),
    )
}

/**
 * The height of one line of [style] as Compose lays it out, in whole pixels. With the non-linear font scale of
 * Android 14, Compose keeps the line height in proportion to the font size it draws, so a line is the converted font
 * size times lineHeight / fontSize, not the line height converted on its own (which is shorter for large text).
 */
private fun Density.lineHeightOf(style: TextStyle): Dp {
    val line = style.lineHeight
    val size = style.fontSize
    val px = if (line.isSp && size.isSp && size.value > 0f) {
        size.toPx() * (line.value / size.value)
    } else {
        line.toPx()
    }
    return ceil(px).toDp()
}

/**
 * The right side of the compact row with no body (plan T-049): the line of the head and the 36 dp cell kept for
 * AdChoices beside it, then the call to action, 16 dp apart.
 */
private fun rowTextColumnMin(header: Dp): Dp =
    max(NativeFullScreenAdDimens.TopBarHeight, header) + PartGap + NativeFullScreenAdDimens.CallToActionHeight

/** Lines of the body that fit in [room] (its 16 dp gap included), 3 at most; 0 or less draws no body. */
private fun bodyLines(room: Dp, line: Dp): Int {
    if (line <= 0.dp) return 0
    return minOf(BODY_MAX_LINES, floor((room - PartGap) / line).toInt())
}

/**
 * The page of a flow step (variant `Flow step` of the design, plans T-032, T-041 and T-049): no close button, a
 * 36 dp cell kept for AdChoices in the top right corner (O-08), no padding for the safe area. The layout follows the
 * height the app gives:
 * - no height bound (a scrolling area): the column, media of 200 dp, body of 3 lines;
 * - [FlowStepMetrics.columnMinHeight] or more: the column of the design, AdChoices alone in its 36 dp top bar; the
 *   media shrinks first, never under 120 dp ([NativeAdDimens.MinMediaSize]), then the body loses lines (3, 2, 1,
 *   none);
 * - less: the compact row, media on the left, the text and the call to action on the right; the 36 dp cell of
 *   AdChoices sits right of the head of the ad, so no part of the ad cuts into the corner where the SDK pins the
 *   real AdChoices. Under [FlowStepMetrics.minHeight] (152 dp at font scale 1.0) the page takes no ad
 *   ([NativeFullScreenStepGate]).
 * The head of the ad grows with the font scale, and so do these heights. Each ad part goes through [binder].
 */
@Composable
internal fun FlowStepPageLayout(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    media: @Composable BoxScope.() -> Unit = {},
    adChoices: @Composable () -> Unit = {},
) {
    val body = assets.body
    FlowStepPageFrame(
        modifier = modifier,
        topBar = { barModifier -> FlowStepTopBar(adChoices, barModifier) },
        media = { mediaModifier ->
            Box(
                modifier = mediaModifier
                    .clip(MaterialTheme.shapes.large)
                    .background(MaterialTheme.colorScheme.surfaceContainer)
                    .testTag(NativeFullScreenAdTestTags.MEDIA),
                content = media,
            )
        },
        header = { NativeFullScreenAdHeader(assets, binder) },
        body = body?.let { text ->
            { lines ->
                NativeAdBody(
                    text,
                    binder,
                    maxLines = lines,
                    modifier = Modifier.fillMaxWidth().testTag(NativeFullScreenAdTestTags.BODY),
                )
            }
        },
        callToAction = { assets.callToAction?.let { NativeFullScreenCallToAction(it, binder) } },
    )
}

/**
 * The page of a flow step while it waits for an ad (plan T-041): static blocks where the media, the head of the ad
 * and the call to action come, in the layout the ad will take. No text, no "Ad" label, no animation (L-048).
 */
@Composable
internal fun FlowStepPagePlaceholder(modifier: Modifier = Modifier) {
    val block = MaterialTheme.colorScheme.surfaceContainer
    FlowStepPageFrame(
        modifier = modifier,
        topBar = { barModifier -> Spacer(barModifier) },
        media = { mediaModifier ->
            Box(mediaModifier.clip(MaterialTheme.shapes.large).background(block))
        },
        header = { headerHeight ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(headerHeight)
                    .testTag(NativeFlowStepTestTags.PLACEHOLDER_HEADER),
                horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    Modifier
                        .size(NativeFullScreenAdDimens.IconSize)
                        .clip(MaterialTheme.shapes.small)
                        .background(block),
                )
                Column(verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
                    PlaceholderLine(NativeAdDimens.LongLineWidth, block)
                    PlaceholderLine(NativeAdDimens.ShortLineWidth, block)
                }
            }
        },
        body = null,
        callToAction = {
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(NativeFullScreenAdDimens.CallToActionHeight)
                    .clip(MaterialTheme.shapes.small)
                    .background(block)
                    .testTag(NativeFlowStepTestTags.PLACEHOLDER_CTA),
            )
        },
    )
}

/** A loading bar of the head, [width] wide at most, so it fits the narrow side of the compact row. */
@Composable
private fun PlaceholderLine(width: Dp, color: Color) {
    Box(
        Modifier
            .widthIn(max = width)
            .fillMaxWidth()
            .height(NativeAdDimens.LineHeight)
            .clip(MaterialTheme.shapes.extraSmall)
            .background(color),
    )
}

/**
 * The 36 dp top bar of the page of a flow step, sized by [modifier] (the width of the column, or the 36 dp cell
 * beside the head in the compact row): AdChoices on the right only, no control.
 */
@Composable
private fun FlowStepTopBar(adChoices: @Composable () -> Unit, modifier: Modifier) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.End,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        adChoices()
    }
}

/**
 * Picks the layout of the page from the height it gets ([FlowStepPageLayout]) and lays out the parts: [topBar] and
 * [media] (each sized by the modifier it gets), [header] (given the head height of the font scale), [body] (given the
 * lines that fit; null for none) and [callToAction].
 */
@Composable
private fun FlowStepPageFrame(
    modifier: Modifier,
    topBar: @Composable (Modifier) -> Unit,
    media: @Composable (Modifier) -> Unit,
    header: @Composable (headerHeight: Dp) -> Unit,
    body: (@Composable (lines: Int) -> Unit)?,
    callToAction: @Composable () -> Unit,
) {
    val metrics = flowStepMetrics(LocalDensity.current, MaterialTheme.typography)
    BoxWithConstraints(modifier = modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        val bounded = constraints.hasBoundedHeight
        when {
            !bounded || maxHeight >= metrics.columnMinHeight -> {
                val lines = if (bounded) {
                    bodyLines(maxHeight - metrics.columnMinHeight, metrics.bodyLineHeight)
                } else {
                    BODY_MAX_LINES
                }
                FlowStepColumn(
                    fixedMediaHeight = if (bounded) null else UnboundedMediaHeight,
                    topBar = { topBar(Modifier.fillMaxWidth().height(NativeFullScreenAdDimens.TopBarHeight)) },
                    media = { media(Modifier.fillMaxSize()) },
                    header = { header(metrics.headerHeight) },
                    body = if (body != null && lines > 0) ({ body(lines) }) else null,
                    callToAction = callToAction,
                )
            }
            else -> {
                val innerHeight = maxHeight - RowPadding * 2
                val innerWidth = maxWidth - RowPadding * 2
                val lines = bodyLines(innerHeight - rowTextColumnMin(metrics.headerHeight), metrics.bodyLineHeight)
                val mediaWidth =
                    max(NativeAdDimens.MinMediaSize, min(innerHeight, innerWidth * ROW_MEDIA_WIDTH_FRACTION))
                Row(
                    modifier = Modifier.fillMaxSize().padding(RowPadding),
                    horizontalArrangement = Arrangement.spacedBy(PartGap),
                ) {
                    media(Modifier.fillMaxHeight().width(mediaWidth))
                    Column(modifier = Modifier.weight(1f).fillMaxHeight()) {
                        // The head and, on its right, the cell kept for AdChoices (plan T-049).
                        Row(modifier = Modifier.fillMaxWidth()) {
                            Box(modifier = Modifier.weight(1f)) { header(metrics.headerHeight) }
                            topBar(Modifier.size(NativeFullScreenAdDimens.TopBarHeight))
                        }
                        if (body != null && lines > 0) {
                            Spacer(Modifier.height(PartGap))
                            body(lines)
                        }
                        Spacer(Modifier.height(PartGap))
                        Spacer(Modifier.weight(1f))
                        callToAction()
                    }
                }
            }
        }
    }
}

/**
 * The column of the page: top bar, media, head, body, call to action, 16 dp apart, in the margins of the design. The
 * media takes what the other parts leave, never under 120 dp, or [fixedMediaHeight] with no height bound. A `Column`
 * cannot hold that floor (a weighted child gets a fixed constraint), so the parts are measured and placed by hand, as
 * the full native ad does (plan T-044).
 */
@Composable
private fun FlowStepColumn(
    fixedMediaHeight: Dp?,
    topBar: @Composable () -> Unit,
    media: @Composable () -> Unit,
    header: @Composable () -> Unit,
    body: (@Composable () -> Unit)?,
    callToAction: @Composable () -> Unit,
) {
    Layout(
        contents = listOf(topBar, media, header, body ?: {}, callToAction),
        modifier = Modifier
            .fillMaxSize()
            .padding(start = Spacing.md, top = ColumnTop, end = Spacing.md, bottom = ColumnBottom),
    ) { (topBars, medias, headers, bodies, callsToAction), constraints ->
        val width = constraints.maxWidth
        val free = Constraints(minWidth = width, maxWidth = width)
        val above = topBars.map { it.measure(free) }
        val below = (headers + bodies + callsToAction).map { it.measure(free) }
        val gap = PartGap.roundToPx()
        val parts = above.size + medias.size + below.size
        val others = above.sumOf { it.height } + below.sumOf { it.height } + gap * (parts - 1)
        val mediaHeight = if (fixedMediaHeight != null || !constraints.hasBoundedHeight) {
            (fixedMediaHeight ?: UnboundedMediaHeight).roundToPx()
        } else {
            fullMediaHeight(constraints.maxHeight, others, NativeAdDimens.MinMediaSize.roundToPx())
        }
        val placedMedia = medias.map { it.measure(Constraints.fixed(width, mediaHeight)) }
        val height = constraints.constrainHeight(others + placedMedia.sumOf { it.height })
        layout(width, if (constraints.hasBoundedHeight) constraints.maxHeight else height) {
            var y = 0
            (above + placedMedia + below).forEach { part ->
                part.placeRelative(0, y)
                y += part.height + gap
            }
        }
    }
}
