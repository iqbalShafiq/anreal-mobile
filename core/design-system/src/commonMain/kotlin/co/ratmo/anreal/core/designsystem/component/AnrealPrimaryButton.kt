package co.ratmo.anreal.core.designsystem.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.LoadingIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.role
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
    val canClick = enabled && !loading
    GlassSurface(
        modifier = modifier
            .fillMaxWidth()
            .semantics {
                role = Role.Button
                if (loading) liveRegion = LiveRegionMode.Polite
            },
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.Thin,
        emphasized = canClick,
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(
                    enabled = canClick,
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClick,
                )
                .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
            contentAlignment = Alignment.Center,
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                if (loading) {
                    LoadingIndicator(
                        modifier = Modifier.size(18.dp),
                        color = MaterialTheme.colorScheme.onSurface,
                    )
                }
                Text(
                    text = resolvedLabel,
                    style = MaterialTheme.typography.labelLarge,
                    color = if (canClick || loading) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                    },
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonIdlePreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPrimaryButton(
                label = "Continue",
                onClick = {},
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonLoadingPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPrimaryButton(
                label = "Continue",
                onClick = {},
                loading = true,
                loadingLabel = "Signing in…",
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun AnrealPrimaryButtonDisabledPreview() {
    AnrealPreview {
        AnrealAtmosphere {
            AnrealPrimaryButton(
                label = "Continue",
                onClick = {},
                enabled = false,
                modifier = Modifier.padding(AnrealSpacing.md),
            )
        }
    }
}
