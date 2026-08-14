package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealEmpty(
    title: String,
    body: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    AnrealCenteredColumn(
        modifier = modifier.padding(AnrealSpacing.lg),
        spacing = AnrealSpacing.sm,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )
        Text(
            text = body,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
        if (actionLabel != null && onAction != null) {
            Button(onClick = onAction) {
                Text(actionLabel)
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealEmptyWithActionPreview() {
    AnrealPreview {
        AnrealEmpty(
            title = "Ask anything about your documents",
            body = "Upload a PDF or image, then ask questions.",
            actionLabel = "Upload",
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealEmptyWithoutActionPreview() {
    AnrealPreview {
        AnrealEmpty(
            title = "No chats yet",
            body = "Start a conversation about your documents.",
        )
    }
}
