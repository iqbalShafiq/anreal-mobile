package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatShareLinkUi
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState

@Composable
internal fun ShareDialog(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {
            if (!state.isCreatingShare && !state.isDeactivatingShares) {
                onAction(ChatAction.OnDismissShare)
            }
        },
        title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_SHARE_TITLE)) },
        text = {
            Column {
                Text(
                    AnrealCopy.get(AnrealCopy.DIALOG_SHARE_BODY),
                    style = MaterialTheme.typography.bodyMedium,
                )
                when {
                    state.shareLoading -> {
                        AnrealLoadingIndicator(
                            modifier = Modifier.padding(top = AnrealSpacing.md),
                        )
                    }
                    state.shareError != null && state.latestShare == null -> {
                        Text(
                            text = state.shareError.asString(),
                            modifier = Modifier.padding(top = AnrealSpacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    state.latestShare != null -> {
                        Text(
                            text = state.latestShare.urlPath,
                            modifier = Modifier.padding(top = AnrealSpacing.sm),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    else -> {
                        Text(
                            text = AnrealCopy.get(AnrealCopy.DIALOG_SHARE_NO_LINK),
                            modifier = Modifier.padding(top = AnrealSpacing.sm),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                    }
                }
                state.shareError?.let { error ->
                    if (state.latestShare != null) {
                        Text(
                            text = error.asString(),
                            modifier = Modifier.padding(top = AnrealSpacing.sm),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnCreateShare) },
                enabled = !state.isCreatingShare && !state.shareLoading,
            ) {
                Text(
                    if (state.isCreatingShare) {
                        AnrealCopy.get(AnrealCopy.ACTION_CREATING_LINK)
                    } else {
                        AnrealCopy.get(AnrealCopy.ACTION_CREATE_LINK)
                    },
                )
            }
        },
        dismissButton = {
            Column {
                state.latestShare?.let {
                    TextButton(
                        onClick = { onAction(ChatAction.OnCopyShareLink) },
                        enabled = !state.isCreatingShare,
                    ) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_COPY_LINK))
                    }
                }
                if (state.shareStatusActive == true) {
                    TextButton(
                        onClick = { onAction(ChatAction.OnRequestDeactivateShares) },
                        enabled = !state.isDeactivatingShares,
                    ) {
                        Text(
                            if (state.isDeactivatingShares) {
                                AnrealCopy.get(AnrealCopy.ACTION_DEACTIVATING_LINKS)
                            } else {
                                AnrealCopy.get(AnrealCopy.ACTION_DEACTIVATE_LINKS)
                            },
                        )
                    }
                }
                TextButton(
                    onClick = { onAction(ChatAction.OnDismissShare) },
                    enabled = !state.isCreatingShare && !state.isDeactivatingShares,
                ) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CLOSE))
                }
            }
        },
    )
    if (state.showDeactivateConfirm) {
        AlertDialog(
            onDismissRequest = { onAction(ChatAction.OnDismissDeactivateShares) },
            title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DEACTIVATE_SHARES_TITLE)) },
            text = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DEACTIVATE_SHARES_BODY)) },
            confirmButton = {
                TextButton(
                    onClick = { onAction(ChatAction.OnConfirmDeactivateShares) },
                    enabled = !state.isDeactivatingShares,
                ) {
                    Text(
                        if (state.isDeactivatingShares) {
                            AnrealCopy.get(AnrealCopy.ACTION_DEACTIVATING_LINKS)
                        } else {
                            AnrealCopy.get(AnrealCopy.ACTION_DEACTIVATE_LINKS)
                        },
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { onAction(ChatAction.OnDismissDeactivateShares) }) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
                }
            },
        )
    }
}

@AnrealPreviews
@Composable
private fun ShareDialogNoLinkPreview() {
    AnrealPreview {
        ShareDialog(
            state = chatPopulatedPreviewState().copy(shareOpen = true),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ShareDialogWithLinkPreview() {
    AnrealPreview {
        ShareDialog(
            state = chatPopulatedPreviewState().copy(
                shareOpen = true,
                shareStatusActive = true,
                latestShare = ChatShareLinkUi("tok", "/share/tok", "Notes"),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ShareDialogLoadingPreview() {
    AnrealPreview {
        ShareDialog(
            state = chatPopulatedPreviewState().copy(shareOpen = true, shareLoading = true),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ShareDialogErrorPreview() {
    AnrealPreview {
        ShareDialog(
            state = chatPopulatedPreviewState().copy(
                shareOpen = true,
                shareError = UiText.StringResource(AnrealCopy.ERROR_SHARE_UNAVAILABLE),
            ),
            onAction = {},
        )
    }
}
