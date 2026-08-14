package co.ratmo.anreal.core.database

import androidx.room3.ColumnInfo
import androidx.room3.Entity

@Entity(tableName = "messages", primaryKeys = ["sessionId", "id"])
data class MessageEntity(
    val sessionId: String,
    val id: String,
    val role: String,
    val text: String,
    val isComplete: Boolean,
    val position: Int,
    @ColumnInfo(defaultValue = "")
    val partsJson: String = "",
)
