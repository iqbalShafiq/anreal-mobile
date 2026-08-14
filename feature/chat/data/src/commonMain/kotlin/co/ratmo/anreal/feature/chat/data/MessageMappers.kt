package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.database.MessageEntity
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.serialization.json.Json

private val partsJsonFormat = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

fun ChatMessage.toEntity(sessionId: String, position: Int): MessageEntity {
    val text = parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
    return MessageEntity(
        sessionId = sessionId,
        id = id,
        role = role.name.lowercase(),
        text = text,
        isComplete = isComplete,
        position = position,
        partsJson = partsJsonFormat.encodeToString(parts),
    )
}

fun MessageEntity.toMessage(): ChatMessage {
    val decoded = if (partsJson.isNotBlank()) {
        runCatching { partsJsonFormat.decodeFromString<List<ChatPart>>(partsJson) }.getOrNull()
    } else {
        null
    }
    val parts = decoded ?: if (text.isEmpty()) {
        emptyList()
    } else {
        listOf(ChatPart.Text(id = "$id-text", text = text))
    }
    return ChatMessage(
        id = id,
        role = when (role) {
            "user" -> ChatRole.User
            "system" -> ChatRole.System
            "tool" -> ChatRole.Tool
            else -> ChatRole.Assistant
        },
        parts = parts,
        isComplete = isComplete,
    )
}
