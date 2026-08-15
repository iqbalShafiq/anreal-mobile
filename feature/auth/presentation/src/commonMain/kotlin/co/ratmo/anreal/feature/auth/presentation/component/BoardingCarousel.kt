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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
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
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Language
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

enum class BoardingSlideKind {
    Documents,
    Web,
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
            kind = BoardingSlideKind.Web,
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
        val next = (pagerState.settledPage + 1) % slides.size
        pagerState.animateScrollToPage(
            page = next,
            animationSpec = tween(
                durationMillis = AnrealMotion.durationMed.inWholeMilliseconds.toInt(),
                easing = AnrealMotion.easeInOut,
            ),
        )
    }
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
    ) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f, fill = true),
            beyondViewportPageCount = 1,
            userScrollEnabled = true,
        ) { page ->
            val slide = slides[page]
            BoardingSlide(
                slide = slide,
                pageDescription = UiText.StringResource(
                    AnrealCopy.CD_BOARDING_PAGE,
                    listOf((page + 1).toString(), slides.size.toString()),
                ).asString(),
            )
        }
        BoardingPageIndicator(
            pageCount = slides.size,
            page = { pagerState.currentPage + pagerState.currentPageOffsetFraction },
            modifier = Modifier.padding(horizontal = AnrealSpacing.screenCompact),
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
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md, Alignment.CenterVertically),
    ) {
        BoardingSlideVisual(
            kind = slide.kind,
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
        )
        Text(
            text = slide.title,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AnrealSpacing.screenCompact),
            style = MaterialTheme.typography.titleLargeEmphasized,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = slide.body,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AnrealSpacing.screenCompact),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun BoardingSlideVisual(
    kind: BoardingSlideKind,
    modifier: Modifier = Modifier,
) {
    GlassSurface(
        modifier = modifier,
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.Pane,
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(AnrealSpacing.md)
                .semantics { hideFromAccessibility() },
            contentAlignment = Alignment.Center,
        ) {
            when (kind) {
                BoardingSlideKind.Documents -> DocumentsPreview()
                BoardingSlideKind.Web -> WebPreview()
                BoardingSlideKind.Images -> ImagesPreview()
            }
        }
    }
}

@Composable
private fun DocumentsPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End,
        ) {
            GlassSurface(
                shape = MaterialTheme.shapes.large,
                tone = GlassTone.Thin,
            ) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_USER_PROMPT),
                    modifier = Modifier.padding(
                        horizontal = AnrealSpacing.sm,
                        vertical = AnrealSpacing.xs,
                    ),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
            Text(
                text = AnrealCopy.get(AnrealCopy.BOARDING_ASSISTANT_REPLY),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Description,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    text = AnrealCopy.get(AnrealCopy.BOARDING_CITATION),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun WebPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Language,
                contentDescription = null,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AnrealCopy.get(AnrealCopy.BOARDING_WEB_TOOL),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
        Text(
            text = AnrealCopy.get(AnrealCopy.BOARDING_WEB_SOURCE),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun ImagesPreview() {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(112.dp)
                .clip(RoundedCornerShape(AnrealSpacing.sm))
                .background(MaterialTheme.colorScheme.surfaceContainerHigh),
            contentAlignment = Alignment.Center,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Image,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = AnrealCopy.get(AnrealCopy.BOARDING_IMAGE_CAPTION),
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
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
                    val index = page().coerceIn(0f, (pageCount - 1).toFloat())
                    IntOffset(x = (index * slotPx).roundToInt(), y = 0)
                }
                .size(width = IndicatorPill, height = IndicatorDot)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.onSurface),
        )
    }
}

private val IndicatorDot = 6.dp
private val IndicatorPill = 16.dp
private val IndicatorGap = 8.dp

@AnrealPreviews
@Composable
private fun BoardingCarouselDocumentsPreview() {
    AnrealPreview {
        Box(modifier = Modifier.height(360.dp).padding(AnrealSpacing.md)) {
            BoardingCarousel(
                paused = true,
                pagerState = rememberPagerState(initialPage = 0, pageCount = { 3 }),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun BoardingCarouselWebPreview() {
    AnrealPreview {
        Box(modifier = Modifier.height(360.dp).padding(AnrealSpacing.md)) {
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
        Box(modifier = Modifier.height(360.dp).padding(AnrealSpacing.md)) {
            BoardingCarousel(
                paused = true,
                pagerState = rememberPagerState(initialPage = 2, pageCount = { 3 }),
            )
        }
    }
}
