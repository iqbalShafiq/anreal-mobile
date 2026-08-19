package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.GlassDrawer
import co.ratmo.anreal.core.designsystem.component.GlassTopBar
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.component.ComposerBar
import co.ratmo.anreal.feature.chat.presentation.component.ContextUsageButton
import co.ratmo.anreal.feature.chat.presentation.component.ContextUsageSheet
import co.ratmo.anreal.feature.chat.presentation.component.ApprovalDialog
import co.ratmo.anreal.feature.chat.presentation.component.ClarificationDialog
import co.ratmo.anreal.feature.chat.presentation.component.DeleteSessionDialog
import co.ratmo.anreal.feature.chat.presentation.component.DocumentLibraryDialog
import co.ratmo.anreal.feature.chat.presentation.component.DocumentsEndDrawer
import co.ratmo.anreal.feature.chat.presentation.component.QueueConflictDialog
import co.ratmo.anreal.feature.chat.presentation.component.RenameSessionDialog
import co.ratmo.anreal.feature.chat.presentation.component.RunActiveDialog
import co.ratmo.anreal.feature.chat.presentation.component.SessionDrawer
import co.ratmo.anreal.feature.chat.presentation.component.ThreadPane
import co.ratmo.anreal.feature.chat.presentation.component.documentsBadgeCount
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
import co.ratmo.anreal.feature.chat.presentation.preview.chatWorkspacePreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewAccount
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Description
import com.composables.icons.materialsymbols.rounded.Menu
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import io.github.vinceglb.filekit.name
import org.koin.compose.viewmodel.koinViewModel

@Composable
@Suppress("DEPRECATION")
fun ChatRoot(
    account: AccountUi = AccountUi(),
    onNavigateAccount: () -> Unit = {},
    onNavigateProjects: () -> Unit = {},
    onNavigateDocuments: () -> Unit = {},
    onNavigateImages: () -> Unit = {},
    viewModel: ChatViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboard = LocalClipboardManager.current
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is ChatEvent.ShowMessage -> snackbarScope.launch {
                snackbarHostState.showSnackbar(event.message.asString())
            }
            is ChatEvent.CopyText -> snackbarScope.launch {
                clipboard.setText(AnnotatedString(event.text))
                snackbarHostState.showSnackbar(AnrealCopy.get(AnrealCopy.TOAST_COPIED))
            }
            is ChatEvent.PickFiles -> snackbarScope.launch {
                try {
                    val type = if (event.imagesOnly) {
                        FileKitType.Image
                    } else {
                        FileKitType.File(listOf("pdf", "png", "jpg", "jpeg", "webp"))
                    }
                    FileKit.openFilePicker(type = type)?.let { file ->
                        viewModel.onAction(
                            ChatAction.OnFilesPicked(
                                files = listOf(
                                    PickedUploadUi(
                                        filename = file.name,
                                        mediaType = file.name.toMediaType(),
                                        bytes = file.readBytes(),
                                    ),
                                ),
                                imagesOnly = event.imagesOnly,
                            ),
                        )
                    }
                } catch (exception: CancellationException) {
                    throw exception
                } catch (exception: Exception) {
                    viewModel.onAction(
                        ChatAction.OnFilePickerFailed(
                            exception.message ?: AnrealCopy.get(AnrealCopy.ERROR_FILE_READ),
                        ),
                    )
                }
            }
            ChatEvent.OpenAccount -> onNavigateAccount()
            ChatEvent.OpenProjects -> onNavigateProjects()
            ChatEvent.OpenDocuments -> onNavigateDocuments()
            ChatEvent.OpenImages -> onNavigateImages()
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        ChatScreen(state = state, onAction = viewModel::onAction, account = account)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
}

private fun String.toMediaType(): String = when (substringAfterLast('.', "").lowercase()) {
    "pdf" -> "application/pdf"
    "jpg", "jpeg" -> "image/jpeg"
    "webp" -> "image/webp"
    else -> "image/png"
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    account: AccountUi = AccountUi(),
    initialChatsDrawer: DrawerValue = DrawerValue.Closed,
    initialDocumentsDrawer: Boolean = false,
) {
    val drawerState = rememberDrawerState(initialValue = initialChatsDrawer)
    var documentsOpen by remember { mutableStateOf(initialDocumentsDrawer) }
    var contextUsageOpen by remember { mutableStateOf(false) }
    var composerHeightPx by remember { mutableIntStateOf(0) }
    val density = LocalDensity.current
    val scope = rememberCoroutineScope()
    val documentCount = documentsBadgeCount(state)
    var skipInitialDrawerClose by remember { mutableStateOf(true) }
    LaunchedEffect(state.selectedSessionId) {
        if (skipInitialDrawerClose) {
            skipInitialDrawerClose = false
            return@LaunchedEffect
        }
        drawerState.close()
        documentsOpen = false
    }

    AnrealAtmosphere {
        Box(modifier = Modifier.fillMaxSize()) {
            ModalNavigationDrawer(
                drawerState = drawerState,
                drawerContent = {
                    GlassDrawer(fromEnd = false) {
                        SessionDrawer(
                            state = state,
                            onAction = onAction,
                            account = account,
                        )
                    }
                },
            ) {
                Scaffold(
                    containerColor = Color.Transparent,
                    topBar = {
                        GlassTopBar {
                            TopAppBar(
                                title = {
                                    Text(
                                        text = state.sessions.firstOrNull { it.id == state.selectedSessionId }?.title
                                            ?: AnrealCopy.get(AnrealCopy.LABEL_CHATS),
                                        style = MaterialTheme.typography.titleMedium,
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
                                    ContextUsageButton(
                                        usage = state.contextUsage,
                                        error = state.contextUsageError,
                                        onClick = { contextUsageOpen = true },
                                    )
                                    IconButton(onClick = { documentsOpen = true }) {
                                        if (documentCount > 0) {
                                            BadgedBox(
                                                badge = { Badge { Text(documentCount.toString()) } },
                                            ) {
                                                Icon(
                                                    imageVector = MaterialSymbols.Rounded.Description,
                                                    contentDescription = AnrealCopy.get(AnrealCopy.CD_OPEN_DOCUMENTS),
                                                )
                                            }
                                        } else {
                                            Icon(
                                                imageVector = MaterialSymbols.Rounded.Description,
                                                contentDescription = AnrealCopy.get(AnrealCopy.CD_OPEN_DOCUMENTS),
                                            )
                                        }
                                    }
                                },
                                colors = TopAppBarDefaults.topAppBarColors(
                                    containerColor = Color.Transparent,
                                    scrolledContainerColor = Color.Transparent,
                                ),
                            )
                        }
                    },
                ) { padding ->
                    Box(modifier = Modifier.fillMaxSize()) {
                        ThreadPane(
                            state = state,
                            onAction = onAction,
                            modifier = Modifier.fillMaxSize(),
                            topContentPadding = padding.calculateTopPadding() + AnrealSpacing.sm,
                            bottomContentPadding = with(density) { composerHeightPx.toDp() } + AnrealSpacing.sm,
                            initialScrollReady = composerHeightPx > 0,
                        )
                        ComposerBar(
                            state = state,
                            onAction = onAction,
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .onSizeChanged { composerHeightPx = it.height },
                        )
                    }
                }
            }
            DocumentsEndDrawer(
                open = documentsOpen,
                state = state,
                onAction = onAction,
                onDismiss = { documentsOpen = false },
            )
            if (contextUsageOpen) {
                ContextUsageSheet(
                    usage = state.contextUsage,
                    error = state.contextUsageError,
                    onDismiss = { contextUsageOpen = false },
                )
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
    state.thread.pendingApprovals.firstOrNull()?.let { approval ->
        ApprovalDialog(approval, state.humanInputBusy, onAction)
    }
    state.thread.pendingClarifications.firstOrNull()?.let { clarification ->
        ClarificationDialog(clarification, state.humanInputBusy, onAction)
    }
    if (state.libraryOpen) {
        DocumentLibraryDialog(state, onAction)
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

@AnrealPreviews
@Composable
private fun ChatWorkspaceDrawerPreview() {
    AnrealPreview {
        ChatScreen(
            state = chatWorkspacePreviewState(),
            onAction = {},
            account = previewAccount,
            initialChatsDrawer = DrawerValue.Open,
        )
    }
}

@AnrealPreviews
@Composable
private fun ChatDocumentsDrawerPreview() {
    AnrealPreview {
        ChatScreen(
            state = chatWorkspacePreviewState(),
            onAction = {},
            account = previewAccount,
            initialDocumentsDrawer = true,
        )
    }
}
