package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Content_copy
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Refresh
import com.composables.icons.materialsymbols.rounded.Reply

@Composable
internal fun MessageActionsBar(
    message: ChatMessage,
    busy: Boolean,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val text = message.parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
    if (text.isBlank()) return
    val isUser = message.role == ChatRole.User
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            onClick = { onAction(ChatAction.OnCopyMessage(text)) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Content_copy,
                contentDescription = AnrealCopy.get(AnrealCopy.CD_COPY_MESSAGE),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(
            onClick = { onAction(ChatAction.OnAddContext(text)) },
            modifier = Modifier.size(40.dp),
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Reply,
                contentDescription = AnrealCopy.get(AnrealCopy.CD_ADD_CONTEXT),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (isUser) {
            IconButton(
                onClick = { onAction(ChatAction.OnEditMessage(message.id)) },
                enabled = !busy && text.isNotBlank(),
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Edit,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_EDIT_MESSAGE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(
                onClick = { onAction(ChatAction.OnRegenerateMessage(message.id)) },
                enabled = !busy,
                modifier = Modifier.size(40.dp),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Refresh,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_REGENERATE),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageActionsBarUserPreview() {
    AnrealPreview {
        MessageActionsBar(
            message = ChatMessage(
                id = "u1",
                role = ChatRole.User,
                parts = listOf(ChatPart.Text(id = "t", text = "Summarize the PDF.")),
                isComplete = true,
            ),
            busy = false,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun MessageActionsBarAssistantPreview() {
    AnrealPreview {
        MessageActionsBar(
            message = ChatMessage(
                id = "a1",
                role = ChatRole.Assistant,
                parts = listOf(ChatPart.Text(id = "t", text = "Revenue grew 12%.")),
                isComplete = true,
            ),
            busy = false,
            onAction = {},
        )
    }
}
