package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
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
                            onClick = { onAction(ChatAction.OnSessionClick(session.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionRow(
    session: ChatSessionUi,
    onClick: () -> Unit,
) {
    ListItem(
        headlineContent = { Text(session.title) },
        supportingContent = if (session.unread) {
            { Text(AnrealCopy.get(AnrealCopy.LABEL_UNREAD)) }
        } else {
            null
        },
        modifier = Modifier.clickable(onClick = onClick),
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
