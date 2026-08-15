package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.hideFromAccessibility
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealMark(
    modifier: Modifier = Modifier,
    size: Dp = 32.dp,
    wordmark: String? = null,
    contentDescription: String? = null,
) {
    val corner = (size.value * 0.31f).dp
    val glyphStyle = when {
        size >= 64.dp -> MaterialTheme.typography.headlineSmall
        size >= 48.dp -> MaterialTheme.typography.titleLarge
        else -> MaterialTheme.typography.labelLarge
    }
    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            if (contentDescription != null) {
                this.contentDescription = contentDescription
            }
        },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        GlassSurface(
            modifier = Modifier.size(size),
            shape = RoundedCornerShape(corner),
            tone = GlassTone.Pane,
        ) {
            Box(
                modifier = Modifier
                    .size(size)
                    .semantics { hideFromAccessibility() },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = "A",
                    style = glyphStyle,
                    color = MaterialTheme.colorScheme.onSurface,
                )
            }
        }
        if (wordmark != null) {
            Text(
                text = wordmark,
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkWordmarkPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealMark(
                wordmark = "Anreal",
                contentDescription = "Anreal",
                modifier = Modifier.size(width = 160.dp, height = 40.dp),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealMarkLargePreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealMark(size = 72.dp, contentDescription = "Anreal")
        }
    }
}
