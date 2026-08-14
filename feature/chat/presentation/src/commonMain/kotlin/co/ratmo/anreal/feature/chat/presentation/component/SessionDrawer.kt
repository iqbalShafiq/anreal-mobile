package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatSessionUi
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatEmptyPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatErrorPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatLoadingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewReadSession
import co.ratmo.anreal.feature.chat.presentation.preview.previewUnreadSession
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.More_vert

@Composable
internal fun SessionDrawer(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxSize().padding(AnrealSpacing.md)) {
        Text(
            text = AnrealCopy.get(AnrealCopy.LABEL_CHATS),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = AnrealSpacing.sm),
        )
        when {
            state.sessionsLoading && state.sessions.isEmpty() -> {
                AnrealSkeletonList(count = 6, itemHeight = 48.dp)
            }
            state.sessionsError != null && state.sessions.isEmpty() -> {
                AnrealError(
                    message = state.sessionsError.asString(),
                    onRetry = { onAction(ChatAction.OnRefreshSessions) },
                )
            }
            state.sessions.isEmpty() -> {
                AnrealEmpty(
                    title = AnrealCopy.get(AnrealCopy.CHAT_SESSIONS_EMPTY_TITLE),
                    body = AnrealCopy.get(AnrealCopy.CHAT_SESSIONS_EMPTY_BODY),
                    actionLabel = AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT),
                    onAction = { onAction(ChatAction.OnNewChat) },
                )
            }
            else -> {
                LazyColumn {
                    items(state.sessions, key = { it.id }) { session ->
                        SessionRow(
                            session = session,
                            selected = session.id == state.selectedSessionId,
                            onClick = { onAction(ChatAction.OnSessionClick(session.id)) },
                            onRename = { onAction(ChatAction.OnSessionMenuRename(session.id)) },
                            onDelete = { onAction(ChatAction.OnSessionMenuDelete(session.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
internal fun SessionRow(
    session: ChatSessionUi,
    selected: Boolean,
    onClick: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit,
    menuExpanded: Boolean? = null,
) {
    var expanded by remember { mutableStateOf(false) }
    val menuOpen = menuExpanded ?: expanded
    ListItem(
        headlineContent = { Text(session.title) },
        supportingContent = if (session.unread) {
            { Text(AnrealCopy.get(AnrealCopy.LABEL_UNREAD)) }
        } else {
            null
        },
        trailingContent = {
            Box {
                IconButton(onClick = { if (menuExpanded == null) expanded = true }) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.More_vert,
                        contentDescription = AnrealCopy.get(AnrealCopy.CD_SESSION_MENU),
                    )
                }
                DropdownMenu(
                    expanded = menuOpen,
                    onDismissRequest = { if (menuExpanded == null) expanded = false },
                ) {
                    DropdownMenuItem(
                        text = { Text(AnrealCopy.get(AnrealCopy.ACTION_RENAME)) },
                        onClick = {
                            if (menuExpanded == null) expanded = false
                            onRename()
                        },
                    )
                    DropdownMenuItem(
                        text = { Text(AnrealCopy.get(AnrealCopy.ACTION_DELETE)) },
                        onClick = {
                            if (menuExpanded == null) expanded = false
                            onDelete()
                        },
                    )
                }
            }
        },
        modifier = Modifier.clickable(onClick = onClick),
        colors = if (selected) {
            ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
            )
        } else {
            ListItemDefaults.colors(
                containerColor = Color.Transparent,
            )
        },
    )
}

@AnrealPreviews
@Composable
private fun SessionDrawerLoadingPreview() {
    AnrealPreview {
        SessionDrawer(state = chatLoadingPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerErrorPreview() {
    AnrealPreview {
        SessionDrawer(state = chatErrorPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerEmptyPreview() {
    AnrealPreview {
        SessionDrawer(state = chatEmptyPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SessionDrawerPopulatedPreview() {
    AnrealPreview {
        SessionDrawer(state = chatPopulatedPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SessionRowUnreadPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = false,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowReadPreview() {
    AnrealPreview {
        SessionRow(
            session = previewReadSession,
            selected = false,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowSelectedPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = true,
            onClick = {},
            onRename = {},
            onDelete = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SessionRowMenuOpenPreview() {
    AnrealPreview {
        SessionRow(
            session = previewUnreadSession,
            selected = true,
            onClick = {},
            onRename = {},
            onDelete = {},
            menuExpanded = true,
        )
    }
}
