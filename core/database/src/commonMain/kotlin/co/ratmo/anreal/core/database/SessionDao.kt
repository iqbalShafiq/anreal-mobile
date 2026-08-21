package co.ratmo.anreal.core.database

import androidx.room3.Dao
import androidx.room3.Query
import androidx.room3.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Query("SELECT * FROM sessions WHERE projectId IS NULL ORDER BY updatedAt DESC")
    fun observeStandalone(): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE projectId = :projectId ORDER BY updatedAt DESC")
    fun observeForProject(projectId: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM sessions WHERE id = :id")
    suspend fun getSession(id: String): SessionEntity?

    @Upsert
    suspend fun upsert(sessions: List<SessionEntity>)

    @Query("DELETE FROM sessions WHERE id = :id")
    suspend fun delete(id: String)

    @Query("UPDATE sessions SET streamId = :streamId, lastEventId = :lastEventId WHERE id = :id")
    suspend fun updateResume(id: String, streamId: String?, lastEventId: Int)
}

@Dao
interface MessageDao {
    @Query("SELECT * FROM messages WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun getMessages(sessionId: String): List<MessageEntity>

    @Upsert
    suspend fun upsert(messages: List<MessageEntity>)

    @Query("DELETE FROM messages WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
