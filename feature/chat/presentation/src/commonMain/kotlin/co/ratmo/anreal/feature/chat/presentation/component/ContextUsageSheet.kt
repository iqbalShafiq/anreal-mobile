package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.ContextUsageUi
import kotlin.math.roundToInt

@Composable
internal fun ContextUsageButton(
    usage: ContextUsageUi?,
    error: Boolean,
    onClick: () -> Unit,
) {
    val description = AnrealCopy.get(AnrealCopy.CD_CONTEXT_USAGE)
    IconButton(
        onClick = onClick,
        modifier = Modifier.semantics { contentDescription = description },
    ) {
        Box(modifier = Modifier.size(26.dp), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.size(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                strokeWidth = 3.dp,
            )
            CircularProgressIndicator(
                progress = { usage?.ratio ?: 0f },
                modifier = Modifier.size(24.dp),
                color = when {
                    error -> MaterialTheme.colorScheme.error
                    usage?.nearThreshold == true -> MaterialTheme.colorScheme.error
                    else -> MaterialTheme.colorScheme.primary
                },
                strokeWidth = 3.dp,
            )
        }
    }
}

@Composable
internal fun ContextUsageSheet(
    usage: ContextUsageUi?,
    error: Boolean,
    onDismiss: () -> Unit,
) {
    ChatBottomSheet(onDismiss = onDismiss) {
        ContextUsageContent(usage = usage, error = error)
    }
}

@Composable
private fun ContextUsageContent(
    usage: ContextUsageUi?,
    error: Boolean,
) {
    Column(modifier = Modifier.padding(bottom = AnrealSpacing.lg)) {
        SheetTitle(AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_USAGE))
        when {
            usage == null && error -> {
                Text(
                    text = AnrealCopy.get(AnrealCopy.STATUS_UNAVAILABLE),
                    modifier = Modifier.padding(AnrealSpacing.md),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                )
            }
            usage == null -> {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.xl),
                    contentAlignment = Alignment.Center,
                ) {
                    AnrealLoadingIndicator()
                }
            }
            else -> {
                LinearProgressIndicator(
                    progress = { usage.ratio },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
                    color = if (usage.nearThreshold) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                )
                ContextUsageRow(AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_MODEL), usage.modelLabel)
                ContextUsageRow(
                    AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_USED),
                    usage.ratio.asPercent(),
                )
                ContextUsageRow(
                    AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_TOKENS),
                    usage.estimatedTokens.grouped(),
                )
                ContextUsageRow(
                    AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_WINDOW),
                    usage.contextWindowTokens.grouped(),
                )
                ContextUsageRow(
                    AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_THRESHOLD),
                    usage.thresholdRatio.asPercent(),
                )
                ContextUsageRow(
                    AnrealCopy.get(AnrealCopy.LABEL_CONTEXT_TARGET),
                    usage.targetRatio.asPercent(),
                )
                usage.reasoningEffort?.let { effort ->
                    ContextUsageRow(AnrealCopy.get(AnrealCopy.LABEL_REASONING), effort)
                }
            }
        }
    }
}

@Composable
private fun ContextUsageRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = label,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

private fun Float.asPercent(): String = "${(this * 100).roundToInt()}%"

private fun Int.grouped(): String = toString()
    .reversed()
    .chunked(3)
    .joinToString(",")
    .reversed()

@AnrealPreviews
@Composable
private fun ContextUsageSheetPreview() {
    AnrealPreview {
        ContextUsageSheet(
            usage = ContextUsageUi(
                modelLabel = "GPT Luna 5.6",
                estimatedTokens = 12_400,
                contextWindowTokens = 128_000,
                ratio = 0.1f,
                thresholdRatio = 0.7f,
                targetRatio = 0.55f,
                reasoningEffort = "high",
                nearThreshold = false,
            ),
            error = false,
            onDismiss = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ContextUsageSheetLoadingPreview() {
    AnrealPreview {
        ContextUsageSheet(usage = null, error = false, onDismiss = {})
    }
}

@AnrealPreviews
@Composable
private fun ContextUsageSheetErrorPreview() {
    AnrealPreview {
        ContextUsageSheet(usage = null, error = true, onDismiss = {})
    }
}
