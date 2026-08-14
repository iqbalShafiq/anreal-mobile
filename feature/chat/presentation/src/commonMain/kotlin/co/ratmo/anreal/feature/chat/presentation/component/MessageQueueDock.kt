package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3ExpressiveApi
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.queue.QueueStatus
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Close

@OptIn(ExperimentalMaterial3ExpressiveApi::class)
@Composable
internal fun MessageQueueDock(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    if (state.queue.isEmpty()) return
    if (state.queueHidden) {
        TextButton(onClick = { onAction(ChatAction.OnShowQueue) }) {
            Text(
                UiText.StringResource(
                    AnrealCopy.LABEL_QUEUED_COUNT,
                    listOf(state.queue.size.toString()),
                ).asString(),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        return
    }
    val visible = if (state.queueExpanded) state.queue else state.queue.take(1)
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs)) {
        visible.forEach { item ->
            QueueChip(
                item = item,
                onRecall = { onAction(ChatAction.OnRecallQueued(item.id)) },
                onRemove = { onAction(ChatAction.OnRemoveQueued(item.id)) },
                onCancelEdit = { onAction(ChatAction.OnCancelQueueEdit) },
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.queue.size > 1) {
                TextButton(onClick = { onAction(ChatAction.OnToggleQueueExpanded) }) {
                    Text(
                        if (state.queueExpanded) {
                            AnrealCopy.get(AnrealCopy.CD_HIDE_QUEUE)
                        } else {
                            AnrealCopy.get(AnrealCopy.CD_EXPAND_QUEUE)
                        },
                    )
                }
            }
            Row(verticalAlignment = Alignment.CenterVertically) {
                if (state.queue.any { it.status == QueueStatus.Pending }) {
                    val sendNowHeight = ButtonDefaults.ExtraSmallContainerHeight
                    FilledTonalButton(
                        onClick = { onAction(ChatAction.OnSendNow) },
                        modifier = Modifier.heightIn(sendNowHeight),
                        contentPadding = ButtonDefaults.contentPaddingFor(sendNowHeight),
                    ) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_SEND_NOW))
                    }
                }
                TextButton(onClick = { onAction(ChatAction.OnHideQueue) }) {
                    Text(AnrealCopy.get(AnrealCopy.CD_HIDE_QUEUE))
                }
            }
        }
    }
}

@Composable
private fun QueueChip(
    item: QueuedItem,
    onRecall: () -> Unit,
    onRemove: () -> Unit,
    onCancelEdit: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onRecall)
            .padding(vertical = AnrealSpacing.xxs),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (item.status == QueueStatus.Editing) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.LABEL_EDITING),
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
        IconButton(
            onClick = if (item.status == QueueStatus.Editing) onCancelEdit else onRemove,
            modifier = Modifier.size(IconButtonDefaults.extraSmallContainerSize()),
            shape = IconButtonDefaults.extraSmallRoundShape,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Close,
                contentDescription = if (item.status == QueueStatus.Editing) {
                    AnrealCopy.get(AnrealCopy.ACTION_CANCEL)
                } else {
                    AnrealCopy.get(AnrealCopy.CD_REMOVE_QUEUED)
                },
                modifier = Modifier.size(IconButtonDefaults.extraSmallIconSize),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun previewQueueState(
    items: List<QueuedItem>,
    expanded: Boolean = false,
    hidden: Boolean = false,
): ChatState = ChatState(
    sessionsLoading = false,
    queue = items,
    queueExpanded = expanded,
    queueHidden = hidden,
)

@AnrealPreviews
@Composable
private fun QueueDockOnePreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(listOf(QueuedItem(id = "1", text = "What about costs?"))),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun QueueDockCollapsedPreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(
                listOf(
                    QueuedItem(id = "1", text = "First"),
                    QueuedItem(id = "2", text = "Second"),
                    QueuedItem(id = "3", text = "Third"),
                ),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun QueueDockExpandedPreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(
                listOf(
                    QueuedItem(id = "1", text = "First"),
                    QueuedItem(id = "2", text = "Second"),
                    QueuedItem(id = "3", text = "Third"),
                ),
                expanded = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun QueueDockEditingPreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(
                listOf(QueuedItem(id = "1", text = "What about costs?", status = QueueStatus.Editing)),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun QueueDockInflightPreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(
                listOf(QueuedItem(id = "1", text = "What about costs?", status = QueueStatus.Inflight)),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun QueueDockHiddenPreview() {
    AnrealPreview {
        MessageQueueDock(
            state = previewQueueState(
                listOf(QueuedItem(id = "1", text = "What about costs?"), QueuedItem(id = "2", text = "And margins?")),
                hidden = true,
            ),
            onAction = {},
        )
    }
}
