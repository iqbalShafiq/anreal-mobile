package co.ratmo.anreal.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.chat.domain.ActiveRun
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    fun bootstrap_opens_draft_even_when_sessions_exist() = runTest {
        val fake = FakeChatRepository().apply {
            refreshResult = Result.Success(
                SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(listOf(null))
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("draft")
        assertThat(viewModel.state.value.sessionsLoading).isFalse()
    }

    @Test
    fun bootstrap_rejoins_an_active_run_instead_of_opening_draft() = runTest {
        val fake = populatedRepo().apply {
            activeRuns = Result.Success(
                listOf(ActiveRun("s1", "stream-1", "running", lastEventId = 2)),
            )
            runStatus = Result.Success(
                RunStatusSnapshot(streamId = "stream-1", status = "running", lastEventId = 2),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("s1")
        assertThat(fake.openedProjectIds).isEqualTo(emptyList())
        assertThat(fake.resumeCalls).isEqualTo(1)
    }

    @Test
    fun bootstrap_creates_draft_in_route_project_when_no_session_exists() = runTest {
        val fake = FakeChatRepository()
        val viewModel = ChatViewModel(SavedStateHandle(mapOf("projectId" to "p1")), fake)

        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(listOf("p1"))
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("draft")
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
    fun streamed_deltas_are_visible_before_request_completes() = runTest {
        val fake = populatedRepo().apply {
            holdSend = true
            streamLines = listOf(
                """{"type":"stream_start","streamId":"stream-1","eventId":0}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"text_delta","messageId":"assistant-1","partId":"text-1","delta":"Hello "}}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":2,"event":{"type":"text_delta","messageId":"assistant-1","partId":"text-1","delta":"there"}}""",
                """{"type":"stream_end","streamId":"stream-1","eventId":3,"status":"completed"}""",
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnDraftChange("Hi"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()

        val streamedText = viewModel.state.value.thread.messages
            .last { it.role == ChatRole.Assistant }
            .parts.filterIsInstance<ChatPart.Text>()
            .joinToString("") { it.text }
        assertThat(streamedText).isEqualTo("Hello there")
        assertThat(viewModel.state.value.isSending).isTrue()

        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
        assertThat(viewModel.state.value.thread.messages.any { it.role == ChatRole.Assistant }).isTrue()
    }

    @Test
    fun late_history_snapshot_does_not_replace_a_live_stream() = runTest {
        val stale = ChatMessage(
            id = "stale-user",
            role = ChatRole.User,
            parts = listOf(ChatPart.Text("stale-text", "old prompt")),
            isComplete = true,
        )
        val fake = populatedRepo().apply {
            holdHistory = true
            history = Result.Success(listOf(stale))
            holdSend = true
            streamLines = listOf(
                """{"type":"stream_start","streamId":"stream-1","eventId":0}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"text_delta","messageId":"assistant-1","partId":"text-1","delta":"Hello"}}""",
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        fake.historyStarted.await()

        viewModel.onAction(ChatAction.OnDraftChange("Hi"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()

        fake.allowHistoryToFinish.complete(Unit)
        advanceUntilIdle()

        val streamedText = viewModel.state.value.thread.messages
            .last { it.role == ChatRole.Assistant }
            .parts.filterIsInstance<ChatPart.Text>()
            .joinToString("") { it.text }
        assertThat(streamedText).isEqualTo("Hello")
        assertThat(viewModel.state.value.isSending).isTrue()

        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun cached_history_is_visible_while_remote_history_refreshes() = runTest {
        val cached = ChatMessage(
            id = "cached-assistant",
            role = ChatRole.Assistant,
            parts = listOf(ChatPart.Text("cached-text", "Cached answer")),
            isComplete = true,
        )
        val refreshed = ChatMessage(
            id = "remote-assistant",
            role = ChatRole.Assistant,
            parts = listOf(ChatPart.Text("remote-text", "Fresh answer")),
            isComplete = true,
        )
        val fake = populatedRepo().apply {
            cachedHistory = listOf(cached)
            history = Result.Success(listOf(refreshed))
            holdHistory = true
            runStatus = Result.Success(
                RunStatusSnapshot(streamId = "stream-1", status = "running", lastEventId = 2),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        fake.historyStarted.await()

        assertThat(viewModel.state.value.thread.messages).isEqualTo(listOf(cached))
        assertThat(viewModel.state.value.historyLoading).isTrue()
        assertThat(fake.runStatusCalls).isEqualTo(0)
        assertThat(fake.resumeCalls).isEqualTo(0)

        fake.allowHistoryToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.thread.messages).isEqualTo(listOf(refreshed))
        assertThat(viewModel.state.value.historyLoading).isFalse()
        assertThat(fake.runStatusCalls).isEqualTo(1)
        assertThat(fake.resumeCalls).isEqualTo(1)
    }

    @Test
    fun retry_history_reloads_messages_after_an_error() = runTest {
        val recovered = ChatMessage(
            id = "assistant-1",
            role = ChatRole.Assistant,
            parts = listOf(ChatPart.Text("text-1", "Recovered answer")),
            isComplete = true,
        )
        val fake = populatedRepo().apply {
            history = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.historyError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET))

        fake.history = Result.Success(listOf(recovered))
        viewModel.onAction(ChatAction.OnRetryHistory)
        advanceUntilIdle()

        assertThat(viewModel.state.value.historyError).isNull()
        assertThat(viewModel.state.value.thread.messages).isEqualTo(listOf(recovered))
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
    fun catalog_restores_valid_model_preferences_and_clears_invalid_values() = runTest {
        val fake = populatedRepo().apply {
            catalogResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("m1", "Luna", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val validPreferences = FakeChatPreferencesRepository(
            AppPreferences(chatModelId = "m1", chatReasoningEffort = "high"),
        )
        val validViewModel = ChatViewModel(SavedStateHandle(), fake, validPreferences)
        advanceUntilIdle()

        assertThat(validViewModel.state.value.selectedModelId).isEqualTo("m1")
        assertThat(validViewModel.state.value.selectedReasoning).isEqualTo("high")

        val invalidPreferences = FakeChatPreferencesRepository(
            AppPreferences(chatModelId = "removed", chatReasoningEffort = "removed"),
        )
        val invalidViewModel = ChatViewModel(SavedStateHandle(), fake, invalidPreferences)
        advanceUntilIdle()

        assertThat(invalidViewModel.state.value.selectedModelId).isEqualTo("m1")
        assertThat(invalidPreferences.current.chatModelId).isNull()
        assertThat(invalidPreferences.current.chatReasoningEffort).isNull()
    }

    @Test
    fun catalog_error_is_visible_and_retry_populates_models() = runTest {
        val fake = populatedRepo().apply {
            catalogResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.catalogLoading).isFalse()
        assertThat(viewModel.state.value.catalogError)
            .isEqualTo(UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET))

        fake.catalogResult = Result.Success(
            ModelCatalog(
                models = listOf(ChatModel("m1", "Luna", listOf("high"))),
                efforts = listOf(ReasoningEffort("high", "High")),
            ),
        )
        viewModel.onAction(ChatAction.OnRetryCatalog)
        advanceUntilIdle()

        assertThat(viewModel.state.value.catalogLoading).isFalse()
        assertThat(viewModel.state.value.catalogError).isNull()
        assertThat(viewModel.state.value.models.single().id).isEqualTo("m1")
    }

    @Test
    fun message_copy_edit_context_and_regenerate_actions_reach_their_owners() = runTest {
        val user = ChatMessage(
            id = "user-1",
            role = ChatRole.User,
            parts = listOf(ChatPart.Text("user-text", "Original prompt")),
            isComplete = true,
            clientMessageId = "user-client-1",
        )
        val assistant = ChatMessage(
            id = "assistant-1",
            role = ChatRole.Assistant,
            parts = listOf(ChatPart.Text("assistant-text", "Original answer")),
            isComplete = true,
        )
        val fake = populatedRepo().apply { history = Result.Success(listOf(user, assistant)) }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnCopyMessage("Original answer"))
            assertThat(awaitItem()).isEqualTo(ChatEvent.CopyText("Original answer"))
            cancelAndIgnoreRemainingEvents()
        }
        viewModel.onAction(ChatAction.OnAddContext("Original answer", ChatRole.Assistant))
        advanceUntilIdle()
        assertThat(fake.contextSnippet?.text).isEqualTo("Original answer")

        viewModel.onAction(ChatAction.OnEditMessage("user-1"))
        assertThat(viewModel.state.value.draft).isEqualTo("Original prompt")
        viewModel.onAction(ChatAction.OnDraftChange("Updated prompt"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()
        assertThat(fake.lastTruncatedMessageId).isEqualTo("user-client-1")
        assertThat(fake.sentText).isEqualTo("Updated prompt")

        viewModel.onAction(ChatAction.OnRegenerateMessage("user-1"))
        advanceUntilIdle()
        assertThat(fake.sentText).isEqualTo("Original prompt")
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

private fun ChatViewModel(
    savedStateHandle: SavedStateHandle,
    chatRepository: FakeChatRepository,
): ChatViewModel = ChatViewModel(
    savedStateHandle = savedStateHandle,
    chatRepository = chatRepository,
    preferencesRepository = FakeChatPreferencesRepository(),
)

private class FakeChatPreferencesRepository(
    initial: AppPreferences = AppPreferences(),
) : AppPreferencesRepository {
    private val values = MutableStateFlow(initial)
    val current: AppPreferences get() = values.value
    override val preferences: Flow<AppPreferences> = values

    override suspend fun setThemeMode(mode: AppThemeMode) {
        values.value = values.value.copy(themeMode = mode)
    }

    override suspend fun setDynamicColor(enabled: Boolean) {
        values.value = values.value.copy(dynamicColor = enabled)
    }

    override suspend fun setReduceMotion(enabled: Boolean) {
        values.value = values.value.copy(reduceMotion = enabled)
    }

    override suspend fun setReduceTransparency(enabled: Boolean) {
        values.value = values.value.copy(reduceTransparency = enabled)
    }

    override suspend fun setChatModel(modelId: String?) {
        values.value = values.value.copy(chatModelId = modelId)
    }

    override suspend fun setChatReasoningEffort(effort: String?) {
        values.value = values.value.copy(chatReasoningEffort = effort)
    }
}
