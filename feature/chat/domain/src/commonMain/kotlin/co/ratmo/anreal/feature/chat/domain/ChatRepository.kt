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

interface ChatRepository {
    fun observeSessions(): Flow<List<ChatSession>>
    suspend fun refreshSessions(): Result<SessionPage, ChatError>
    suspend fun openDraft(): Result<ChatSession, ChatError>
    suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError>
    suspend fun deleteSession(sessionId: String): EmptyResult<ChatError>
    suspend fun markRead(sessionId: String): EmptyResult<ChatError>
    suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError>
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
    suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int)
    suspend fun loadCatalog(): Result<ModelCatalog, ChatError>
    suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError>
}
