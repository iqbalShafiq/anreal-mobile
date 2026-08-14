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
internal fun RunActiveDialog(
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(ChatAction.OnDismissConflict) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_RUN_ACTIVE_TITLE)) },
        text = { Text(AnrealCopy.get(AnrealCopy.ERROR_RUN_ACTIVE)) },
        confirmButton = {
            TextButton(onClick = { onAction(ChatAction.OnResumeConflict) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_RESUME))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ChatAction.OnDismissConflict) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_WAIT))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun RunActiveDialogPreview() {
    AnrealPreview {
        RunActiveDialog(onAction = {})
    }
}
