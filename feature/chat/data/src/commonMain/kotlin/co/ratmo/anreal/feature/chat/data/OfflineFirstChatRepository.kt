package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
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

    override fun observeSessions(): Flow<List<ChatSession>> = local.observeSessions()

    override suspend fun refreshSessions(): Result<SessionPage, ChatError> {
        return remote.listSessions().onSuccess { page ->
            local.replaceSessions(page.items)
        }
    }

    override suspend fun openDraft(): Result<ChatSession, ChatError> {
        return remote.openDraft().onSuccess { local.upsertSession(it) }
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
            onLine = onLine,
        )
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

    override suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) {
        local.saveResume(sessionId, streamId, lastEventId)
    }
}
