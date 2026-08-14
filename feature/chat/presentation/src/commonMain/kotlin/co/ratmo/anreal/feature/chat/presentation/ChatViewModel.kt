package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.model.ChatSession
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
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.SessionDocument
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ChatSessionUi(
    val id: String,
    val title: String,
    val unread: Boolean,
    val updatedAt: String = "",
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
    val selectedSessionId: String? = null,
    val thread: ChatThreadState = ChatThreadState(),
    val historyLoading: Boolean = false,
    val historyError: UiText? = null,
    val draft: String = "",
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
    val catalogError: UiText? = null,
    val recentProjects: List<RecentProjectUi> = emptyList(),
    val activeDocuments: List<SessionDocumentUi> = emptyList(),
    val citedDocuments: List<CitedDocumentUi> = emptyList(),
)

sealed interface ChatAction {
    data object OnRefreshSessions : ChatAction
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
    data class OnAddContext(val text: String) : ChatAction
    data object OnClearContext : ChatAction
    data class OnEditMessage(val messageId: String) : ChatAction
    data class OnRegenerateMessage(val messageId: String) : ChatAction
    data object OnPickPhotos : ChatAction
    data object OnPickLocalDocument : ChatAction
    data object OnOpenLibrary : ChatAction
    data object OnOpenProjects : ChatAction
    data object OnOpenDocumentsLibrary : ChatAction
    data object OnOpenImages : ChatAction
    data object OnOpenSettings : ChatAction
    data object OnSignOut : ChatAction
    data class OnOpenRecentProject(val projectId: String) : ChatAction
    data class OnRemoveActiveDocument(val documentId: String) : ChatAction
}

sealed interface ChatEvent {
    data class ShowMessage(val message: UiText) : ChatEvent
    data class CopyText(val text: String) : ChatEvent
    data class PickFiles(val imagesOnly: Boolean) : ChatEvent
    data object SignOut : ChatEvent
}

class ChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
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
            is ChatAction.OnSelectReasoning -> _state.update { it.copy(selectedReasoning = action.effort) }
            ChatAction.OnToggleWebSearch -> _state.update { it.copy(webSearchEnabled = !it.webSearchEnabled) }
            ChatAction.OnToggleImageGeneration -> _state.update {
                it.copy(imageGenerationEnabled = !it.imageGenerationEnabled)
            }
            ChatAction.OnRetryCatalog -> viewModelScope.launch { loadCatalog() }
            is ChatAction.OnCopyMessage -> viewModelScope.launch {
                _events.send(ChatEvent.CopyText(action.text))
            }
            is ChatAction.OnAddContext -> _state.update { it.copy(contextSnippet = action.text) }
            ChatAction.OnClearContext -> _state.update { it.copy(contextSnippet = null) }
            is ChatAction.OnEditMessage -> editMessage(action.messageId)
            is ChatAction.OnRegenerateMessage -> viewModelScope.launch {
                _events.send(
                    ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_REGENERATE_SOON)),
                )
            }
            ChatAction.OnPickPhotos -> viewModelScope.launch {
                _events.send(ChatEvent.PickFiles(imagesOnly = true))
            }
            ChatAction.OnPickLocalDocument -> viewModelScope.launch {
                _events.send(ChatEvent.PickFiles(imagesOnly = false))
            }
            ChatAction.OnOpenLibrary -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ATTACH_LIBRARY_EMPTY)))
            }
            ChatAction.OnOpenProjects -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_PROJECTS_SOON)))
            }
            ChatAction.OnOpenDocumentsLibrary -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_DOCUMENTS_SOON)))
            }
            ChatAction.OnOpenImages -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_IMAGES_SOON)))
            }
            ChatAction.OnOpenSettings -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_SETTINGS_SOON)))
            }
            ChatAction.OnSignOut -> viewModelScope.launch { _events.send(ChatEvent.SignOut) }
            is ChatAction.OnOpenRecentProject -> viewModelScope.launch {
                _events.send(ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.TOAST_PROJECTS_SOON)))
            }
            is ChatAction.OnRemoveActiveDocument -> viewModelScope.launch {
                unlinkDocument(action.documentId)
            }
        }
    }

    private suspend fun bootstrap() {
        val page = refreshSessions()
        val selected = _state.value.selectedSessionId
            ?: page?.items?.firstOrNull()?.id
            ?: _state.value.sessions.firstOrNull()?.id
        if (selected != null) {
            selectSession(selected)
        } else {
            openDraft()
        }
    }

    private suspend fun refreshSessions(): SessionPage? {
        _state.update { it.copy(sessionsLoading = true, sessionsError = null) }
        var page: SessionPage? = null
        chatRepository.refreshSessions()
            .onSuccess { loaded ->
                page = loaded
                _state.update { it.copy(sessionsLoading = false) }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(sessionsLoading = false, sessionsError = error.toUiText())
                }
            }
        return page
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

    private suspend fun openDraft() {
        chatRepository.openDraft()
            .onSuccess { session -> selectSession(session.id) }
            .onFailure { error ->
                _state.update { it.copy(sessionsError = error.toUiText()) }
            }
    }

    private suspend fun selectSession(sessionId: String) {
        savedStateHandle[SESSION_KEY] = sessionId
        val queue = restoreQueue(chatRepository.loadQueue(sessionId))
        _state.update {
            it.copy(
                selectedSessionId = sessionId,
                runActiveConflict = false,
                queueConflict = false,
                thread = ChatThreadState(),
                queue = queue,
                queueHidden = false,
                activeDocuments = emptyList(),
                citedDocuments = emptyList(),
            )
        }
        loadHistory(sessionId)
        loadSessionDocuments(sessionId)
        chatRepository.markRead(sessionId)
        maybeResume(sessionId)
    }

    private suspend fun loadHistory(sessionId: String) {
        _state.update { it.copy(historyLoading = true, historyError = null) }
        chatRepository.loadHistory(sessionId)
            .onSuccess { messages ->
                _state.update {
                    it.copy(
                        historyLoading = false,
                        thread = it.thread.copy(messages = messages, status = RunStatus.Idle),
                    )
                }
            }
            .onFailure { error ->
                _state.update {
                    it.copy(historyLoading = false, historyError = error.toUiText())
                }
            }
    }

    private fun isStreaming(state: ChatState): Boolean {
        return state.isSending || state.thread.status == RunStatus.Streaming
    }

    private suspend fun submitComposer() {
        val current = _state.value
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
        val result = chatRepository.sendMessage(
            sessionId = sessionId,
            text = text,
            clientMessageId = clientMessageId,
            options = currentRunOptions(),
        ) { line ->
            applyLine(sessionId, line)
        }
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
        _state.update { it.copy(isSending = true) }
        chatRepository.resume(sessionId, streamId, after) { line ->
            applyLine(sessionId, line)
        }.onFailure { error -> handleSendError(error) }
        _state.update { it.copy(isSending = false) }
    }

    private suspend fun applyLine(sessionId: String, line: String) {
        val envelope = parseStreamLine(line) ?: return
        if (envelope is StreamEnvelope.Event) {
            val event = envelope.event
            if (event is ChatStreamEvent.QueuedMessageApplied) {
                ackQueued(event)
            }
        }
        _state.update { it.copy(thread = it.thread.reduce(envelope)) }
        val thread = _state.value.thread
        chatRepository.saveResume(sessionId, thread.streamId, thread.lastEventId)
    }

    private suspend fun loadCatalog() {
        chatRepository.loadCatalog()
            .onSuccess { catalog ->
                val selected = _state.value.selectedModelId
                    ?: catalog.models.firstOrNull()?.id
                val allowed = catalog.models.firstOrNull { it.id == selected }?.reasoningEfforts.orEmpty()
                val reasoning = _state.value.selectedReasoning?.takeIf { it in allowed }
                _state.update {
                    it.copy(
                        models = catalog.models,
                        reasoningEfforts = catalog.efforts,
                        selectedModelId = selected,
                        selectedReasoning = reasoning,
                        catalogError = null,
                    )
                }
            }
            .onFailure { error ->
                _state.update { it.copy(catalogError = error.toUiText()) }
            }
        chatRepository.loadCapabilities()
            .onSuccess { capabilities ->
                _state.update { it.copy(capabilities = capabilities) }
            }
    }

    private fun selectModel(modelId: String) {
        val model = _state.value.models.firstOrNull { it.id == modelId } ?: return
        val allowed = model.reasoningEfforts
        val reasoning = _state.value.selectedReasoning?.takeIf { it in allowed }
        _state.update {
            it.copy(selectedModelId = modelId, selectedReasoning = reasoning)
        }
    }

    private fun editMessage(messageId: String) {
        val message = _state.value.thread.messages.firstOrNull { it.id == messageId } ?: return
        val text = message.parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
        setDraft(text)
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
                _state.update { it.copy(activeDocuments = documents.map { document -> document.toUi() }) }
            }
            .onFailure {
                _state.update { it.copy(activeDocuments = emptyList()) }
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
        const val DRAFT_KEY = "draft"
        const val SESSION_TITLE_MAX = 48
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
)

private fun RecentProject.toUi(): RecentProjectUi = RecentProjectUi(id = id, name = name)

private fun SessionDocument.toUi(): SessionDocumentUi = SessionDocumentUi(
    id = id,
    filename = filename,
    summary = summary,
)
