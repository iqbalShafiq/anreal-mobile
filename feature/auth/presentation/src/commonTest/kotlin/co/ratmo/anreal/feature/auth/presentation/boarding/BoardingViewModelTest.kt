package co.ratmo.anreal.feature.auth.presentation.boarding

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
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
class BoardingViewModelTest {

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
    fun register_rejects_invalid_email() = runTest {
        val viewModel = BoardingViewModel(SavedStateHandle())

        viewModel.onAction(BoardingAction.OnEmailChange("nope"))
        viewModel.onAction(BoardingAction.OnRegisterClick)

        assertThat(viewModel.state.value.emailError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_INVALID_EMAIL))
    }

    @Test
    fun register_navigates_with_trimmed_email() = runTest {
        val viewModel = BoardingViewModel(SavedStateHandle())

        viewModel.events.test {
            viewModel.onAction(BoardingAction.OnEmailChange("  ada@company.com "))
            viewModel.onAction(BoardingAction.OnRegisterClick)

            assertThat(awaitItem())
                .isEqualTo(BoardingEvent.NavigateRegister("ada@company.com"))
            assertThat(viewModel.state.value.emailError).isNull()
        }
    }

    @Test
    fun login_navigates_with_email_without_validation() = runTest {
        val viewModel = BoardingViewModel(SavedStateHandle())

        viewModel.events.test {
            viewModel.onAction(BoardingAction.OnEmailChange("ada@company.com"))
            viewModel.onAction(BoardingAction.OnLoginClick)

            assertThat(awaitItem())
                .isEqualTo(BoardingEvent.NavigateLogin("ada@company.com"))
        }
    }

    @Test
    fun restores_email_from_saved_state() {
        val viewModel = BoardingViewModel(
            SavedStateHandle(mapOf("email" to "saved@company.com")),
        )

        assertThat(viewModel.state.value.email).isEqualTo("saved@company.com")
    }
}
