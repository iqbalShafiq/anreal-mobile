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
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
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

    @Test
    fun send_while_streaming_queues_and_does_not_call_repository() = runTest {
        val fake = populatedRepo().apply { holdSend = true }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()

        viewModel.onAction(ChatAction.OnDraftChange("Follow up"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.sentText).isEqualTo("Hello")
        assertThat(viewModel.state.value.draft).isEqualTo("")
        assertThat(viewModel.state.value.queue.single().text).isEqualTo("Follow up")
        fake.allowSendToFinish.complete(Unit)
    }

    @Test
    fun stop_holds_auto_flush() = runTest {
        val fake = populatedRepo().apply { holdSend = true }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Queued"))
        viewModel.onAction(ChatAction.OnSend)
        viewModel.onAction(ChatAction.OnStop)
        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(fake.sentText).isEqualTo("Hello")
        assertThat(viewModel.state.value.queue.single().text).isEqualTo("Queued")
    }

    @Test
    fun completed_without_hold_flushes_pending_item() = runTest {
        val fake = populatedRepo().apply { holdSend = true }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Queued"))
        viewModel.onAction(ChatAction.OnSend)
        fake.holdSend = false
        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(fake.sentText).isEqualTo("Queued")
        assertThat(viewModel.state.value.queue).isEqualTo(emptyList())
    }

    @Test
    fun steer_no_active_run_falls_back_to_send() = runTest {
        val fake = populatedRepo().apply {
            holdSend = true
            steerResult = Result.Error(ChatError.NoActiveRun)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Queued"))
        viewModel.onAction(ChatAction.OnSend)
        viewModel.onAction(ChatAction.OnStop)
        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
        fake.sentText = null
        fake.holdSend = false

        viewModel.onAction(ChatAction.OnSendNow)
        advanceUntilIdle()

        assertThat(fake.sentText).isEqualTo("Queued")
    }

    @Test
    fun idle_with_queue_opens_conflict_and_send_new_keeps_queue() = runTest {
        val fake = populatedRepo().apply { holdSend = true }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Queued"))
        viewModel.onAction(ChatAction.OnSend)
        viewModel.onAction(ChatAction.OnStop)
        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
        fake.holdSend = false

        viewModel.onAction(ChatAction.OnDraftChange("Another"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(viewModel.state.value.queueConflict).isTrue()
        val queuedBefore = viewModel.state.value.queue.size
        viewModel.onAction(ChatAction.OnSendNewMessage)
        advanceUntilIdle()
        assertThat(viewModel.state.value.queueConflict).isFalse()
        assertThat(viewModel.state.value.queue.size).isEqualTo(queuedBefore)
    }

    @Test
    fun selecting_model_and_send_stamps_run_options() = runTest {
        val fake = populatedRepo().apply {
            catalogResult = Result.Success(
                ModelCatalog(
                    models = listOf(
                        ChatModel(id = "m1", label = "DeepSeek", reasoningEfforts = listOf("high")),
                    ),
                    efforts = listOf(ReasoningEffort(key = "high", label = "High")),
                ),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnSelectModel("m1"))
        viewModel.onAction(ChatAction.OnSelectReasoning("high"))
        viewModel.onAction(ChatAction.OnToggleWebSearch)
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.sentOptions?.model).isEqualTo("m1")
        assertThat(fake.sentOptions?.reasoningEffort).isEqualTo("high")
        assertThat(fake.sentOptions?.webSearchEnabled).isEqualTo(true)
    }

    @Test
    fun opening_settings_navigates_to_account() = runTest {
        val viewModel = ChatViewModel(SavedStateHandle(), populatedRepo())
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnOpenSettings)
            assertThat(awaitItem()).isEqualTo(ChatEvent.OpenAccount)
        }
    }

    private fun populatedRepo(): FakeChatRepository = FakeChatRepository().apply {
        refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
        )
    }
}
