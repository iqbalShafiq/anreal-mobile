package co.ratmo.anreal.feature.auth.presentation.component

import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealMark
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Format_quote
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Language
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class BoardingSlideKind {
    Documents,
    Research,
    Images,
}

data class BoardingSlideUi(
    val kind: BoardingSlideKind,
    val title: String,
    val body: String,
)

fun boardingSlides(): List<BoardingSlideUi> {
    return listOf(
        BoardingSlideUi(
            kind = BoardingSlideKind.Documents,
            title = AnrealCopy.get(AnrealCopy.BOARDING_DOCUMENTS_TITLE),
            body = AnrealCopy.get(AnrealCopy.BOARDING_DOCUMENTS_BODY),
        ),
        BoardingSlideUi(
            kind = BoardingSlideKind.Research,
            title = AnrealCopy.get(AnrealCopy.BOARDING_WEB_TITLE),
            body = AnrealCopy.get(AnrealCopy.BOARDING_WEB_BODY),
        ),
        BoardingSlideUi(
            kind = BoardingSlideKind.Images,
            title = AnrealCopy.get(AnrealCopy.BOARDING_IMAGES_TITLE),
            body = AnrealCopy.get(AnrealCopy.BOARDING_IMAGES_BODY),
        ),
    )
}

@Composable
fun BoardingBrandHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AnrealMark(
            size = 36.dp,
            wordmark = AnrealCopy.get(AnrealCopy.LABEL_APP_NAME),
            contentDescription = AnrealCopy.get(AnrealCopy.CD_APP_MARK),
        )
        Text(
            text = AnrealCopy.get(AnrealCopy.BOARDING_TAGLINE),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
fun BoardingCarousel(
    modifier: Modifier = Modifier,
    paused: Boolean = false,
    slides: List<BoardingSlideUi> = remember { boardingSlides() },
    pagerState: PagerState = rememberPagerState(pageCount = { slides.size }),
) {
    val reduceMotion = LocalAnrealReduceMotion.current
    LaunchedEffect(pagerState.settledPage, paused, reduceMotion, slides.size) {
        if (paused || reduceMotion || slides.size <= 1) return@LaunchedEffect
        delay(AnrealMotion.durationBoardingHold)
        if (pagerState.isScrollInProgress) return@LaunchedEffect
        pagerState.animateScrollToPage(
            page = (pagerState.settledPage + 1) % slides.size,
            animationSpec = tween(
                durationMillis = AnrealMotion.durationMed.inWholeMilliseconds.toInt(),
                easing = AnrealMotion.easeInOut,
            ),
        )
    }

    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            beyondViewportPageCount = 1,
        ) { page ->
            BoardingSlide(
                slide = slides[page],
                pageDescription = UiText.StringResource(
                    AnrealCopy.CD_BOARDING_PAGE,
                    listOf((page + 1).toString(), slides.size.toString()),
                ).asString(),
            )
        }
        BoardingPageIndicator(
            pageCount = slides.size,
            page = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            modifier = Modifier.align(Alignment.CenterHorizontally),
        )
    }
}

@Composable
private fun BoardingSlide(
    slide: BoardingSlideUi,
    pageDescription: String,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .semantics { contentDescription = pageDescription },
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md, Alignment.CenterVertically),
    ) {
        BoardingSlideVisual(
            kind = slide.kind,
            modifier = Modifier
                .fillMaxWidth()
                .height(BoardingStageHeight),
        )
        Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
            Text(
                text = slide.title,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.headlineSmallEmphasized,
                color = MaterialTheme.colorScheme.onSurface,
            )
            Text(
                text = slide.body,
                modifier = Modifier.fillMaxWidth(),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun BoardingSlideVisual(
    kind: BoardingSlideKind,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier.semantics { hideFromAccessibility() }) {
        when (kind) {
            BoardingSlideKind.Documents -> DocumentsStoryVisual()
            BoardingSlideKind.Research -> ResearchStoryVisual()
            BoardingSlideKind.Images -> ImagesStoryVisual()
        }
    }
}

@Composable
private fun DocumentsStoryVisual() {
    Box(modifier = Modifier.fillMaxSize()) {
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(176.dp)
                .height(152.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Regular,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                StoryLabel(
                    icon = MaterialSymbols.Rounded.Description,
                    label = AnrealCopy.get(AnrealCopy.BOARDING_DOCUMENT_NAME),
                )
                repeat(4) { index -> StoryLine(fraction = 1f - index * 0.12f) }
            }
        }
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(248.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Thin,
            emphasized = true,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_USER_PROMPT),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_ASSISTANT_REPLY),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
                StoryLabel(
                    icon = MaterialSymbols.Rounded.Format_quote,
                    label = AnrealCopy.get(AnrealCopy.BOARDING_CITATION),
                    compact = true,
                )
            }
        }
    }
}

@Composable
private fun ResearchStoryVisual() {
    Box(modifier = Modifier.fillMaxSize()) {
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopEnd)
                .width(244.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Regular,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                StoryLabel(
                    icon = MaterialSymbols.Rounded.Language,
                    label = AnrealCopy.get(AnrealCopy.BOARDING_WEB_TOOL),
                )
                SourceRow(
                    domain = AnrealCopy.get(AnrealCopy.BOARDING_SOURCE_ONE_NAME),
                    detail = AnrealCopy.get(AnrealCopy.BOARDING_SOURCE_ONE_DETAIL),
                )
                SourceRow(
                    domain = AnrealCopy.get(AnrealCopy.BOARDING_SOURCE_TWO_NAME),
                    detail = AnrealCopy.get(AnrealCopy.BOARDING_SOURCE_TWO_DETAIL),
                )
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_WEB_SOURCE),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .width(236.dp),
            shape = MaterialTheme.shapes.large,
            tone = GlassTone.Pane,
            emphasized = true,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_APPROVAL_TITLE),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_APPROVAL_ACTION),
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
        }
    }
}

@Composable
private fun ImagesStoryVisual() {
    Box(modifier = Modifier.fillMaxSize()) {
        GlassSurface(
            modifier = Modifier
                .align(Alignment.TopStart)
                .width(224.dp)
                .height(164.dp),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Regular,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.sm),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                StoryLabel(
                    icon = MaterialSymbols.Rounded.Image,
                    label = AnrealCopy.get(AnrealCopy.BOARDING_IMAGE_PROMPT),
                    compact = true,
                )
                AbstractHeatmap()
            }
        }
        GlassSurface(
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .width(220.dp),
            shape = MaterialTheme.shapes.large,
            tone = GlassTone.Thin,
            emphasized = true,
        ) {
            Row(
                modifier = Modifier.padding(AnrealSpacing.sm),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                StoryIcon(
                    icon = MaterialSymbols.Rounded.Edit,
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
                )
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.BOARDING_IMAGE_EDIT),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                    Text(
                        text = AnrealCopy.get(AnrealCopy.BOARDING_IMAGE_CAPTION),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun AbstractHeatmap() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(MaterialTheme.shapes.medium)
            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
            .padding(AnrealSpacing.xs),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
    ) {
        repeat(3) { row ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            ) {
                repeat(4) { column ->
                    val color = when ((row + column) % 3) {
                        0 -> MaterialTheme.colorScheme.primaryContainer
                        1 -> MaterialTheme.colorScheme.secondaryContainer
                        else -> MaterialTheme.colorScheme.tertiaryContainer
                    }
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(20.dp)
                            .clip(MaterialTheme.shapes.extraSmall)
                            .background(color),
                    )
                }
            }
        }
    }
}

@Composable
private fun SourceRow(domain: String, detail: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary),
        )
        Text(
            text = domain,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = detail,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoryLabel(
    icon: ImageVector,
    label: String,
    compact: Boolean = false,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        StoryIcon(
            icon = icon,
            size = if (compact) 28.dp else 36.dp,
            iconSize = if (compact) 16.dp else 20.dp,
        )
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = if (compact) {
                MaterialTheme.typography.labelMedium
            } else {
                MaterialTheme.typography.labelLarge
            },
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun StoryIcon(
    icon: ImageVector,
    size: Dp = 36.dp,
    iconSize: Dp = 20.dp,
    containerColor: Color = MaterialTheme.colorScheme.surfaceContainerHighest,
    contentColor: Color = MaterialTheme.colorScheme.onSurfaceVariant,
) {
    Box(
        modifier = Modifier
            .size(size)
            .background(containerColor, MaterialTheme.shapes.medium),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(iconSize),
            tint = contentColor,
        )
    }
}

@Composable
private fun StoryLine(fraction: Float) {
    Box(
        modifier = Modifier
            .fillMaxWidth(fraction)
            .height(6.dp)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.14f)),
    )
}

@Composable
private fun BoardingPageIndicator(
    pageCount: Int,
    page: () -> Float,
    modifier: Modifier = Modifier,
) {
    val density = LocalDensity.current
    val slotPx = with(density) { (IndicatorPill + IndicatorGap).roundToPx() }
    val trackWidth = IndicatorPill * pageCount + IndicatorGap * (pageCount - 1).coerceAtLeast(0)
    Box(
        modifier = modifier
            .width(trackWidth)
            .height(IndicatorDot)
            .semantics { hideFromAccessibility() },
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(IndicatorGap),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            repeat(pageCount) {
                Box(
                    modifier = Modifier.width(IndicatorPill),
                    contentAlignment = Alignment.CenterStart,
                ) {
                    Box(
                        modifier = Modifier
                            .size(IndicatorDot)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f)),
                    )
                }
            }
        }
        Box(
            modifier = Modifier
                .offset {
                    IntOffset(
                        x = (page().coerceIn(0f, (pageCount - 1).toFloat()) * slotPx).roundToInt(),
                        y = 0,
                    )
                }
                .size(width = IndicatorPill, height = IndicatorDot)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

private val BoardingStageHeight = 212.dp
private val IndicatorDot = 6.dp
private val IndicatorPill = 16.dp
private val IndicatorGap = 8.dp

@AnrealPreviews
@Composable
private fun BoardingBrandHeaderPreview() {
    AnrealPreview {
        BoardingBrandHeader(modifier = Modifier.padding(AnrealSpacing.md))
    }
}

@AnrealPreviews
@Composable
private fun BoardingCarouselDocumentsPreview() {
    AnrealPreview {
        Box(modifier = Modifier.height(380.dp).padding(AnrealSpacing.md)) {
            BoardingCarousel(
                paused = true,
                pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 }),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun BoardingCarouselResearchPreview() {
    AnrealPreview {
        Box(modifier = Modifier.height(380.dp).padding(AnrealSpacing.md)) {
            BoardingCarousel(
                paused = true,
                pagerState = rememberPagerState(initialPage = 1, pageCount = { 3 }),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun BoardingCarouselImagesPreview() {
    AnrealPreview {
        Box(modifier = Modifier.height(380.dp).padding(AnrealSpacing.md)) {
            BoardingCarousel(
                paused = true,
                pagerState = rememberPagerState(initialPage = 2, pageCount = { 3 }),
            )
        }
    }
}
