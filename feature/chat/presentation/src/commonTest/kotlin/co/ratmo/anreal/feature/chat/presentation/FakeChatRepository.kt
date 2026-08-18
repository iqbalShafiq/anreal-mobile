package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ActiveRun
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.ContextSnippet
import co.ratmo.anreal.feature.chat.domain.ContextUsage
import co.ratmo.anreal.feature.chat.domain.DocumentIngest
import co.ratmo.anreal.feature.chat.domain.DocumentStorage
import co.ratmo.anreal.feature.chat.domain.LibraryDocumentPage
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionImage
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatRepository : ChatRepository {
    val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    var refreshResult: Result<SessionPage, ChatError> = Result.Success(SessionPage(emptyList()))
    var draft: ChatSession = ChatSession(id = "draft", title = "New chat", updatedAt = "now")
    var history: Result<List<ChatMessage>, ChatError> = Result.Success(emptyList())
    var sendResult: EmptyResult<ChatError> = Result.Success(Unit)
    var sentText: String? = null
    var sentClientMessageId: String? = null
    var sentOptions: ChatRunOptions? = null
    var catalogResult: Result<ModelCatalog, ChatError> = Result.Success(ModelCatalog())
    var capabilitiesResult: Result<ChatCapabilities, ChatError> = Result.Success(ChatCapabilities())
    var steerResult: EmptyResult<ChatError> = Result.Success(Unit)
    var steered: List<QueuedItem> = emptyList()
    var syncResult: Result<List<String>, ChatError> = Result.Success(emptyList())
    var queues: MutableMap<String, List<QueuedItem>> = mutableMapOf()
    var holdSend: Boolean = false
    var sendStarted: CompletableDeferred<Unit> = CompletableDeferred()
    var allowSendToFinish: CompletableDeferred<Unit> = CompletableDeferred()
    var lastRenamed: Pair<String, String>? = null
    var lastDeleted: String? = null
    var renameResult: Result<ChatSession, ChatError>? = null
    var deleteResult: EmptyResult<ChatError> = Result.Success(Unit)
    var runStatus: Result<RunStatusSnapshot, ChatError> = Result.Success(
        RunStatusSnapshot(streamId = null, status = "idle", lastEventId = null),
    )
    var sessionDocuments: Result<List<SessionDocument>, ChatError> = Result.Success(emptyList())
    var recentProjects: Result<List<RecentProject>, ChatError> = Result.Success(emptyList())
    var lastUnlinked: Pair<String, String>? = null
    var unlinkResult: EmptyResult<ChatError> = Result.Success(Unit)
    var uploadDocumentResult: Result<DocumentIngest, ChatError> = Result.Success(
        DocumentIngest("doc", "file.pdf", "ready", 1, 1, null, null),
    )
    var uploadImageResult: Result<SessionImage, ChatError> = Result.Success(
        SessionImage("image", "image.png", "image/png", 1, 1, "user-upload"),
    )
    var lastUpload: Triple<String, ChatUpload, Boolean>? = null
    var contextSnippet: ContextSnippet? = null
    val openedProjectIds = mutableListOf<String?>()

    override fun observeSessions(): Flow<List<ChatSession>> = sessions

    override suspend fun refreshSessions(cursor: String?): Result<SessionPage, ChatError> {
        when (val result = refreshResult) {
            is Result.Success -> sessions.value = result.data.items
            is Result.Error -> Unit
        }
        return refreshResult
    }

    override suspend fun createSession(
        sessionId: String?,
        projectId: String?,
    ): Result<ChatSession, ChatError> = openDraft(projectId)

    override suspend fun openDraft(projectId: String?): Result<ChatSession, ChatError> {
        openedProjectIds += projectId
        sessions.value = listOf(draft) + sessions.value.filterNot { it.id == draft.id }
        return Result.Success(draft)
    }

    override suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError> {
        lastRenamed = sessionId to title
        val forced = renameResult
        if (forced is Result.Error) return forced
        val updated = ChatSession(id = sessionId, title = title, updatedAt = "now")
        sessions.value = sessions.value.map { if (it.id == sessionId) updated else it }
        return Result.Success(updated)
    }

    override suspend fun deleteSession(sessionId: String): EmptyResult<ChatError> {
        lastDeleted = sessionId
        if (deleteResult is Result.Error) return deleteResult
        sessions.value = sessions.value.filterNot { it.id == sessionId }
        return deleteResult
    }

    override suspend fun markRead(sessionId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError> = history

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        clientMessageId: String?,
        options: ChatRunOptions,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        sentText = text
        sentClientMessageId = clientMessageId
        sentOptions = options
        if (holdSend) {
            if (!sendStarted.isCompleted) sendStarted.complete(Unit)
            allowSendToFinish.await()
        }
        return sendResult
    }

    override suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError> {
        steered = items
        return steerResult
    }

    override suspend fun syncQueue(sessionId: String, ids: List<String>): Result<List<String>, ChatError> {
        return syncResult
    }

    override suspend fun loadQueue(sessionId: String): List<QueuedItem> {
        return queues[sessionId].orEmpty()
    }

    override suspend fun replaceQueue(sessionId: String, items: List<QueuedItem>) {
        queues[sessionId] = items
    }

    override suspend fun loadCatalog(): Result<ModelCatalog, ChatError> = catalogResult

    override suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError> = capabilitiesResult

    override suspend fun resume(
        sessionId: String,
        streamId: String,
        after: Int,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun stop(streamId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError> = runStatus

    override suspend fun listActiveRuns(): Result<List<ActiveRun>, ChatError> = Result.Success(emptyList())

    override suspend fun getSessionMessageCount(sessionId: String): Result<Int, ChatError> =
        Result.Success((history as? Result.Success)?.data?.size ?: 0)

    override suspend fun getContextUsage(
        sessionId: String,
        model: String?,
        reasoningEffort: String?,
    ): Result<ContextUsage, ChatError> = Result.Success(
        ContextUsage(model.orEmpty(), model.orEmpty(), 100, 10, 0.1, 0.7, 0.3, reasoningEffort),
    )

    override suspend fun truncateSession(
        sessionId: String,
        mode: String,
        clientMessageId: String?,
        memoryPosition: Int?,
    ): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) = Unit

    override suspend fun listSessionDocuments(sessionId: String): Result<List<SessionDocument>, ChatError> {
        return sessionDocuments
    }

    override suspend fun unlinkSessionDocument(
        sessionId: String,
        documentId: String,
    ): EmptyResult<ChatError> {
        lastUnlinked = sessionId to documentId
        return unlinkResult
    }

    override suspend fun getDocumentStorage(): Result<DocumentStorage, ChatError> =
        Result.Success(DocumentStorage(0, 100, 100))

    override suspend fun listLibraryDocuments(
        query: String?,
        cursor: String?,
        projectId: String?,
    ): Result<LibraryDocumentPage, ChatError> = Result.Success(LibraryDocumentPage(emptyList(), null))

    override suspend fun linkDocuments(
        sessionId: String,
        documentIds: List<String>,
    ): Result<List<SessionDocument>, ChatError> = sessionDocuments

    override suspend fun uploadDocument(sessionId: String, file: ChatUpload): Result<DocumentIngest, ChatError> {
        lastUpload = Triple(sessionId, file, false)
        return uploadDocumentResult
    }

    override suspend fun getDocumentStatus(
        sessionId: String,
        documentId: String,
    ): Result<DocumentIngest, ChatError> = uploadDocumentResult

    override suspend fun uploadImage(sessionId: String, file: ChatUpload): Result<SessionImage, ChatError> {
        lastUpload = Triple(sessionId, file, true)
        return uploadImageResult
    }

    override suspend fun listSessionImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        Result.Success(emptyList())

    override suspend fun listPinnedImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        Result.Success(emptyList())

    override suspend fun pinImage(sessionId: String, imageId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun unpinImage(sessionId: String, imageId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun loadImageBytes(imageId: String): Result<ByteArray, ChatError> = Result.Success(ByteArray(0))

    override suspend fun loadContextSnippet(sessionId: String): Result<ContextSnippet?, ChatError> =
        Result.Success(contextSnippet)

    override suspend fun saveContextSnippet(
        sessionId: String,
        text: String,
        sourceRole: String,
    ): Result<ContextSnippet, ChatError> {
        val snippet = ContextSnippet("snippet", text, sourceRole)
        contextSnippet = snippet
        return Result.Success(snippet)
    }

    override suspend fun clearContextSnippet(
        sessionId: String,
        snippetId: String,
    ): EmptyResult<ChatError> {
        contextSnippet = null
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

    override suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError> = recentProjects
}
