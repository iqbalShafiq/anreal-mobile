package co.ratmo.anreal.feature.auth.presentation.boarding

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.validation.validateEmail
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.errorText
import co.ratmo.anreal.core.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BoardingState(
    val email: String = "",
    val emailError: UiText? = null,
)

sealed interface BoardingAction {
    data class OnEmailChange(val email: String) : BoardingAction
    data object OnRegisterClick : BoardingAction
    data object OnLoginClick : BoardingAction
}

sealed interface BoardingEvent {
    data class NavigateRegister(val email: String) : BoardingEvent
    data class NavigateLogin(val email: String) : BoardingEvent
}

class BoardingViewModel(
    private val savedStateHandle: SavedStateHandle,
) : ViewModel() {

    private val _state = MutableStateFlow(
        BoardingState(email = savedStateHandle[EMAIL_KEY] ?: ""),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<BoardingEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: BoardingAction) {
        when (action) {
            is BoardingAction.OnEmailChange -> {
                savedStateHandle[EMAIL_KEY] = action.email
                _state.update { it.copy(email = action.email, emailError = null) }
            }
            BoardingAction.OnRegisterClick -> submitRegister()
            BoardingAction.OnLoginClick -> {
                viewModelScope.launch {
                    _events.send(BoardingEvent.NavigateLogin(_state.value.email.trim()))
                }
            }
        }
    }

    private fun submitRegister() {
        val current = _state.value
        val emailError = validateEmail(current.email).errorText { it.toUiText() }
        _state.update { it.copy(emailError = emailError) }
        if (emailError != null) return
        viewModelScope.launch {
            _events.send(BoardingEvent.NavigateRegister(current.email.trim()))
        }
    }

    private companion object {
        const val EMAIL_KEY = "email"
    }
}
