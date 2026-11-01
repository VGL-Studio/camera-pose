package com.vglstudio.camera.pose.line.guide.core.ads

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.Layout
import androidx.compose.ui.layout.Placeable
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.constrainHeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.AppTheme
import com.vglstudio.camera.pose.line.guide.core.designsystem.theme.Spacing
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Size of a native ad drawn inside a screen (ads-guide.md, section 4). [FULL] is the full native ad of
 * ads-business-rules.md 5.7; no ad format draws it yet (plan T-027, Q2). [CARD] is the portrait card of a grid, a list
 * or a pager page (`native_in_collection`, 5.7; plan T-028). [STEP] is the 250 dp card of a flow step (`native_step`,
 * 5.7; plan T-032).
 */
enum class NativeAdSize { SMALL, MEDIUM, FULL, CARD, STEP }

/**
 * What a native ad shows; the SDK fills it in the app, tests and previews use fixed values. [starRating] is the
 * store rating of the advertised app, when the ad has one.
 */
data class NativeAdAssets(
    val headline: String,
    val advertiser: String?,
    val body: String?,
    val callToAction: String?,
    val icon: ImageBitmap?,
    val hasMedia: Boolean,
    val starRating: Double? = null,
)

/**
 * A native ad with fixed content, for screenshots and previews of screens that show a native ad. The app shows real
 * ads with [NativeAdSlot], which uses the same layout. A 16 dp marker shows where the SDK draws AdChoices; real ads
 * never draw it (ads-guide.md, section 2). [cardHeight] sizes a card ([NativeAdSize.CARD], 300 dp without it) to the
 * cards of the app next to it, like a grid cell of [itemsWithInListAds]; other sizes ignore it.
 */
@Composable
fun NativeAdCard(
    assets: NativeAdAssets,
    size: NativeAdSize,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    NativeAdLayout(
        assets,
        size,
        PreviewNativeAdBinder,
        showAdChoicesMarker = true,
        modifier = modifier,
        ctaLarge = ctaLarge,
        cardHeight = cardHeight,
    )
}

/**
 * The loading state of a native ad: same frame and height as the ad, so nothing moves when the ad arrives (KB-1).
 * [animated] off keeps screenshots independent of time. [ctaLarge] is the medium ad with the large call to action.
 * The step card's placeholder is [NativeStepAdDefaults.height] high, like the card, at every font scale (plan T-045).
 * A card's placeholder is [cardHeight] high when set, like the card ([NativeAdCard]).
 */
@Composable
fun NativeAdPlaceholder(
    size: NativeAdSize,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    NativeAdPlaceholderLayout(
        size = size,
        height = when (size) {
            NativeAdSize.STEP -> NativeStepAdDefaults.height()
            NativeAdSize.CARD -> cardHeight ?: NativeAdDimens.CardHeight
            else -> NativeAdDimens.heightOf(size, ctaLarge)
        },
        modifier = modifier,
        animated = animated,
        mediumCallToActionHeight = NativeAdDimens.mediumCallToActionHeight(ctaLarge),
    )
}

/**
 * Sizes of the native ad components, from docs/features/ads/design/context/ (design README, section 3) and
 * ads-business-rules.md 5.7.
 */
internal object NativeAdDimens {
    /** Small native: the drawn card, its placeholder and the space kept for the bottom slot (D-02, D-04). */
    val BottomSlotHeight = 80.dp

    /**
     * Medium, from the redrawn design (design/context/AdGalleryScreen_default_light.md): 1 + 24 top, head 56 (headline,
     * "Ad" line, star row), media 196, body 40, call to action 44, three 12 dp gaps, 12 + 1 bottom.
     */
    val MediumHeight = 410.dp

    /** Medium with the large call to action (`cta_large`, 5.7): 12 dp more. */
    val MediumLargeHeight = 422.dp
    val BorderWidth = 1.dp

    /** Inner margin of the frame; outside the design system scale (design README, section 3). */
    val InnerPadding = 12.dp

    /** Gap between the parts of an ad; outside the design system scale. */
    val PartGap = 12.dp

    /** Gap between the headline and the line below it; outside the design system scale. */
    val HeadlineGap = 2.dp

    /** Gap between the "Ad" label and the text next to it; outside the design system scale. */
    val BadgeGap = 6.dp
    val BadgeCorner = 4.dp
    val IconSize = 40.dp
    val SmallIconSize = 36.dp
    val FullIconSize = 40.dp
    val AssetCorner = 8.dp
    val CallToActionHeight = 36.dp
    val SmallCallToActionHeight = 34.dp
    val MediumCallToActionHeight = 44.dp
    val LargeCallToActionHeight = 56.dp
    val LargeCallToActionTextSize = 17.sp

    /** Short labels ("Install") still get the design's 80 dp, the width of the loading block. */
    val CallToActionMinWidth = 80.dp

    /**
     * Small: the widest its call to action gets, so a long label (cut with "…") leaves room for the headline and the
     * "Ad" label (KB-1, O-08; plan T-060). "Install" stays under it up to font scale 2.0.
     */
    val SmallCallToActionMaxWidth = 120.dp
    val MediaHeight = 196.dp
    val FullMediaHeight = 200.dp

    /** The least media of a native ad, 120 x 120 dp (KB-2, AdMob); the full native ad never shrinks it below this. */
    val MinMediaSize = 120.dp
    val BodyHeight = 40.dp
    val AdChoicesSize = 16.dp
    val StarRowHeight = 16.dp
    val StarSize = StarRowHeight
    val StarGap = 2.dp

    /** Card (5.7): default height of the README example (180 x 300 dp); the cell of a grid may set another. */
    val CardHeight = 300.dp
    val CardPadding = 8.dp
    val CardIconSize = 32.dp
    val CardHeadlineTextSize = 13.sp

    /**
     * Card of a flow step (5.7, component `Native/Step` of the design, T-035): 250 dp at the default font scale
     * ([NativeStepAdDefaults.height] at others, plan T-045), 32 dp icon, 36 dp call to
     * action, 13 sp headline; the 250 dp layout of design/context/AdGalleryScreen_collapsible-expanded_light.md (12 dp
     * margin, 8 dp gaps, media filling the rest). Its own constants on purpose: `Native/Collapsible` has the same
     * numbers today, but the two components change apart.
     */
    val StepHeight = 250.dp
    val StepIconSize = 32.dp
    val StepCallToActionHeight = 36.dp
    val StepHeadlineTextSize = 13.sp

    /**
     * Full native, the height of its placeholder: 1 + 24 top, head 40, media 200, body 40 (two lines), call to action
     * 36, star row 16, four 12 dp gaps between these five parts, 12 + 1 bottom. With large text the media shrinks to
     * keep this height, down to [MinMediaSize]; only past that does the card grow ([fullMediaHeight], plan T-044).
     */
    val FullHeight = 418.dp
    val FullPlaceholderHeight = FullHeight

    /** Loading bars for the two text lines, and the call-to-action block. */
    val LineHeight = 10.dp
    val LongLineWidth = 180.dp
    val ShortLineWidth = 120.dp
    val CallToActionPlaceholderWidth = CallToActionMinWidth

    /**
     * The design height of [size], at the default font scale. Medium and full stay this high up to font scale 2.0;
     * the step card grows with larger text, so composables read [NativeStepAdDefaults.height] for it (plan T-045).
     */
    fun heightOf(size: NativeAdSize, ctaLarge: Boolean = false): Dp = when (size) {
        NativeAdSize.SMALL -> BottomSlotHeight
        NativeAdSize.MEDIUM -> if (ctaLarge) MediumLargeHeight else MediumHeight
        NativeAdSize.FULL -> FullPlaceholderHeight
        NativeAdSize.CARD -> CardHeight
        NativeAdSize.STEP -> StepHeight
    }

    /** The call to action of medium: 44 dp, 56 dp when large. */
    fun mediumCallToActionHeight(ctaLarge: Boolean): Dp =
        if (ctaLarge) LargeCallToActionHeight else MediumCallToActionHeight
}

/** Text sizes of the small native outside the type scale (ads-business-rules.md 5.7, design README, T-035). */
private val SmallCallToActionFontSize = 13.sp
private val SmallBadgeFontSize = 10.sp

internal object NativeAdTestTags {
    const val CARD = "native_ad_card"
    const val PLACEHOLDER = "native_ad_placeholder"
    const val ICON = "native_ad_icon"
    const val HEADLINE = "native_ad_headline"
    const val BADGE = "native_ad_badge"
    const val MEDIA = "native_ad_media"
    const val CTA = "native_ad_cta"
    const val AD_CHOICES_AREA = "native_ad_choices_area"
    const val STAR_ROW = "native_ad_star_row"
    const val STAR = "native_ad_star"
    const val PLACEHOLDER_CTA = "native_ad_placeholder_cta"
}

/** Filled stars of [rating] (O-08): none without a real rating above 0, else rounded half up, at most 5. */
internal fun nativeStarCount(rating: Double?): Int {
    if (rating == null || rating.isNaN() || rating <= 0.0) return 0
    if (rating >= MAX_STARS) return MAX_STARS
    return rating.roundToInt().coerceIn(0, MAX_STARS)
}

private const val MAX_STARS = 5

/**
 * Side margins are measured from the outer edge, as the plan's table reads them (media 355 dp wide in a 379 dp card);
 * the design's export places side parts 1 dp further in, inside the border, which is within its 4 dp tolerance.
 */
private val InnerStart = NativeAdDimens.InnerPadding

/** End margin of small; it keeps the AdChoices corner free. */
private val SmallInnerEnd = Spacing.lg

/** Bottom margin of medium inside the border, so the parts add up to the 410 dp of the design. */
private val MediumInnerBottom = NativeAdDimens.BorderWidth + NativeAdDimens.InnerPadding

/** Top margin of medium inside the border; it keeps the AdChoices corner free. */
private val MediumInnerTop = NativeAdDimens.BorderWidth + Spacing.lg

private const val PULSE_MIN_ALPHA = 0.5f
private const val PULSE_MILLIS = 800

/**
 * The layout of a native ad of [size]. Each ad part goes through [binder], so a fake ad and a real ad bound to the SDK
 * share this layout. A card takes the height a cell gives it; its own height is the default, or [cardHeight] when set.
 */
@Composable
internal fun NativeAdLayout(
    assets: NativeAdAssets,
    size: NativeAdSize,
    binder: NativeAdBinder,
    showAdChoicesMarker: Boolean,
    modifier: Modifier = Modifier,
    ctaLarge: Boolean = false,
    cardHeight: Dp? = null,
) {
    NativeAdFrame(
        modifier = modifier
            .fillMaxWidth()
            .then(
                when (size) {
                    // Medium and full grow past their design height only when their media reaches the floor
                    // (MediaShrinksFirstLayout, plans T-044, T-045).
                    NativeAdSize.MEDIUM, NativeAdSize.FULL ->
                        Modifier.heightIn(min = NativeAdDimens.heightOf(size, ctaLarge))
                    // The step card: 250 dp, more with large text so its media keeps 120 dp (plan T-045).
                    NativeAdSize.STEP -> Modifier.height(NativeStepAdDefaults.height())
                    // A card next to the cards of an app takes their height (plan T-080).
                    NativeAdSize.CARD -> Modifier.height(cardHeight ?: NativeAdDimens.CardHeight)
                    else -> Modifier.height(NativeAdDimens.heightOf(size, ctaLarge))
                },
            )
            .testTag(NativeAdTestTags.CARD),
    ) {
        when (size) {
            NativeAdSize.SMALL -> SmallLayout(assets, binder)
            NativeAdSize.MEDIUM -> MediumLayout(assets, binder, ctaLarge)
            NativeAdSize.FULL -> FullLayout(assets, binder)
            NativeAdSize.CARD -> CardLayout(assets, binder)
            NativeAdSize.STEP -> StepLayout(assets, binder)
        }
        if (showAdChoicesMarker) AdChoicesMarker(Modifier.align(Alignment.TopEnd))
    }
}

@Composable
private fun SmallLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Row(
        modifier = Modifier.fillMaxSize().padding(start = InnerStart, end = SmallInnerEnd),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = NativeAdDimens.SmallIconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            NativeAdHeadline(assets.headline, binder)
            NativeAdBadgeLine(assets, binder, badgeFontSize = SmallBadgeFontSize)
        }
        assets.callToAction?.let {
            NativeAdCallToAction(
                text = it,
                binder = binder,
                modifier = Modifier.widthIn(max = NativeAdDimens.SmallCallToActionMaxWidth),
                height = NativeAdDimens.SmallCallToActionHeight,
                fontSize = SmallCallToActionFontSize,
            )
        }
    }
}

/**
 * Medium (redrawn design, T-035): head with the star row, 196 dp media over a token backdrop, body, call to action of
 * 44 dp (56 dp and 17 sp with [ctaLarge]; the text of the SDK is kept as it is). With larger text the media shrinks
 * first, never below 120 dp, so the card stays 410 dp (422 dp) up to font scale 2.0 ([MediaShrinksFirstLayout], plan
 * T-045).
 */
@Composable
private fun MediumLayout(assets: NativeAdAssets, binder: NativeAdBinder, ctaLarge: Boolean) {
    val ctaHeight = NativeAdDimens.mediumCallToActionHeight(ctaLarge)
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.heightOf(NativeAdSize.MEDIUM, ctaLarge) - MediumInnerTop - MediumInnerBottom,
        gap = NativeAdDimens.PartGap,
        mediaIndex = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
    ) {
        NativeAdHeader(assets, binder, starRow = true)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth(), backdrop = true)
        val bodyModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.BodyHeight)
        val body = assets.body
        if (body != null) NativeAdBody(body, binder, maxLines = 2, modifier = bodyModifier) else Spacer(bodyModifier)
        val callToAction = assets.callToAction
        if (callToAction != null) {
            NativeAdCallToAction(
                text = callToAction,
                binder = binder,
                modifier = Modifier.fillMaxWidth(),
                height = ctaHeight,
                fontSize = if (ctaLarge) NativeAdDimens.LargeCallToActionTextSize else TextUnit.Unspecified,
            )
        } else {
            // Keeps the media at its design size when the ad has no call to action.
            Spacer(Modifier.fillMaxWidth().height(ctaHeight))
        }
    }
}

/**
 * The card of a collection (5.7, portrait): media fills the top (AdChoices sits on it, top right), then the 32 dp
 * icon with a one-line 13 sp headline and the "Ad" line, then a 36 dp call to action as wide as the card.
 */
@Composable
private fun CardLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    Column(
        modifier = Modifier.fillMaxSize().padding(NativeAdDimens.CardPadding),
        verticalArrangement = Arrangement.spacedBy(Spacing.sm),
    ) {
        NativeAdMediaArea(binder, Modifier.fillMaxWidth().weight(1f))
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeAdIcon(assets.icon, binder, size = NativeAdDimens.CardIconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
            ) {
                NativeAdHeadline(assets.headline, binder, fontSize = NativeAdDimens.CardHeadlineTextSize)
                NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
            }
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.CallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
    }
}

/**
 * The card of a flow step (5.7, 250 dp): media fills the top (AdChoices sits on it, top right, O-08), then the 32 dp
 * icon with a one-line 13 sp headline and the "Ad" line, then a 36 dp call to action as wide as the card; 12 dp
 * margin, 8 dp gaps. With larger text the media shrinks first, never below 120 dp; past that the card grows, to
 * [NativeStepAdDefaults.height] ([MediaShrinksFirstLayout], plan T-045).
 */
@Composable
private fun StepLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.StepHeight - NativeAdDimens.InnerPadding * 2,
        gap = Spacing.sm,
        mediaIndex = 0,
        // Filling the card: the media takes what the card's height leaves (the card is set by the same numbers).
        modifier = Modifier.fillMaxSize().padding(NativeAdDimens.InnerPadding),
    ) {
        NativeAdMediaArea(binder, Modifier.fillMaxWidth())
        Row(
            horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            NativeAdIcon(assets.icon, binder, size = NativeAdDimens.StepIconSize)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
            ) {
                NativeAdHeadline(assets.headline, binder, fontSize = NativeAdDimens.StepHeadlineTextSize)
                NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
            }
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.StepCallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
    }
}

/**
 * The full native ad (ads-business-rules.md 5.7): head, media, body of two lines, call to action, star row, 12 dp
 * apart. At font scale 1.0 the media is 200 dp and the card 418 dp, the height of its placeholder. When the text
 * grows (system font size), the media shrinks first, never below [NativeAdDimens.MinMediaSize] ([fullMediaHeight],
 * plan T-044): the call to action, the star row and the two body lines keep their size, and the card stays 418 dp up
 * to font scale 2.0. Only past the floor (font scales a device does not offer) does the card grow. The call to action
 * is 36 dp at least, as high as its line of text with large text (plan T-045).
 */
@Composable
private fun FullLayout(assets: NativeAdAssets, binder: NativeAdBinder) {
    MediaShrinksFirstLayout(
        innerHeight = NativeAdDimens.FullHeight - MediumInnerTop - MediumInnerBottom,
        gap = NativeAdDimens.PartGap,
        mediaIndex = 1,
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom),
    ) {
        NativeAdHeader(assets, binder, iconSize = NativeAdDimens.FullIconSize)
        NativeAdMediaArea(binder, Modifier.fillMaxWidth())
        val bodyModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.BodyHeight)
        val body = assets.body
        if (body != null) {
            NativeAdBody(
                body,
                binder,
                maxLines = FULL_BODY_LINES,
                modifier = bodyModifier,
                minLines = FULL_BODY_LINES,
            )
        } else {
            Spacer(bodyModifier)
        }
        val ctaModifier = Modifier.fillMaxWidth().heightIn(min = NativeAdDimens.CallToActionHeight)
        val callToAction = assets.callToAction
        if (callToAction != null) NativeAdCallToAction(callToAction, binder, ctaModifier) else Spacer(ctaModifier)
        NativeAdStarRow(assets.starRating, binder, Modifier.fillMaxWidth().height(NativeAdDimens.StarRowHeight))
    }
}

private const val FULL_BODY_LINES = 2

/**
 * Lays out the parts of a native ad from the top, [gap] apart, the part at [mediaIndex] being the media: the media
 * shrinks first (plans T-044, T-045). Every other part is measured at its own height (the width is fixed); the media
 * takes what [innerHeight] (or the least height the parent sets, when more) leaves after them
 * ([fullMediaHeight]), never below [NativeAdDimens.MinMediaSize]. Past that floor the layout grows instead. A `Column`
 * cannot hold this floor (a weighted child gets a fixed constraint), so the parts are measured and placed by hand.
 * Each child of [content] is one part.
 */
@Composable
internal fun MediaShrinksFirstLayout(
    innerHeight: Dp,
    gap: Dp,
    mediaIndex: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    Layout(content = content, modifier = modifier) { measurables, constraints ->
        check(mediaIndex in measurables.indices) { "No media at $mediaIndex among ${measurables.size} parts" }
        val width = constraints.maxWidth
        val free = Constraints(minWidth = width, maxWidth = width)
        val placeables = arrayOfNulls<Placeable>(measurables.size)
        measurables.forEachIndexed { index, part -> if (index != mediaIndex) placeables[index] = part.measure(free) }
        val gapPx = gap.roundToPx()
        val others = placeables.sumOf { it?.height ?: 0 } + gapPx * (measurables.size - 1)
        val inner = max(innerHeight.roundToPx(), constraints.minHeight)
        val mediaHeight = fullMediaHeight(inner, others, NativeAdDimens.MinMediaSize.roundToPx())
        placeables[mediaIndex] = measurables[mediaIndex].measure(Constraints.fixed(width, mediaHeight))
        layout(width, constraints.constrainHeight(others + mediaHeight)) {
            var y = 0
            placeables.forEach { part ->
                checkNotNull(part).placeRelative(0, y)
                y += part.height + gapPx
            }
        }
    }
}

/**
 * Height of the media of a native ad whose media shrinks first (full, medium, the step card, the collapsible layer;
 * plans T-044, T-045): what the [inner] height of the card leaves after the [others] (the other parts and their
 * gaps), never below [floor] (120 dp, KB-2). Past the floor the card grows instead.
 */
internal fun fullMediaHeight(inner: Int, others: Int, floor: Int): Int = max(inner - others, floor)

/**
 * Filled stars of the ad's rating (O-08); nothing when the ad has no rating above 0. The outer box keeps its height
 * either way. The stars are the ad's star rating view for the SDK (through [binder]), so a touch on them counts as a
 * touch on the ad (KB-2, plan T-060); with no stars nothing is registered.
 */
@Composable
private fun NativeAdStarRow(rating: Double?, binder: NativeAdBinder, modifier: Modifier = Modifier) {
    val count = nativeStarCount(rating)
    Box(modifier = modifier) {
        if (count > 0) {
            binder.Asset(NativeAdAsset.STAR_RATING, Modifier.height(NativeAdDimens.StarRowHeight)) {
                val description = stringResource(R.string.ads_native_star_rating, count)
                Row(
                    modifier = Modifier
                        .testTag(NativeAdTestTags.STAR_ROW)
                        .semantics { contentDescription = description },
                    horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.StarGap),
                ) {
                    val color = MaterialTheme.colorScheme.tertiary
                    repeat(count) { NativeAdStar(color, Modifier.size(NativeAdDimens.StarSize)) }
                }
            }
        }
    }
}

/** One filled five-pointed star. */
@Composable
private fun NativeAdStar(color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.testTag(NativeAdTestTags.STAR)) {
        val center = Offset(size.width / 2, size.height / 2)
        val outer = size.minDimension / 2
        val inner = outer * STAR_INNER_RATIO
        val path = Path()
        repeat(STAR_POINTS * 2) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / STAR_POINTS
            val point = Offset(center.x + radius * cos(angle).toFloat(), center.y + radius * sin(angle).toFloat())
            if (i == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, color)
    }
}

private const val STAR_POINTS = 5
private const val STAR_INNER_RATIO = 0.45f

/**
 * Icon, headline and the "Ad" line in one row: the head of the medium and full ads. With [starRow] (medium), the
 * 16 dp star row under the "Ad" line, kept empty when the ad has no rating, so the head stays 56 dp high.
 */
@Composable
internal fun NativeAdHeader(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    iconSize: Dp = NativeAdDimens.IconSize,
    starRow: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdIcon(assets.icon, binder, size = iconSize)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(NativeAdDimens.HeadlineGap),
        ) {
            NativeAdHeadline(assets.headline, binder)
            NativeAdBadgeLine(assets, binder, bodyWhenNoAdvertiser = false)
            if (starRow) NativeAdStarRow(assets.starRating, binder, Modifier.height(NativeAdDimens.StarRowHeight))
        }
    }
}

/** Background, 1 dp border and rounded corners that set a native ad apart from the app's content. */
@Composable
internal fun NativeAdFrame(modifier: Modifier = Modifier, content: @Composable BoxScope.() -> Unit) {
    val shape = MaterialTheme.shapes.medium
    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceContainerLow)
            .border(NativeAdDimens.BorderWidth, MaterialTheme.colorScheme.outlineVariant, shape),
        content = content,
    )
}

@Composable
internal fun NativeAdIcon(
    icon: ImageBitmap?,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    size: Dp = NativeAdDimens.IconSize,
) {
    val shape = RoundedCornerShape(NativeAdDimens.AssetCorner)
    binder.Asset(NativeAdAsset.ICON, modifier.size(size).testTag(NativeAdTestTags.ICON)) {
        if (icon != null) {
            Image(
                bitmap = icon,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.size(size).clip(shape),
            )
        } else {
            Box(
                modifier = Modifier
                    .size(size)
                    .clip(shape)
                    .background(MaterialTheme.colorScheme.secondaryContainer),
            )
        }
    }
}

@Composable
internal fun NativeAdHeadline(
    text: String,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    binder.Asset(NativeAdAsset.HEADLINE, modifier.fillMaxWidth().testTag(NativeAdTestTags.HEADLINE)) {
        Text(
            text = text,
            style = MaterialTheme.typography.titleSmall.merge(TextStyle(fontSize = fontSize)),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The "Ad" label and, next to it, the advertiser. Without an advertiser it shows the body when
 * [bodyWhenNoAdvertiser] is true (small has no other place for the body). [badgeFontSize] overrides the label's
 * size where the design sets one outside the type scale.
 */
@Composable
internal fun NativeAdBadgeLine(
    assets: NativeAdAssets,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    bodyWhenNoAdvertiser: Boolean = true,
    badgeFontSize: TextUnit = TextUnit.Unspecified,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.BadgeGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        NativeAdBadge(fontSize = badgeFontSize)
        val advertiser = assets.advertiser
        val body = assets.body
        when {
            advertiser != null -> BadgeLineText(NativeAdAsset.ADVERTISER, advertiser, binder)
            bodyWhenNoAdvertiser && body != null -> BadgeLineText(NativeAdAsset.BODY, body, binder)
        }
    }
}

@Composable
private fun BadgeLineText(asset: NativeAdAsset, text: String, binder: NativeAdBinder) {
    binder.Asset(asset, Modifier.fillMaxWidth()) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** The "Ad" label required on every native ad. It belongs to the app, not to the ad's assets. */
@Composable
internal fun NativeAdBadge(modifier: Modifier = Modifier, fontSize: TextUnit = TextUnit.Unspecified) {
    Box(
        modifier = modifier
            .testTag(NativeAdTestTags.BADGE)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.tertiary)
            .padding(horizontal = Spacing.xs),
    ) {
        Text(
            text = stringResource(R.string.ads_native_badge),
            style = MaterialTheme.typography.labelSmall.merge(TextStyle(fontSize = fontSize)),
            color = MaterialTheme.colorScheme.onTertiary,
            maxLines = 1,
        )
    }
}

@Composable
internal fun NativeAdBody(
    text: String,
    binder: NativeAdBinder,
    maxLines: Int,
    modifier: Modifier = Modifier,
    minLines: Int = 1,
) {
    binder.Asset(NativeAdAsset.BODY, modifier) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = maxLines,
            minLines = minLines,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/**
 * The call to action: tertiary container, at least [height] high (36 dp, 34 dp on small, 44 dp or 56 dp on medium);
 * with large text it is as high as one line of its text (plan T-045). 8 dp corners, unlike the app's PrimaryButton.
 * It is a plain box, not a Button, so a touch reaches the SDK's call-to-action view.
 */
@Composable
internal fun NativeAdCallToAction(
    text: String,
    binder: NativeAdBinder,
    modifier: Modifier = Modifier,
    height: Dp = NativeAdDimens.CallToActionHeight,
    fontSize: TextUnit = TextUnit.Unspecified,
) {
    binder.Asset(NativeAdAsset.CALL_TO_ACTION, modifier.testTag(NativeAdTestTags.CTA)) {
        Box(
            modifier = Modifier
                .heightIn(min = height)
                .widthIn(min = NativeAdDimens.CallToActionMinWidth)
                .clip(RoundedCornerShape(NativeAdDimens.AssetCorner))
                .background(MaterialTheme.colorScheme.tertiaryContainer)
                .padding(horizontal = Spacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge.merge(TextStyle(fontSize = fontSize)),
                color = MaterialTheme.colorScheme.onTertiaryContainer,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

/**
 * The media of the ad (image or video), 8 dp corners. With [backdrop] (medium), an `outlineVariant` layer at 0.6
 * under the media fills what the image leaves free (the "Blurred backdrop" of the design, drawn as a token: plan
 * T-028, question 4).
 */
@Composable
internal fun NativeAdMediaArea(binder: NativeAdBinder, modifier: Modifier = Modifier, backdrop: Boolean = false) {
    val shape = RoundedCornerShape(NativeAdDimens.AssetCorner)
    if (!backdrop) {
        binder.Media(modifier.clip(shape).testTag(NativeAdTestTags.MEDIA))
        return
    }
    Box(modifier = modifier.clip(shape)) {
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(MaterialTheme.colorScheme.outlineVariant.copy(alpha = MEDIA_BACKDROP_ALPHA)),
        )
        binder.Media(Modifier.matchParentSize().testTag(NativeAdTestTags.MEDIA))
    }
}

private const val MEDIA_BACKDROP_ALPHA = 0.6f

/** Where the SDK draws AdChoices; drawn on fake ads only, so screenshots match the design. */
@Composable
private fun AdChoicesMarker(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .padding(top = Spacing.xs, end = Spacing.xs)
            .size(NativeAdDimens.AdChoicesSize)
            .clip(RoundedCornerShape(NativeAdDimens.BadgeCorner))
            .background(MaterialTheme.colorScheme.surfaceContainerHighest)
            .testTag(NativeAdTestTags.AD_CHOICES_AREA),
    )
}

/**
 * The loading blocks of [size] in a frame of [height]. The media block of medium takes the height left, so a layout
 * of another height (collapsible, full screen) reuses it; [mediumCallToActionHeight] is the call-to-action block of
 * medium (36 dp by default, the collapsible form; 44 dp or 56 dp for the medium ad).
 */
@Composable
internal fun NativeAdPlaceholderLayout(
    size: NativeAdSize,
    height: Dp,
    modifier: Modifier = Modifier,
    animated: Boolean = true,
    mediumCallToActionHeight: Dp = NativeAdDimens.CallToActionHeight,
) {
    val alpha = placeholderAlpha(animated)
    NativeAdFrame(modifier = modifier.fillMaxWidth().height(height).testTag(NativeAdTestTags.PLACEHOLDER)) {
        val pulse = Modifier.graphicsLayer { this.alpha = alpha.value }
        when (size) {
            NativeAdSize.SMALL -> Row(
                modifier = Modifier.fillMaxSize().padding(start = InnerStart, end = SmallInnerEnd).then(pulse),
                horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                PlaceholderBlock(Modifier.size(NativeAdDimens.SmallIconSize))
                PlaceholderLines(Modifier.weight(1f))
                PlaceholderBlock(
                    Modifier.size(NativeAdDimens.CallToActionPlaceholderWidth, NativeAdDimens.SmallCallToActionHeight),
                )
            }
            NativeAdSize.FULL -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom)
                    .then(pulse),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            ) {
                FullPlaceholderBlocks()
            }
            NativeAdSize.MEDIUM -> Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = InnerStart, top = MediumInnerTop, end = InnerStart, bottom = MediumInnerBottom)
                    .then(pulse),
                verticalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
            ) {
                MediumPlaceholderBlocks(mediumCallToActionHeight)
            }
            NativeAdSize.CARD -> Column(
                modifier = Modifier.fillMaxSize().padding(NativeAdDimens.CardPadding).then(pulse),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CardPlaceholderBlocks()
            }
            NativeAdSize.STEP -> Column(
                modifier = Modifier.fillMaxSize().padding(NativeAdDimens.InnerPadding).then(pulse),
                verticalArrangement = Arrangement.spacedBy(Spacing.sm),
            ) {
                CardPlaceholderBlocks(NativeAdDimens.StepIconSize, NativeAdDimens.StepCallToActionHeight)
            }
        }
    }
}

@Composable
private fun ColumnScope.MediumPlaceholderBlocks(callToActionHeight: Dp) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.IconSize))
        PlaceholderLines(Modifier.weight(1f))
    }
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
    PlaceholderLines(Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight))
    PlaceholderBlock(
        Modifier.fillMaxWidth().height(callToActionHeight).testTag(NativeAdTestTags.PLACEHOLDER_CTA),
    )
}

@Composable
private fun ColumnScope.CardPlaceholderBlocks(
    iconSize: Dp = NativeAdDimens.CardIconSize,
    callToActionHeight: Dp = NativeAdDimens.CallToActionHeight,
) {
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
    Row(
        horizontalArrangement = Arrangement.spacedBy(Spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(iconSize))
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
            val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
            PlaceholderBlock(Modifier.fillMaxWidth().height(NativeAdDimens.LineHeight), bar)
            PlaceholderBlock(Modifier.fillMaxWidth(fraction = CARD_SHORT_LINE).height(NativeAdDimens.LineHeight), bar)
        }
    }
    PlaceholderBlock(
        Modifier.fillMaxWidth().height(callToActionHeight).testTag(NativeAdTestTags.PLACEHOLDER_CTA),
    )
}

private const val CARD_SHORT_LINE = 0.6f

@Composable
private fun ColumnScope.FullPlaceholderBlocks() {
    Row(
        horizontalArrangement = Arrangement.spacedBy(NativeAdDimens.PartGap),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        PlaceholderBlock(Modifier.size(NativeAdDimens.FullIconSize))
        PlaceholderLines(Modifier.weight(1f))
    }
    PlaceholderBlock(Modifier.fillMaxWidth().height(NativeAdDimens.FullMediaHeight))
    PlaceholderLines(Modifier.fillMaxWidth().height(NativeAdDimens.BodyHeight))
    PlaceholderBlock(Modifier.fillMaxWidth().weight(1f))
}

@Composable
private fun PlaceholderLines(modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(Spacing.sm)) {
        val bar = RoundedCornerShape(NativeAdDimens.LineHeight / 2)
        PlaceholderBlock(Modifier.size(NativeAdDimens.LongLineWidth, NativeAdDimens.LineHeight), bar)
        PlaceholderBlock(Modifier.size(NativeAdDimens.ShortLineWidth, NativeAdDimens.LineHeight), bar)
    }
}

@Composable
private fun PlaceholderBlock(
    modifier: Modifier = Modifier,
    shape: RoundedCornerShape = RoundedCornerShape(NativeAdDimens.AssetCorner),
) {
    Box(modifier = modifier.clip(shape).background(MaterialTheme.colorScheme.surfaceContainerHighest))
}

/** A soft pulse of the loading blocks; full opacity, fixed, when not [animated]. Shared with the full-screen one. */
@Composable
internal fun placeholderAlpha(animated: Boolean): State<Float> {
    if (!animated) return remember { mutableFloatStateOf(1f) }
    return rememberInfiniteTransition(label = "nativeAdPlaceholder").animateFloat(
        initialValue = 1f,
        targetValue = PULSE_MIN_ALPHA,
        animationSpec = infiniteRepeatable(tween(PULSE_MILLIS), RepeatMode.Reverse),
        label = "alpha",
    )
}

@Preview
@Composable
private fun NativeAdCardPreview() {
    val assets = NativeAdAssets(
        headline = "Headline of the ad",
        advertiser = "Advertiser",
        body = "Body text of the ad.",
        callToAction = "Install",
        icon = null,
        hasMedia = true,
    )
    AppTheme {
        Column(verticalArrangement = Arrangement.spacedBy(Spacing.md)) {
            NativeAdCard(assets, NativeAdSize.SMALL)
            NativeAdPlaceholder(NativeAdSize.SMALL, animated = false)
            NativeAdCard(assets.copy(starRating = 4.0), NativeAdSize.MEDIUM)
            NativeAdCard(assets.copy(starRating = 4.0), NativeAdSize.FULL)
            NativeAdCard(assets, NativeAdSize.CARD, Modifier.size(180.dp, 300.dp))
            NativeAdCard(assets, NativeAdSize.STEP)
        }
    }
}
