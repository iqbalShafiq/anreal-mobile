package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonCard
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.workspace.domain.ArtifactType
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Folder
import com.composables.icons.materialsymbols.rounded.Image
import com.composables.icons.materialsymbols.rounded.Visibility

@Composable
fun ArtifactsBrowser(
    artifacts: List<ArtifactItemUi>,
    isLoading: Boolean,
    loaded: Boolean,
    hasScope: Boolean,
    error: UiText?,
    typeFilter: ArtifactType?,
    detail: ArtifactItemUi?,
    captionDraft: String,
    captionSaving: Boolean,
    captionError: UiText?,
    onAction: (WorkspaceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth()
                .horizontalScroll(rememberScrollState())
                .padding(horizontal = AnrealSpacing.screenCompact),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
        ) {
            FilterChip(
                selected = typeFilter == null,
                onClick = { onAction(WorkspaceAction.OnArtifactTypeFilter(null)) },
                label = { Text(AnrealCopy.get(AnrealCopy.LABEL_ALL)) },
            )
            ArtifactType.entries.filter { it != ArtifactType.Unknown }.forEach { type ->
                FilterChip(
                    selected = typeFilter == type,
                    onClick = { onAction(WorkspaceAction.OnArtifactTypeFilter(type)) },
                    label = { Text(typeLabel(type)) },
                )
            }
        }
        when {
            !hasScope -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_ARTIFACTS),
                body = AnrealCopy.get(AnrealCopy.ARTIFACTS_NO_SCOPE),
                icon = MaterialSymbols.Rounded.Folder,
                modifier = Modifier.fillMaxSize(),
            )
            isLoading && !loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WorkspaceListPadding,
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(4) { AnrealSkeletonCard() }
            }
            error != null && !loaded -> AnrealError(
                message = error.asString(),
                retryLabel = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
                onRetry = { onAction(WorkspaceAction.Retry) },
                modifier = Modifier.fillMaxSize(),
            )
            artifacts.isEmpty() -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_ARTIFACTS),
                body = AnrealCopy.get(AnrealCopy.ARTIFACTS_EMPTY),
                icon = MaterialSymbols.Rounded.Folder,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WorkspaceListPadding,
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(artifacts, key = ArtifactItemUi::id) { artifact ->
                    ArtifactCard(
                        artifact = artifact,
                        onOpen = { onAction(WorkspaceAction.OnArtifactOpen(artifact.id)) },
                    )
                }
            }
        }
    }
    if (detail != null) {
        ArtifactDetailDialog(
            detail = detail,
            captionDraft = captionDraft,
            captionSaving = captionSaving,
            captionError = captionError,
            onAction = onAction,
        )
    }
}

private fun typeLabel(type: ArtifactType): String = when (type) {
    ArtifactType.Document -> "documents"
    ArtifactType.Image -> "images"
    ArtifactType.WebBundle -> "web"
    ArtifactType.Site -> "sites"
    ArtifactType.Task -> "tasks"
    ArtifactType.Schedule -> "schedules"
    ArtifactType.Session -> "sessions"
    ArtifactType.Unknown -> "other"
}

@Composable
private fun ArtifactCard(
    artifact: ArtifactItemUi,
    onOpen: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onOpen,
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                when (artifact.type) {
                    ArtifactType.Image -> MaterialSymbols.Rounded.Image
                    else -> MaterialSymbols.Rounded.Description
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = artifact.title ?: artifact.caption ?: artifact.prompt ?: artifact.id,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = listOfNotNull(
                    typeLabel(artifact.type),
                    artifact.status,
                    artifact.caption?.takeIf { artifact.title != null },
                ).joinToString(" · ")
                if (subtitle.isNotEmpty()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

@Composable
private fun ArtifactDetailDialog(
    detail: ArtifactItemUi,
    captionDraft: String,
    captionSaving: Boolean,
    captionError: UiText?,
    onAction: (WorkspaceAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.OnArtifactClose) },
        title = { Text(detail.title ?: detail.caption ?: detail.id) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                detail.prompt?.let { Text(it, style = MaterialTheme.typography.bodyMedium) }
                detail.status?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (detail.type == ArtifactType.Image) {
                    OutlinedTextField(
                        value = captionDraft,
                        onValueChange = { onAction(WorkspaceAction.OnCaptionChange(it)) },
                        label = { Text(AnrealCopy.get(AnrealCopy.LABEL_CAPTION)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.fillMaxWidth(),
                    )
                    captionError?.let {
                        Text(it.asString(), color = MaterialTheme.colorScheme.error)
                    }
                    TextButton(
                        onClick = { onAction(WorkspaceAction.OnCaptionSave) },
                        enabled = !captionSaving,
                    ) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_SAVE))
                    }
                }
                if (detail.type == ArtifactType.Task) {
                    TextButton(onClick = {
                        onAction(WorkspaceAction.OnArtifactClose)
                        onAction(WorkspaceAction.SelectSection(WorkspaceSection.Tasks))
                    }) {
                        Text(AnrealCopy.get(AnrealCopy.ARTIFACTS_OPEN_TASKS))
                    }
                }
                if (detail.type == ArtifactType.Schedule) {
                    TextButton(onClick = {
                        onAction(WorkspaceAction.OnArtifactClose)
                        onAction(WorkspaceAction.SelectSection(WorkspaceSection.Schedules))
                    }) {
                        Text(AnrealCopy.get(AnrealCopy.ARTIFACTS_OPEN_SCHEDULES))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onAction(WorkspaceAction.OnArtifactClose) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CLOSE))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun ArtifactsPopulatedPreview() {
    AnrealPreview {
        ArtifactsBrowser(
            artifacts = listOf(
                ArtifactItemUi("i1", ArtifactType.Image, null, "Chart", "Draw", null, null, null, null),
                ArtifactItemUi("s1", ArtifactType.Site, null, null, null, "ready", "/api/sites/s1/v1/preview/index.html", "/api/sites/s1/v1/download", 1),
                ArtifactItemUi("t1", ArtifactType.Task, "Fix login", null, null, "doing", null, null, null),
            ),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            typeFilter = null,
            detail = null,
            captionDraft = "",
            captionSaving = false,
            captionError = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ArtifactsDetailPreview() {
    AnrealPreview {
        ArtifactsBrowser(
            artifacts = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            typeFilter = null,
            detail = ArtifactItemUi("i1", ArtifactType.Image, null, "Chart", "Draw", null, null, null, null),
            captionDraft = "Chart",
            captionSaving = false,
            captionError = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ArtifactsEmptyPreview() {
    AnrealPreview {
        ArtifactsBrowser(
            artifacts = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            typeFilter = null,
            detail = null,
            captionDraft = "",
            captionSaving = false,
            captionError = null,
            onAction = {},
        )
    }
}
