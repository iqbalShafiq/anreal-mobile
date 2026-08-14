package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.database.MessageDao
import co.ratmo.anreal.core.database.SessionDao
import co.ratmo.anreal.core.database.toEntity
import co.ratmo.anreal.core.database.toSession
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatLocalDataSource(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
) {
    fun observeSessions(): Flow<List<ChatSession>> {
        return sessionDao.observeSessions().map { rows -> rows.map { it.toSession() } }
    }

    suspend fun replaceSessions(sessions: List<ChatSession>) {
        sessionDao.upsert(sessions.map { it.toEntity() })
    }

    suspend fun upsertSession(session: ChatSession) {
        sessionDao.upsert(listOf(session.toEntity()))
    }

    suspend fun deleteSession(sessionId: String) {
        messageDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }

    suspend fun saveResume(sessionId: String, streamId: String?, lastEventId: Int) {
        sessionDao.updateResume(sessionId, streamId, lastEventId)
    }

    suspend fun replaceMessages(sessionId: String, messages: List<ChatMessage>) {
        messageDao.deleteForSession(sessionId)
        messageDao.upsert(
            messages.mapIndexed { index, message -> message.toEntity(sessionId, index) },
        )
    }

    suspend fun loadMessages(sessionId: String): List<ChatMessage> {
        return messageDao.getMessages(sessionId).map { it.toMessage() }
    }
}
