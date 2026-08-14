package co.ratmo.anreal.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ChatViewModelTest {

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
    fun bootstrap_opens_existing_session() = runTest {
        val fake = FakeChatRepository().apply {
            refreshResult = Result.Success(
                SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("s1")
        assertThat(viewModel.state.value.sessionsLoading).isFalse()
    }

    @Test
    fun send_keeps_user_bubble_and_records_text() = runTest {
        val fake = FakeChatRepository().apply {
            refreshResult = Result.Success(
                SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnDraftChange("Hello docs"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.sentText).isEqualTo("Hello docs")
        assertThat(viewModel.state.value.draft).isEqualTo("")
        assertThat(viewModel.state.value.thread.messages.any { it.role == ChatRole.User }).isTrue()
    }

    @Test
    fun send_conflict_opens_resume_dialog() = runTest {
        val fake = FakeChatRepository().apply {
            refreshResult = Result.Success(
                SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
            )
            sendResult = Result.Error(ChatError.RunActive)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(viewModel.state.value.runActiveConflict).isTrue()
    }
}
