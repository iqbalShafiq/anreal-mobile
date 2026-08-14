package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.presentation.ChatAction

@Composable
internal fun QueueConflictDialog(
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(ChatAction.OnDismissQueueConflict) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_QUEUE_CONFLICT_TITLE)) },
        text = { Text(AnrealCopy.get(AnrealCopy.DIALOG_QUEUE_CONFLICT_BODY)) },
        confirmButton = {
            TextButton(onClick = { onAction(ChatAction.OnSendQueue) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_SEND_QUEUE))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ChatAction.OnSendNewMessage) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_SEND_NEW_MESSAGE))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun QueueConflictDialogPreview() {
    AnrealPreview {
        QueueConflictDialog(onAction = {})
    }
}
