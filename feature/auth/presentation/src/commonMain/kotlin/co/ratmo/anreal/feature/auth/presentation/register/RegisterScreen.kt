package co.ratmo.anreal.feature.auth.presentation.register

import androidx.compose.foundation.text.KeyboardOptions
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
import co.ratmo.anreal.feature.auth.presentation.component.AuthSwitchRow
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun RegisterRoot(
    onNavigateHome: () -> Unit,
    onNavigateLogin: (String) -> Unit,
    onNavigateBack: () -> Unit,
    viewModel: RegisterViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            RegisterEvent.NavigateHome -> onNavigateHome()
            is RegisterEvent.NavigateLogin -> onNavigateLogin(event.email)
            RegisterEvent.NavigateBack -> onNavigateBack()
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
        title = AnrealCopy.get(AnrealCopy.REGISTER_TITLE),
        subtitle = AnrealCopy.get(AnrealCopy.REGISTER_SUBTITLE),
        wordmark = AnrealCopy.get(AnrealCopy.LABEL_APP_NAME),
        markDescription = AnrealCopy.get(AnrealCopy.CD_APP_MARK),
        onBack = { onAction(RegisterAction.OnBackClick) },
        backDescription = AnrealCopy.get(AnrealCopy.CD_BACK),
        footer = {
            AuthSwitchRow(
                prompt = AnrealCopy.get(AnrealCopy.AUTH_HAVE_ACCOUNT),
                actionLabel = AnrealCopy.get(AnrealCopy.ACTION_SIGN_IN),
                onClick = { onAction(RegisterAction.OnLoginClick) },
                enabled = !state.isSubmitting,
            )
        },
    ) {
        AnrealTextField(
            value = state.name,
            onValueChange = { onAction(RegisterAction.OnNameChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_NAME),
            error = state.nameError?.asString(),
            enabled = !state.isSubmitting,
        )
        AnrealTextField(
            value = state.email,
            onValueChange = { onAction(RegisterAction.OnEmailChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_EMAIL),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_EMAIL),
            error = state.emailError?.asString(),
            enabled = !state.isSubmitting,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
        )
        AnrealPasswordField(
            value = state.password,
            onValueChange = { onAction(RegisterAction.OnPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_PASSWORD),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_PASSWORD_NEW),
            error = state.passwordError?.asString(),
            enabled = !state.isSubmitting,
            showPasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_SHOW_PASSWORD),
            hidePasswordDescription = AnrealCopy.get(AnrealCopy.ACTION_HIDE_PASSWORD),
        )
        AnrealPasswordField(
            value = state.confirmPassword,
            onValueChange = { onAction(RegisterAction.OnConfirmPasswordChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_CONFIRM_PASSWORD),
            placeholder = AnrealCopy.get(AnrealCopy.PLACEHOLDER_PASSWORD_REPEAT),
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
