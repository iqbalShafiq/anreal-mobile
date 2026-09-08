package co.ratmo.anreal.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ForkResult
import co.ratmo.anreal.feature.chat.domain.PublicShareSnapshot
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SharedChatViewModelTest {

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
    fun load_success_shows_snapshot() = runTest {
        val fake = FakeChatRepository()
        val viewModel = viewModel(fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isLoading).isFalse()
        assertThat(viewModel.state.value.snapshot?.token).isEqualTo("tok")
    }

    @Test
    fun load_revoked_token_shows_error() = runTest {
        val fake = FakeChatRepository().apply {
            publicShare = Result.Error(ChatError.ShareNotFound)
        }
        val viewModel = viewModel(fake, isAuthenticated = false)
        advanceUntilIdle()

        assertThat(viewModel.state.value.error).isEqualTo(ChatError.ShareNotFound.toUiText())
    }

    @Test
    fun guest_sees_auth_gate_and_login_navigates() = runTest {
        val viewModel = viewModel(FakeChatRepository(), isAuthenticated = false)
        advanceUntilIdle()

        assertThat(viewModel.state.value.isAuthenticated).isFalse()
        viewModel.events.test {
            viewModel.onAction(SharedChatAction.OnLoginClick)
            assertThat(awaitItem()).isEqualTo(SharedChatEvent.NavigateToLogin("tok"))
        }
        viewModel.events.test {
            viewModel.onAction(SharedChatAction.OnRegisterClick)
            assertThat(awaitItem()).isEqualTo(SharedChatEvent.NavigateToRegister("tok"))
        }
    }

    @Test
    fun submit_forks_and_navigates() = runTest {
        val fake = FakeChatRepository().apply {
            forkResult = Result.Success(ForkResult("s2", 2))
        }
        val viewModel = viewModel(fake)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(SharedChatAction.OnDraftChange("Continue here"))
            viewModel.onAction(SharedChatAction.OnSubmit)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(SharedChatEvent.NavigateToFork("s2", "Continue here"))
        }
        assertThat(viewModel.state.value.isForking).isFalse()
    }

    @Test
    fun fork_failure_shows_retryable_error() = runTest {
        val fake = FakeChatRepository().apply {
            forkResult = Result.Error(ChatError.ShareUnavailable)
        }
        val viewModel = viewModel(fake)
        advanceUntilIdle()

        viewModel.onAction(SharedChatAction.OnDraftChange("Continue here"))
        viewModel.onAction(SharedChatAction.OnSubmit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.forkError)
            .isEqualTo(ChatError.ShareUnavailable.toUiText())
    }

    @Test
    fun unsigned_fork_navigates_to_sign_in() = runTest {
        val fake = FakeChatRepository().apply {
            createSessionResult = Result.Error(ChatError.Network(DataError.Network.UNAUTHORIZED))
        }
        val viewModel = viewModel(fake)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(SharedChatAction.OnDraftChange("Continue here"))
            viewModel.onAction(SharedChatAction.OnSubmit)
            advanceUntilIdle()

            assertThat(awaitItem()).isEqualTo(SharedChatEvent.NavigateToSignIn("tok"))
        }
    }

    private fun viewModel(
        fake: FakeChatRepository,
        isAuthenticated: Boolean = true,
    ): SharedChatViewModel {
        return SharedChatViewModel(
            savedStateHandle = SavedStateHandle(
                mapOf(SharedChatViewModel.TOKEN_KEY to "tok"),
            ),
            chatRepository = fake,
            isAuthenticated = MutableStateFlow(isAuthenticated),
        )
    }
}
