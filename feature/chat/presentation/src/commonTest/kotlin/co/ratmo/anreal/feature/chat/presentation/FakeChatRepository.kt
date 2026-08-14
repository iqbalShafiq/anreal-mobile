package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRepository
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow

class FakeChatRepository : ChatRepository {
    val sessions = MutableStateFlow<List<ChatSession>>(emptyList())
    var refreshResult: Result<SessionPage, ChatError> = Result.Success(SessionPage(emptyList()))
    var draft: ChatSession = ChatSession(id = "draft", title = "New chat", updatedAt = "now")
    var history: Result<List<ChatMessage>, ChatError> = Result.Success(emptyList())
    var sendResult: EmptyResult<ChatError> = Result.Success(Unit)
    var sentText: String? = null
    var runStatus: Result<RunStatusSnapshot, ChatError> = Result.Success(
        RunStatusSnapshot(streamId = null, status = "idle", lastEventId = null),
    )

    override fun observeSessions(): Flow<List<ChatSession>> = sessions

    override suspend fun refreshSessions(): Result<SessionPage, ChatError> {
        when (val result = refreshResult) {
            is Result.Success -> sessions.value = result.data.items
            is Result.Error -> Unit
        }
        return refreshResult
    }

    override suspend fun openDraft(): Result<ChatSession, ChatError> {
        sessions.value = listOf(draft) + sessions.value.filterNot { it.id == draft.id }
        return Result.Success(draft)
    }

    override suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError> {
        val updated = ChatSession(id = sessionId, title = title, updatedAt = "now")
        sessions.value = sessions.value.map { if (it.id == sessionId) updated else it }
        return Result.Success(updated)
    }

    override suspend fun deleteSession(sessionId: String): EmptyResult<ChatError> {
        sessions.value = sessions.value.filterNot { it.id == sessionId }
        return Result.Success(Unit)
    }

    override suspend fun markRead(sessionId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError> = history

    override suspend fun sendMessage(
        sessionId: String,
        text: String,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        sentText = text
        return sendResult
    }

    override suspend fun resume(
        sessionId: String,
        streamId: String,
        after: Int,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun stop(streamId: String): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError> = runStatus

    override suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) = Unit
}
