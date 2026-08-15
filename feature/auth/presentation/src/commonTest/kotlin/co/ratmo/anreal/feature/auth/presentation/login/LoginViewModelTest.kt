package co.ratmo.anreal.feature.auth.presentation.login

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import co.ratmo.anreal.core.domain.util.Result
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
class LoginViewModelTest {

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
    fun submit_rejects_invalid_email_and_short_password() = runTest {
        val viewModel = LoginViewModel(SavedStateHandle(), FakeAuthRemoteDataSource())

        viewModel.onAction(LoginAction.OnEmailChange("nope"))
        viewModel.onAction(LoginAction.OnPasswordChange("123"))
        viewModel.onAction(LoginAction.OnSubmit)

        assertThat(viewModel.state.value.emailError)
            .isEqualTo(UiText.StringResource("error_invalid_email"))
        assertThat(viewModel.state.value.passwordError)
            .isEqualTo(UiText.StringResource("error_password_too_short"))
    }

    @Test
    fun submit_success_navigates_home() = runTest {
        val fake = FakeAuthRemoteDataSource()
        val viewModel = LoginViewModel(SavedStateHandle(), fake)

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnEmailChange("ada@company.com"))
            viewModel.onAction(LoginAction.OnPasswordChange("password1"))
            viewModel.onAction(LoginAction.OnSubmit)

            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateHome)
            assertThat(fake.signedInEmail).isEqualTo("ada@company.com")
            assertThat(viewModel.state.value.formError).isNull()
        }
    }

    @Test
    fun submit_invalid_credentials_sets_form_error() = runTest {
        val fake = FakeAuthRemoteDataSource().apply {
            signInResult = Result.Error(AuthError.InvalidCredentials)
        }
        val viewModel = LoginViewModel(SavedStateHandle(), fake)

        viewModel.onAction(LoginAction.OnEmailChange("ada@company.com"))
        viewModel.onAction(LoginAction.OnPasswordChange("password1"))
        viewModel.onAction(LoginAction.OnSubmit)

        assertThat(viewModel.state.value.formError).isNotNull()
        assertThat(viewModel.state.value.formError)
            .isEqualTo(UiText.StringResource("error_invalid_credentials"))
    }

    @Test
    fun restores_email_from_saved_state() {
        val viewModel = LoginViewModel(
            SavedStateHandle(mapOf("email" to "saved@company.com")),
            FakeAuthRemoteDataSource(),
        )

        assertThat(viewModel.state.value.email).isEqualTo("saved@company.com")
    }

    @Test
    fun register_click_sends_email() = runTest {
        val viewModel = LoginViewModel(SavedStateHandle(), FakeAuthRemoteDataSource())

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnEmailChange("ada@company.com"))
            viewModel.onAction(LoginAction.OnRegisterClick)
            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateRegister("ada@company.com"))
        }
    }

    @Test
    fun back_click_navigates_back() = runTest {
        val viewModel = LoginViewModel(SavedStateHandle(), FakeAuthRemoteDataSource())

        viewModel.events.test {
            viewModel.onAction(LoginAction.OnBackClick)
            assertThat(awaitItem()).isEqualTo(LoginEvent.NavigateBack)
        }
    }
}
