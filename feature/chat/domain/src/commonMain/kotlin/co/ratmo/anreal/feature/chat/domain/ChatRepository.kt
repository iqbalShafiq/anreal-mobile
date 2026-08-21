package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.flow.Flow

data class SessionPage(
    val items: List<ChatSession>,
    val nextCursor: String? = null,
)

data class RunStatusSnapshot(
    val streamId: String?,
    val status: String,
    val lastEventId: Int?,
)

data class ActiveRun(
    val sessionId: String,
    val streamId: String,
    val status: String,
    val lastEventId: Int,
)

data class ContextUsage(
    val modelId: String,
    val modelLabel: String,
    val contextWindowTokens: Int,
    val estimatedTokens: Int,
    val ratio: Double,
    val thresholdRatio: Double,
    val targetRatio: Double,
    val reasoningEffort: String?,
)

data class DocumentStorage(
    val usedBytes: Long,
    val maxBytes: Long,
    val remainingBytes: Long,
)

data class LibraryDocument(
    val id: String,
    val filename: String,
    val summary: String,
    val sizeBytes: Long,
    val pageCount: Int,
)

data class LibraryDocumentPage(
    val items: List<LibraryDocument>,
    val nextCursor: String?,
)

data class DocumentIngest(
    val id: String,
    val filename: String,
    val status: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val errorMessage: String?,
    val summary: String?,
)

data class SessionImage(
    val id: String,
    val prompt: String,
    val mediaType: String,
    val width: Int,
    val height: Int,
    val modelId: String,
    val isPinned: Boolean = false,
    val bytes: ByteArray? = null,
)

data class ChatUpload(
    val filename: String,
    val mediaType: String,
    val bytes: ByteArray,
)

data class ContextSnippet(
    val id: String,
    val text: String,
    val sourceRole: String,
)

interface ChatRepository {
    fun observeSessions(projectId: String? = null): Flow<List<ChatSession>>
    suspend fun refreshSessions(
        cursor: String? = null,
        projectId: String? = null,
    ): Result<SessionPage, ChatError>
    suspend fun createSession(sessionId: String? = null, projectId: String? = null): Result<ChatSession, ChatError>
    suspend fun openDraft(projectId: String? = null): Result<ChatSession, ChatError>
    suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError>
    suspend fun deleteSession(sessionId: String): EmptyResult<ChatError>
    suspend fun markRead(sessionId: String): EmptyResult<ChatError>
    suspend fun loadCachedHistory(sessionId: String): HistoryWindow
    suspend fun cacheHistory(
        sessionId: String,
        messages: List<ChatMessage>,
        startPosition: Int = 0,
    )
    suspend fun loadHistory(sessionId: String): Result<HistoryWindow, ChatError>
    suspend fun loadOlderHistory(sessionId: String, beforePosition: Int): HistoryWindow
    suspend fun trimCachedHistory(sessionId: String, keepCount: Int)
    suspend fun sendMessage(
        sessionId: String,
        text: String,
        clientMessageId: String? = null,
        options: ChatRunOptions = ChatRunOptions(),
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError>
    suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError>
    suspend fun syncQueue(sessionId: String, ids: List<String>): Result<List<String>, ChatError>
    suspend fun loadQueue(sessionId: String): List<QueuedItem>
    suspend fun replaceQueue(sessionId: String, items: List<QueuedItem>)
    suspend fun resume(
        sessionId: String,
        streamId: String,
        after: Int,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError>
    suspend fun stop(streamId: String): EmptyResult<ChatError>
    suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError>
    suspend fun listActiveRuns(): Result<List<ActiveRun>, ChatError>
    suspend fun getSessionMessageCount(sessionId: String): Result<Int, ChatError>
    suspend fun getContextUsage(
        sessionId: String,
        model: String?,
        reasoningEffort: String?,
    ): Result<ContextUsage, ChatError>
    suspend fun truncateSession(
        sessionId: String,
        mode: String,
        clientMessageId: String?,
        memoryPosition: Int?,
    ): EmptyResult<ChatError>
    suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int)
    suspend fun loadCatalog(): Result<ModelCatalog, ChatError>
    suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError>
    suspend fun listSessionDocuments(sessionId: String): Result<List<SessionDocument>, ChatError>
    suspend fun unlinkSessionDocument(sessionId: String, documentId: String): EmptyResult<ChatError>
    suspend fun getDocumentStorage(): Result<DocumentStorage, ChatError>
    suspend fun listLibraryDocuments(
        query: String? = null,
        cursor: String? = null,
        projectId: String? = null,
    ): Result<LibraryDocumentPage, ChatError>
    suspend fun linkDocuments(sessionId: String, documentIds: List<String>): Result<List<SessionDocument>, ChatError>
    suspend fun uploadDocument(sessionId: String, file: ChatUpload): Result<DocumentIngest, ChatError>
    suspend fun getDocumentStatus(sessionId: String, documentId: String): Result<DocumentIngest, ChatError>
    suspend fun uploadImage(sessionId: String, file: ChatUpload): Result<SessionImage, ChatError>
    suspend fun listSessionImages(sessionId: String): Result<List<SessionImage>, ChatError>
    suspend fun listPinnedImages(sessionId: String): Result<List<SessionImage>, ChatError>
    suspend fun pinImage(sessionId: String, imageId: String): EmptyResult<ChatError>
    suspend fun unpinImage(sessionId: String, imageId: String): EmptyResult<ChatError>
    suspend fun loadImageBytes(imageId: String): Result<ByteArray, ChatError>
    suspend fun loadContextSnippet(sessionId: String): Result<ContextSnippet?, ChatError>
    suspend fun saveContextSnippet(
        sessionId: String,
        text: String,
        sourceRole: String,
    ): Result<ContextSnippet, ChatError>
    suspend fun clearContextSnippet(sessionId: String, snippetId: String): EmptyResult<ChatError>
    suspend fun decideApproval(approvalId: String, approved: Boolean): EmptyResult<ChatError>
    suspend fun respondClarification(
        clarificationId: String,
        answers: Map<String, List<String>>,
        skipped: List<String>,
    ): EmptyResult<ChatError>
    suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError>
    suspend fun openProject(id: String): Result<RecentProject, ChatError>
}
