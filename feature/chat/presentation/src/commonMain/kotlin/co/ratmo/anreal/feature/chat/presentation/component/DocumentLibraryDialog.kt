package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState

@Composable
internal fun DocumentLibraryDialog(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(ChatAction.OnDismissLibrary) },
        title = { Text(AnrealCopy.get(AnrealCopy.LABEL_DOCUMENT_LIBRARY)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                AnrealTextField(
                    value = state.libraryQuery,
                    onValueChange = { onAction(ChatAction.OnLibraryQueryChange(it)) },
                    label = AnrealCopy.get(AnrealCopy.LABEL_SEARCH_DOCUMENTS),
                    placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_SEARCH_DOCUMENTS),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                )
                when {
                    state.libraryLoading -> AnrealSkeletonList(count = 4, itemHeight = 56.dp)
                    state.libraryError != null -> AnrealError(
                        message = state.libraryError.asString(),
                        onRetry = { onAction(ChatAction.OnRetryLibrary) },
                    )
                    state.libraryDocuments.isEmpty() -> Text(
                        AnrealCopy.get(AnrealCopy.ATTACH_LIBRARY_EMPTY),
                    )
                    else -> LazyColumn(modifier = Modifier.heightIn(max = 360.dp)) {
                        items(state.libraryDocuments, key = { it.id }) { document ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        onAction(ChatAction.OnToggleLibraryDocument(document.id))
                                    }
                                    .padding(vertical = AnrealSpacing.xs),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                            ) {
                                Checkbox(
                                    checked = document.selected,
                                    onCheckedChange = {
                                        onAction(ChatAction.OnToggleLibraryDocument(document.id))
                                    },
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(document.filename, maxLines = 1, overflow = TextOverflow.Ellipsis)
                                    Text(
                                        listOf(document.summary, document.detail)
                                            .filter(String::isNotBlank)
                                            .joinToString(" · "),
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                    )
                                }
                            }
                        }
                        if (state.libraryNextCursor != null) {
                            item(key = "load-more") {
                                TextButton(
                                    onClick = { onAction(ChatAction.OnLoadMoreLibrary) },
                                    enabled = !state.libraryLoadingMore,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Text(
                                        AnrealCopy.get(
                                            if (state.libraryLoadingMore) {
                                                AnrealCopy.STATUS_LOADING
                                            } else {
                                                AnrealCopy.ACTION_LOAD_MORE
                                            },
                                        ),
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnAttachLibraryDocuments) },
                enabled = state.libraryDocuments.any { it.selected } && !state.libraryLoading,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_ATTACH)) }
        },
        dismissButton = {
            TextButton(onClick = { onAction(ChatAction.OnDismissLibrary) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
        },
    )
}
