package co.ratmo.anreal.feature.auth.presentation.register

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.util.onFailure
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.core.domain.validation.validateEmail
import co.ratmo.anreal.core.domain.validation.validatePassword
import co.ratmo.anreal.core.domain.validation.validatePasswordMatch
import co.ratmo.anreal.core.domain.validation.validateRequiredName
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

data class RegisterState(
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val nameError: UiText? = null,
    val emailError: UiText? = null,
    val passwordError: UiText? = null,
    val confirmError: UiText? = null,
    val formError: UiText? = null,
    val isSubmitting: Boolean = false,
)

sealed interface RegisterAction {
    data class OnNameChange(val name: String) : RegisterAction
    data class OnEmailChange(val email: String) : RegisterAction
    data class OnPasswordChange(val password: String) : RegisterAction
    data class OnConfirmPasswordChange(val confirmPassword: String) : RegisterAction
    data object OnSubmit : RegisterAction
    data object OnLoginClick : RegisterAction
}

sealed interface RegisterEvent {
    data object NavigateHome : RegisterEvent
    data class NavigateLogin(val email: String) : RegisterEvent
}

class RegisterViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val authRemoteDataSource: AuthRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(
        RegisterState(
            name = savedStateHandle[NAME_KEY] ?: "",
            email = savedStateHandle[EMAIL_KEY] ?: "",
        ),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<RegisterEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: RegisterAction) {
        when (action) {
            is RegisterAction.OnNameChange -> {
                savedStateHandle[NAME_KEY] = action.name
                _state.update { it.copy(name = action.name, nameError = null, formError = null) }
            }
            is RegisterAction.OnEmailChange -> {
                savedStateHandle[EMAIL_KEY] = action.email
                _state.update { it.copy(email = action.email, emailError = null, formError = null) }
            }
            is RegisterAction.OnPasswordChange -> {
                _state.update { it.copy(password = action.password, passwordError = null, formError = null) }
            }
            is RegisterAction.OnConfirmPasswordChange -> {
                _state.update {
                    it.copy(confirmPassword = action.confirmPassword, confirmError = null, formError = null)
                }
            }
            RegisterAction.OnSubmit -> submit()
            RegisterAction.OnLoginClick -> {
                viewModelScope.launch {
                    _events.send(RegisterEvent.NavigateLogin(_state.value.email.trim()))
                }
            }
        }
    }

    private fun submit() {
        val current = _state.value
        if (current.isSubmitting) return

        val nameError = validateRequiredName(current.name).errorText { it.toUiText() }
        val emailError = validateEmail(current.email).errorText { it.toUiText() }
        val passwordError = validatePassword(current.password).errorText { it.toUiText() }
        val confirmError = validatePasswordMatch(current.password, current.confirmPassword)
            .errorText { it.toUiText() }

        _state.update {
            it.copy(
                nameError = nameError,
                emailError = emailError,
                passwordError = passwordError,
                confirmError = confirmError,
                formError = null,
            )
        }
        if (nameError != null || emailError != null || passwordError != null || confirmError != null) {
            return
        }

        viewModelScope.launch {
            _state.update { it.copy(isSubmitting = true) }
            authRemoteDataSource.signUp(
                name = current.name.trim(),
                email = current.email.trim(),
                password = current.password,
            )
                .onSuccess {
                    _state.update { it.copy(isSubmitting = false) }
                    _events.send(RegisterEvent.NavigateHome)
                }
                .onFailure { error ->
                    _state.update {
                        it.copy(isSubmitting = false, formError = error.toUiText())
                    }
                }
        }
    }

    private companion object {
        const val NAME_KEY = "name"
        const val EMAIL_KEY = "email"
    }
}
