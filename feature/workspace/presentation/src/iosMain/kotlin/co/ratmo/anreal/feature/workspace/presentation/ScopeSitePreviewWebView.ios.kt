package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy

// TODO: host a WKWebView with a non-persistent data store (no cookies/file access)
// and load the public preview URL without auth headers.
internal actual @Composable fun ScopeSitePreviewWebView(url: String, modifier: Modifier) {
    Text(
        text = AnrealCopy.get(AnrealCopy.SITE_PREVIEW_UNAVAILABLE),
        modifier = modifier.padding(AnrealSpacing.md),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
}
