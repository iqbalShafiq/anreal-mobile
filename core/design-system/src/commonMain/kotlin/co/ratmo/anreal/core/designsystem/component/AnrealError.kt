package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@Composable
fun AnrealError(
    message: String,
    modifier: Modifier = Modifier,
    retryLabel: String = "Retry",
    onRetry: (() -> Unit)? = null,
) {
    AnrealCenteredColumn(
        modifier = modifier
            .padding(AnrealSpacing.md)
            .semantics { liveRegion = LiveRegionMode.Polite },
        spacing = AnrealSpacing.xs,
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.error,
            textAlign = TextAlign.Center,
        )
        if (onRetry != null) {
            TextButton(onClick = onRetry) {
                Text(retryLabel)
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealErrorWithRetryPreview() {
    AnrealPreview {
        AnrealError(
            message = "Could not load chats. Check your connection and try again.",
            onRetry = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealErrorInlinePreview() {
    AnrealPreview {
        AnrealError(message = "Invalid email or password.")
    }
}
