package co.ratmo.anreal.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
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

    @Test
    fun rename_blank_title_does_not_hit_repository() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnSessionMenuRename("s1"))
        viewModel.onAction(ChatAction.OnRenameDraftChange("   "))
        viewModel.onAction(ChatAction.OnConfirmRename)
        advanceUntilIdle()

        assertThat(fake.lastRenamed).isNull()
        assertThat(viewModel.state.value.renameSessionId).isEqualTo("s1")
        assertThat(viewModel.state.value.renameError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_TITLE_REQUIRED))
    }

    @Test
    fun rename_success_dismisses_dialog() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnSessionMenuRename("s1"))
        viewModel.onAction(ChatAction.OnRenameDraftChange("  Q3  report  "))
        viewModel.onAction(ChatAction.OnConfirmRename)
        advanceUntilIdle()

        assertThat(fake.lastRenamed).isEqualTo("s1" to "Q3 report")
        assertThat(viewModel.state.value.renameSessionId).isNull()
        assertThat(viewModel.state.value.sessionBusy).isFalse()
    }

    @Test
    fun rename_error_stays_open_with_message() = runTest {
        val fake = populatedRepo().apply {
            renameResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnSessionMenuRename("s1"))
        viewModel.onAction(ChatAction.OnConfirmRename)
        advanceUntilIdle()

        assertThat(viewModel.state.value.renameSessionId).isEqualTo("s1")
        assertThat(viewModel.state.value.renameError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET))
        assertThat(viewModel.state.value.sessionBusy).isFalse()
    }

    @Test
    fun delete_selected_opens_draft_and_emits_toast() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnSessionMenuDelete("s1"))
            viewModel.onAction(ChatAction.OnConfirmDelete)
            advanceUntilIdle()

            assertThat(fake.lastDeleted).isEqualTo("s1")
            assertThat(viewModel.state.value.selectedSessionId).isEqualTo("draft")
            assertThat(viewModel.state.value.deleteSessionId).isNull()
            val event = awaitItem()
            assertThat((event as ChatEvent.ShowMessage).message)
                .isEqualTo(UiText.StringResource(AnrealCopy.TOAST_CHAT_DELETED))
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun delete_error_sets_dialog_error() = runTest {
        val fake = populatedRepo().apply {
            deleteResult = Result.Error(ChatError.Network(DataError.Network.CONFLICT))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnSessionMenuDelete("s1"))
        viewModel.onAction(ChatAction.OnConfirmDelete)
        advanceUntilIdle()

        assertThat(viewModel.state.value.deleteSessionId).isEqualTo("s1")
        assertThat(viewModel.state.value.deleteError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_CONFLICT))
    }

    private fun populatedRepo(): FakeChatRepository = FakeChatRepository().apply {
        refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
        )
    }
}
