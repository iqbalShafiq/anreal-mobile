package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.feature.chat.domain.ChatShareDeactivation
import co.ratmo.anreal.feature.chat.domain.ChatShareLink
import co.ratmo.anreal.feature.chat.domain.ChatShareStatus
import co.ratmo.anreal.feature.chat.domain.ForkResult
import co.ratmo.anreal.feature.chat.domain.ForkSeed
import co.ratmo.anreal.feature.chat.domain.MAX_FORK_MESSAGE_CHARS
import co.ratmo.anreal.feature.chat.domain.PublicShareSnapshot
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

@Serializable
data class ShareLinkDto(
    val token: String,
    val urlPath: String,
    val sessionId: String,
    val title: String? = null,
    val createdAt: String? = null,
)

@Serializable
data class ShareStatusDto(
    val sessionId: String,
    val active: Boolean = false,
)

@Serializable
data class DeactivateSharesDto(
    val sessionId: String,
    val revoked: Int = 0,
)

@Serializable
data class ForkFromDto(
    val token: String,
    val title: String,
)

@Serializable
data class ForkMessageDto(
    val id: String? = null,
    val role: String,
    val content: JsonElement? = null,
    val metadata: JsonObject? = null,
)

@Serializable
data class ForkRequestDto(
    val sessionId: String,
    val forkedFrom: ForkFromDto,
    val messages: List<ForkMessageDto> = emptyList(),
    val firstMessage: String,
)

@Serializable
data class ForkResponseDto(
    val sessionId: String,
    val seededMessages: Int = 0,
)

@Serializable
data class PublicShareDto(
    val token: String,
    val title: String? = null,
    val createdAt: String? = null,
    val ownerName: String? = null,
    val messages: JsonElement? = null,
)

fun ShareLinkDto.toShareLink(): ChatShareLink = ChatShareLink(
    token = token,
    urlPath = urlPath,
    sessionId = sessionId,
    title = title,
    createdAt = createdAt,
)

fun ShareStatusDto.toShareStatus(): ChatShareStatus = ChatShareStatus(
    sessionId = sessionId,
    active = active,
)

fun DeactivateSharesDto.toDeactivation(): ChatShareDeactivation = ChatShareDeactivation(
    sessionId = sessionId,
    revoked = revoked,
)

fun ForkResponseDto.toForkResult(): ForkResult = ForkResult(
    sessionId = sessionId,
    seededMessages = seededMessages,
)

fun ForkSeed.toRequestDto(): ForkRequestDto = ForkRequestDto(
    sessionId = sessionId,
    forkedFrom = ForkFromDto(token = forkedFromToken, title = forkedFromTitle),
    messages = messages.map { message -> message.toForkMessageDto() },
    firstMessage = firstMessage.trim(),
)

private fun ChatMessage.toForkMessageDto(): ForkMessageDto {
    val text = parts.filterIsInstance<ChatPart.Text>()
        .joinToString("") { it.text }
        .take(MAX_FORK_MESSAGE_CHARS)
    return ForkMessageDto(
        role = when (role) {
            ChatRole.User -> "user"
            else -> "assistant"
        },
        content = JsonPrimitive(text),
    )
}

fun PublicShareDto.toSnapshot(): PublicShareSnapshot = PublicShareSnapshot(
    token = token,
    title = title,
    createdAt = createdAt,
    ownerName = ownerName,
    messages = messages.toSnapshotMessages(),
)

private fun JsonElement?.toSnapshotMessages(): List<ChatMessage> {
    val array = this as? JsonArray ?: return emptyList()
    return array.mapIndexedNotNull { index, element ->
        val dto = runCatching {
            snapshotJson.decodeFromJsonElement(HistoryMessageDto.serializer(), element)
        }.getOrNull() ?: return@mapIndexedNotNull null
        dto.toMessage(index).copy(id = "share-$index")
    }.mergeToolResultMessages()
}

private val snapshotJson = Json { ignoreUnknownKeys = true }
