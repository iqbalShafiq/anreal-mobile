package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.database.MessageDao
import co.ratmo.anreal.core.database.QueuedItemDao
import co.ratmo.anreal.core.database.QueuedItemEntity
import co.ratmo.anreal.core.database.SessionDao
import co.ratmo.anreal.core.database.toEntity
import co.ratmo.anreal.core.database.toSession
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.feature.chat.domain.queue.QueueStatus
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.queue.restoreQueue
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class RoomChatLocalDataSource(
    private val sessionDao: SessionDao,
    private val messageDao: MessageDao,
    private val queuedItemDao: QueuedItemDao,
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
        queuedItemDao.deleteForSession(sessionId)
        messageDao.deleteForSession(sessionId)
        sessionDao.delete(sessionId)
    }

    suspend fun loadQueue(sessionId: String): List<QueuedItem> {
        return restoreQueue(
            queuedItemDao.getForSession(sessionId).map { entity ->
                QueuedItem(
                    id = entity.id,
                    text = entity.text,
                    status = when (entity.status) {
                        "Inflight" -> QueueStatus.Inflight
                        "Editing" -> QueueStatus.Editing
                        else -> QueueStatus.Pending
                    },
                )
            },
        )
    }

    suspend fun replaceQueue(sessionId: String, items: List<QueuedItem>) {
        queuedItemDao.deleteForSession(sessionId)
        queuedItemDao.upsert(
            items.mapIndexed { index, item ->
                QueuedItemEntity(
                    sessionId = sessionId,
                    id = item.id,
                    text = item.text,
                    status = item.status.name,
                    position = index,
                )
            },
        )
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
