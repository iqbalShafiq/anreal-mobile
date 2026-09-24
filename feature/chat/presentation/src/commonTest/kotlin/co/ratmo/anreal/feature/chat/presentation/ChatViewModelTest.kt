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
import co.ratmo.anreal.feature.chat.domain.EnhancementSelectionStore
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpStatus
import co.ratmo.anreal.feature.chat.domain.McpTestResult
import co.ratmo.anreal.feature.chat.domain.McpTool
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.CachedModelCatalog
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionSiteEntry
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SiteStatus
import co.ratmo.anreal.feature.chat.domain.SitesRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillStatus
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.HISTORY_PAGE_SIZE
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.InteractionResponse
import co.ratmo.anreal.feature.chat.domain.stream.QuestionAnswer
import co.ratmo.anreal.feature.chat.domain.stream.SessionGrant
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withTimeout
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
    fun bootstrap_opens_requested_session_from_saved_state() = runTest {
        val fake = FakeChatRepository().apply {
            refreshResult = Result.Success(
                SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
            )
        }
        val viewModel = ChatViewModel(
            SavedStateHandle(mapOf("sessionId" to "s1")),
            fake,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("s1")
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
    fun bootstrap_creates_standalone_draft_even_if_enter_project_keys_are_absent() = runTest {
        val fake = FakeChatRepository()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)

        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(listOf(null))
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("draft")
        assertThat(viewModel.state.value.activeProjectId).isNull()
    }

    @Test
    fun opening_recent_project_selects_existing_chat_without_opening_a_draft() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.openedProjectIds.clear()
        fake.refreshResult = Result.Success(
            SessionPage(
                listOf(
                    ChatSession(id = "p-s1", title = "Docs", updatedAt = "now", projectId = "p1"),
                    ChatSession(id = "p-s2", title = "Notes", updatedAt = "earlier", projectId = "p1"),
                ),
            ),
        )

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ChatEvent.RevealChatsDrawer)
        }

        assertThat(fake.openedProjectIds).isEqualTo(emptyList())
        assertThat(fake.openProjectCalls).isEqualTo(listOf("p1"))
        assertThat(fake.refreshProjectIds.last()).isEqualTo("p1")
        assertThat(viewModel.state.value.activeProjectId).isEqualTo("p1")
        assertThat(viewModel.state.value.activeProjectName).isEqualTo("Research")
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("p-s1")
        assertThat(viewModel.state.value.inProject).isTrue()
    }

    @Test
    fun enter_project_action_uses_supplied_name_and_scopes_sessions() = runTest {
        val fake = populatedRepo().apply {
            openProjectResult = Result.Success(RecentProject("a1", "Anvia Project"))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.refreshResult = Result.Success(
            SessionPage(
                listOf(ChatSession(id = "a-s1", title = "Docs", updatedAt = "now", projectId = "a1")),
            ),
        )

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnEnterProject("a1", "Anvia Project"))
            advanceUntilIdle()
            assertThat(awaitItem()).isEqualTo(ChatEvent.RevealChatsDrawer)
        }

        assertThat(viewModel.state.value.activeProjectId).isEqualTo("a1")
        assertThat(viewModel.state.value.activeProjectName).isEqualTo("Anvia Project")
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("a-s1")
        assertThat(fake.refreshProjectIds.last()).isEqualTo("a1")
    }

    @Test
    fun opening_project_prefers_empty_draft_in_the_list() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.openedProjectIds.clear()
        fake.refreshResult = Result.Success(
            SessionPage(
                listOf(
                    ChatSession(id = "p-s1", title = "Docs", updatedAt = "now", projectId = "p1"),
                    ChatSession(id = "p-draft", title = "New chat", updatedAt = "now", projectId = "p1"),
                ),
            ),
        )

        viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(emptyList())
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("p-draft")
    }

    @Test
    fun opening_empty_project_opens_a_project_draft() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
            draft = ChatSession(id = "p-draft", title = "New chat", updatedAt = "now", projectId = "p1")
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.openedProjectIds.clear()
        fake.refreshResult = Result.Success(SessionPage(emptyList()))

        viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(listOf("p1"))
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("p-draft")
        assertThat(viewModel.state.value.activeProjectId).isEqualTo("p1")
    }

    @Test
    fun new_chat_in_project_uses_project_id() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
            draft = ChatSession(id = "p-draft", title = "New chat", updatedAt = "now", projectId = "p1")
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "p-s1", title = "Docs", updatedAt = "now", projectId = "p1"))),
        )
        viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
        advanceUntilIdle()
        fake.openedProjectIds.clear()

        viewModel.onAction(ChatAction.OnNewChat)
        advanceUntilIdle()

        assertThat(fake.openedProjectIds).isEqualTo(listOf("p1"))
    }

    @Test
    fun all_chats_leaves_project_and_restores_standalone_session() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        val standaloneId = viewModel.state.value.selectedSessionId
        fake.refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "p-s1", title = "Docs", updatedAt = "now", projectId = "p1"))),
        )
        viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
        advanceUntilIdle()
        fake.refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = standaloneId ?: "draft", title = "New chat", updatedAt = "now"))),
        )
        fake.openedProjectIds.clear()

        viewModel.onAction(ChatAction.OnOpenAllChats)
        advanceUntilIdle()

        assertThat(viewModel.state.value.activeProjectId).isNull()
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo(standaloneId)
        assertThat(fake.openedProjectIds).isEqualTo(emptyList())
        assertThat(fake.refreshProjectIds.last()).isEqualTo(null)
    }

    @Test
    fun deleting_selected_project_chat_opens_a_project_draft() = runTest {
        val fake = populatedRepo().apply {
            recentProjects = Result.Success(listOf(RecentProject("p1", "Research")))
            draft = ChatSession(id = "p-draft", title = "New chat", updatedAt = "now", projectId = "p1")
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "p-s1", title = "Docs", updatedAt = "now", projectId = "p1"))),
        )
        fake.openedProjectIds.clear()
        viewModel.events.test {
            viewModel.onAction(ChatAction.OnOpenRecentProject("p1"))
            assertThat(awaitItem()).isEqualTo(ChatEvent.RevealChatsDrawer)
            fake.openedProjectIds.clear()
            viewModel.onAction(ChatAction.OnSessionMenuDelete("p-s1"))
            viewModel.onAction(ChatAction.OnConfirmDelete)
            assertThat((awaitItem() as ChatEvent.ShowMessage).message)
                .isEqualTo(UiText.StringResource(AnrealCopy.TOAST_CHAT_DELETED))
        }

        assertThat(fake.openedProjectIds).isEqualTo(listOf("p1"))
        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("p-draft")
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
    fun session_history_opens_on_the_latest_page_only() = runTest {
        val messages = historyMessages(HISTORY_PAGE_SIZE + 5)
        val fake = populatedRepo().apply {
            cachedHistory = messages
            history = Result.Success(messages)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.thread.messages).isEqualTo(messages.takeLast(HISTORY_PAGE_SIZE))
        assertThat(viewModel.state.value.canLoadOlderHistory).isTrue()
        assertThat(viewModel.state.value.olderHistoryLoading).isFalse()
        assertThat(viewModel.state.value.historyLoading).isFalse()
    }

    @Test
    fun load_older_history_prepends_earlier_messages() = runTest {
        val messages = historyMessages(HISTORY_PAGE_SIZE + 5)
        val fake = populatedRepo().apply {
            cachedHistory = messages
            history = Result.Success(messages)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnLoadOlderHistory)
        advanceUntilIdle()

        assertThat(viewModel.state.value.thread.messages).isEqualTo(messages)
        assertThat(viewModel.state.value.canLoadOlderHistory).isFalse()
        assertThat(viewModel.state.value.olderHistoryLoading).isFalse()
    }

    @Test
    fun load_older_history_is_ignored_when_the_page_is_complete() = runTest {
        val messages = historyMessages(3)
        val fake = populatedRepo().apply {
            history = Result.Success(messages)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnLoadOlderHistory)
        advanceUntilIdle()

        assertThat(viewModel.state.value.thread.messages).isEqualTo(messages)
        assertThat(viewModel.state.value.canLoadOlderHistory).isFalse()
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
    fun catalog_prefers_room_selection_over_legacy_preferences() = runTest {
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
        assertThat(invalidPreferences.current.chatModelId).isEqualTo("m1")
        assertThat(invalidPreferences.current.chatReasoningEffort).isEqualTo("high")
    }

    @Test
    fun startup_catalog_refresh_waits_for_delayed_room_selection_before_using_legacy_preferences() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("room", "Room", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
                selectedModelId = "room",
                selectedReasoningEffort = "high",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogResult = Result.Success(
                ModelCatalog(
                    models = listOf(
                        ChatModel("room", "Room", listOf("high")),
                        ChatModel("legacy", "Legacy", listOf("high")),
                    ),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
            holdCatalogRefresh = true
            allowCatalogCacheInitialEmission = CompletableDeferred()
        }
        val preferences = FakeChatPreferencesRepository(
            AppPreferences(chatModelId = "legacy", chatReasoningEffort = "high"),
        )
        val viewModel = ChatViewModel(SavedStateHandle(), fake, preferences)

        fake.catalogCacheObservationStarted.await()
        runCurrent()
        fake.allowCatalogCacheInitialEmission?.complete(Unit)
        withTimeout(1_000) { fake.catalogRefreshStarted.await() }
        fake.allowCatalogRefreshToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedModelId).isEqualTo("room")
        assertThat(viewModel.state.value.selectedReasoning).isEqualTo("high")
        assertThat(fake.persistedCatalogSelection).isEqualTo("room" to "high")
        assertThat(preferences.current.chatModelId).isEqualTo("room")
    }

    @Test
    fun startup_catalog_refresh_releases_after_delayed_null_room_cache_emission() = runTest {
        val fake = populatedRepo().apply {
            catalogResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("live", "Live", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
            allowCatalogCacheInitialEmission = CompletableDeferred()
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)

        fake.catalogCacheObservationStarted.await()
        fake.allowCatalogCacheInitialEmission?.complete(Unit)
        withTimeout(1_000) { fake.catalogRefreshStarted.await() }
        advanceUntilIdle()

        assertThat(viewModel.state.value.models.single().id).isEqualTo("live")
        assertThat(viewModel.state.value.catalogError).isNull()
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
    fun cached_catalog_appears_before_startup_refresh_completes_and_live_success_clears_cache_marker() = runTest {
        val cached = ModelCatalog(
            models = listOf(ChatModel("cached", "Cached Luna", listOf("high"))),
            efforts = listOf(ReasoningEffort("high", "High")),
        )
        val live = ModelCatalog(
            models = listOf(ChatModel("live", "Live Luna", listOf("high"))),
            efforts = listOf(ReasoningEffort("high", "High")),
        )
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = cached,
                selectedModelId = "cached",
                selectedReasoningEffort = "high",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(live)
            holdCatalogRefresh = true
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)

        fake.catalogRefreshStarted.await()
        assertThat(viewModel.state.value.models.single().id).isEqualTo("cached")
        assertThat(viewModel.state.value.catalogFromCache).isTrue()

        fake.allowCatalogRefreshToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.models.single().id).isEqualTo("live")
        assertThat(viewModel.state.value.catalogFromCache).isFalse()
    }

    @Test
    fun live_refresh_removed_model_clears_selection_and_exposes_cached_human_label() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("removed", "Human readable model", listOf("xhigh"))),
                    efforts = listOf(ReasoningEffort("xhigh", "Xhigh")),
                ),
                selectedModelId = "removed",
                selectedReasoningEffort = "xhigh",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("current", "Current", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val preferences = FakeChatPreferencesRepository(
            AppPreferences(chatModelId = "removed", chatReasoningEffort = "xhigh"),
        )
        val viewModel = ChatViewModel(SavedStateHandle(), fake, preferences)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedModelId).isNull()
        assertThat(viewModel.state.value.selectedReasoning).isNull()
        assertThat(viewModel.state.value.modelUnavailable)
            .isEqualTo(ModelUnavailableUi("removed", "Human readable model"))
        assertThat(fake.persistedCatalogSelection).isEqualTo(null to null)
        assertThat(preferences.current.chatModelId).isNull()
        assertThat(preferences.current.chatReasoningEffort).isNull()
    }

    @Test
    fun live_refresh_downgrades_unsupported_xhigh_to_nearest_lower_high() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("m1", "Luna", listOf("xhigh"))),
                    efforts = listOf(ReasoningEffort("xhigh", "Xhigh")),
                ),
                selectedModelId = "m1",
                selectedReasoningEffort = "xhigh",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("m1", "Luna", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedModelId).isEqualTo("m1")
        assertThat(viewModel.state.value.selectedReasoning).isEqualTo("high")
        assertThat(viewModel.state.value.modelUnavailable).isNull()
    }

    @Test
    fun send_waits_for_startup_refresh_then_sends_with_reconciled_options() = runTest {
        val fake = populatedRepo().apply {            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("live", "Live", listOf("xhigh", "high"))),
                    efforts = listOf(
                        ReasoningEffort("xhigh", "Xhigh"),
                        ReasoningEffort("high", "High"),
                    ),
                ),
                selectedModelId = "live",
                selectedReasoningEffort = "xhigh",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("live", "Live", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
            holdCatalogRefresh = true
            holdSend = true
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        fake.catalogRefreshStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        runCurrent()

        assertThat(fake.sentOptions).isNull()
        fake.allowCatalogRefreshToFinish.complete(Unit)
        fake.sendStarted.await()
        assertThat(fake.sentOptions?.model).isEqualTo("live")
        assertThat(fake.sentOptions?.reasoningEffort).isEqualTo("high")

        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun failed_startup_refresh_is_retried_once_by_send_and_successful_retry_sends() = runTest {
        val fake = populatedRepo().apply {
            catalogRefreshResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        fake.catalogRefreshResult = Result.Success(
            ModelCatalog(
                models = listOf(ChatModel("m1", "Luna", listOf("high"))),
                efforts = listOf(ReasoningEffort("high", "High")),
            ),
        )
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.catalogRefreshCalls).isEqualTo(2)
        assertThat(fake.sentText).isEqualTo("Hello")
    }

    @Test
    fun failed_startup_and_send_retry_emit_dedicated_error_without_mutating_draft() = runTest {
        val fake = populatedRepo().apply {
            catalogRefreshResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Keep this draft"))
        viewModel.events.test {
            viewModel.onAction(ChatAction.OnSend)
            assertThat(awaitItem()).isEqualTo(
                ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_MODEL_CATALOG_UNAVAILABLE)),
            )
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertThat(fake.catalogRefreshCalls).isEqualTo(2)
        assertThat(viewModel.state.value.draft).isEqualTo("Keep this draft")
        assertThat(fake.sentOptions).isNull()
    }

    @Test
    fun concurrent_startup_retry_and_send_share_one_catalog_refresh() = runTest {
        val fake = populatedRepo().apply {
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("m1", "Luna", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
            holdCatalogRefresh = true
            holdSend = true
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        fake.catalogRefreshStarted.await()
        viewModel.onAction(ChatAction.OnRetryCatalog)
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.catalogRefreshCalls).isEqualTo(1)
        assertThat(fake.sentOptions).isNull()
        fake.allowCatalogRefreshToFinish.complete(Unit)
        fake.sendStarted.await()
        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun dismissing_or_choosing_a_model_clears_model_unavailable() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("removed", "Old", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
                selectedModelId = "removed",
                selectedReasoningEffort = "high",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("current", "Current", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        assertThat(viewModel.state.value.modelUnavailable).isEqualTo(ModelUnavailableUi("removed", "Old"))

        viewModel.onAction(ChatAction.OnDismissModelUnavailable)
        assertThat(viewModel.state.value.modelUnavailable).isNull()

        viewModel.onAction(ChatAction.OnSelectModel("current"))
        assertThat(viewModel.state.value.modelUnavailable).isNull()
    }

    @Test
    fun removed_model_stays_unselected_after_a_later_successful_refresh() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("removed", "Old", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
                selectedModelId = "removed",
                selectedReasoningEffort = "high",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("current", "Current", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        assertThat(viewModel.state.value.modelUnavailable).isEqualTo(ModelUnavailableUi("removed", "Old"))

        viewModel.onAction(ChatAction.OnRetryCatalog)
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedModelId).isNull()
        assertThat(viewModel.state.value.selectedReasoning).isNull()
        assertThat(viewModel.state.value.modelUnavailable).isEqualTo(ModelUnavailableUi("removed", "Old"))
    }

    @Test
    fun dismissing_removed_model_still_blocks_send_until_a_model_is_selected() = runTest {
        val fake = populatedRepo().apply {
            cachedCatalog.value = CachedModelCatalog(
                catalog = ModelCatalog(
                    models = listOf(ChatModel("removed", "Old", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
                selectedModelId = "removed",
                selectedReasoningEffort = "high",
                lastSuccessfulRefreshEpochMillis = 1L,
            )
            catalogRefreshResult = Result.Success(
                ModelCatalog(
                    models = listOf(ChatModel("current", "Current", listOf("high"))),
                    efforts = listOf(ReasoningEffort("high", "High")),
                ),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDismissModelUnavailable)
        viewModel.onAction(ChatAction.OnDraftChange("Keep this draft"))

        viewModel.events.test {
            viewModel.onAction(ChatAction.OnSend)
            assertThat(awaitItem()).isEqualTo(
                ChatEvent.ShowMessage(UiText.StringResource(AnrealCopy.ERROR_MODEL_CATALOG_UNAVAILABLE)),
            )
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertThat(viewModel.state.value.draft).isEqualTo("Keep this draft")
        assertThat(fake.sentOptions).isNull()
    }

    @Test
    fun no_active_run_catalog_failure_preserves_queue_item() = runTest {
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
        fake.holdSend = false
        advanceUntilIdle()

        fake.sentOptions = null
        fake.catalogRefreshResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        viewModel.onAction(ChatAction.OnRetryCatalog)
        advanceUntilIdle()
        fake.steerResult = Result.Error(ChatError.NoActiveRun)
        viewModel.onAction(ChatAction.OnSendNow)
        advanceUntilIdle()

        assertThat(viewModel.state.value.queue.map { it.text }).isEqualTo(listOf("Queued"))
        assertThat(fake.sentOptions).isNull()
    }

    @Test
    fun auto_flush_catalog_failure_preserves_queue_item() = runTest {
        val fake = populatedRepo().apply { holdSend = true }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        viewModel.onAction(ChatAction.OnDraftChange("Queued"))
        viewModel.onAction(ChatAction.OnSend)
        viewModel.onAction(ChatAction.OnStop)

        fake.catalogRefreshResult = Result.Error(ChatError.Network(DataError.Network.NO_INTERNET))
        viewModel.onAction(ChatAction.OnRetryCatalog)
        advanceUntilIdle()
        fake.allowSendToFinish.complete(Unit)
        fake.holdSend = false
        advanceUntilIdle()

        assertThat(viewModel.state.value.queue.map { it.text }).isEqualTo(listOf("Queued"))
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

    @Test
    fun first_catalog_load_defaults_selection_to_all_enabled() = runTest {
        val selection = FakeSelectionStore()
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = populatedRepo(),
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(
                listOf(
                    Skill(id = "s1", name = "alpha", description = "Alpha", status = SkillStatus.Active, isEnabled = true),
                    Skill(id = "s2", name = "beta", description = "Beta", status = SkillStatus.Invalid, isEnabled = true),
                    Skill(id = "s3", name = "gamma", description = "Gamma", status = SkillStatus.Active, isEnabled = false),
                ),
            ),
            mcpSource = FakeMcpSource(
                listOf(
                    McpServer(id = "m1", name = "docs", url = "https://mcp.example.com/mcp", isEnabled = true, status = McpStatus.Ok, tools = listOf(McpTool("search", "Search"))),
                    McpServer(id = "m2", name = "empty", url = "https://mcp.example.com/empty", isEnabled = true, status = McpStatus.Ok),
                    McpServer(id = "m3", name = "down", url = "https://mcp.example.com/down", isEnabled = true, status = McpStatus.Error, tools = listOf(McpTool("search", "Search"))),
                ),
            ),
            sitesSource = FakeSitesSource(),
            selectionStore = selection,
        )
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSkillIds).isEqualTo(listOf("s1"))
        assertThat(viewModel.state.value.selectedMcpServerIds).isEqualTo(listOf("m1"))
        assertThat(selection.savedSkills).isEqualTo(listOf("s1"))
        assertThat(selection.savedMcp).isEqualTo(listOf("m1"))
    }

    @Test
    fun toggle_persists_intersected_ids_and_drops_ghosts() = runTest {
        val selection = FakeSelectionStore(initialSkills = listOf("s1"))
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = populatedRepo(),
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(
                listOf(
                    Skill(id = "s1", name = "alpha", description = "Alpha"),
                    Skill(id = "s2", name = "beta", description = "Beta"),
                ),
            ),
            mcpSource = FakeMcpSource(),
            sitesSource = FakeSitesSource(),
            selectionStore = selection,
        )
        advanceUntilIdle()
        assertThat(viewModel.state.value.selectedSkillIds).isEqualTo(listOf("s1"))

        viewModel.onAction(ChatAction.OnSkillsToggle(listOf("s1", "s2", "ghost")))
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSkillIds).isEqualTo(listOf("s1", "s2"))
        assertThat(selection.savedSkills).isEqualTo(listOf("s1", "s2"))

        viewModel.onAction(ChatAction.OnMcpToggle(listOf("ghost")))
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedMcpServerIds).isEqualTo(emptyList())
        assertThat(selection.savedMcp).isEqualTo(emptyList())
    }

    @Test
    fun send_includes_selected_skill_and_mcp_ids() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = fake,
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(listOf(Skill(id = "s1", name = "alpha", description = "Alpha"))),
            mcpSource = FakeMcpSource(
                listOf(
                    McpServer(id = "m1", name = "docs", url = "https://mcp.example.com/mcp", status = McpStatus.Ok, tools = listOf(McpTool("search", "Search"))),
                ),
            ),
            sitesSource = FakeSitesSource(),
            selectionStore = FakeSelectionStore(initialSkills = listOf("s1"), initialMcp = listOf("m1")),
        )
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnDraftChange("Hello"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        assertThat(fake.sentOptions?.skillIds).isEqualTo(listOf("s1"))
        assertThat(fake.sentOptions?.mcpServerIds).isEqualTo(listOf("m1"))
    }

    @Test
    fun stream_progress_then_ready_updates_site_builds() = runTest {
        val fake = populatedRepo().apply {
            streamLines = listOf(
                """{"type":"stream_start","streamId":"stream-1","eventId":0}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"data","name":"siteBuildProgress","data":{"siteId":"s1","version":1,"phase":"building","message":"Membangun hero."}}}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":2,"event":{"type":"data","name":"siteBuildReady","data":{"siteId":"s1","version":1,"previewUrl":"/api/sites/s1/v1/preview/index.html","downloadUrl":"/api/sites/s1/v1/download"}}}""",
                """{"type":"stream_end","streamId":"stream-1","eventId":3,"status":"completed"}""",
            )
        }
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = fake,
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(),
            mcpSource = FakeMcpSource(),
            sitesSource = FakeSitesSource(),
            selectionStore = FakeSelectionStore(),
        )
        advanceUntilIdle()
        val sessionId = viewModel.state.value.selectedSessionId

        viewModel.onAction(ChatAction.OnDraftChange("Build a site"))
        viewModel.onAction(ChatAction.OnSend)
        advanceUntilIdle()

        val build = viewModel.state.value.siteBuilds[sessionId]
        assertThat(build?.phase).isEqualTo(SiteBuildPhase.Ready)
        assertThat(build?.previewUrl).isEqualTo("/api/sites/s1/v1/preview/index.html")
        assertThat(viewModel.state.value.siteVersions[sessionId]?.single()?.stable).isEqualTo(true)
    }

    @Test
    fun empty_poll_during_streaming_keeps_streaming_state() = runTest {
        val sites = FakeSitesSource().apply {
            holdPoll = true
            entries = Result.Success(emptyList())
        }
        val fake = populatedRepo().apply {
            holdSend = true
            streamLines = listOf(
                """{"type":"stream_start","streamId":"stream-1","eventId":0}""",
                """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"data","name":"siteBuildProgress","data":{"siteId":"s1","version":1,"phase":"building","message":"Membangun hero."}}}""",
            )
        }
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = fake,
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(),
            mcpSource = FakeMcpSource(),
            sitesSource = sites,
            selectionStore = FakeSelectionStore(),
        )
        sites.pollStarted.await()

        viewModel.onAction(ChatAction.OnDraftChange("Build a site"))
        viewModel.onAction(ChatAction.OnSend)
        fake.sendStarted.await()
        val sessionId = viewModel.state.value.selectedSessionId
        assertThat(viewModel.state.value.siteBuilds[sessionId]?.phase).isEqualTo(SiteBuildPhase.Building)

        sites.allowPollToFinish.complete(Unit)
        advanceUntilIdle()

        assertThat(viewModel.state.value.siteBuilds[sessionId]?.phase).isEqualTo(SiteBuildPhase.Building)
        assertThat(sites.pollCalls).isEqualTo(listOf("draft"))

        fake.allowSendToFinish.complete(Unit)
        advanceUntilIdle()
    }

    @Test
    fun session_poll_populates_site_maps_once_per_session() = runTest {
        val sites = FakeSitesSource().apply {
            entries = Result.Success(
                listOf(
                    SessionSiteEntry(siteId = "s1", version = 2, stableVersion = 2, status = SiteStatus.Ready, previewUrl = "/api/sites/s1/v2/preview/index.html", downloadUrl = "/api/sites/s1/v2/download", updatedAt = "t"),
                ),
            )
        }
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = populatedRepo(),
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(),
            mcpSource = FakeMcpSource(),
            sitesSource = sites,
            selectionStore = FakeSelectionStore(),
        )
        advanceUntilIdle()
        val sessionId = viewModel.state.value.selectedSessionId

        val build = viewModel.state.value.siteBuilds[sessionId]
        assertThat(build?.phase).isEqualTo(SiteBuildPhase.Ready)
        assertThat(build?.previewUrl).isEqualTo("/api/sites/s1/v2/preview/index.html")
        assertThat(viewModel.state.value.siteVersions[sessionId]?.single()?.stable).isEqualTo(true)
        assertThat(sites.pollCalls).isEqualTo(listOf("draft"))

        viewModel.onAction(ChatAction.OnSessionClick("s1"))
        advanceUntilIdle()

        assertThat(sites.pollCalls).isEqualTo(listOf("draft", "s1"))
    }

    @Test
    fun site_retry_replaces_build_with_retried_state() = runTest {
        val sites = FakeSitesSource().apply {
            entries = Result.Success(
                listOf(
                    SessionSiteEntry(siteId = "s1", version = 1, stableVersion = 1, status = SiteStatus.Failed, previewUrl = null, downloadUrl = "/api/sites/s1/v1/download", updatedAt = "t"),
                ),
            )
            retryResult = Result.Success(SiteBuildState("s1", 2, SiteBuildPhase.Starting, "Mengulang build."))
        }
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = populatedRepo(),
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(),
            mcpSource = FakeMcpSource(),
            sitesSource = sites,
            selectionStore = FakeSelectionStore(),
        )
        advanceUntilIdle()
        val sessionId = viewModel.state.value.selectedSessionId
        assertThat(viewModel.state.value.siteBuilds[sessionId]?.phase).isEqualTo(SiteBuildPhase.Failed)

        viewModel.onAction(ChatAction.OnSiteRetry("s1"))
        advanceUntilIdle()

        val build = viewModel.state.value.siteBuilds[sessionId]
        assertThat(build?.phase).isEqualTo(SiteBuildPhase.Starting)
        assertThat(build?.version).isEqualTo(2)
    }

    @Test
    fun site_rollback_marks_target_version_stable() = runTest {
        val sites = FakeSitesSource().apply {
            entries = Result.Success(
                listOf(
                    SessionSiteEntry(siteId = "s1", version = 1, stableVersion = 1, status = SiteStatus.Ready, previewUrl = "/api/sites/s1/v1/preview/index.html", downloadUrl = "/api/sites/s1/v1/download", updatedAt = "t"),
                    SessionSiteEntry(siteId = "s1", version = 2, stableVersion = 1, status = SiteStatus.Ready, previewUrl = "/api/sites/s1/v2/preview/index.html", downloadUrl = "/api/sites/s1/v2/download", updatedAt = "t"),
                ),
            )
            rollbackResult = Result.Success(2)
        }
        val viewModel = ChatViewModel(
            savedStateHandle = SavedStateHandle(),
            chatRepository = populatedRepo(),
            preferencesRepository = FakeChatPreferencesRepository(),
            skillsSource = FakeSkillsSource(),
            mcpSource = FakeMcpSource(),
            sitesSource = sites,
            selectionStore = FakeSelectionStore(),
        )
        advanceUntilIdle()
        val sessionId = viewModel.state.value.selectedSessionId

        viewModel.onAction(ChatAction.OnSiteRollback("s1", 2))
        advanceUntilIdle()

        val versions = viewModel.state.value.siteVersions[sessionId]
        assertThat(versions?.first { it.version == 2 }?.stable).isEqualTo(true)
        assertThat(versions?.first { it.version == 1 }?.stable).isEqualTo(false)
    }

    private fun populatedRepo(): FakeChatRepository = FakeChatRepository().apply {
        refreshResult = Result.Success(
            SessionPage(listOf(ChatSession(id = "s1", title = "Docs", updatedAt = "now"))),
        )
    }

    @Test
    fun interaction_allow_once_stages_then_answers() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnInteractionAllowOnce("i1"))
        advanceUntilIdle()

        assertThat(fake.stageCalls.single().interactionId).isEqualTo("i1")
        assertThat(fake.stageCalls.single().grantScope).isNull()
        assertThat(fake.answeredInteractions.single().interactionId).isEqualTo("i1")
        assertThat(viewModel.state.value.humanInputBusy).isFalse()
    }

    @Test
    fun interaction_allow_session_sends_session_grant() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnInteractionAllowSession("i1"))
        advanceUntilIdle()

        assertThat(fake.stageCalls.single().grantScope).isEqualTo(SessionGrant.Session)
        assertThat(fake.answeredInteractions).isEqualTo(
            listOf(
                AnsweredInteraction(
                    sessionId = "draft",
                    interactionId = "i1",
                    response = InteractionResponse.ToolApproval(approved = true),
                    options = fake.answeredInteractions.single().options,
                ),
            ),
        )
    }

    @Test
    fun interaction_reject_answers_without_staging() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnInteractionReject("i1"))
        advanceUntilIdle()

        assertThat(fake.stageCalls).isEqualTo(emptyList())
        assertThat(fake.answeredInteractions.single().response)
            .isEqualTo(InteractionResponse.ToolApproval(approved = false))
    }

    @Test
    fun stale_interaction_is_dismissed_silently() = runTest {
        val fake = populatedRepo().apply {
            answerResult = Result.Error(ChatError.InteractionHandled)
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnInteractionAllowOnce("i1"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.thread.pendingInteractions).isEqualTo(emptyList())
        assertThat(viewModel.state.value.thread.staleInteractionIds).isEqualTo(setOf("i1"))
    }

    @Test
    fun question_answer_sends_tool_question_response() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        val answers = listOf(QuestionAnswer("q1", "broad"))
        viewModel.onAction(ChatAction.OnInteractionQuestionAnswer("i2", answers))
        advanceUntilIdle()

        assertThat(fake.stageCalls).isEqualTo(emptyList())
        assertThat(fake.answeredInteractions.single().response)
            .isEqualTo(InteractionResponse.ToolQuestion(answers))
    }

    @Test
    fun open_share_loads_latest_link_when_active() = runTest {
        val fake = populatedRepo().apply {
            shareStatus = Result.Success(co.ratmo.anreal.feature.chat.domain.ChatShareStatus("draft", true))
            latestShare = Result.Success(
                co.ratmo.anreal.feature.chat.domain.ChatShareLink("tok", "/share/tok", "draft", "Notes", "now"),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnOpenShare)
        advanceUntilIdle()

        assertThat(viewModel.state.value.shareOpen).isTrue()
        assertThat(viewModel.state.value.shareStatusActive == true).isTrue()
        assertThat(viewModel.state.value.latestShare?.token).isEqualTo("tok")
    }

    @Test
    fun create_share_updates_status_and_link() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnOpenShare)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnCreateShare)
        advanceUntilIdle()

        assertThat(viewModel.state.value.shareStatusActive == true).isTrue()
        assertThat(viewModel.state.value.latestShare?.urlPath).isEqualTo("/share/tok")
    }

    @Test
    fun deactivate_shares_clears_link() = runTest {
        val fake = populatedRepo().apply {
            shareStatus = Result.Success(co.ratmo.anreal.feature.chat.domain.ChatShareStatus("draft", true))
            latestShare = Result.Success(
                co.ratmo.anreal.feature.chat.domain.ChatShareLink("tok", "/share/tok", "draft", "Notes", "now"),
            )
        }
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnOpenShare)
        advanceUntilIdle()
        viewModel.onAction(ChatAction.OnRequestDeactivateShares)
        viewModel.onAction(ChatAction.OnConfirmDeactivateShares)
        advanceUntilIdle()

        assertThat(viewModel.state.value.shareStatusActive == false).isTrue()
        assertThat(viewModel.state.value.latestShare).isNull()
    }

    @Test
    fun fork_send_selects_session_and_sends_first_message() = runTest {
        val fake = populatedRepo()
        val viewModel = ChatViewModel(SavedStateHandle(), fake)
        advanceUntilIdle()

        viewModel.onAction(ChatAction.OnForkSend("s1", "Hello from share"))
        advanceUntilIdle()

        assertThat(viewModel.state.value.selectedSessionId).isEqualTo("s1")
        assertThat(fake.sentText).isEqualTo("Hello from share")
    }
}

private fun historyMessages(count: Int): List<ChatMessage> = List(count) { index ->
    ChatMessage(
        id = "history-$index",
        role = if (index % 2 == 0) ChatRole.User else ChatRole.Assistant,
        parts = listOf(ChatPart.Text("t$index", "Message $index")),
        isComplete = true,
    )
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

private class FakeSkillsSource(
    private var skills: List<Skill> = emptyList(),
) : SkillsRemoteDataSource {
    override suspend fun listSkills(): Result<List<Skill>, DataError.Network> = Result.Success(skills)

    override suspend fun getSkill(id: String): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network.NOT_FOUND)

    override suspend fun createSkill(name: String, description: String, bodyMd: String): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun updateSkill(id: String, name: String, description: String, bodyMd: String, markReviewed: Boolean): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun deleteSkill(id: String): Result<Unit, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)
}

private class FakeMcpSource(
    private var servers: List<McpServer> = emptyList(),
) : McpRemoteDataSource {
    override suspend fun listServers(): Result<List<McpServer>, DataError.Network> = Result.Success(servers)

    override suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun deleteServer(id: String): Result<Unit, DataError.Network> =
        Result.Error(DataError.Network.UNKNOWN)

    override suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network> =
        Result.Success(McpTestResult(ok = false))
}

private class FakeSitesSource : SitesRemoteDataSource {
    var entries: Result<List<SessionSiteEntry>, DataError.Network> = Result.Success(emptyList())
    var retryResult: Result<SiteBuildState, DataError.Network> =
        Result.Success(SiteBuildState("s1", 1, SiteBuildPhase.Starting, "Mengulang build."))
    var rollbackResult: Result<Int, DataError.Network> = Result.Success(1)
    val pollCalls = mutableListOf<String>()
    var holdPoll: Boolean = false
    var pollStarted: CompletableDeferred<Unit> = CompletableDeferred()
    var allowPollToFinish: CompletableDeferred<Unit> = CompletableDeferred()

    override suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network> {
        pollCalls += sessionId
        if (holdPoll) {
            if (!pollStarted.isCompleted) pollStarted.complete(Unit)
            allowPollToFinish.await()
        }
        return entries
    }

    override suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network> =
        Result.Success(ByteArray(0))

    override suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network> = retryResult

    override suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network> = rollbackResult
}

private class FakeSelectionStore(
    initialSkills: List<String> = emptyList(),
    initialMcp: List<String> = emptyList(),
) : EnhancementSelectionStore {
    private val skillIds = MutableStateFlow(initialSkills)
    private val mcpIds = MutableStateFlow(initialMcp)
    var savedSkills: List<String>? = null
    var savedMcp: List<String>? = null

    override fun observeSkillIds(): Flow<List<String>> = skillIds

    override fun observeMcpServerIds(): Flow<List<String>> = mcpIds

    override suspend fun saveSkillIds(ids: List<String>) {
        savedSkills = ids
        skillIds.value = ids
    }

    override suspend fun saveMcpServerIds(ids: List<String>) {
        savedMcp = ids
        mcpIds.value = ids
    }
}
