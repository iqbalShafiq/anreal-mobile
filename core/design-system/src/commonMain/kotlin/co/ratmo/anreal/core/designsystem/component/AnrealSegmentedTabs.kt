package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextOverflow
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun <T> AnrealSegmentedTabs(
    items: List<T>,
    selected: T,
    label: (T) -> String,
    onSelect: (T) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    GlassSurface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.Thin,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(AnrealSpacing.xxs)
                .selectableGroup(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
        ) {
            items.forEach { item ->
                val isSelected = item == selected
                Surface(
                    modifier = Modifier.weight(1f),
                    shape = if (isSelected) MaterialTheme.shapes.extraLarge else MaterialTheme.shapes.large,
                    color = if (isSelected) glassHighlightColor() else Color.Transparent,
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(min = AnrealSpacing.touch)
                            .selectable(
                                selected = isSelected,
                                enabled = enabled,
                                role = Role.Tab,
                                onClick = { onSelect(item) },
                            )
                            .alpha(if (enabled) 1f else 0.38f)
                            .padding(horizontal = AnrealSpacing.xs),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = label(item),
                            style = MaterialTheme.typography.labelMedium,
                            color = if (isSelected) {
                                MaterialTheme.colorScheme.onSurface
                            } else {
                                glassMutedTextColor()
                            },
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
        }
    }
}
