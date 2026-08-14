package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Send
import com.composables.icons.materialsymbols.rounded.Stop

@Composable
internal fun ComposerBar(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(AnrealSpacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        OutlinedTextField(
            value = state.draft,
            onValueChange = { onAction(ChatAction.OnDraftChange(it)) },
            modifier = Modifier.weight(1f),
            placeholder = { Text(AnrealCopy.get(AnrealCopy.COMPOSER_PLACEHOLDER)) },
            singleLine = false,
            maxLines = 4,
        )
        if (state.isSending || state.thread.status == RunStatus.Streaming) {
            FilledIconButton(onClick = { onAction(ChatAction.OnStop) }) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Stop,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_STOP),
                )
            }
        } else {
            FilledIconButton(
                onClick = { onAction(ChatAction.OnSend) },
                enabled = state.draft.isNotBlank(),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Send,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_SEND),
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarEmptyPreview() {
    AnrealPreview {
        ComposerBar(
            state = ChatState(draft = ""),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarFilledPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(draft = "What about costs?"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarStreamingPreview() {
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
