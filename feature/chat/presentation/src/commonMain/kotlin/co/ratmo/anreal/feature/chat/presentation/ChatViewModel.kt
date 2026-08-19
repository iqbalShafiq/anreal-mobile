package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.onFailure
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.ContextUsage
import co.ratmo.anreal.feature.chat.domain.DocumentIngest
import co.ratmo.anreal.feature.chat.domain.LibraryDocument
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionImage
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.queue.QueueStatus
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.queue.addItem
import co.ratmo.anreal.feature.chat.domain.queue.applyAck
import co.ratmo.anreal.feature.chat.domain.queue.cancelEdit
import co.ratmo.anreal.feature.chat.domain.queue.finishEdit
import co.ratmo.anreal.feature.chat.domain.queue.markInflight
import co.ratmo.anreal.feature.chat.domain.queue.nextFlushable
import co.ratmo.anreal.feature.chat.domain.queue.removeItem
import co.ratmo.anreal.feature.chat.domain.queue.restoreQueue
import co.ratmo.anreal.feature.chat.domain.queue.revertInflight
import co.ratmo.anreal.feature.chat.domain.queue.startEdit
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatStreamEvent
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.domain.stream.StreamEnvelope
import co.ratmo.anreal.feature.chat.domain.stream.parseStreamLine
import co.ratmo.anreal.feature.chat.domain.stream.reduce
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield

private const val MAX_UPLOAD_BYTES = 10 * 1024 * 1024

data class ChatSessionUi(
    val id: String,
    val title: String,
    val unread: Boolean,
    val updatedAt: String = "",
    val projectId: String? = null,
)

data class AccountUi(
    val name: String = "",
    val email: String = "",
)

data class RecentProjectUi(
    val id: String,
    val name: String,
)

data class SessionDocumentUi(
    val id: String,
    val filename: String,
    val summary: String = "",
)

data class LibraryDocumentUi(
    val id: String,
    val filename: String,
    val summary: String,
    val detail: String,
    val selected: Boolean,
)

data class SessionImageUi(
    val id: String,
    val prompt: String,
    val detail: String,
    val bytes: ByteArray?,
    val pinned: Boolean,
)

data class DocumentIngestUi(
    val id: String,
    val filename: String,
    val status: String,
    val error: String?,
)

data class ContextUsageUi(
    val modelLabel: String,
    val estimatedTokens: Int,
    val contextWindowTokens: Int,
    val ratio: Float,
    val thresholdRatio: Float,
    val targetRatio: Float,
    val reasoningEffort: String?,
    val nearThreshold: Boolean,
) {
    val label: String get() = "$estimatedTokens / $contextWindowTokens tokens"
}

data class CitedDocumentUi(
    val id: String,
    val filename: String,
    val citationCount: Int,
)

@Stable
data class ChatState(
    val sessions: List<ChatSessionUi> = emptyList(),
    val sessionsLoading: Boolean = true,
    val sessionsError: UiText? = null,
    val sessionsNextCursor: String? = null,
    val sessionsLoadingMore: Boolean = false,
    val selectedSessionId: String? = null,
    val thread: ChatThreadState = ChatThreadState(),
    val historyLoading: Boolean = false,
    val historyError: UiText? = null,
    val draft: String = "",
    val editingMessageId: String? = null,
    val isSending: Boolean = false,
    val runActiveConflict: Boolean = false,
    val renameSessionId: String? = null,
    val renameDraft: String = "",
    val renameError: UiText? = null,
    val deleteSessionId: String? = null,
    val deleteError: UiText? = null,
    val sessionBusy: Boolean = false,
    val queue: List<QueuedItem> = emptyList(),
    val queueExpanded: Boolean = false,
    val queueHidden: Boolean = false,
    val queueConflict: Boolean = false,
    val models: List<ChatModel> = emptyList(),
    val reasoningEfforts: List<ReasoningEffort> = emptyList(),
    val selectedModelId: String? = null,
    val selectedReasoning: String? = null,
    val webSearchEnabled: Boolean = false,
    val imageGenerationEnabled: Boolean = false,
    val capabilities: ChatCapabilities = ChatCapabilities(),
    val contextSnippet: String? = null,
    val contextSnippetId: String? = null,
    val catalogLoading: Boolean = true,
    val catalogError: UiText? = null,
    val recentProjects: List<RecentProjectUi> = emptyList(),
    val activeDocuments: List<SessionDocumentUi> = emptyList(),
    val libraryOpen: Boolean = false,
    val libraryQuery: String = "",
    val libraryDocuments: List<LibraryDocumentUi> = emptyList(),
    val libraryNextCursor: String? = null,
    val libraryLoading: Boolean = false,
    val libraryLoadingMore: Boolean = false,
    val libraryError: UiText? = null,
    val sessionImages: List<SessionImageUi> = emptyList(),
    val imagesLoading: Boolean = false,
    val uploadingDocuments: List<DocumentIngestUi> = emptyList(),
    val contextUsage: ContextUsageUi? = null,
    val contextUsageError: Boolean = false,
    val citedDocuments: List<CitedDocumentUi> = emptyList(),
    val isUploading: Boolean = false,
    val humanInputBusy: Boolean = false,
)

data class PickedUploadUi(
    val filename: String,
    val mediaType: String,
    val bytes: ByteArray,
)

sealed interface ChatAction {
    data object OnRefreshSessions : ChatAction
    data object OnLoadMoreSessions : ChatAction
    data object OnNewChat : ChatAction
    data class OnSessionClick(val sessionId: String) : ChatAction
    data class OnSessionMenuRename(val sessionId: String) : ChatAction
    data class OnSessionMenuDelete(val sessionId: String) : ChatAction
    data class OnRenameDraftChange(val draft: String) : ChatAction
    data object OnConfirmRename : ChatAction
    data object OnConfirmDelete : ChatAction
    data object OnDismissSessionDialog : ChatAction
    data class OnDraftChange(val draft: String) : ChatAction
    data object OnSend : ChatAction
    data object OnStop : ChatAction
    data object OnSendNow : ChatAction
    data class OnRemoveQueued(val id: String) : ChatAction
    data class OnRecallQueued(val id: String) : ChatAction
    data object OnCancelQueueEdit : ChatAction
    data object OnHideQueue : ChatAction
    data object OnShowQueue : ChatAction
    data object OnToggleQueueExpanded : ChatAction
    data object OnSendQueue : ChatAction
    data object OnSendNewMessage : ChatAction
    data object OnDismissQueueConflict : ChatAction
    data object OnResumeConflict : ChatAction
    data object OnDismissConflict : ChatAction
    data object OnRetryHistory : ChatAction
    data class OnSelectModel(val modelId: String) : ChatAction
    data class OnSelectReasoning(val effort: String?) : ChatAction
    data object OnToggleWebSearch : ChatAction
    data object OnToggleImageGeneration : ChatAction
    data object OnRetryCatalog : ChatAction
    data class OnCopyMessage(val text: String) : ChatAction
    data class OnAddContext(val text: String, val sourceRole: ChatRole) : ChatAction
    data object OnClearContext : ChatAction
    data class OnEditMessage(val messageId: String) : ChatAction
    data class OnRegenerateMessage(val messageId: String) : ChatAction
    data object OnPickPhotos : ChatAction
    data object OnPickLocalDocument : ChatAction
    data class OnFilesPicked(val files: List<PickedUploadUi>, val imagesOnly: Boolean) : ChatAction
    data class OnFilePickerFailed(val message: String) : ChatAction
    data class OnApprovalDecision(val approvalId: String, val approved: Boolean) : ChatAction
    data class OnClarificationResponse(
        val clarificationId: String,
        val answers: Map<String, List<String>>,
        val skipped: List<String>,
    ) : ChatAction
    data object OnOpenLibrary : ChatAction
    data object OnDismissLibrary : ChatAction
    data class OnLibraryQueryChange(val query: String) : ChatAction
    data object OnRetryLibrary : ChatAction
    data object OnLoadMoreLibrary : ChatAction
    data class OnToggleLibraryDocument(val documentId: String) : ChatAction
    data object OnAttachLibraryDocuments : ChatAction
    data class OnToggleImageContext(val imageId: String) : ChatAction
    data object OnOpenProjects : ChatAction
    data object OnOpenDocumentsLibrary : ChatAction
    data object OnOpenImages : ChatAction
    data object OnOpenSettings : ChatAction
    data class OnOpenRecentProject(val projectId: String) : ChatAction
    data class OnRemoveActiveDocument(val documentId: String) : ChatAction
}

sealed interface ChatEvent {
    data class ShowMessage(val message: UiText) : ChatEvent
    data class CopyText(val text: String) : ChatEvent
    data class PickFiles(val imagesOnly: Boolean) : ChatEvent
    data object OpenAccount : ChatEvent
    data object OpenProjects : ChatEvent
    data object OpenDocuments : ChatEvent
    data object OpenImages : ChatEvent
}

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    private val preferencesRepository: AppPreferencesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(
        ChatState(
            selectedSessionId = savedStateHandle[SESSION_KEY],
            draft = savedStateHandle[DRAFT_KEY] ?: "",
        ),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<ChatEvent>()
    val events = _events.receiveAsFlow()
    private var hold: Boolean = false
    private var librarySearchJob: Job? = null

    init {
        viewModelScope.launch {
            chatRepository.observeSessions().collect { sessions ->
                _state.update { it.copy(sessions = sessions.map { session -> session.toUi() }) }
            }
        }
        viewModelScope.launch { bootstrap() }
        viewModelScope.launch { loadCatalog() }
        viewModelScope.launch { loadRecentProjects() }
    }

    fun onAction(action: ChatAction) {
        when (action) {
            ChatAction.OnRefreshSessions -> viewModelScope.launch { refreshSessions() }
            ChatAction.OnLoadMoreSessions -> viewModelScope.launch { loadMoreSessions() }
            ChatAction.OnNewChat -> viewModelScope.launch { openDraft() }
            is ChatAction.OnSessionClick -> viewModelScope.launch { selectSession(action.sessionId) }
            is ChatAction.OnSessionMenuRename -> openRename(action.sessionId)
            is ChatAction.OnSessionMenuDelete -> openDelete(action.sessionId)
            is ChatAction.OnRenameDraftChange -> _state.update {
                it.copy(renameDraft = action.draft.take(SESSION_TITLE_MAX), renameError = null)
            }
            ChatAction.OnConfirmRename -> viewModelScope.launch { confirmRename() }
            ChatAction.OnConfirmDelete -> viewModelScope.launch { confirmDelete() }
            ChatAction.OnDismissSessionDialog -> dismissSessionDialog()
            is ChatAction.OnDraftChange -> {
                savedStateHandle[DRAFT_KEY] = action.draft
                _state.update { it.copy(draft = action.draft) }
            }
            ChatAction.OnSend -> viewModelScope.launch { submitComposer() }
            ChatAction.OnStop -> viewModelScope.launch { stop() }
            ChatAction.OnSendNow -> viewModelScope.launch { sendNow() }
            is ChatAction.OnRemoveQueued -> viewModelScope.launch { dropQueued(action.id) }
            is ChatAction.OnRecallQueued -> recallQueued(action.id)
            ChatAction.OnCancelQueueEdit -> cancelQueueEdit()
            ChatAction.OnHideQueue -> _state.update { it.copy(queueHidden = true) }
            ChatAction.OnShowQueue -> _state.update { it.copy(queueHidden = false) }
            ChatAction.OnToggleQueueExpanded -> _state.update { it.copy(queueExpanded = !it.queueExpanded) }
            ChatAction.OnSendQueue -> viewModelScope.launch { sendQueueFromConflict() }
            ChatAction.OnSendNewMessage -> viewModelScope.launch { sendNewFromConflict() }
            ChatAction.OnDismissQueueConflict -> _state.update { it.copy(queueConflict = false) }
            ChatAction.OnResumeConflict -> viewModelScope.launch {
                _state.update { it.copy(runActiveConflict = false) }
                resumeActiveRun()
            }
            ChatAction.OnDismissConflict -> _state.update { it.copy(runActiveConflict = false) }
            ChatAction.OnRetryHistory -> viewModelScope.launch {
                _state.value.selectedSessionId?.let { loadHistory(it) }
            }
            is ChatAction.OnSelectModel -> selectModel(action.modelId)
            is ChatAction.OnSelectReasoning -> {
                val allowed = _state.value.models
                    .firstOrNull { it.id == _state.value.selectedModelId }
                    ?.reasoningEfforts
                    .orEmpty()
                val effort = action.effort?.takeIf { it in allowed }
                _state.update { it.copy(selectedReasoning = effort) }
                viewModelScope.launch {
                    preferencesRepository.setChatReasoningEffort(effort)
                    _state.value.selectedSessionId?.let { loadContextUsage(it) }
                }
            }
            ChatAction.OnToggleWebSearch -> _state.update { it.copy(webSearchEnabled = !it.webSearchEnabled) }
            ChatAction.OnToggleImageGeneration -> _state.update {
                it.copy(imageGenerationEnabled = !it.imageGenerationEnabled)
            }
            ChatAction.OnRetryCatalog -> viewModelScope.launch { loadCatalog() }
            is ChatAction.OnCopyMessage -> viewModelScope.launch {
                _events.send(ChatEvent.CopyText(action.text))
            }
            is ChatAction.OnAddContext -> viewModelScope.launch {
                saveContextSnippet(action.text, action.sourceRole)
            }
            ChatAction.OnClearContext -> viewModelScope.launch { clearContextSnippet() }
            is ChatAction.OnEditMessage -> beginEditMessage(action.messageId)
            is ChatAction.OnRegenerateMessage -> viewModelScope.launch { regenerateMessage(action.messageId) }
            ChatAction.OnPickPhotos -> viewModelScope.launch {
                _events.send(ChatEvent.PickFiles(imagesOnly = true))
            }
            ChatAction.OnPickLocalDocument -> viewModelScope.launch {
                _events.send(ChatEvent.PickFiles(imagesOnly = false))
            }
            is ChatAction.OnFilesPicked -> viewModelScope.launch {
                uploadFiles(action.files, action.imagesOnly)
            }
            is ChatAction.OnFilePickerFailed -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.DynamicString(action.message)))
            }
            is ChatAction.OnApprovalDecision -> viewModelScope.launch {
                decideApproval(action.approvalId, action.approved)
            }
            is ChatAction.OnClarificationResponse -> viewModelScope.launch {
                respondClarification(action.clarificationId, action.answers, action.skipped)
            }
            ChatAction.OnOpenLibrary -> viewModelScope.launch { openLibrary() }
            ChatAction.OnDismissLibrary -> _state.update { it.copy(libraryOpen = false) }
            is ChatAction.OnLibraryQueryChange -> {
                _state.update { it.copy(libraryQuery = action.query) }
                librarySearchJob?.cancel()
                librarySearchJob = viewModelScope.launch {
                    delay(LIBRARY_SEARCH_DEBOUNCE_MS)
                    while (_state.value.libraryLoading || _state.value.libraryLoadingMore) {
                        delay(LIBRARY_LOAD_WAIT_INTERVAL_MS)
                    }
                    if (_state.value.libraryOpen) loadLibrary(reset = true)
                }
            }
            ChatAction.OnRetryLibrary -> viewModelScope.launch { loadLibrary(reset = true) }
            ChatAction.OnLoadMoreLibrary -> viewModelScope.launch { loadLibrary(reset = false) }
            is ChatAction.OnToggleLibraryDocument -> toggleLibraryDocument(action.documentId)
            ChatAction.OnAttachLibraryDocuments -> viewModelScope.launch { attachLibraryDocuments() }
            is ChatAction.OnToggleImageContext -> viewModelScope.launch { toggleImageContext(action.imageId) }
            ChatAction.OnOpenProjects -> viewModelScope.launch {
                _events.send(ChatEvent.OpenProjects)
            }
            ChatAction.OnOpenDocumentsLibrary -> viewModelScope.launch {
                _events.send(ChatEvent.OpenDocuments)
            }
            ChatAction.OnOpenImages -> viewModelScope.launch {
                _events.send(ChatEvent.OpenImages)
            }
            ChatAction.OnOpenSettings -> viewModelScope.launch {
                _events.send(ChatEvent.OpenAccount)
            }
            is ChatAction.OnOpenRecentProject -> viewModelScope.launch {
                _events.send(ChatEvent.OpenProjects)
            }
            is ChatAction.OnRemoveActiveDocument -> viewModelScope.launch {
                unlinkDocument(action.documentId)
            }
        }
    }

    private suspend fun bootstrap() {
        val activeSessionId = when (val active = chatRepository.listActiveRuns()) {
            is Result.Success -> active.data.firstOrNull { it.status == "running" }?.sessionId
            is Result.Error -> null
        }
        refreshSessions()
        // A fresh launch always starts on a New chat draft. Rejoin a live run
        // so a process death mid-stream does not drop the in-flight answer.
        if (activeSessionId != null) {
            selectSession(activeSessionId)
        } else {
            openDraft(savedStateHandle[PROJECT_KEY])
        }
    }

    private suspend fun refreshSessions(): SessionPage? {
        _state.update { it.copy(sessionsLoading = true, sessionsError = null) }
        var page: SessionPage? = null
        chatRepository.refreshSessions(cursor = null)
            .onSuccess { loaded ->
                page = loaded
                _state.update {
                    it.copy(sessionsLoading = false, sessionsNextCursor = loaded.nextCursor)
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(sessionsLoading = false, sessionsError = error.toUiText())
                }
            }
        return page
    }

    private suspend fun loadMoreSessions() {
        val cursor = _state.value.sessionsNextCursor ?: return
        if (_state.value.sessionsLoadingMore) return
        _state.update { it.copy(sessionsLoadingMore = true) }
        chatRepository.refreshSessions(cursor)
            .onSuccess { page ->
                _state.update {
                    it.copy(sessionsLoadingMore = false, sessionsNextCursor = page.nextCursor)
                }
            }
            .onFailure { error ->
                _state.update { it.copy(sessionsLoadingMore = false) }
                _events.send(ChatEvent.ShowMessage(error.toUiText()))
            }
    }

    private fun openRename(sessionId: String) {
        val title = _state.value.sessions.firstOrNull { it.id == sessionId }?.title.orEmpty()
        _state.update {
            it.copy(
                renameSessionId = sessionId,
                renameDraft = title.take(SESSION_TITLE_MAX),
                renameError = null,
                deleteSessionId = null,
                deleteError = null,
            )
        }
    }

    private fun openDelete(sessionId: String) {
        _state.update {
            it.copy(
                deleteSessionId = sessionId,
                deleteError = null,
                renameSessionId = null,
                renameError = null,
            )
        }
    }

    private fun dismissSessionDialog() {
        _state.update {
            it.copy(
                renameSessionId = null,
                renameDraft = "",
                renameError = null,
                deleteSessionId = null,
                deleteError = null,
                sessionBusy = false,
            )
        }
    }

    private suspend fun confirmRename() {
        val sessionId = _state.value.renameSessionId ?: return
        val title = normalizeSessionTitle(_state.value.renameDraft)
        if (title.isEmpty()) {
            _state.update {
                it.copy(renameError = UiText.StringResource(AnrealCopy.ERROR_TITLE_REQUIRED))
            }
            return
        }
        _state.update { it.copy(sessionBusy = true, renameError = null) }
        chatRepository.renameSession(sessionId, title)
            .onSuccess { dismissSessionDialog() }
            .onFailure { error ->
                _state.update {
                    it.copy(sessionBusy = false, renameError = error.toUiText())
                }
            }
    }

    private suspend fun confirmDelete() {
        val sessionId = _state.value.deleteSessionId ?: return
        _state.update { it.copy(sessionBusy = true, deleteError = null) }
        chatRepository.deleteSession(sessionId)
            .onSuccess {
                val selected = _state.value.selectedSessionId
                dismissSessionDialog()
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_CHAT_DELETED)))
                if (selected == sessionId) {
                    openDraft()
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(sessionBusy = false, deleteError = error.toUiText())
                }
            }
    }

    private suspend fun openDraft(projectId: String? = null) {
        chatRepository.openDraft(projectId)
            .onSuccess { session -> selectSession(session.id) }
            .onFailure { error ->
                _state.update { it.copy(sessionsError = error.toUiText()) }
            }
    }

    private suspend fun uploadFiles(files: List<PickedUploadUi>, imagesOnly: Boolean) {
        if (files.isEmpty() || _state.value.isUploading) return
        if (files.any { it.bytes.size > MAX_UPLOAD_BYTES }) {
            _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_FILE_TOO_LARGE)))
            return
        }
        if (_state.value.selectedSessionId == null) openDraft()
        val sessionId = _state.value.selectedSessionId ?: return
        if (!imagesOnly) {
            when (val storage = chatRepository.getDocumentStorage()) {
                is Result.Success -> if (files.sumOf { it.bytes.size.toLong() } > storage.data.remainingBytes) {
                    _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_STORAGE_QUOTA)))
                    return
                }
                is Result.Error -> {
                    _events.send(ChatEvent.ShowMessage(storage.error.toUiText()))
                    return
                }
            }
        }
        _state.update { it.copy(isUploading = true) }
        for (file in files) {
            val upload = ChatUpload(file.filename, file.mediaType, file.bytes)
            if (imagesOnly) {
                when (val result = chatRepository.uploadImage(sessionId, upload)) {
                    is Result.Success -> {
                        chatRepository.pinImage(sessionId, result.data.id)
                        val image = result.data.copy(isPinned = true, bytes = file.bytes).toUi()
                        _state.update { it.copy(sessionImages = listOf(image) + it.sessionImages) }
                    }
                    is Result.Error -> {
                        _state.update { it.copy(isUploading = false) }
                        _events.send(ChatEvent.ShowMessage(result.error.toUiText()))
                        return
                    }
                }
            } else {
                when (val result = chatRepository.uploadDocument(sessionId, upload)) {
                    is Result.Success -> pollDocument(sessionId, result.data)
                    is Result.Error -> {
                        _state.update { it.copy(isUploading = false) }
                        _events.send(ChatEvent.ShowMessage(result.error.toUiText()))
                        return
                    }
                }
            }
        }
        _state.update { it.copy(isUploading = false) }
        loadSessionDocuments(sessionId)
        loadSessionImages(sessionId)
        loadContextSnippet(sessionId)
        _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_UPLOAD_COMPLETE)))
    }

    private suspend fun pollDocument(sessionId: String, initial: DocumentIngest) {
        var current = initial
        updateIngest(current)
        repeat(DOCUMENT_POLL_ATTEMPTS) {
            if (current.status == "ready") return
            if (current.status in DOCUMENT_FAILURE_STATUSES) {
                _events.send(
                    ChatEvent.ShowMessage(
                        current.errorMessage?.let(UiText::DynamicString)
                            ?: UiText.StringResource(AnrealCopy.ERROR_DOCUMENT_INGEST),
                    ),
                )
                return
            }
            delay(DOCUMENT_POLL_INTERVAL_MS)
            when (val result = chatRepository.getDocumentStatus(sessionId, current.id)) {
                is Result.Success -> {
                    current = result.data
                    updateIngest(current)
                }
                is Result.Error -> return
            }
        }
    }

    private fun updateIngest(document: DocumentIngest) {
        val item = document.toUi()
        _state.update { state ->
            state.copy(
                uploadingDocuments = state.uploadingDocuments.filterNot { it.id == item.id } + item,
            )
        }
    }

    private suspend fun selectSession(sessionId: String) {
        savedStateHandle[SESSION_KEY] = sessionId
        _state.update {
            it.copy(
                selectedSessionId = sessionId,
                runActiveConflict = false,
                queueConflict = false,
                thread = ChatThreadState(),
                historyLoading = true,
                historyError = null,
                queue = emptyList(),
                queueHidden = false,
                activeDocuments = emptyList(),
                citedDocuments = emptyList(),
                contextSnippet = null,
                contextSnippetId = null,
                editingMessageId = null,
                sessionImages = emptyList(),
                imagesLoading = false,
                uploadingDocuments = emptyList(),
                contextUsage = null,
            )
        }
        val cachedMessages = chatRepository.loadCachedHistory(sessionId)
        if (cachedMessages.isNotEmpty() && _state.value.selectedSessionId == sessionId) {
            _state.update { current ->
                current.copy(thread = current.thread.copy(messages = cachedMessages))
            }
        }
        val queue = restoreQueue(chatRepository.loadQueue(sessionId))
        if (_state.value.selectedSessionId == sessionId) {
            _state.update { it.copy(queue = queue) }
        }
        coroutineScope {
            launch {
                // The history snapshot must settle before a live run resumes. Running
                // these concurrently lets a late snapshot overwrite streamed parts.
                loadHistory(sessionId)
                maybeResume(sessionId)
            }
            launch { loadSessionDocuments(sessionId) }
            launch { loadSessionImages(sessionId) }
            launch { loadContextSnippet(sessionId) }
            launch { loadContextUsage(sessionId) }
            launch { chatRepository.markRead(sessionId) }
        }
    }

    private suspend fun loadHistory(sessionId: String) {
        if (_state.value.selectedSessionId != sessionId) return
        _state.update { it.copy(historyLoading = true, historyError = null) }
        chatRepository.loadHistory(sessionId)
            .onSuccess { messages ->
                if (_state.value.selectedSessionId != sessionId) return@onSuccess
                _state.update { current ->
                    // A late snapshot must not overwrite tokens from a live run.
                    if (isStreaming(current)) {
                        current.copy(historyLoading = false, historyError = null)
                    } else {
                        current.copy(
                            historyLoading = false,
                            thread = current.thread.copy(
                                messages = messages.ifEmpty { current.thread.messages },
                                status = RunStatus.Idle,
                            ),
                        )
                    }
                }
            }
            .onFailure { error ->
                if (_state.value.selectedSessionId != sessionId) return@onFailure
                _state.update {
                    it.copy(historyLoading = false, historyError = error.toUiText())
                }
            }
    }

    private suspend fun loadContextSnippet(sessionId: String) {
        chatRepository.loadContextSnippet(sessionId)
            .onSuccess { snippet ->
                if (_state.value.selectedSessionId != sessionId) return@onSuccess
                _state.update {
                    it.copy(contextSnippet = snippet?.text, contextSnippetId = snippet?.id)
                }
            }
            .onFailure { error ->
                if (_state.value.selectedSessionId == sessionId) {
                    _events.send(ChatEvent.ShowMessage(error.toUiText()))
                }
            }
    }

    private suspend fun saveContextSnippet(text: String, sourceRole: ChatRole) {
        val sessionId = _state.value.selectedSessionId ?: return
        chatRepository.saveContextSnippet(sessionId, text.take(2_000), sourceRole.name.lowercase())
            .onSuccess { snippet ->
                _state.update {
                    it.copy(contextSnippet = snippet.text, contextSnippetId = snippet.id)
                }
            }
            .onFailure { error -> _events.send(ChatEvent.ShowMessage(error.toUiText())) }
    }

    private suspend fun clearContextSnippet() {
        val sessionId = _state.value.selectedSessionId ?: return
        val snippetId = _state.value.contextSnippetId ?: return
        chatRepository.clearContextSnippet(sessionId, snippetId)
            .onSuccess {
                _state.update { it.copy(contextSnippet = null, contextSnippetId = null) }
            }
            .onFailure { error -> _events.send(ChatEvent.ShowMessage(error.toUiText())) }
    }

    private suspend fun decideApproval(approvalId: String, approved: Boolean) {
        if (_state.value.humanInputBusy) return
        _state.update { it.copy(humanInputBusy = true) }
        chatRepository.decideApproval(approvalId, approved)
            .onSuccess {
                _state.update {
                    it.copy(
                        humanInputBusy = false,
                        thread = it.thread.copy(
                            pendingApprovals = it.thread.pendingApprovals.filterNot { item ->
                                item.id == approvalId
                            },
                        ),
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(humanInputBusy = false) }
                _events.send(ChatEvent.ShowMessage(error.toUiText()))
            }
    }

    private suspend fun respondClarification(
        clarificationId: String,
        answers: Map<String, List<String>>,
        skipped: List<String>,
    ) {
        if (_state.value.humanInputBusy) return
        _state.update { it.copy(humanInputBusy = true) }
        chatRepository.respondClarification(clarificationId, answers, skipped)
            .onSuccess {
                _state.update {
                    it.copy(
                        humanInputBusy = false,
                        thread = it.thread.copy(
                            pendingClarifications = it.thread.pendingClarifications.filterNot { item ->
                                item.id == clarificationId
                            },
                        ),
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(humanInputBusy = false) }
                _events.send(ChatEvent.ShowMessage(error.toUiText()))
            }
    }

    private fun isStreaming(state: ChatState): Boolean {
        return state.isSending || state.thread.status == RunStatus.Streaming
    }

    private suspend fun submitComposer() {
        val current = _state.value
        val editingMessageId = current.editingMessageId
        if (editingMessageId != null) {
            val text = current.draft.trim()
            if (text.isNotEmpty()) resubmitFromMessage(editingMessageId, text)
            return
        }
        val editingId = current.queue.firstOrNull { it.status == QueueStatus.Editing }?.id
        if (editingId != null) {
            val text = current.draft.trim()
            if (text.isEmpty()) return
            updateQueue(finishEdit(current.queue, editingId, text))
            setDraft("")
            return
        }
        val text = current.draft.trim()
        if (text.isEmpty()) return
        when {
            isStreaming(current) -> enqueue(text)
            current.queue.isNotEmpty() -> _state.update { it.copy(queueConflict = true) }
            else -> sendText(text, newClientMessageId())
        }
    }

    private suspend fun enqueue(text: String) {
        val item = QueuedItem(id = newClientMessageId(), text = text)
        updateQueue(addItem(_state.value.queue, item))
        setDraft("")
    }

    private suspend fun sendText(text: String, clientMessageId: String) {
        val sessionId = _state.value.selectedSessionId ?: return
        val userMessage = ChatMessage(
            id = clientMessageId,
            role = ChatRole.User,
            parts = listOf(ChatPart.Text(id = "$clientMessageId-text", text = text)),
            isComplete = true,
            clientMessageId = clientMessageId,
        )
        setDraft("")
        _state.update {
            it.copy(
                isSending = true,
                thread = it.thread.copy(
                    messages = it.thread.messages + userMessage,
                    status = RunStatus.Streaming,
                ),
            )
        }
        chatRepository.cacheHistory(sessionId, _state.value.thread.messages)
        val result = chatRepository.sendMessage(
            sessionId = sessionId,
            text = text,
            clientMessageId = clientMessageId,
            options = currentRunOptions(),
        ) { line ->
            applyLine(sessionId, line)
        }
        if (_state.value.selectedSessionId != sessionId) return
        _state.update { current ->
            val status = if (result is Result.Success && current.thread.status == RunStatus.Streaming) {
                RunStatus.Completed
            } else {
                current.thread.status
            }
            current.copy(isSending = false, thread = current.thread.copy(status = status))
        }
        result.onFailure { error -> handleSendError(error) }
        if (result is Result.Success) {
            // Persist the reducer-owned thread first so a failed or stale
            // history snapshot cannot fall back to the pre-send cache.
            chatRepository.cacheHistory(sessionId, _state.value.thread.messages)
            loadHistory(sessionId)
            loadContextUsage(sessionId)
            loadSessionImages(sessionId)
            maybeAutoFlush()
        }
    }

    private suspend fun sendNow() {
        hold = false
        val sessionId = _state.value.selectedSessionId ?: return
        val flushable = mutableListOf<QueuedItem>()
        var remaining = _state.value.queue
        while (true) {
            val next = nextFlushable(remaining) ?: break
            flushable += next
            remaining = markInflight(remaining, listOf(next.id))
        }
        if (flushable.isEmpty()) return
        updateQueue(markInflight(_state.value.queue, flushable.map { it.id }))
        chatRepository.steer(sessionId, flushable)
            .onFailure { error ->
                updateQueue(revertInflight(_state.value.queue))
                if (error is ChatError.NoActiveRun) {
                    val first = flushable.first()
                    updateQueue(removeItem(_state.value.queue, first.id))
                    sendText(first.text, first.id)
                } else {
                    _events.send(ChatEvent.ShowMessage(error.toUiText()))
                }
            }
    }

    private suspend fun sendQueueFromConflict() {
        val draft = _state.value.draft.trim()
        _state.update { it.copy(queueConflict = false) }
        if (draft.isNotEmpty()) enqueue(draft)
        hold = false
        sendNow()
        if (!isStreaming(_state.value)) {
            maybeAutoFlush()
        }
    }

    private suspend fun sendNewFromConflict() {
        val text = _state.value.draft.trim()
        _state.update { it.copy(queueConflict = false) }
        if (text.isEmpty()) return
        sendText(text, newClientMessageId())
    }

    private suspend fun dropQueued(id: String) {
        updateQueue(removeItem(_state.value.queue, id))
    }

    private fun recallQueued(id: String) {
        val item = _state.value.queue.firstOrNull { it.id == id } ?: return
        updateQueue(startEdit(_state.value.queue, id))
        setDraft(item.text)
    }

    private fun cancelQueueEdit() {
        val editingId = _state.value.queue.firstOrNull { it.status == QueueStatus.Editing }?.id ?: return
        updateQueue(cancelEdit(_state.value.queue, editingId))
        setDraft("")
    }

    private fun setDraft(value: String) {
        savedStateHandle[DRAFT_KEY] = value
        _state.update { it.copy(draft = value) }
    }

    private fun updateQueue(items: List<QueuedItem>) {
        _state.update {
            it.copy(
                queue = items,
                queueHidden = if (items.isEmpty()) false else it.queueHidden,
            )
        }
        val sessionId = _state.value.selectedSessionId ?: return
        viewModelScope.launch { chatRepository.replaceQueue(sessionId, items) }
    }

    private suspend fun maybeAutoFlush() {
        if (hold) return
        if (_state.value.thread.status == RunStatus.Failed) return
        if (isStreaming(_state.value)) return
        val sessionId = _state.value.selectedSessionId ?: return
        val ids = _state.value.queue.map { it.id }
        if (ids.isNotEmpty()) {
            chatRepository.syncQueue(sessionId, ids)
                .onSuccess { applied ->
                    var next = _state.value.queue
                    applied.forEach { id -> next = applyAck(next, id) }
                    updateQueue(next)
                }
        }
        val next = nextFlushable(_state.value.queue) ?: return
        updateQueue(removeItem(_state.value.queue, next.id))
        sendText(next.text, next.id)
    }

    private fun ackQueued(event: ChatStreamEvent.QueuedMessageApplied) {
        val item = _state.value.queue.firstOrNull { it.id == event.clientMessageId }
        updateQueue(applyAck(_state.value.queue, event.clientMessageId))
        val exists = _state.value.thread.messages.any { it.id == event.clientMessageId }
        if (!exists) {
            val text = item?.text ?: event.text
            if (text.isNotBlank()) {
                _state.update {
                    it.copy(
                        thread = it.thread.copy(
                            messages = it.thread.messages + ChatMessage(
                                id = event.clientMessageId,
                                role = ChatRole.User,
                                parts = listOf(ChatPart.Text(id = "${event.clientMessageId}-text", text = text)),
                                isComplete = true,
                                clientMessageId = event.clientMessageId,
                            ),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun stop() {
        hold = true
        val streamId = _state.value.thread.streamId ?: return
        chatRepository.stop(streamId)
        _state.update { it.copy(isSending = false, thread = it.thread.copy(status = RunStatus.Idle)) }
    }

    private suspend fun maybeResume(sessionId: String) {
        chatRepository.runStatus(sessionId)
            .onSuccess { snapshot ->
                if (_state.value.selectedSessionId != sessionId) return@onSuccess
                val streamId = snapshot.streamId
                if (snapshot.status == "running" && streamId != null) {
                    resume(sessionId, streamId, snapshot.lastEventId ?: 0)
                }
            }
    }

    private suspend fun resumeActiveRun() {
        val sessionId = _state.value.selectedSessionId ?: return
        maybeResume(sessionId)
    }

    private suspend fun resume(sessionId: String, streamId: String, after: Int) {
        if (_state.value.selectedSessionId != sessionId) return
        _state.update { it.copy(isSending = true) }
        val result = chatRepository.resume(sessionId, streamId, after) { line ->
            applyLine(sessionId, line)
        }
        if (_state.value.selectedSessionId != sessionId) return
        result.onFailure { error -> handleSendError(error) }
        if (result is Result.Success) loadHistory(sessionId)
        if (_state.value.selectedSessionId == sessionId) {
            _state.update { it.copy(isSending = false) }
        }
    }

    private suspend fun applyLine(sessionId: String, line: String) {
        if (_state.value.selectedSessionId != sessionId) return
        val envelope = parseStreamLine(line) ?: return
        if (_state.value.selectedSessionId != sessionId) return
        if (envelope is StreamEnvelope.Event) {
            val event = envelope.event
            if (event is ChatStreamEvent.QueuedMessageApplied) {
                ackQueued(event)
            }
        }
        _state.update { current ->
            if (current.selectedSessionId == sessionId) {
                current.copy(thread = current.thread.reduce(envelope))
            } else {
                current
            }
        }
        // Buffered transports may deliver every JSONL record in one burst.
        // Yield so Compose can paint each reduced frame instead of jumping
        // straight to the terminal answer.
        yield()
        if (_state.value.selectedSessionId != sessionId) return
        val thread = _state.value.thread
        chatRepository.saveResume(sessionId, thread.streamId, thread.lastEventId)
        if (envelope is StreamEnvelope.End) {
            chatRepository.cacheHistory(sessionId, thread.messages)
            loadSessionImages(sessionId)
            loadContextUsage(sessionId)
        }
    }

    private suspend fun loadCatalog() {
        _state.update { it.copy(catalogLoading = true, catalogError = null) }
        chatRepository.loadCatalog()
            .onSuccess { catalog ->
                val preferences = preferencesRepository.preferences.first()
                val requestedModel = _state.value.selectedModelId ?: preferences.chatModelId
                val selectedModel = catalog.models.firstOrNull { it.id == requestedModel }
                    ?: catalog.models.firstOrNull()
                val selected = selectedModel?.id
                val requestedReasoning = _state.value.selectedReasoning
                    ?: preferences.chatReasoningEffort
                val reasoning = requestedReasoning?.takeIf {
                    it in selectedModel?.reasoningEfforts.orEmpty()
                }
                if (preferences.chatModelId != null && preferences.chatModelId != selected) {
                    preferencesRepository.setChatModel(null)
                }
                if (preferences.chatReasoningEffort != null && reasoning == null) {
                    preferencesRepository.setChatReasoningEffort(null)
                }
                _state.update {
                    it.copy(
                        models = catalog.models,
                        reasoningEfforts = catalog.efforts,
                        selectedModelId = selected,
                        selectedReasoning = reasoning,
                        catalogLoading = false,
                        catalogError = null,
                    )
                }
                _state.value.selectedSessionId?.let { loadContextUsage(it) }
            }
            .onFailure { error ->
                _state.update { it.copy(catalogLoading = false, catalogError = error.toUiText()) }
            }
        chatRepository.loadCapabilities()
            .onSuccess { capabilities ->
                _state.update { it.copy(capabilities = capabilities) }
            }
    }

    private fun selectModel(modelId: String) {
        val model = _state.value.models.firstOrNull { it.id == modelId } ?: return
        val allowed = model.reasoningEfforts
        val previousReasoning = _state.value.selectedReasoning
        val reasoning = previousReasoning?.takeIf { it in allowed }
        _state.update {
            it.copy(selectedModelId = modelId, selectedReasoning = reasoning)
        }
        viewModelScope.launch {
            preferencesRepository.setChatModel(modelId)
            if (reasoning != previousReasoning) {
                preferencesRepository.setChatReasoningEffort(reasoning)
            }
        }
        _state.value.selectedSessionId?.let { sessionId ->
            viewModelScope.launch { loadContextUsage(sessionId) }
        }
    }

    private fun beginEditMessage(messageId: String) {
        val message = _state.value.thread.messages.firstOrNull { it.id == messageId } ?: return
        val text = message.parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
        setDraft(text)
        _state.update { it.copy(editingMessageId = messageId) }
    }

    private suspend fun regenerateMessage(messageId: String) {
        if (isStreaming(_state.value)) return
        val messages = _state.value.thread.messages
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        if (targetIndex < 0) return
        val target = messages[targetIndex]
        val user = if (target.role == ChatRole.User) {
            target
        } else {
            messages.take(targetIndex).lastOrNull { it.role == ChatRole.User }
        } ?: return
        val text = user.parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
        resubmitFromMessage(user.id, text)
    }

    private suspend fun resubmitFromMessage(messageId: String, text: String) {
        if (isStreaming(_state.value)) return
        val sessionId = _state.value.selectedSessionId ?: return
        val messages = _state.value.thread.messages
        val targetIndex = messages.indexOfFirst { it.id == messageId }
        val target = messages.getOrNull(targetIndex) ?: return
        val targetIdentityAvailable = target.clientMessageId != null || target.memoryPosition != null ||
            target.id.startsWith("local-") || !target.id.startsWith("history-")
        if (!targetIdentityAvailable) {
            _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_REGENERATE_UNAVAILABLE)))
            return
        }
        when (val count = chatRepository.getSessionMessageCount(sessionId)) {
            is Result.Success -> {
                val localCount = messages.count { it.role == ChatRole.User || it.role == ChatRole.Assistant }
                if (count.data != localCount) {
                    loadHistory(sessionId)
                    _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_SESSION_STALE)))
                    return
                }
            }
            is Result.Error -> {
                _events.send(ChatEvent.ShowMessage(count.error.toUiText()))
                return
            }
        }
        val clientId = target.clientMessageId ?: target.id.takeUnless { it.startsWith("history-") }
        chatRepository.truncateSession(
            sessionId = sessionId,
            mode = "exclude",
            clientMessageId = clientId,
            memoryPosition = target.memoryPosition,
        ).onSuccess {
            _state.update {
                it.copy(
                    editingMessageId = null,
                    thread = it.thread.copy(messages = messages.take(targetIndex)),
                )
            }
            sendText(text, newClientMessageId())
        }.onFailure { error -> _events.send(ChatEvent.ShowMessage(error.toUiText())) }
    }

    private fun currentRunOptions(): ChatRunOptions {
        val current = _state.value
        return ChatRunOptions(
            model = current.selectedModelId,
            reasoningEffort = current.selectedReasoning,
            webSearchEnabled = current.webSearchEnabled,
            imageGenerationEnabled = current.imageGenerationEnabled,
        )
    }

    private fun handleSendError(error: ChatError) {
        hold = true
        if (error is ChatError.RunActive) {
            _state.update { it.copy(runActiveConflict = true, isSending = false) }
        } else {
            _state.update {
                it.copy(historyError = error.toUiText(), isSending = false)
            }
        }
    }

    private suspend fun loadRecentProjects() {
        chatRepository.listRecentProjects()
            .onSuccess { projects ->
                _state.update { it.copy(recentProjects = projects.map { project -> project.toUi() }) }
            }
    }

    private suspend fun loadSessionDocuments(sessionId: String) {
        chatRepository.listSessionDocuments(sessionId)
            .onSuccess { documents ->
                if (_state.value.selectedSessionId != sessionId) return@onSuccess
                _state.update { it.copy(activeDocuments = documents.map { document -> document.toUi() }) }
            }
            .onFailure {
                if (_state.value.selectedSessionId == sessionId) {
                    _state.update { it.copy(activeDocuments = emptyList()) }
                }
            }
    }

    private suspend fun openLibrary() {
        _state.update { it.copy(libraryOpen = true, libraryQuery = "") }
        loadLibrary(reset = true)
    }

    private suspend fun loadLibrary(reset: Boolean) {
        if (_state.value.libraryLoading || _state.value.libraryLoadingMore) return
        val cursor = if (reset) null else _state.value.libraryNextCursor ?: return
        _state.update {
            it.copy(
                libraryLoading = reset,
                libraryLoadingMore = !reset,
                libraryError = null,
            )
        }
        chatRepository.listLibraryDocuments(
            query = _state.value.libraryQuery.trim().ifBlank { null },
            cursor = cursor,
            projectId = _state.value.sessions
                .firstOrNull { it.id == _state.value.selectedSessionId }
                ?.projectId,
        ).onSuccess { page ->
            val selectedIds = _state.value.libraryDocuments.filter { it.selected }.mapTo(mutableSetOf()) { it.id }
            val mapped = page.items.map { document -> document.toUi(document.id in selectedIds) }
            _state.update {
                it.copy(
                    libraryDocuments = if (reset) mapped else (it.libraryDocuments + mapped).distinctBy { item -> item.id },
                    libraryNextCursor = page.nextCursor,
                    libraryLoading = false,
                    libraryLoadingMore = false,
                )
            }
        }.onFailure { error ->
            _state.update {
                it.copy(
                    libraryLoading = false,
                    libraryLoadingMore = false,
                    libraryError = error.toUiText(),
                )
            }
        }
    }

    private fun toggleLibraryDocument(documentId: String) {
        _state.update { state ->
            state.copy(
                libraryDocuments = state.libraryDocuments.map { document ->
                    if (document.id == documentId) document.copy(selected = !document.selected) else document
                },
            )
        }
    }

    private suspend fun attachLibraryDocuments() {
        val sessionId = _state.value.selectedSessionId ?: return
        val ids = _state.value.libraryDocuments.filter { it.selected }.map { it.id }
        if (ids.isEmpty()) return
        _state.update { it.copy(libraryLoading = true, libraryError = null) }
        chatRepository.linkDocuments(sessionId, ids)
            .onSuccess { linked ->
                _state.update {
                    it.copy(
                        activeDocuments = linked.map { document -> document.toUi() },
                        libraryOpen = false,
                        libraryLoading = false,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(libraryLoading = false, libraryError = error.toUiText()) }
            }
    }

    private suspend fun loadSessionImages(sessionId: String) {
        if (_state.value.selectedSessionId != sessionId) return
        _state.update { it.copy(imagesLoading = true) }
        val images = when (val result = chatRepository.listSessionImages(sessionId)) {
            is Result.Success -> result.data
            is Result.Error -> {
                if (_state.value.selectedSessionId == sessionId) {
                    _state.update { it.copy(imagesLoading = false) }
                }
                return
            }
        }
        if (_state.value.selectedSessionId != sessionId) return
        val pinnedIds = when (val result = chatRepository.listPinnedImages(sessionId)) {
            is Result.Success -> result.data.mapTo(mutableSetOf()) { it.id }
            is Result.Error -> emptySet()
        }
        if (_state.value.selectedSessionId != sessionId) return
        val previousBytes = _state.value.sessionImages.associate { it.id to it.bytes }
        _state.update {
            it.copy(
                imagesLoading = false,
                sessionImages = images.map { image ->
                    image.copy(isPinned = image.id in pinnedIds, bytes = previousBytes[image.id]).toUi()
                },
            )
        }
        images.filter { previousBytes[it.id] == null }.forEach { image ->
            chatRepository.loadImageBytes(image.id).onSuccess { bytes ->
                if (_state.value.selectedSessionId != sessionId) return@onSuccess
                _state.update { state ->
                    state.copy(
                        sessionImages = state.sessionImages.map { item ->
                            if (item.id == image.id) item.copy(bytes = bytes) else item
                        },
                    )
                }
            }
        }
    }

    private suspend fun toggleImageContext(imageId: String) {
        val sessionId = _state.value.selectedSessionId ?: return
        val image = _state.value.sessionImages.firstOrNull { it.id == imageId } ?: return
        val result = if (image.pinned) {
            chatRepository.unpinImage(sessionId, imageId)
        } else {
            chatRepository.pinImage(sessionId, imageId)
        }
        result.onSuccess {
            _state.update { state ->
                state.copy(
                    sessionImages = state.sessionImages.map { item ->
                        if (item.id == imageId) item.copy(pinned = !item.pinned) else item
                    },
                )
            }
        }.onFailure { error -> _events.send(ChatEvent.ShowMessage(error.toUiText())) }
    }

    private suspend fun loadContextUsage(sessionId: String) {
        if (_state.value.selectedSessionId != sessionId) return
        chatRepository.getContextUsage(
            sessionId,
            _state.value.selectedModelId,
            _state.value.selectedReasoning,
        ).onSuccess { usage ->
            if (_state.value.selectedSessionId != sessionId) return@onSuccess
            _state.update { it.copy(contextUsage = usage.toUi(), contextUsageError = false) }
        }.onFailure {
            if (_state.value.selectedSessionId == sessionId) {
                _state.update { it.copy(contextUsageError = true) }
            }
        }
    }

    private suspend fun unlinkDocument(documentId: String) {
        val sessionId = _state.value.selectedSessionId ?: return
        chatRepository.unlinkSessionDocument(sessionId, documentId)
            .onSuccess {
                _state.update { current ->
                    current.copy(activeDocuments = current.activeDocuments.filterNot { it.id == documentId })
                }
            }
            .onFailure { error ->
                _events.send(ChatEvent.ShowMessage(error.toUiText()))
            }
    }

    private companion object {
        const val SESSION_KEY = "sessionId"
        const val PROJECT_KEY = "projectId"
        const val DRAFT_KEY = "draft"
        const val SESSION_TITLE_MAX = 48
        const val DOCUMENT_POLL_ATTEMPTS = 20
        const val DOCUMENT_POLL_INTERVAL_MS = 1_000L
        const val LIBRARY_SEARCH_DEBOUNCE_MS = 300L
        const val LIBRARY_LOAD_WAIT_INTERVAL_MS = 25L
        val DOCUMENT_FAILURE_STATUSES = setOf("error", "failed")
    }
}

internal fun normalizeSessionTitle(raw: String): String {
    return raw.trim().replace(WHITESPACE, " ").take(48)
}

internal fun newClientMessageId(): String {
    val alphabet = "abcdefghijklmnopqrstuvwxyz0123456789"
    return buildString(20) { repeat(20) { append(alphabet.random()) } }
}

private val WHITESPACE = Regex("\\s+")

private fun ChatSession.toUi(): ChatSessionUi = ChatSessionUi(
    id = id,
    title = title.ifBlank { AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT) },
    unread = unread,
    updatedAt = updatedAt,
    projectId = projectId,
)

private fun RecentProject.toUi(): RecentProjectUi = RecentProjectUi(id = id, name = name)

private fun SessionDocument.toUi(): SessionDocumentUi = SessionDocumentUi(
    id = id,
    filename = filename,
    summary = summary,
)

private fun LibraryDocument.toUi(selected: Boolean): LibraryDocumentUi = LibraryDocumentUi(
    id = id,
    filename = filename,
    summary = summary,
    detail = "$pageCount pages · ${sizeBytes.toFileSize()}",
    selected = selected,
)

private fun SessionImage.toUi(): SessionImageUi = SessionImageUi(
    id = id,
    prompt = prompt,
    detail = listOf(modelId, if (width > 0 && height > 0) "${width}×$height" else "")
        .filter(String::isNotBlank)
        .joinToString(" · "),
    bytes = bytes,
    pinned = isPinned,
)

private fun DocumentIngest.toUi(): DocumentIngestUi = DocumentIngestUi(
    id = id,
    filename = filename,
    status = status,
    error = errorMessage,
)

private fun ContextUsage.toUi(): ContextUsageUi = ContextUsageUi(
    modelLabel = modelLabel,
    estimatedTokens = estimatedTokens,
    contextWindowTokens = contextWindowTokens,
    ratio = ratio.toFloat().coerceIn(0f, 1f),
    thresholdRatio = thresholdRatio.toFloat().coerceIn(0f, 1f),
    targetRatio = targetRatio.toFloat().coerceIn(0f, 1f),
    reasoningEffort = reasoningEffort,
    nearThreshold = ratio >= thresholdRatio,
)

private fun Long.toFileSize(): String = when {
    this >= 1_048_576 -> "${(this / 104_857.6).toLong() / 10.0} MB"
    else -> "${this / 1_024} KB"
}
