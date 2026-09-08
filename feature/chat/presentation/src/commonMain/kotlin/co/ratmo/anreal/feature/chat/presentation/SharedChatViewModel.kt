package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.ForkSeed
import co.ratmo.anreal.feature.chat.domain.PublicShareSnapshot
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

@Stable
data class SharedChatState(
    val token: String = "",
    val isLoading: Boolean = true,
    val error: UiText? = null,
    val snapshot: PublicShareSnapshot? = null,
    val draft: String = "",
    val isForking: Boolean = false,
    val forkError: UiText? = null,
    val isAuthenticated: Boolean = false,
) {
    val messages: List<ChatMessage> get() = snapshot?.messages.orEmpty()
}

sealed interface SharedChatAction {
    data class OnDraftChange(val draft: String) : SharedChatAction
    data object OnSubmit : SharedChatAction
    data object OnRetry : SharedChatAction
    data object OnLoginClick : SharedChatAction
    data object OnRegisterClick : SharedChatAction
    data object OnSignInClick : SharedChatAction
}

sealed interface SharedChatEvent {
    data class NavigateToFork(val sessionId: String, val firstMessage: String) : SharedChatEvent
    data class NavigateToLogin(val token: String) : SharedChatEvent
    data class NavigateToRegister(val token: String) : SharedChatEvent
    data class NavigateToSignIn(val token: String) : SharedChatEvent
    data class ShowMessage(val message: UiText) : SharedChatEvent
}

class SharedChatViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val chatRepository: ChatRepository,
    isAuthenticated: kotlinx.coroutines.flow.Flow<Boolean>,
) : ViewModel() {
    private val token: String = savedStateHandle[TOKEN_KEY] ?: ""

    private val _state = MutableStateFlow(
        SharedChatState(
            token = token,
            draft = savedStateHandle[DRAFT_KEY] ?: "",
        ),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<SharedChatEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            isAuthenticated.collect { authenticated ->
                _state.update { it.copy(isAuthenticated = authenticated) }
            }
        }
        viewModelScope.launch { load() }
    }

    fun onAction(action: SharedChatAction) {
        when (action) {
            is SharedChatAction.OnDraftChange -> {
                savedStateHandle[DRAFT_KEY] = action.draft
                _state.update { it.copy(draft = action.draft, forkError = null) }
            }
            SharedChatAction.OnSubmit -> viewModelScope.launch { fork() }
            SharedChatAction.OnRetry -> viewModelScope.launch { load() }
            SharedChatAction.OnLoginClick -> viewModelScope.launch {
                _events.send(SharedChatEvent.NavigateToLogin(token))
            }
            SharedChatAction.OnRegisterClick -> viewModelScope.launch {
                _events.send(SharedChatEvent.NavigateToRegister(token))
            }
            SharedChatAction.OnSignInClick -> viewModelScope.launch {
                _events.send(SharedChatEvent.NavigateToSignIn(token))
            }
        }
    }

    private suspend fun load() {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = chatRepository.getPublicShare(token)) {
            is Result.Success -> _state.update {
                it.copy(isLoading = false, snapshot = result.data)
            }
            is Result.Error -> _state.update {
                it.copy(isLoading = false, error = result.error.toUiText())
            }
        }
    }

    private suspend fun fork() {
        val current = _state.value
        val snapshot = current.snapshot ?: return
        val firstMessage = current.draft.trim()
        if (firstMessage.isEmpty() || current.isForking) return
        _state.update { it.copy(isForking = true, forkError = null) }
        val sessionResult = chatRepository.createSession()
        val sessionId = when (sessionResult) {
            is Result.Success -> sessionResult.data.id
            is Result.Error -> {
                _state.update { it.copy(isForking = false) }
                if (sessionResult.error.isUnauthorized()) {
                    _events.send(SharedChatEvent.NavigateToSignIn(token))
                } else {
                    _state.update { it.copy(forkError = sessionResult.error.toUiText()) }
                }
                return
            }
        }
        when (
            val result = chatRepository.forkSharedChat(
                ForkSeed(
                    sessionId = sessionId,
                    forkedFromToken = snapshot.token,
                    forkedFromTitle = snapshot.title.orEmpty(),
                    messages = snapshot.messages,
                    firstMessage = firstMessage,
                ),
            )
        ) {
            is Result.Success -> {
                _state.update { it.copy(isForking = false, draft = "") }
                savedStateHandle[DRAFT_KEY] = ""
                _events.send(SharedChatEvent.NavigateToFork(result.data.sessionId, firstMessage))
            }
            is Result.Error -> _state.update {
                it.copy(isForking = false, forkError = result.error.toUiText())
            }
        }
    }

    companion object {
        const val TOKEN_KEY = "token"
        const val DRAFT_KEY = "draft"
    }
}

private fun ChatError.isUnauthorized(): Boolean {
    return this is ChatError.Network && error.kind == DataError.Network.Kind.UNAUTHORIZED
}
