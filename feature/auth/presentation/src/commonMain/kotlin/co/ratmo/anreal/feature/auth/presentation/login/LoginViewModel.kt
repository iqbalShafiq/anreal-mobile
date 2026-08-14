package co.ratmo.anreal.feature.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.util.onFailure
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.core.domain.validation.validateEmail
import co.ratmo.anreal.core.domain.validation.validatePassword
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.errorText
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import co.ratmo.anreal.feature.auth.presentation.toUiText
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class LoginState(
    val email: String = "",
    val password: String = "",
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val formError: UiText? = null,
    val isSubmitting: Boolean = false,
)

sealed interface LoginAction {
    data class OnEmailChange(val email: String) : LoginAction
    data class OnPasswordChange(val password: String) : LoginAction
    data object OnSubmit : LoginAction
    data object OnRegisterClick : LoginAction
}

sealed interface LoginEvent {
    data object NavigateHome : LoginEvent
    data object NavigateRegister : LoginEvent
}

class LoginViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRemoteDataSource: AuthRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        LoginState(email = savedStateHandle[EMAIL_KEY] ?: ""),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<LoginEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: LoginAction) {
        when (action) {
            is LoginAction.OnEmailChange -> {
                savedStateHandle[EMAIL_KEY] = action.email
                _state.update { it.copy(email = action.email, emailError = null, formError = null) }
            }
            is LoginAction.OnPasswordChange -> {
                _state.update { it.copy(password = action.password, passwordError = null, formError = null) }
            }
            LoginAction.OnSubmit -> submit()
            LoginAction.OnRegisterClick -> {
                viewModelScope.launch { _events.send(LoginEvent.NavigateRegister) }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val emailError = validateEmail(current.email).errorText { it.toUiText() }
        val passwordError = validatePassword(current.password).errorText { it.toUiText() }
        _state.update {
            it.copy(
                emailError = emailError,
                passwordError = passwordError,
                formError = null,
            )
        }
        if (emailError != null || passwordError != null) return

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authRemoteDataSource.signIn(current.email.trim(), current.password)
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(LoginEvent.NavigateHome)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSubmitting = false, formError = error.toUiText())
                    }
                }
        }
    }

    private companion object {
        const val EMAIL_KEY = "email"
    }
}
