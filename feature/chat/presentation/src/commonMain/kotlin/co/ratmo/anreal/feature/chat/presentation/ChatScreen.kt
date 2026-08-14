package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.component.ComposerBar
import co.ratmo.anreal.feature.chat.presentation.component.DeleteSessionDialog
import co.ratmo.anreal.feature.chat.presentation.component.MessageQueueDock
import co.ratmo.anreal.feature.chat.presentation.component.QueueConflictDialog
import co.ratmo.anreal.feature.chat.presentation.component.RenameSessionDialog
import co.ratmo.anreal.feature.chat.presentation.component.RunActiveDialog
import co.ratmo.anreal.feature.chat.presentation.component.SessionDrawer
import co.ratmo.anreal.feature.chat.presentation.component.ThreadPane
import co.ratmo.anreal.feature.chat.presentation.preview.chatConflictPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatEmptyPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatErrorPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatLoadingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatDeletePreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatRenamePreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatQueueConflictPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatQueuedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatStreamingPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Menu
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun ChatRoot(
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChatEvent.ShowMessage -> snackbarScope.launch {
                snackbarHostState.showSnackbar(event.message.asString())
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ChatScreen(state = state, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
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
                MessageQueueDock(state = state, onAction = onAction)
                ComposerBar(state = state, onAction = onAction)
            }
        }
    }

    if (state.runActiveConflict) {
        RunActiveDialog(onAction = onAction)
    }
    if (state.renameSessionId != null) {
        RenameSessionDialog(state = state, onAction = onAction)
    }
    if (state.deleteSessionId != null) {
        DeleteSessionDialog(state = state, onAction = onAction)
    }
    if (state.queueConflict) {
        QueueConflictDialog(onAction = onAction)
    }
}

@AnrealPreviews
@Composable
private fun ChatEmptyPreview() {
    AnrealPreview {
        ChatScreen(state = chatEmptyPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatLoadingPreview() {
    AnrealPreview {
        ChatScreen(state = chatLoadingPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatErrorPreview() {
    AnrealPreview {
        ChatScreen(state = chatErrorPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatPopulatedPreview() {
    AnrealPreview {
        ChatScreen(state = chatPopulatedPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatStreamingPreview() {
    AnrealPreview {
        ChatScreen(state = chatStreamingPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatConflictPreview() {
    AnrealPreview {
        ChatScreen(state = chatConflictPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatRenamePreview() {
    AnrealPreview {
        ChatScreen(state = chatRenamePreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatDeletePreview() {
    AnrealPreview {
        ChatScreen(state = chatDeletePreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatQueuedPreview() {
    AnrealPreview {
        ChatScreen(state = chatQueuedPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ChatQueueConflictPreview() {
    AnrealPreview {
        ChatScreen(state = chatQueueConflictPreviewState(), onAction = {})
    }
}
