package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealComposerField
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.component.GlassChrome
import co.ratmo.anreal.core.designsystem.component.GlassChromeMode
import co.ratmo.anreal.core.designsystem.component.GlassTopBar
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.PublicShareSnapshot
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.presentation.component.MessageBubble
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_back
import com.composables.icons.materialsymbols.rounded.Arrow_upward
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun SharedChatRoot(
    onBack: () -> Unit = {},
    onNavigateToChat: (String, String) -> Unit = { _, _ -> },
    onNavigateToLogin: (String) -> Unit = {},
    onNavigateToRegister: (String) -> Unit = {},
    isAuthenticated: Flow<Boolean> = flowOf(false),
    viewModel: SharedChatViewModel = koinViewModel { parametersOf(isAuthenticated) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SharedChatEvent.NavigateToFork -> onNavigateToChat(event.sessionId, event.firstMessage)
            is SharedChatEvent.NavigateToLogin -> onNavigateToLogin(event.token)
            is SharedChatEvent.NavigateToRegister -> onNavigateToRegister(event.token)
            is SharedChatEvent.NavigateToSignIn -> onNavigateToLogin(event.token)
            is SharedChatEvent.ShowMessage -> Unit
        }
    }
    SharedChatScreen(state = state, onAction = viewModel::onAction, onBack = onBack)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SharedChatScreen(
    state: SharedChatState,
    onAction: (SharedChatAction) -> Unit,
    onBack: () -> Unit = {},
) {
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    AnrealAtmosphere {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            topBar = {
                GlassTopBar(frosted = false, surfaceTinted = true) {
                    TopAppBar(
                        title = {
                            Text(
                                text = state.snapshot?.title?.takeIf { it.isNotBlank() }
                                    ?: AnrealCopy.get(AnrealCopy.SHARED_CHAT_TITLE),
                                style = MaterialTheme.typography.titleMedium,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                            )
                        },
                        navigationIcon = {
                            IconButton(onClick = onBack) {
                                Icon(
                                    imageVector = MaterialSymbols.Rounded.Arrow_back,
                                    contentDescription = AnrealCopy.get(AnrealCopy.CD_BACK),
                                )
                            }
                        },
                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                    )
                }
            },
        ) { padding ->
            Box(modifier = Modifier.fillMaxSize()) {
                when {
                    state.isLoading -> Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        AnrealLoadingIndicator()
                    }
                    state.error != null -> AnrealError(
                        message = state.error.asString(),
                        onRetry = { onAction(SharedChatAction.OnRetry) },
                    )
                    state.messages.isEmpty() -> AnrealEmpty(
                        title = AnrealCopy.get(AnrealCopy.SHARED_CHAT_TITLE),
                        body = AnrealCopy.get(AnrealCopy.SHARED_CHAT_EMPTY),
                    )
                    else -> {
                        val listState = rememberLazyListState()
                        LaunchedEffect(state.messages.size) {
                            if (state.messages.isNotEmpty()) {
                                listState.scrollToItem(state.messages.lastIndex)
                            }
                        }
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            state = listState,
                            contentPadding = PaddingValues(
                                start = AnrealSpacing.md,
                                end = AnrealSpacing.md,
                                top = padding.calculateTopPadding() + AnrealSpacing.md,
                                bottom = with(density) { composerHeightPx.toDp() } + AnrealSpacing.sm,
                            ),
                        ) {
                            val visible = state.messages.filter { it.isVisibleSnapshotMessage() }
                            itemsIndexed(visible, key = { _, message -> message.id }) { index, message ->
                                MessageBubble(
                                    message = message,
                                    busy = false,
                                    showActions = false,
                                    modifier = Modifier.padding(top = if (index == 0) 0.dp else AnrealSpacing.md),
                                )
                            }
                        }
                        if (state.isAuthenticated) {
                            SharedComposerBar(
                                state = state,
                                onAction = onAction,
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .onSizeChanged { composerHeightPx = it.height },
                            )
                        } else {
                            SharedAuthGate(
                                onLogin = { onAction(SharedChatAction.OnLoginClick) },
                                onRegister = { onAction(SharedChatAction.OnRegisterClick) },
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .onSizeChanged { composerHeightPx = it.height },
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun ChatMessage.isVisibleSnapshotMessage(): Boolean {
    if (role != ChatRole.User && role != ChatRole.Assistant) return false
    return parts.filterIsInstance<ChatPart.Text>().any { it.text.isNotBlank() }
}

@Composable
private fun SharedComposerBar(
    state: SharedChatState,
    onAction: (SharedChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    val keyboard = LocalSoftwareKeyboardController.current
    val canSubmit = state.draft.isNotBlank() && !state.isForking
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
    ) {
        GlassChrome(
            modifier = Modifier.fillMaxWidth(),
            mode = GlassChromeMode.Surface,
            emphasized = canSubmit || state.isForking,
        ) {
            Column(
                modifier = Modifier.padding(
                    start = AnrealSpacing.md,
                    end = AnrealSpacing.sm,
                    top = AnrealSpacing.md,
                    bottom = AnrealSpacing.sm,
                ),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                Surface(
                    shape = MaterialTheme.shapes.extraLarge,
                    color = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
                ) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.SHARED_CHAT_FORK_CHIP),
                        modifier = Modifier.padding(
                            horizontal = AnrealSpacing.md,
                            vertical = AnrealSpacing.xs,
                        ),
                        style = MaterialTheme.typography.labelLarge,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                }
                AnrealComposerField(
                    value = state.draft,
                    onValueChange = { onAction(SharedChatAction.OnDraftChange(it)) },
                    placeholder = AnrealCopy.get(AnrealCopy.COMPOSER_PLACEHOLDER),
                    enabled = !state.isForking,
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    FilledIconButton(
                        onClick = {
                            keyboard?.hide()
                            onAction(SharedChatAction.OnSubmit)
                        },
                        enabled = canSubmit,
                        modifier = Modifier.size(AnrealSpacing.touch),
                        shape = CircleShape,
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Arrow_upward,
                            contentDescription = AnrealCopy.get(AnrealCopy.ACTION_SEND),
                        )
                    }
                }
                state.forkError?.let { error ->
                    Text(
                        text = error.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            }
        }
    }
}

@Composable
private fun SharedAuthGate(
    onLogin: () -> Unit,
    onRegister: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
    ) {
        GlassChrome(
            modifier = Modifier.fillMaxWidth(),
            mode = GlassChromeMode.Surface,
            emphasized = false,
        ) {
            Column(
                modifier = Modifier.padding(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Text(
                    text = AnrealCopy.get(AnrealCopy.SHARED_CHAT_GATE_BODY),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                ) {
                    TextButton(onClick = onLogin) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_SIGN_IN))
                    }
                    FilledTonalButton(onClick = onRegister) {
                        Text(AnrealCopy.get(AnrealCopy.ACTION_CREATE_ACCOUNT))
                    }
                }
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun SharedChatLoadingPreview() {
    AnrealPreview {
        SharedChatScreen(state = SharedChatState(token = "tok"), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SharedChatPopulatedPreview() {
    AnrealPreview {
        SharedChatScreen(
            state = SharedChatState(
                token = "tok",
                isLoading = false,
                isAuthenticated = true,
                snapshot = PublicShareSnapshot(
                    token = "tok",
                    title = "Q3 revenue notes",
                    createdAt = "now",
                    ownerName = "Ada",
                    messages = listOf(
                        ChatMessage(
                            id = "share-0",
                            role = ChatRole.User,
                            parts = listOf(ChatPart.Text("share-0-0", "Summarize the PDF.")),
                            isComplete = true,
                        ),
                        ChatMessage(
                            id = "share-1",
                            role = ChatRole.Assistant,
                            parts = listOf(ChatPart.Text("share-1-0", "Revenue grew 12%.")),
                            isComplete = true,
                        ),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SharedChatGuestPreview() {
    AnrealPreview {
        SharedChatScreen(
            state = SharedChatState(
                token = "tok",
                isLoading = false,
                isAuthenticated = false,
                snapshot = PublicShareSnapshot(
                    token = "tok",
                    title = "Q3 revenue notes",
                    createdAt = "now",
                    ownerName = "Ada",
                    messages = listOf(
                        ChatMessage(
                            id = "share-0",
                            role = ChatRole.User,
                            parts = listOf(ChatPart.Text("share-0-0", "Summarize the PDF.")),
                            isComplete = true,
                        ),
                    ),
                ),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SharedChatErrorPreview() {
    AnrealPreview {
        SharedChatScreen(
            state = SharedChatState(
                token = "dead",
                isLoading = false,
                error = UiText.StringResource(AnrealCopy.ERROR_SHARE_NOT_FOUND),
            ),
            onAction = {},
        )
    }
}
