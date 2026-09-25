package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Chevron_left
import com.composables.icons.materialsymbols.rounded.Chevron_right
import kotlinx.coroutines.launch

@Composable
fun <T> AnrealSegmentedTabs(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    glass: Boolean = true,
    containerColor: Color? = null,
    scrollable: Boolean = false,
    backContentDescription: String? = null,
    forwardContentDescription: String? = null,
) {
    val tabsContent: @Composable () -> Unit = {
        if (scrollable) {
            ScrollableTabRow(
                items = items,
                selected = selected,
                label = label,
                onSelect = onSelect,
                enabled = enabled,
                backContentDescription = backContentDescription,
                forwardContentDescription = forwardContentDescription,
            )
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(AnrealSpacing.xxs)
                    .selectableGroup(),
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            ) {
                items.forEach { item ->
                    SegmentedTabItem(
                        selected = item == selected,
                        label = label(item),
                        onSelect = { onSelect(item) },
                        enabled = enabled,
                        glass = glass,
                        modifier = Modifier.weight(1f),
                    )
                }
            }
        }
    }
    if (glass) {
        GlassSurface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            tone = GlassTone.Thin,
        ) { tabsContent() }
    } else {
        Surface(
            modifier = modifier.fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = containerColor ?: MaterialTheme.colorScheme.surfaceContainer,
            contentColor = MaterialTheme.colorScheme.onSurface,
        ) { tabsContent() }
    }
}

@Composable
private fun SegmentedTabItem(
    selected: Boolean,
    label: String,
    onSelect: () -> Unit,
    enabled: Boolean,
    glass: Boolean,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = if (selected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
        color = if (selected) {
            if (glass) glassHighlightColor() else MaterialTheme.colorScheme.secondaryContainer
        } else {
            Color.Transparent
        },
        contentColor = if (selected) {
            if (glass) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSecondaryContainer
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = AnrealSpacing.touch)
                .selectable(
                    selected = selected,
                    enabled = enabled,
                    role = Role.Tab,
                    onClick = onSelect,
                )
                .alpha(if (enabled) 1f else 0.38f)
                .padding(horizontal = AnrealSpacing.md),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = if (selected) {
                    if (glass) MaterialTheme.colorScheme.onSurface
                    else MaterialTheme.colorScheme.onSecondaryContainer
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
        }
    }
}

@Composable
private fun <T> ScrollableTabRow(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    enabled: Boolean,
    backContentDescription: String?,
    forwardContentDescription: String?,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    val scope = rememberCoroutineScope()
    val density = LocalDensity.current
    val fadePx = with(density) { 56.dp.toPx() }
    Box(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    val fadeFraction = (fadePx / size.width).coerceIn(0f, 0.5f)
                    if (scrollState.canScrollBackward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                0f to Color.Transparent,
                                fadeFraction to Color.Black,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                    if (scrollState.canScrollForward) {
                        drawRect(
                            brush = Brush.horizontalGradient(
                                (1f - fadeFraction) to Color.Black,
                                1f to Color.Transparent,
                            ),
                            blendMode = BlendMode.DstIn,
                        )
                    }
                }
                .padding(AnrealSpacing.xxs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
        ) {
            items.forEach { item ->
                SegmentedTabItem(
                    selected = item == selected,
                    label = label(item),
                    onSelect = { onSelect(item) },
                    enabled = enabled,
                    glass = false,
                )
            }
            Spacer(modifier = Modifier.width(AnrealSpacing.touch))
        }
        if (scrollState.canScrollBackward) {
            IconButton(
                onClick = {
                    scope.launch {
                        with(density) {
                            scrollState.animateScrollTo(scrollState.value - 240.dp.roundToPx())
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterStart),
            ) {
                Icon(
                    MaterialSymbols.Rounded.Chevron_left,
                    contentDescription = backContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        if (scrollState.canScrollForward) {
            IconButton(
                onClick = {
                    scope.launch {
                        with(density) {
                            scrollState.animateScrollTo(scrollState.value + 240.dp.roundToPx())
                        }
                    }
                },
                modifier = Modifier.align(Alignment.CenterEnd),
            ) {
                Icon(
                    MaterialSymbols.Rounded.Chevron_right,
                    contentDescription = forwardContentDescription,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
