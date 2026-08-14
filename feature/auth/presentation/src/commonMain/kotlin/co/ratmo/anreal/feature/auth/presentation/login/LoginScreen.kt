package co.ratmo.anreal.feature.auth.presentation.login

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
fun LoginRoot(
    onNavigateHome: () -> Unit,
    onNavigateRegister: () -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.NavigateHome -> onNavigateHome()
            LoginEvent.NavigateRegister -> onNavigateRegister()
        }
    }
    LoginScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    AnrealFormScreen(
        title = "Sign in",
        subtitle = "Use the email and password for your Anreal workspace.",
        footer = {
            TextButton(
                onClick = { onAction(LoginAction.OnRegisterClick) },
                enabled = !state.isSubmitting,
            ) {
                Text("New here? Create an account")
            }
        },
    ) {
        AnrealTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
            placeholder = "you@company.com",
            error = state.emailError?.asString(),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AnrealPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_PASSWORD),
            placeholder = "Your password",
            error = state.passwordError?.asString(),
            enabled = !state.isSubmitting,
        )
        state.formError?.let { error ->
            AnrealError(message = error.asString())
        }
        AnrealPrimaryButton(
            label = AnrealCopy.get(AnrealCopy.ACTION_CONTINUE),
            onClick = { onAction(LoginAction.OnSubmit) },
            loading = state.isSubmitting,
            loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_SIGNING_IN),
        )
    }
}

@AnrealPreviews
@Composable
private fun LoginIdlePreview() {
    AnrealPreview {
        LoginScreen(state = LoginState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun LoginFilledPreview() {
    AnrealPreview {
        LoginScreen(
            state = LoginState(
                email = "you@company.com",
                password = "password1",
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun LoginFieldErrorsPreview() {
    AnrealPreview {
        LoginScreen(
            state = LoginState(
                email = "nope",
                password = "123",
                emailError = UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL),
                passwordError = UiText.StringResource(AnrealCopy.ERROR_PASSWORD_TOO_SHORT),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun LoginFormErrorPreview() {
    AnrealPreview {
        LoginScreen(
            state = LoginState(
                email = "you@company.com",
                password = "password1",
                formError = UiText.StringResource(AnrealCopy.ERROR_INVALID_CREDENTIALS),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun LoginSubmittingPreview() {
    AnrealPreview {
        LoginScreen(
            state = LoginState(
                email = "you@company.com",
                password = "password1",
                isSubmitting = true,
            ),
            onAction = {},
        )
    }
}
