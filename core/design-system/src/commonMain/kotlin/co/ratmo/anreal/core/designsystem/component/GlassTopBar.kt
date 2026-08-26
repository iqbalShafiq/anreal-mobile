package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

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
    surfaceTinted: Boolean = frosted,
    content: @Composable BoxScope.() -> Unit,
) {
    GlassChrome(
        modifier = modifier.fillMaxWidth(),
        mode = if (surfaceTinted) GlassChromeMode.Surface else GlassChromeMode.Aurora,
        emphasized = frosted,
        shape = RectangleShape,
        content = content,
    )
}

@AnrealPreviews
@Composable
private fun GlassTopBarAuroraPreview() {
    AnrealPreview {
        GlassTopBar(frosted = false) {
            Text("Aurora chrome", modifier = Modifier.padding(AnrealSpacing.md))
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
