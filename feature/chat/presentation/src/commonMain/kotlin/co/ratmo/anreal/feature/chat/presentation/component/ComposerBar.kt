package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealComposerField
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatComposerCatalogPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Arrow_upward
import com.composables.icons.materialsymbols.rounded.Close
import com.composables.icons.materialsymbols.rounded.Expand_more
import com.composables.icons.materialsymbols.rounded.South_west
import com.composables.icons.materialsymbols.rounded.Stop

@Composable
internal fun ComposerBar(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
    initialSheet: ComposerSheet? = null,
) {
    var sheet by remember { mutableStateOf(initialSheet) }
    val streaming = state.isSending || state.thread.status == RunStatus.Streaming
    val canSubmit = state.draft.isNotBlank()
    val modelTriggerLabel = modelAndReasoningLabel(state)
    val modelTriggerDescription = AnrealCopy.get(AnrealCopy.CD_MODEL)
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
    ) {
        GlassSurface(
            modifier = Modifier.fillMaxWidth(),
            tone = GlassTone.Thin,
            emphasized = canSubmit || streaming,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = AnrealSpacing.md,
                    end = AnrealSpacing.sm,
                    top = AnrealSpacing.md,
                    bottom = AnrealSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                MessageQueueDock(state = state, onAction = onAction)
                SessionImageStrip(state = state, onAction = onAction)
                state.uploadingDocuments.filter { it.status != "ready" }.forEach { document ->
                    Text(
                        text = "${document.filename} · ${document.status.toUploadStatusLabel()}",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (document.error == null) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
                state.contextSnippet?.let { snippet ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = snippet,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                        IconButton(
                            onClick = { onAction(ChatAction.OnClearContext) },
                            modifier = Modifier.size(40.dp),
                        ) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Close,
                                contentDescription = AnrealCopy.get(AnrealCopy.ACTION_CANCEL),
                            )
                        }
                    }
                }
                AnrealComposerField(
                    value = state.draft,
                    onValueChange = { onAction(ChatAction.OnDraftChange(it)) },
                    placeholder = AnrealCopy.get(AnrealCopy.COMPOSER_PLACEHOLDER),
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
                    ) {
                        ComposerAddButton(
                            featuresActive = state.webSearchEnabled ||
                                state.imageGenerationEnabled ||
                                state.isUploading,
                            onClick = { sheet = ComposerSheet.Features },
                        )
                        ComposerModelTrigger(
                            label = modelTriggerLabel,
                            contentDescription = modelTriggerDescription,
                            onClick = {
                                if (state.models.isEmpty() && !state.catalogLoading) {
                                    onAction(ChatAction.OnRetryCatalog)
                                }
                                sheet = ComposerSheet.Model
                            },
                            modifier = Modifier.weight(1f, fill = false),
                        )
                    }
                    ComposerSubmitButton(
                        streaming = streaming,
                        canSubmit = canSubmit,
                        onAction = onAction,
                    )
                }
            }
        }
    }
    ComposerSheets(
        sheet = sheet,
        state = state,
        onAction = onAction,
        onOpenAttachments = { sheet = ComposerSheet.Attach },
        onDismiss = { sheet = null },
    )
}

private fun String.toUploadStatusLabel(): String = AnrealCopy.get(
    when (lowercase()) {
        "queued" -> AnrealCopy.STATUS_UPLOAD_QUEUED
        "processing" -> AnrealCopy.STATUS_UPLOAD_PROCESSING
        "uploading" -> AnrealCopy.STATUS_UPLOAD_UPLOADING
        "error", "failed" -> AnrealCopy.STATUS_UPLOAD_FAILED
        else -> AnrealCopy.STATUS_UPLOAD_PROCESSING
    },
)

@Composable
private fun ComposerAddButton(
    featuresActive: Boolean,
    onClick: () -> Unit,
) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(AnrealSpacing.touch),
        shape = CircleShape,
        colors = if (featuresActive) {
            IconButtonDefaults.filledTonalIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer,
                contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
            )
        } else {
            IconButtonDefaults.filledTonalIconButtonColors()
        },
    ) {
        Icon(
            imageVector = MaterialSymbols.Rounded.Add,
            contentDescription = AnrealCopy.get(AnrealCopy.CD_FEATURES),
        )
    }
}

@Composable
private fun ComposerModelTrigger(
    label: String,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        onClick = onClick,
        modifier = modifier
            .heightIn(min = AnrealSpacing.touch)
            .semantics { this.contentDescription = contentDescription },
        shape = CircleShape,
        color = MaterialTheme.colorScheme.secondaryContainer,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
    ) {
        Row(
            modifier = Modifier.padding(
                start = AnrealSpacing.sm,
                end = AnrealSpacing.xs,
            ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
        ) {
            Text(
                text = label,
                modifier = Modifier.weight(1f, fill = false),
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Icon(
                imageVector = MaterialSymbols.Rounded.Expand_more,
                contentDescription = null,
            )
        }
    }
}

@Composable
private fun ComposerSubmitButton(
    streaming: Boolean,
    canSubmit: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    val colors = IconButtonDefaults.filledIconButtonColors()
    val keyboard = LocalSoftwareKeyboardController.current
    val submit: () -> Unit = {
        keyboard?.hide()
        onAction(ChatAction.OnSend)
    }
    when {
        streaming && !canSubmit -> {
            FilledIconButton(
                onClick = { onAction(ChatAction.OnStop) },
                modifier = Modifier.size(AnrealSpacing.touch),
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Stop,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_STOP),
                )
            }
        }
        streaming -> {
            FilledIconButton(
                onClick = submit,
                modifier = Modifier.size(AnrealSpacing.touch),
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.South_west,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_QUEUE),
                )
            }
        }
        else -> {
            FilledIconButton(
                onClick = submit,
                modifier = Modifier.size(AnrealSpacing.touch),
                enabled = canSubmit,
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Arrow_upward,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_SEND),
                )
            }
        }
    }
}

internal fun modelAndReasoningLabel(state: ChatState): String {
    val modelLabel = state.models.firstOrNull { it.id == state.selectedModelId }?.label
        ?: AnrealCopy.get(AnrealCopy.LABEL_MODEL)
    val effortLabel = state.selectedReasoning?.let { key ->
        state.reasoningEfforts.firstOrNull { it.key == key }?.label
    }
    return if (effortLabel.isNullOrBlank()) modelLabel else "$modelLabel $effortLabel"
}

@AnrealPreviews
@Composable
private fun ComposerBarEmptyPreview() {
    AnrealPreview {
        ComposerBar(state = ChatState(draft = ""), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarFilledPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState(),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarLongModelPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState().copy(
                models = chatComposerCatalogPreviewState().models.map { model ->
                    if (model.id == "luna") {
                        model.copy(label = "GPT Luna 5.6 Extended Reasoning")
                    } else {
                        model
                    }
                },
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarReasoningNonePreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState(selectedReasoning = null),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarStreamingStopPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(draft = "", isSending = true, status = RunStatus.Streaming),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarStreamingQueuePreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(
                draft = "What about costs?",
                isSending = true,
                status = RunStatus.Streaming,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarFeaturesSheetPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            initialSheet = ComposerSheet.Features,
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarModelSheetPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            initialSheet = ComposerSheet.Model,
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarAttachSheetPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatComposerCatalogPreviewState(),
            onAction = {},
            initialSheet = ComposerSheet.Attach,
        )
    }
}
