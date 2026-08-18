package co.ratmo.anreal.feature.auth.presentation.register

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.auth.domain.AuthError
import co.ratmo.anreal.feature.auth.presentation.FakeAuthRemoteDataSource
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class RegisterViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun submit_rejects_invalid_fields() = runTest {
        val viewModel = RegisterViewModel(SavedStateHandle(), FakeAuthRemoteDataSource())

        viewModel.onAction(RegisterAction.OnEmailChange("nope"))
        viewModel.onAction(RegisterAction.OnPasswordChange("123"))
        viewModel.onAction(RegisterAction.OnConfirmPasswordChange("456"))
        viewModel.onAction(RegisterAction.OnSubmit)

        assertThat(viewModel.state.value.nameError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_NAME_REQUIRED))
        assertThat(viewModel.state.value.emailError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL))
        assertThat(viewModel.state.value.passwordError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_PASSWORD_TOO_SHORT))
        assertThat(viewModel.state.value.confirmError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_PASSWORD_MISMATCH))
    }

    @Test
    fun submit_success_navigates_home() = runTest {
        val fake = FakeAuthRemoteDataSource()
        val viewModel = RegisterViewModel(SavedStateHandle(), fake)

        viewModel.events.test {
            viewModel.onAction(RegisterAction.OnNameChange("Ada"))
            viewModel.onAction(RegisterAction.OnEmailChange("ada@company.com"))
            viewModel.onAction(RegisterAction.OnPasswordChange("password1"))
            viewModel.onAction(RegisterAction.OnConfirmPasswordChange("password1"))
            viewModel.onAction(RegisterAction.OnSubmit)

            assertThat(awaitItem()).isEqualTo(RegisterEvent.NavigateHome)
            assertThat(fake.signedInEmail).isEqualTo("ada@company.com")
            assertThat(viewModel.state.value.formError).isNull()
        }
    }

    @Test
    fun submit_email_taken_sets_form_error() = runTest {
        val fake = FakeAuthRemoteDataSource().apply {
            signUpResult = Result.Error(AuthError.EmailTaken)
        }
        val viewModel = RegisterViewModel(SavedStateHandle(), fake)

        viewModel.onAction(RegisterAction.OnNameChange("Ada"))
        viewModel.onAction(RegisterAction.OnEmailChange("ada@company.com"))
        viewModel.onAction(RegisterAction.OnPasswordChange("password1"))
        viewModel.onAction(RegisterAction.OnConfirmPasswordChange("password1"))
        viewModel.onAction(RegisterAction.OnSubmit)

        assertThat(viewModel.state.value.formError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_EMAIL_TAKEN))
    }

    @Test
    fun restores_name_and_email_from_saved_state() {
        val viewModel = RegisterViewModel(
            SavedStateHandle(
                mapOf(
                    "name" to "Ada",
                    "email" to "saved@company.com",
                ),
            ),
            FakeAuthRemoteDataSource(),
        )

        assertThat(viewModel.state.value.name).isEqualTo("Ada")
        assertThat(viewModel.state.value.email).isEqualTo("saved@company.com")
    }

    @Test
    fun login_click_sends_email() = runTest {
        val viewModel = RegisterViewModel(SavedStateHandle(), FakeAuthRemoteDataSource())

        viewModel.events.test {
            viewModel.onAction(RegisterAction.OnEmailChange("ada@company.com"))
            viewModel.onAction(RegisterAction.OnLoginClick)
            assertThat(awaitItem()).isEqualTo(RegisterEvent.NavigateLogin("ada@company.com"))
        }
    }

}
