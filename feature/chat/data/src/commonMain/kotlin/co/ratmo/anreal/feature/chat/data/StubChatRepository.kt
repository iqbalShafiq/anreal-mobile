package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ActiveRun
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.ContextSnippet
import co.ratmo.anreal.feature.chat.domain.ContextUsage
import co.ratmo.anreal.feature.chat.domain.DocumentIngest
import co.ratmo.anreal.feature.chat.domain.DocumentStorage
import co.ratmo.anreal.feature.chat.domain.LibraryDocument
import co.ratmo.anreal.feature.chat.domain.LibraryDocumentPage
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionImage
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
    private val snippets = mutableMapOf<String, ContextSnippet>()

    override fun observeSessions(): Flow<List<ChatSession>> = sessions.asStateFlow()

    override suspend fun refreshSessions(cursor: String?): Result<SessionPage, ChatError> {
        return Result.Success(SessionPage(items = sessions.value))
    }

    override suspend fun createSession(
        sessionId: String?,
        projectId: String?,
    ): Result<ChatSession, ChatError> = openDraft(projectId)

    override suspend fun openDraft(projectId: String?): Result<ChatSession, ChatError> {
        val session = ChatSession(
            id = "dev-${sessions.value.size + 1}",
            title = "New chat",
            updatedAt = nowIso(),
            projectId = projectId,
        )
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

    override suspend fun loadCachedHistory(sessionId: String): List<ChatMessage> {
        return histories[sessionId].orEmpty()
    }

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

    override suspend fun listActiveRuns(): Result<List<ActiveRun>, ChatError> = Result.Success(emptyList())

    override suspend fun getSessionMessageCount(sessionId: String): Result<Int, ChatError> =
        Result.Success(histories[sessionId].orEmpty().size)

    override suspend fun getContextUsage(
        sessionId: String,
        model: String?,
        reasoningEffort: String?,
    ): Result<ContextUsage, ChatError> = Result.Success(
        ContextUsage(model ?: "luna", "GPT Luna 5.6", 200_000, 2_000, 0.01, 0.7, 0.3, reasoningEffort),
    )

    override suspend fun truncateSession(
        sessionId: String,
        mode: String,
        clientMessageId: String?,
        memoryPosition: Int?,
    ): EmptyResult<ChatError> {
        val target = histories[sessionId].orEmpty().indexOfFirst {
            it.clientMessageId == clientMessageId || it.id == clientMessageId || it.memoryPosition == memoryPosition
        }
        if (target >= 0) histories[sessionId] = histories[sessionId].orEmpty().take(target)
        return Result.Success(Unit)
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

    override suspend fun getDocumentStorage(): Result<DocumentStorage, ChatError> =
        Result.Success(DocumentStorage(0, 209_715_200, 209_715_200))

    override suspend fun listLibraryDocuments(
        query: String?,
        cursor: String?,
        projectId: String?,
    ): Result<LibraryDocumentPage, ChatError> = Result.Success(
        LibraryDocumentPage(
            documents.values.flatten().map {
                LibraryDocument(it.id, it.filename, it.summary, 1024, 1)
            },
            null,
        ),
    )

    override suspend fun linkDocuments(
        sessionId: String,
        documentIds: List<String>,
    ): Result<List<SessionDocument>, ChatError> {
        val library = documents.values.flatten().associateBy { it.id }
        documents[sessionId] = (documents[sessionId].orEmpty() + documentIds.mapNotNull(library::get)).distinctBy { it.id }
        return Result.Success(documents[sessionId].orEmpty())
    }

    override suspend fun uploadDocument(sessionId: String, file: ChatUpload): Result<DocumentIngest, ChatError> {
        val document = SessionDocument(
            id = "dev-doc-${documents[sessionId].orEmpty().size + 1}",
            filename = file.filename,
            summary = "Uploaded in the development environment.",
        )
        documents[sessionId] = documents[sessionId].orEmpty() + document
        return Result.Success(DocumentIngest(document.id, file.filename, "ready", 1, file.bytes.size.toLong(), null, document.summary))
    }

    override suspend fun getDocumentStatus(
        sessionId: String,
        documentId: String,
    ): Result<DocumentIngest, ChatError> {
        val document = documents[sessionId].orEmpty().first { it.id == documentId }
        return Result.Success(DocumentIngest(document.id, document.filename, "ready", 1, 1024, null, document.summary))
    }

    override suspend fun uploadImage(sessionId: String, file: ChatUpload): Result<SessionImage, ChatError> =
        Result.Success(SessionImage("image-${file.filename}", file.filename, file.mediaType, 0, 0, "user-upload", bytes = file.bytes))

    override suspend fun listSessionImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        Result.Success(emptyList())

    override suspend fun listPinnedImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        Result.Success(emptyList())

    override suspend fun pinImage(sessionId: String, imageId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun unpinImage(sessionId: String, imageId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun loadImageBytes(imageId: String): Result<ByteArray, ChatError> = Result.Success(ByteArray(0))

    override suspend fun loadContextSnippet(sessionId: String): Result<ContextSnippet?, ChatError> =
        Result.Success(snippets[sessionId])

    override suspend fun saveContextSnippet(
        sessionId: String,
        text: String,
        sourceRole: String,
    ): Result<ContextSnippet, ChatError> {
        val snippet = ContextSnippet("snippet-$sessionId", text, sourceRole)
        snippets[sessionId] = snippet
        return Result.Success(snippet)
    }

    override suspend fun clearContextSnippet(
        sessionId: String,
        snippetId: String,
    ): EmptyResult<ChatError> {
        snippets.remove(sessionId)
        return Result.Success(Unit)
    }

    override suspend fun decideApproval(
        approvalId: String,
        approved: Boolean,
    ): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun respondClarification(
        clarificationId: String,
        answers: Map<String, List<String>>,
        skipped: List<String>,
    ): EmptyResult<ChatError> = Result.Success(Unit)

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
