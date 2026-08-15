package co.ratmo.anreal.feature.auth.presentation.register

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.text.input.KeyboardType
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealFormScreen
import co.ratmo.anreal.core.designsystem.component.AnrealPasswordField
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    onNavigateHome: () -> Unit,
    onNavigateLogin: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegisterEvent.NavigateHome -> onNavigateHome()
            RegisterEvent.NavigateLogin -> onNavigateLogin()
        }
    }
    RegisterScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun RegisterScreen(
    state: RegisterState,
    onAction: (RegisterAction) -> Unit,
) {
    AnrealFormScreen(
        title = "Create an account",
        subtitle = "Start a workspace with your name, email, and a password.",
        footer = {
            TextButton(
                onClick = { onAction(RegisterAction.OnLoginClick) },
                enabled = !state.isSubmitting,
            ) {
                Text("Already have an account? Sign in")
            }
        },
    ) {
        AnrealTextField(
            value = state.name,
            onValueChange = { onAction(RegisterAction.OnNameChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
            placeholder = "Ada Lovelace",
            error = state.nameError?.asString(),
            enabled = !state.isSubmitting,
        )
        AnrealTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
            placeholder = "you@company.com",
            error = state.emailError?.asString(),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AnrealPasswordField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_PASSWORD),
            placeholder = "At least 8 characters",
            error = state.passwordError?.asString(),
            enabled = !state.isSubmitting,
            showPasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_SHOW_PASSWORD),
            hidePasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_HIDE_PASSWORD),
        )
        AnrealPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(RegisterAction.OnConfirmPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_CONFIRM_PASSWORD),
            placeholder = "Repeat your password",
            error = state.confirmError?.asString(),
            enabled = !state.isSubmitting,
            showPasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_SHOW_PASSWORD),
            hidePasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_HIDE_PASSWORD),
        )
        state.formError?.let { error ->
            AnrealError(message = error.asString())
        }
        AnrealPrimaryButton(
            label = AnrealCopy.get(AnrealCopy.ACTION_CREATE_ACCOUNT),
            onClick = { onAction(RegisterAction.OnSubmit) },
            loading = state.isSubmitting,
            loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_CREATING_ACCOUNT),
        )
    }
}

@AnrealPreviews
@Composable
private fun RegisterIdlePreview() {
    AnrealPreview {
        RegisterScreen(state = RegisterState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun RegisterFilledPreview() {
    AnrealPreview {
        RegisterScreen(
            state = RegisterState(
                name = "Ada",
                email = "ada@company.com",
                password = "password1",
                confirmPassword = "password1",
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RegisterFieldErrorsPreview() {
    AnrealPreview {
        RegisterScreen(
            state = RegisterState(
                nameError = UiText.StringResource(AnrealCopy.ERROR_NAME_REQUIRED),
                emailError = UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL),
                passwordError = UiText.StringResource(AnrealCopy.ERROR_PASSWORD_TOO_SHORT),
                confirmError = UiText.StringResource(AnrealCopy.ERROR_PASSWORD_MISMATCH),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RegisterFormErrorPreview() {
    AnrealPreview {
        RegisterScreen(
            state = RegisterState(
                name = "Ada",
                email = "ada@company.com",
                password = "password1",
                confirmPassword = "password1",
                formError = UiText.StringResource(AnrealCopy.ERROR_EMAIL_TAKEN),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun RegisterSubmittingPreview() {
    AnrealPreview {
        RegisterScreen(
            state = RegisterState(
                name = "Ada",
                email = "ada@company.com",
                password = "password1",
                confirmPassword = "password1",
                isSubmitting = true,
            ),
            onAction = {},
        )
    }
}
