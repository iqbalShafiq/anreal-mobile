package co.ratmo.anreal.core.database

import androidx.room3.Dao
import androidx.room3.Entity
import androidx.room3.Query
import androidx.room3.Upsert

@Entity(tableName = "queued_items", primaryKeys = ["sessionId", "id"])
data class QueuedItemEntity(
    val sessionId: String,
    val id: String,
    val text: String,
    val status: String,
    val position: Int,
)

@Dao
interface QueuedItemDao {
    @Query("SELECT * FROM queued_items WHERE sessionId = :sessionId ORDER BY position ASC")
    suspend fun getForSession(sessionId: String): List<QueuedItemEntity>

    @Upsert
    suspend fun upsert(items: List<QueuedItemEntity>)

    @Query("DELETE FROM queued_items WHERE sessionId = :sessionId")
    suspend fun deleteForSession(sessionId: String)
}
