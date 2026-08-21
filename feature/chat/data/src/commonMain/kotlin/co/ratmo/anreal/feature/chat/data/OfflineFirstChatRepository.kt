package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.onSuccess
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
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.coroutines.flow.Flow

class OfflineFirstChatRepository(
    private val remote: KtorChatRemoteDataSource,
    private val local: RoomChatLocalDataSource,
) : ChatRepository {

    override fun observeSessions(projectId: String?): Flow<List<ChatSession>> =
        local.observeSessions(projectId)

    override suspend fun refreshSessions(
        cursor: String?,
        projectId: String?,
    ): Result<SessionPage, ChatError> {
        return remote.listSessions(cursor, projectId).onSuccess { page ->
            local.replaceSessions(page.items)
        }
    }

    override suspend fun createSession(
        sessionId: String?,
        projectId: String?,
    ): Result<ChatSession, ChatError> = remote.createSession(sessionId, projectId)
        .onSuccess { local.upsertSession(it) }

    override suspend fun openDraft(projectId: String?): Result<ChatSession, ChatError> {
        return remote.openDraft(projectId).onSuccess { local.upsertSession(it) }
    }

    override suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError> {
        return remote.renameSession(sessionId, title).onSuccess { local.upsertSession(it) }
    }

    override suspend fun deleteSession(sessionId: String): EmptyResult<ChatError> {
        return remote.deleteSession(sessionId).onSuccess { local.deleteSession(sessionId) }
    }

    override suspend fun markRead(sessionId: String): EmptyResult<ChatError> {
        return remote.markRead(sessionId)
    }

    override suspend fun loadCachedHistory(sessionId: String): List<ChatMessage> {
        return local.loadMessages(sessionId)
    }

    override suspend fun cacheHistory(sessionId: String, messages: List<ChatMessage>) {
        local.replaceMessages(sessionId, messages)
    }

    override suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError> {
        return when (val remoteResult = remote.loadHistory(sessionId)) {
            is Result.Success -> {
                local.replaceMessages(sessionId, remoteResult.data)
                remoteResult
            }
            is Result.Error -> {
                val cached = local.loadMessages(sessionId)
                if (cached.isNotEmpty()) Result.Success(cached) else remoteResult
            }
        }
    }

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        clientMessageId: String?,
        options: ChatRunOptions,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        val id = clientMessageId ?: "local-user"
        val user = ChatMessage(
            id = id,
            role = ChatRole.User,
            parts = listOf(ChatPart.Text(id = "$id-text", text = text)),
            isComplete = true,
        )
        return remote.send(
            sessionId = sessionId,
            messages = listOf(user),
            clientMessageId = clientMessageId,
            options = options,
            onLine = onLine,
        )
    }

    override suspend fun loadCatalog(): Result<ModelCatalog, ChatError> = remote.loadCatalog()

    override suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError> {
        return remote.loadCapabilities()
    }

    override suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError> {
        return remote.steer(sessionId, items)
    }

    override suspend fun syncQueue(sessionId: String, ids: List<String>): Result<List<String>, ChatError> {
        return remote.syncQueue(sessionId, ids)
    }

    override suspend fun loadQueue(sessionId: String): List<QueuedItem> = local.loadQueue(sessionId)

    override suspend fun replaceQueue(sessionId: String, items: List<QueuedItem>) {
        local.replaceQueue(sessionId, items)
    }

    override suspend fun resume(
        sessionId: String,
        streamId: String,
        after: Int,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        return remote.send(
            sessionId = sessionId,
            messages = emptyList(),
            resume = ResumeDto(streamId = streamId, after = after),
            onLine = onLine,
        )
    }

    override suspend fun stop(streamId: String): EmptyResult<ChatError> = remote.stop(streamId)

    override suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError> {
        return remote.runStatus(sessionId)
    }

    override suspend fun listActiveRuns(): Result<List<ActiveRun>, ChatError> = remote.listActiveRuns()

    override suspend fun getSessionMessageCount(sessionId: String): Result<Int, ChatError> =
        remote.getSessionMessageCount(sessionId)

    override suspend fun getContextUsage(
        sessionId: String,
        model: String?,
        reasoningEffort: String?,
    ): Result<ContextUsage, ChatError> = remote.getContextUsage(sessionId, model, reasoningEffort)

    override suspend fun truncateSession(
        sessionId: String,
        mode: String,
        clientMessageId: String?,
        memoryPosition: Int?,
    ): EmptyResult<ChatError> = remote.truncateSession(sessionId, mode, clientMessageId, memoryPosition)

    override suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) {
        local.saveResume(sessionId, streamId, lastEventId)
    }

    override suspend fun listSessionDocuments(sessionId: String): Result<List<SessionDocument>, ChatError> {
        return remote.listSessionDocuments(sessionId)
    }

    override suspend fun unlinkSessionDocument(
        sessionId: String,
        documentId: String,
    ): EmptyResult<ChatError> {
        return remote.unlinkSessionDocument(sessionId, documentId)
    }

    override suspend fun getDocumentStorage(): Result<DocumentStorage, ChatError> =
        remote.getDocumentStorage()

    override suspend fun listLibraryDocuments(
        query: String?,
        cursor: String?,
        projectId: String?,
    ): Result<LibraryDocumentPage, ChatError> = remote.listLibraryDocuments(query, cursor, projectId)

    override suspend fun linkDocuments(
        sessionId: String,
        documentIds: List<String>,
    ): Result<List<SessionDocument>, ChatError> = remote.linkDocuments(sessionId, documentIds)

    override suspend fun uploadDocument(
        sessionId: String,
        file: ChatUpload,
    ): Result<DocumentIngest, ChatError> {
        return remote.uploadDocument(sessionId, file)
    }

    override suspend fun getDocumentStatus(
        sessionId: String,
        documentId: String,
    ): Result<DocumentIngest, ChatError> = remote.getDocumentStatus(sessionId, documentId)

    override suspend fun uploadImage(
        sessionId: String,
        file: ChatUpload,
    ): Result<SessionImage, ChatError> {
        return remote.uploadImage(sessionId, file)
    }

    override suspend fun listSessionImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        remote.listSessionImages(sessionId)

    override suspend fun listPinnedImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        remote.listPinnedImages(sessionId)

    override suspend fun pinImage(sessionId: String, imageId: String): EmptyResult<ChatError> =
        remote.pinImage(sessionId, imageId)

    override suspend fun unpinImage(sessionId: String, imageId: String): EmptyResult<ChatError> =
        remote.unpinImage(sessionId, imageId)

    override suspend fun loadImageBytes(imageId: String): Result<ByteArray, ChatError> =
        remote.loadImageBytes(imageId)

    override suspend fun loadContextSnippet(sessionId: String): Result<ContextSnippet?, ChatError> =
        remote.loadContextSnippet(sessionId)

    override suspend fun saveContextSnippet(
        sessionId: String,
        text: String,
        sourceRole: String,
    ): Result<ContextSnippet, ChatError> = remote.saveContextSnippet(sessionId, text, sourceRole)

    override suspend fun clearContextSnippet(
        sessionId: String,
        snippetId: String,
    ): EmptyResult<ChatError> = remote.clearContextSnippet(sessionId, snippetId)

    override suspend fun decideApproval(
        approvalId: String,
        approved: Boolean,
    ): EmptyResult<ChatError> = remote.decideApproval(approvalId, approved)

    override suspend fun respondClarification(
        clarificationId: String,
        answers: Map<String, List<String>>,
        skipped: List<String>,
    ): EmptyResult<ChatError> = remote.respondClarification(clarificationId, answers, skipped)

    override suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError> {
        return remote.listRecentProjects()
    }

    override suspend fun openProject(id: String): Result<RecentProject, ChatError> {
        return remote.openProject(id)
    }
}
