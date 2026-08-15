package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class StubChatRepository : ChatRepository {

    private val sessions = MutableStateFlow(
        listOf(
            ChatSession(
                id = "dev-session",
                title = "Development chat",
                updatedAt = nowIso(),
            ),
        ),
    )
    private val histories = mutableMapOf<String, List<ChatMessage>>()
    private val queues = mutableMapOf<String, List<QueuedItem>>()
    private val documents = mutableMapOf(
        "dev-session" to listOf(
            SessionDocument(
                id = "dev-doc",
                filename = "Anvia_Framework.pdf",
                summary = "Stub document for the development environment.",
            ),
        ),
    )

    override fun observeSessions(): Flow<List<ChatSession>> = sessions.asStateFlow()

    override suspend fun refreshSessions(): Result<SessionPage, ChatError> {
        return Result.Success(SessionPage(items = sessions.value))
    }

    override suspend fun openDraft(): Result<ChatSession, ChatError> {
        val session = ChatSession(id = "dev-${sessions.value.size + 1}", title = "New chat", updatedAt = nowIso())
        sessions.update { listOf(session) + it }
        return Result.Success(session)
    }

    override suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError> {
        var updated: ChatSession? = null
        sessions.update { current ->
            current.map { session ->
                if (session.id != sessionId) {
                    session
                } else {
                    session.copy(title = title, updatedAt = nowIso()).also { updated = it }
                }
            }
        }
        return Result.Success(updated ?: ChatSession(id = sessionId, title = title, updatedAt = nowIso()))
    }

    override suspend fun deleteSession(sessionId: String): EmptyResult<ChatError> {
        sessions.update { it.filterNot { session -> session.id == sessionId } }
        histories.remove(sessionId)
        return Result.Success(Unit)
    }

    override suspend fun markRead(sessionId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError> {
        return Result.Success(histories[sessionId].orEmpty())
    }

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        clientMessageId: String?,
        options: ChatRunOptions,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        val userId = clientMessageId ?: "user-${histories[sessionId].orEmpty().size}"
        val user = ChatMessage(
            id = userId,
            role = ChatRole.User,
            parts = listOf(ChatPart.Text(id = "$userId-text", text = text)),
            isComplete = true,
        )
        val assistantId = "assistant-$userId"
        val partId = "$assistantId-text"
        val streamId = "stub-$userId"
        val reply = "Got it — $text. This is a development stub reply."
        onLine("""{"type":"stream_start","streamId":"$streamId"}""")
        onLine(
            """{"type":"stream_event","streamId":"$streamId","eventId":1,"event":{"type":"message_start","message":{"id":"$assistantId","role":"assistant","parts":[{"type":"text","id":"$partId","text":""}]}}}""",
        )
        onLine(
            """{"type":"stream_event","streamId":"$streamId","eventId":2,"event":{"type":"text_delta","messageId":"$assistantId","partId":"$partId","delta":${jsonString(reply)}}}""",
        )
        onLine(
            """{"type":"stream_event","streamId":"$streamId","eventId":3,"event":{"type":"message_end","messageId":"$assistantId"}}""",
        )
        onLine("""{"type":"stream_end","streamId":"$streamId","eventId":4,"status":"completed"}""")
        val assistant = ChatMessage(
            id = assistantId,
            role = ChatRole.Assistant,
            parts = listOf(ChatPart.Text(id = partId, text = reply)),
            isComplete = true,
        )
        histories[sessionId] = histories[sessionId].orEmpty() + user + assistant
        sessions.update { current ->
            current.map { session ->
                if (session.id == sessionId) session.copy(updatedAt = nowIso()) else session
            }
        }
        return Result.Success(Unit)
    }

    override suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError> {
        return Result.Success(Unit)
    }

    override suspend fun syncQueue(sessionId: String, ids: List<String>): Result<List<String>, ChatError> {
        return Result.Success(ids)
    }

    override suspend fun loadQueue(sessionId: String): List<QueuedItem> = queues[sessionId].orEmpty()

    override suspend fun replaceQueue(sessionId: String, items: List<QueuedItem>) {
        queues[sessionId] = items
    }

    override suspend fun resume(
        sessionId: String,
        streamId: String,
        after: Int,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun stop(streamId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError> {
        return Result.Success(RunStatusSnapshot(streamId = null, status = "idle", lastEventId = null))
    }

    override suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) = Unit

    override suspend fun loadCatalog(): Result<ModelCatalog, ChatError> {
        return Result.Success(
            ModelCatalog(
                models = listOf(
                    ChatModel(
                        id = "luna",
                        label = "GPT Luna 5.6",
                        reasoningEfforts = listOf("low", "high", "xhigh"),
                        contextWindowTokens = 200_000,
                    ),
                ),
                efforts = listOf(
                    ReasoningEffort(key = "low", label = "Low"),
                    ReasoningEffort(key = "high", label = "High"),
                    ReasoningEffort(key = "xhigh", label = "Xhigh"),
                ),
            ),
        )
    }

    override suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError> {
        return Result.Success(ChatCapabilities(webSearchAvailable = true, imageGenerationAvailable = true))
    }

    override suspend fun listSessionDocuments(sessionId: String): Result<List<SessionDocument>, ChatError> {
        return Result.Success(documents[sessionId].orEmpty())
    }

    override suspend fun unlinkSessionDocument(
        sessionId: String,
        documentId: String,
    ): EmptyResult<ChatError> {
        documents[sessionId] = documents[sessionId].orEmpty().filterNot { it.id == documentId }
        return Result.Success(Unit)
    }

    override suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError> {
        return Result.Success(
            listOf(
                RecentProject(id = "p1", name = "Agentic Course"),
                RecentProject(id = "p2", name = "Anvia Project"),
            ),
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun nowIso(): String = Clock.System.now().toString()

private fun jsonString(value: String): String {
    return buildString {
        append('"')
        value.forEach { char ->
            when (char) {
                '\\' -> append("\\\\")
                '"' -> append("\\\"")
                '\n' -> append("\\n")
                else -> append(char)
            }
        }
        append('"')
    }
}
