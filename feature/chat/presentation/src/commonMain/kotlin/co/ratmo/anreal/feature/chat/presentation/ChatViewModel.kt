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
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
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
    data object OnResumeConflict : ChatAction
    data object OnDismissConflict : ChatAction
    data object OnRetryHistory : ChatAction
}

sealed interface ChatEvent {
    data class ShowMessage(val message: UiText) : ChatEvent
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

    init {
        viewModelScope.launch {
            chatRepository.observeSessions().collect { sessions ->
                _state.update { it.copy(sessions = sessions.map { session -> session.toUi() }) }
            }
        }
        viewModelScope.launch { bootstrap() }
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
            ChatAction.OnSend -> viewModelScope.launch { send() }
            ChatAction.OnStop -> viewModelScope.launch { stop() }
            ChatAction.OnResumeConflict -> viewModelScope.launch {
                _state.update { it.copy(runActiveConflict = false) }
                resumeActiveRun()
            }
            ChatAction.OnDismissConflict -> _state.update { it.copy(runActiveConflict = false) }
            ChatAction.OnRetryHistory -> viewModelScope.launch {
                _state.value.selectedSessionId?.let { loadHistory(it) }
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
        _state.update {
            it.copy(
                selectedSessionId = sessionId,
                runActiveConflict = false,
                thread = ChatThreadState(),
            )
        }
        loadHistory(sessionId)
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

    private suspend fun send() {
        val current = _state.value
        val sessionId = current.selectedSessionId ?: return
        val text = current.draft.trim()
        if (text.isEmpty() || current.isSending) return

        val userMessage = ChatMessage(
            id = "user-${current.thread.messages.size}",
            role = ChatRole.User,
            parts = listOf(ChatPart.Text(id = "user-text-${current.thread.messages.size}", text = text)),
            isComplete = true,
        )
        savedStateHandle[DRAFT_KEY] = ""
        _state.update {
            it.copy(
                draft = "",
                isSending = true,
                thread = it.thread.copy(
                    messages = it.thread.messages + userMessage,
                    status = RunStatus.Streaming,
                ),
            )
        }

        val result = chatRepository.sendMessage(sessionId, text) { line ->
            applyLine(sessionId, line)
        }
        _state.update { it.copy(isSending = false) }
        result.onFailure { error -> handleSendError(error) }
    }

    private suspend fun stop() {
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
        _state.update { it.copy(thread = it.thread.reduce(envelope)) }
        val thread = _state.value.thread
        chatRepository.saveResume(sessionId, thread.streamId, thread.lastEventId)
    }

    private fun handleSendError(error: ChatError) {
        if (error is ChatError.RunActive) {
            _state.update { it.copy(runActiveConflict = true, isSending = false) }
        } else {
            _state.update {
                it.copy(historyError = error.toUiText(), isSending = false)
            }
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

private val WHITESPACE = Regex("\\s+")

private fun ChatSession.toUi(): ChatSessionUi = ChatSessionUi(
    id = id,
    title = title.ifBlank { AnrealCopy.get(AnrealCopy.ACTION_NEW_CHAT) },
    unread = unread,
)
