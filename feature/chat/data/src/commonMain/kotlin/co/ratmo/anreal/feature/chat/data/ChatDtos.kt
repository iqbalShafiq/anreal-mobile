package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.serialization.Serializable

@Serializable
data class SessionListPageDto(
    val items: List<SessionListItemDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class SessionListItemDto(
    val sessionId: String,
    val title: String,
    val updatedAt: String,
    val projectId: String? = null,
    val unread: Boolean = false,
)

@Serializable
data class SessionMutationDto(
    val sessionId: String,
    val title: String? = null,
    val projectId: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null,
)

@Serializable
data class SessionTitleDto(
    val title: String,
)

@Serializable
data class DraftRequestDto(
    val projectId: String? = null,
)

@Serializable
data class MarkReadDto(
    val sessionId: String,
)

@Serializable
data class StopRunDto(
    val streamId: String,
)

@Serializable
data class RunStatusDto(
    val streamId: String? = null,
    val status: String = "idle",
    val lastEventId: Int? = null,
)

@Serializable
data class ChatRequestDto(
    val sessionId: String,
    val messages: List<HistoryMessageDto>,
    val stream: Boolean = true,
    val resume: ResumeDto? = null,
)

@Serializable
data class ResumeDto(
    val streamId: String,
    val after: Int,
)

@Serializable
data class HistoryMessageDto(
    val role: String,
    val content: List<HistoryContentDto> = emptyList(),
)

@Serializable
data class HistoryContentDto(
    val type: String,
    val text: String? = null,
)

fun SessionListItemDto.toSession(): ChatSession = ChatSession(
    id = sessionId,
    title = title,
    updatedAt = updatedAt,
    projectId = projectId,
    unread = unread,
)

fun SessionMutationDto.toSession(): ChatSession = ChatSession(
    id = sessionId,
    title = title ?: "New chat",
    updatedAt = updatedAt ?: createdAt.orEmpty(),
    projectId = projectId,
)

fun HistoryMessageDto.toMessage(index: Int): ChatMessage {
    val text = content.filter { it.type == "text" }.mapNotNull { it.text }.joinToString("")
    return ChatMessage(
        id = "history-$index",
        role = when (role) {
            "user" -> ChatRole.User
            "system" -> ChatRole.System
            "tool" -> ChatRole.Tool
            else -> ChatRole.Assistant
        },
        parts = if (text.isEmpty()) emptyList() else listOf(ChatPart.Text(id = "history-$index-text", text = text)),
        isComplete = true,
    )
}

fun ChatMessage.toHistoryDto(): HistoryMessageDto {
    val text = parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
    return HistoryMessageDto(
        role = when (role) {
            ChatRole.User -> "user"
            ChatRole.System -> "system"
            ChatRole.Tool -> "tool"
            ChatRole.Assistant -> "assistant"
        },
        content = listOf(HistoryContentDto(type = "text", text = text)),
    )
}
