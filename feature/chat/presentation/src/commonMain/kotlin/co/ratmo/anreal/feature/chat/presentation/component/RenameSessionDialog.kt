package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.input.ImeAction
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState

@Composable
internal fun RenameSessionDialog(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { if (!state.sessionBusy) onAction(ChatAction.OnDismissSessionDialog) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_RENAME_TITLE)) },
        text = {
            AnrealTextField(
                value = state.renameDraft,
                onValueChange = { onAction(ChatAction.OnRenameDraftChange(it)) },
                label = AnrealCopy.get(AnrealCopy.LABEL_SESSION_TITLE),
                error = state.renameError?.asString(),
                enabled = !state.sessionBusy,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(
                    onDone = { onAction(ChatAction.OnConfirmRename) },
                ),
            )
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnConfirmRename) },
                enabled = !state.sessionBusy,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_RENAME))
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
private fun RenameSessionDialogIdlePreview() {
    AnrealPreview {
        RenameSessionDialog(
            state = ChatState(renameSessionId = "s1", renameDraft = ""),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RenameSessionDialogFilledPreview() {
    AnrealPreview {
        RenameSessionDialog(
            state = ChatState(renameSessionId = "s1", renameDraft = "Q3 report"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RenameSessionDialogBlankErrorPreview() {
    AnrealPreview {
        RenameSessionDialog(
            state = ChatState(
                renameSessionId = "s1",
                renameDraft = "",
                renameError = UiText.StringResource(AnrealCopy.ERROR_TITLE_REQUIRED),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RenameSessionDialogBusyPreview() {
    AnrealPreview {
        RenameSessionDialog(
            state = ChatState(
                renameSessionId = "s1",
                renameDraft = "Q3 report",
                sessionBusy = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RenameSessionDialogNetworkErrorPreview() {
    AnrealPreview {
        RenameSessionDialog(
            state = ChatState(
                renameSessionId = "s1",
                renameDraft = "Q3 report",
                renameError = UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET),
            ),
            onAction = {},
        )
    }
}
