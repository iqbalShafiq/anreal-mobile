package co.ratmo.anreal.feature.auth.presentation.login

import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
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
import co.ratmo.anreal.feature.auth.presentation.component.AuthSwitchRow
import co.ratmo.anreal.feature.auth.presentation.component.AuthWorkspaceNote
import co.ratmo.anreal.feature.auth.presentation.component.AuthWorkspaceNoteKind
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun LoginRoot(
    onNavigateHome: () -> Unit,
    onNavigateRegister: (String) -> Unit,
    viewModel: LoginViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            LoginEvent.NavigateHome -> onNavigateHome()
            is LoginEvent.NavigateRegister -> onNavigateRegister(event.email)
        }
    }
    LoginScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun LoginScreen(
    state: LoginState,
    onAction: (LoginAction) -> Unit,
) {
    val focusManager = LocalFocusManager.current
    AnrealFormScreen(
        title = AnrealCopy.get(AnrealCopy.LOGIN_TITLE),
        subtitle = AnrealCopy.get(AnrealCopy.LOGIN_SUBTITLE),
        wordmark = AnrealCopy.get(AnrealCopy.LABEL_APP_NAME),
        markDescription = AnrealCopy.get(AnrealCopy.CD_APP_MARK),
        footer = {
            AuthSwitchRow(
                prompt = AnrealCopy.get(AnrealCopy.AUTH_NEW_HERE),
                actionLabel = AnrealCopy.get(AnrealCopy.ACTION_CREATE_ACCOUNT),
                onClick = { onAction(LoginAction.OnRegisterClick) },
                enabled = !state.isSubmitting,
            )
        },
    ) {
        AuthWorkspaceNote(kind = AuthWorkspaceNoteKind.Returning)
        AnrealTextField(
            value = state.email,
            onValueChange = { onAction(LoginAction.OnEmailChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_EMAIL),
            error = state.emailError?.asString(),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Email,
                imeAction = ImeAction.Next,
            ),
            keyboardActions = KeyboardActions(
                onNext = { focusManager.moveFocus(FocusDirection.Down) },
            ),
        )
        AnrealPasswordField(
            value = state.password,
            onValueChange = { onAction(LoginAction.OnPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_PASSWORD),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_PASSWORD),
            error = state.passwordError?.asString(),
            enabled = !state.isSubmitting,
            showPasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_SHOW_PASSWORD),
            hidePasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_HIDE_PASSWORD),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Password,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = { onAction(LoginAction.OnSubmit) },
            ),
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
