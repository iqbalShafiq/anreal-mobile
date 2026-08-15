package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
fun AnrealPrimaryButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    loadingLabel: String = label,
) {
    val resolvedLabel = if (loading) loadingLabel else label
    Button(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                if (loading) liveRegion = LiveRegionMode.Polite
            },
        enabled = enabled && !loading,
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (loading) {
                LoadingIndicator(
                    modifier = Modifier.size(18.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                )
            }
            Text(resolvedLabel)
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonIdlePreview() {
    AnrealPreview {
        AnrealPrimaryButton(label = "Continue", onClick = {})
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonLoadingPreview() {
    AnrealPreview {
        AnrealPrimaryButton(
            label = "Continue",
            onClick = {},
            loading = true,
            loadingLabel = "Signing in…",
        )
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonDisabledPreview() {
    AnrealPreview {
        AnrealPrimaryButton(label = "Continue", onClick = {}, enabled = false)
    }
}
