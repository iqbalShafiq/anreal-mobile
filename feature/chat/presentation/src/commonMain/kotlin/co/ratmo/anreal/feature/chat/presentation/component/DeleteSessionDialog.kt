package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewUnreadSession

@Composable
internal fun DeleteSessionDialog(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val title = state.sessions.firstOrNull { it.id == state.deleteSessionId }?.title
        ?: AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT)
    val running = state.deleteSessionId == state.selectedSessionId &&
        state.thread.status == RunStatus.Streaming
    AlertDialog(
        onDismissRequest = { if (!state.sessionBusy) onAction(ChatAction.OnDismissSessionDialog) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DELETE_TITLE)) },
        text = {
            Column {
                Text(title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = AnrealCopy.get(AnrealCopy.DIALOG_DELETE_BODY),
                    modifier = Modifier.padding(top = AnrealSpacing.sm),
                    style = MaterialTheme.typography.bodyMedium,
                )
                if (running) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.DIALOG_DELETE_BODY_RUNNING),
                        modifier = Modifier.padding(top = AnrealSpacing.xs),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                }
                state.deleteError?.let { error ->
                    Text(
                        text = error.asString(),
                        modifier = Modifier.padding(top = AnrealSpacing.sm),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnConfirmDelete) },
                enabled = !state.sessionBusy,
            ) {
                Text(
                    if (state.sessionBusy) {
                        AnrealCopy.get(AnrealCopy.ACTION_DELETING)
                    } else {
                        AnrealCopy.get(AnrealCopy.ACTION_DELETE)
                    },
                )
            }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnDismissSessionDialog) },
                enabled = !state.sessionBusy,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun DeleteSessionDialogIdlePreview() {
    AnrealPreview {
        DeleteSessionDialog(
            state = chatPopulatedPreviewState().copy(deleteSessionId = previewUnreadSession.id),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun DeleteSessionDialogRunningPreview() {
    AnrealPreview {
        DeleteSessionDialog(
            state = chatPopulatedPreviewState(
                status = RunStatus.Streaming,
            ).copy(deleteSessionId = previewUnreadSession.id),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun DeleteSessionDialogBusyPreview() {
    AnrealPreview {
        DeleteSessionDialog(
            state = chatPopulatedPreviewState().copy(
                deleteSessionId = previewUnreadSession.id,
                sessionBusy = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun DeleteSessionDialogErrorPreview() {
    AnrealPreview {
        DeleteSessionDialog(
            state = chatPopulatedPreviewState().copy(
                deleteSessionId = previewUnreadSession.id,
                deleteError = UiText.StringResource(AnrealCopy.ERROR_CONFLICT),
            ),
            onAction = {},
        )
    }
}
