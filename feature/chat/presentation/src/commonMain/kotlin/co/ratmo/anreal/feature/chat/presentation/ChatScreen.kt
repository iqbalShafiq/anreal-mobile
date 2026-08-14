package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Menu
import com.composables.icons.materialsymbols.rounded.Send
import com.composables.icons.materialsymbols.rounded.Stop
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoot(
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { }
    ChatScreen(state = state, onAction = viewModel::onAction)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    LaunchedEffect(state.selectedSessionId) {
        drawerState.close()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                SessionDrawer(
                    state = state,
                    onAction = onAction,
                )
            }
        },
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            state.sessions.firstOrNull { it.id == state.selectedSessionId }?.title
                                ?: AnrealCopy.get(AnrealCopy.LABEL_CHATS),
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { scope.launch { drawerState.open() } }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Menu,
                                contentDescription = AnrealCopy.get(AnrealCopy.CD_OPEN_CHATS),
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { onAction(ChatAction.OnNewChat) }) {
                            Icon(
                                imageVector = MaterialSymbols.Rounded.Add,
                                contentDescription = AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT),
                            )
                        }
                    },
                )
            },
        ) { padding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
            ) {
                ThreadPane(
                    state = state,
                    onAction = onAction,
                    modifier = Modifier.weight(1f),
                )
                ComposerBar(state = state, onAction = onAction)
            }
        }
    }

    if (state.runActiveConflict) {
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
}

@Composable
private fun SessionDrawer(
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
                        ListItem(
                            headlineContent = { Text(session.title) },
                            supportingContent = if (session.unread) {
                                { Text(AnrealCopy.get(AnrealCopy.LABEL_UNREAD)) }
                            } else {
                                null
                            },
                            modifier = Modifier.clickable { onAction(ChatAction.OnSessionClick(session.id)) },
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ThreadPane(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.historyLoading && state.thread.messages.isEmpty() -> {
            AnrealSkeletonList(
                modifier = modifier.padding(AnrealSpacing.md),
                count = 5,
                itemHeight = 64.dp,
            )
        }
        state.historyError != null && state.thread.messages.isEmpty() -> {
            AnrealError(
                modifier = modifier,
                message = state.historyError.asString(),
                onRetry = { onAction(ChatAction.OnRetryHistory) },
            )
        }
        state.thread.messages.isEmpty() -> {
            AnrealEmpty(
                modifier = modifier,
                title = AnrealCopy.get(AnrealCopy.CHAT_EMPTY_TITLE),
                body = AnrealCopy.get(AnrealCopy.CHAT_EMPTY_BODY),
            )
        }
        else -> {
            val listState = rememberLazyListState()
            LaunchedEffect(state.thread.messages.size) {
                listState.animateScrollToItem(state.thread.messages.lastIndex.coerceAtLeast(0))
            }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(state.thread.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val text = message.parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
    val reasoning = message.parts.filterIsInstance<ChatPart.Reasoning>().joinToString("") { it.text }
    val isUser = message.role == ChatRole.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        if (reasoning.isNotBlank()) {
            Text(
                text = reasoning,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (text.isNotBlank()) {
            Text(
                text = text,
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurface,
            )
        }
    }
}

@Composable
private fun ComposerBar(
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
private fun ChatEmptyPreview() {
    AnrealPreview {
        ChatScreen(state = ChatState(sessionsLoading = false), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatLoadingPreview() {
    AnrealPreview {
        ChatScreen(state = ChatState(sessionsLoading = true, historyLoading = true), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatErrorPreview() {
    AnrealPreview {
        ChatScreen(
            state = ChatState(
                sessionsLoading = false,
                sessionsError = UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET),
                historyError = UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ChatPopulatedPreview() {
    AnrealPreview {
        ChatScreen(
            state = populatedState(),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ChatStreamingPreview() {
    AnrealPreview {
        ChatScreen(
            state = populatedState().copy(
                isSending = true,
                thread = populatedState().thread.copy(status = RunStatus.Streaming),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ChatConflictPreview() {
    AnrealPreview {
        ChatScreen(
            state = populatedState().copy(runActiveConflict = true),
            onAction = {},
        )
    }
}

private fun populatedState(): ChatState = ChatState(
    sessionsLoading = false,
    sessions = listOf(
        ChatSessionUi(id = "s1", title = "Q3 report", unread = true),
        ChatSessionUi(id = "s2", title = "New chat", unread = false),
    ),
    selectedSessionId = "s1",
    thread = ChatThreadState(
        messages = listOf(
            ChatMessage(
                id = "u1",
                role = ChatRole.User,
                parts = listOf(ChatPart.Text(id = "u1t", text = "Summarize the PDF.")),
                isComplete = true,
            ),
            ChatMessage(
                id = "a1",
                role = ChatRole.Assistant,
                parts = listOf(ChatPart.Text(id = "a1t", text = "Revenue grew 12% year over year.")),
                isComplete = true,
            ),
        ),
    ),
    draft = "What about costs?",
)
