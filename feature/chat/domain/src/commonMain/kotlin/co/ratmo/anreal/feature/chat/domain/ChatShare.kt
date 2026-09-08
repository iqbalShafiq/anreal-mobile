package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage

data class ChatShareLink(
    val token: String,
    val urlPath: String,
    val sessionId: String,
    val title: String?,
    val createdAt: String?,
)

data class ChatShareStatus(
    val sessionId: String,
    val active: Boolean,
)

data class ChatShareDeactivation(
    val sessionId: String,
    val revoked: Int,
)

data class PublicShareSnapshot(
    val token: String,
    val title: String?,
    val createdAt: String?,
    val ownerName: String?,
    val messages: List<ChatMessage>,
)

data class ForkSeed(
    val sessionId: String,
    val forkedFromToken: String,
    val forkedFromTitle: String,
    val messages: List<ChatMessage>,
    val firstMessage: String,
)

data class ForkResult(
    val sessionId: String,
    val seededMessages: Int,
)

const val MAX_FORK_MESSAGES = 40
const val MAX_FORK_MESSAGE_CHARS = 32_000

fun ForkSeed.validate(): EmptyResult<ChatError> {
    if (forkedFromToken.isBlank()) return Result.Error(ChatError.InvalidForkRequest)
    if (messages.size > MAX_FORK_MESSAGES) return Result.Error(ChatError.InvalidForkRequest)
    val trimmedFirst = firstMessage.trim()
    if (trimmedFirst.isEmpty() || trimmedFirst.length > MAX_FORK_MESSAGE_CHARS) {
        return Result.Error(ChatError.InvalidForkRequest)
    }
    return Result.Success(Unit)
}
