package co.ratmo.anreal.core.database

import androidx.room3.Entity
import androidx.room3.PrimaryKey
import co.ratmo.anreal.core.domain.model.ChatSession

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey val id: String,
    val title: String,
    val updatedAt: String,
    val projectId: String?,
    val unread: Boolean,
    val streamId: String?,
    val lastEventId: Int,
)

fun SessionEntity.toSession(): ChatSession = ChatSession(
    id = id,
    title = title,
    updatedAt = updatedAt,
    projectId = projectId,
    unread = unread,
    streamId = streamId,
    lastEventId = lastEventId,
)

fun ChatSession.toEntity(): SessionEntity = SessionEntity(
    id = id,
    title = title,
    updatedAt = updatedAt,
    projectId = projectId,
    unread = unread,
    streamId = streamId,
    lastEventId = lastEventId,
)
