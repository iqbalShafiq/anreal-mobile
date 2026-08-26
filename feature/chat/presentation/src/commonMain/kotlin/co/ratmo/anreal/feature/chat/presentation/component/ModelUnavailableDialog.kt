package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ModelUnavailableUi

@Composable
internal fun ModelUnavailableDialog(
    model: ModelUnavailableUi,
    onAction: (ChatAction) -> Unit,
    onChooseModel: () -> Unit = {},
) {
    AlertDialog(
        onDismissRequest = { onAction(ChatAction.OnDismissModelUnavailable) },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_MODEL_UNAVAILABLE_TITLE)) },
        text = {
            Text(
                UiText.StringResource(
                    AnrealCopy.DIALOG_MODEL_UNAVAILABLE_BODY,
                    listOf(model.label),
                ).asString(),
            )
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(ChatAction.OnDismissModelUnavailable)
                    onChooseModel()
                },
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CHOOSE_MODEL))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun ModelUnavailableDialogPreview() {
    AnrealPreview {
        ModelUnavailableDialog(
            model = ModelUnavailableUi(modelId = "removed", label = "Old model"),
            onAction = {},
        )
    }
}
