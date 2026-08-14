package co.ratmo.anreal.feature.chat.domain.stream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

private val streamJson = Json { ignoreUnknownKeys = true }

fun parseStreamLines(payload: String): List<StreamEnvelope> {
    return payload.lineSequence().mapNotNull(::parseStreamLine).toList()
}

fun parseStreamLine(line: String): StreamEnvelope? {
    val trimmed = line.trim()
    if (trimmed.isEmpty()) return null
    val root = runCatching { streamJson.parseToJsonElement(trimmed).jsonObject }.getOrNull()
        ?: return null
    val type = root.string("type") ?: return null
    val streamId = root.string("streamId") ?: return null
    return when (type) {
        "stream_start" -> StreamEnvelope.Start(streamId = streamId)
        "stream_end" -> StreamEnvelope.End(
            streamId = streamId,
            eventId = root.int("eventId") ?: return null,
            status = parseEndStatus(root.string("status")),
        )
        "stream_event" -> StreamEnvelope.Event(
            streamId = streamId,
            eventId = root.int("eventId") ?: return null,
            event = parseInnerEvent(root["event"]?.jsonObject ?: return null),
        )
        else -> null
    }
}

private fun parseInnerEvent(event: JsonObject): ChatStreamEvent {
    return when (val type = event.string("type")) {
        "message_start" -> {
            val message = event["message"]?.jsonObject
            val id = message?.string("id")
            if (id == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.MessageStart(
                    message = ChatMessage(
                        id = id,
                        role = parseRole(message.string("role")),
                        parts = parseParts(message),
                    ),
                )
            }
        }
        "text_delta" -> {
            val messageId = event.string("messageId")
            val partId = event.string("partId")
            val delta = event.string("delta")
            if (messageId == null || partId == null || delta == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.TextDelta(messageId = messageId, partId = partId, delta = delta)
            }
        }
        "reasoning_delta" -> {
            val messageId = event.string("messageId")
            val partId = event.string("partId")
            val delta = event.string("delta")
            if (messageId == null || partId == null || delta == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.ReasoningDelta(
                    messageId = messageId,
                    partId = partId,
                    delta = delta,
                )
            }
        }
        "tool_update" -> {
            val messageId = event.string("messageId")
            val part = event["part"]?.jsonObject
            val partId = part?.string("id") ?: event.string("partId")
            val toolName = part?.string("toolName")
            val toolCallId = part?.string("toolCallId")
            val state = part?.string("state") ?: "input-streaming"
            if (messageId == null || partId == null || toolName == null || toolCallId == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.ToolUpdate(
                    messageId = messageId,
                    part = ChatPart.Tool(
                        id = partId,
                        toolName = toolName,
                        toolCallId = toolCallId,
                        state = state,
                    ),
                )
            }
        }
        "message_end" -> {
            val messageId = event.string("messageId")
            if (messageId == null) ChatStreamEvent.Unknown(type = type)
            else ChatStreamEvent.MessageEnd(messageId = messageId)
        }
        "queued_message_applied" -> {
            val clientMessageId = event.string("clientMessageId")
            if (clientMessageId == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.QueuedMessageApplied(
                    clientMessageId = clientMessageId,
                    text = event.string("text").orEmpty(),
                )
            }
        }
        "error" -> {
            val message = event["error"]?.let { error ->
                if (error is JsonObject) error.string("message") else error.jsonPrimitive.content
            } ?: event.string("message")
            ChatStreamEvent.Error(message = message ?: "Something went wrong. Try again.")
        }
        else -> ChatStreamEvent.Unknown(type = type ?: "unknown")
    }
}

private fun parseParts(message: JsonObject): List<ChatPart> {
    val parts = message["parts"] as? JsonArray ?: return emptyList()
    return parts.mapNotNull { element ->
        val part = element as? JsonObject ?: return@mapNotNull null
        val id = part.string("id") ?: return@mapNotNull null
        when (part.string("type")) {
            "text" -> ChatPart.Text(id = id, text = part.string("text").orEmpty())
            "reasoning" -> ChatPart.Reasoning(id = id, text = part.string("text").orEmpty())
            "tool" -> ChatPart.Tool(
                id = id,
                toolName = part.string("toolName").orEmpty(),
                toolCallId = part.string("toolCallId").orEmpty(),
                state = part.string("state") ?: "input-streaming",
            )
            else -> null
        }
    }
}

private fun parseRole(raw: String?): ChatRole {
    return when (raw) {
        "user" -> ChatRole.User
        "system" -> ChatRole.System
        "tool" -> ChatRole.Tool
        else -> ChatRole.Assistant
    }
}

private fun parseEndStatus(raw: String?): StreamEndStatus {
    return when (raw) {
        "error" -> StreamEndStatus.Error
        "running" -> StreamEndStatus.Running
        "missing" -> StreamEndStatus.Missing
        else -> StreamEndStatus.Completed
    }
}

private fun JsonObject.string(key: String): String? {
    return this[key]?.jsonPrimitive?.content
}

private fun JsonObject.int(key: String): Int? {
    return this[key]?.jsonPrimitive?.intOrNull
}
