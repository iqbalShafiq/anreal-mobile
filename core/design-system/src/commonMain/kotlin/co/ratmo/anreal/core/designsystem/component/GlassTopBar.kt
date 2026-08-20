package co.ratmo.anreal.core.designsystem.component

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.snap
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion

internal const val FrostedTopBarSlopPx = 8

internal fun isScrolledFromStart(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffset: Int,
): Boolean = firstVisibleItemIndex > 0 || firstVisibleItemScrollOffset > FrostedTopBarSlopPx

@Composable
fun rememberFrostedTopBar(listState: LazyListState): Boolean {
    return remember(listState) {
        derivedStateOf {
            val inProgress = listState.isScrollInProgress
            val scrolled = isScrolledFromStart(
                firstVisibleItemIndex = listState.firstVisibleItemIndex,
                firstVisibleItemScrollOffset = listState.firstVisibleItemScrollOffset,
            )
            scrolled || (inProgress && scrolled)
        }
    }.value
}

@Composable
fun rememberFrostedTopBar(scrollState: ScrollState): Boolean {
    return remember(scrollState) {
        derivedStateOf {
            val inProgress = scrollState.isScrollInProgress
            val scrolled = scrollState.value > FrostedTopBarSlopPx
            scrolled || (inProgress && scrolled)
        }
    }.value
}

@Composable
fun GlassTopBar(
    modifier: Modifier = Modifier,
    frosted: Boolean = false,
    content: @Composable BoxScope.() -> Unit,
) {
    val reduceMotion = LocalAnrealReduceMotion.current
    val frostAlpha by animateFloatAsState(
        targetValue = if (frosted) 1f else 0f,
        animationSpec = if (reduceMotion) {
            snap()
        } else {
            AnrealMotion.fadeSpec()
        },
        label = "glassTopBarFrost",
    )
    if (frostAlpha <= 0f) {
        Box(modifier = modifier.fillMaxWidth(), content = content)
        return
    }
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = RectangleShape,
        tone = GlassTone.Thin,
        borderColor = glassDrawerBorderColor(),
        fallbackColor = glassDrawerFallbackColor(),
        effectAlpha = frostAlpha,
    ) {
        Box(content = content)
    }
}

@AnrealPreviews
@Composable
private fun GlassTopBarClearPreview() {
    AnrealPreview {
        GlassTopBar(frosted = false) {
            Text("Clear chrome", modifier = Modifier.padding(AnrealSpacing.md))
        }
    }
}

@AnrealPreviews
@Composable
private fun GlassTopBarFrostedPreview() {
    AnrealPreview {
        GlassTopBar(frosted = true) {
            Text("Frosted chrome", modifier = Modifier.padding(AnrealSpacing.md))
        }
    }
}
