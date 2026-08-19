package co.ratmo.anreal.feature.chat.domain.stream

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.booleanOrNull
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
            val delta = event.string("delta")
            val messageId = event.string("messageId") ?: turnMessageId(event)
            val partId = event.string("partId") ?: messageId?.let { "$it-text" }
            if (messageId == null || partId == null || delta == null) {
                ChatStreamEvent.Unknown(type = type)
            } else {
                ChatStreamEvent.TextDelta(messageId = messageId, partId = partId, delta = delta)
            }
        }
        "reasoning_delta" -> {
            val delta = event.string("delta")
            val messageId = event.string("messageId") ?: turnMessageId(event)
            val partId = event.string("partId") ?: event.string("id")
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
            val messageId = event.string("messageId") ?: turnMessageId(event)
            val part = event["part"]?.jsonObject
            val partId = part?.string("id") ?: event.string("partId") ?: event.string("id")
            val toolName = part?.string("toolName") ?: event.string("toolName")
            val toolCallId = part?.string("toolCallId") ?: event.string("toolCallId")
            val state = part?.string("state") ?: event.string("state") ?: "input-streaming"
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
                        input = part?.jsonValue("input") ?: event.jsonValue("input"),
                        output = part?.jsonValue("output") ?: event.jsonValue("output"),
                        errorMessage = part?.errorMessage() ?: event.errorMessage(),
                    ),
                )
            }
        }
        "message_end" -> {
            val messageId = event.string("messageId")
            if (messageId == null) ChatStreamEvent.Unknown(type = type)
            else ChatStreamEvent.MessageEnd(messageId = messageId)
        }
        "turn_end" -> {
            val turn = event.int("turn")
            if (turn == null) ChatStreamEvent.Unknown(type = type)
            else ChatStreamEvent.MessageEnd(messageId = "turn-$turn")
        }
        "turn_start", "generation_start", "final" -> ChatStreamEvent.Unknown(type = type)
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
        "tool_approval_request" -> parseApprovalRequest(event, type)
        "tool_approval_result" -> parseResolvedId(event, "approval")?.let {
            ChatStreamEvent.ApprovalResolved(it)
        } ?: ChatStreamEvent.Unknown(type)
        "clarification_request" -> parseClarificationRequest(event, type)
        "clarification_response" -> parseResolvedId(event, "clarification")?.let {
            ChatStreamEvent.ClarificationResolved(it)
        } ?: ChatStreamEvent.Unknown(type)
        "compaction" -> ChatStreamEvent.Compaction(event.string("phase") ?: "unknown")
        else -> ChatStreamEvent.Unknown(type = type ?: "unknown")
    }
}

private fun parseApprovalRequest(event: JsonObject, type: String): ChatStreamEvent {
    val approval = event["approval"] as? JsonObject ?: return ChatStreamEvent.Unknown(type)
    val id = approval.string("id") ?: return ChatStreamEvent.Unknown(type)
    val toolName = approval.string("toolName") ?: return ChatStreamEvent.Unknown(type)
    return ChatStreamEvent.ApprovalRequested(
        ToolApproval(
            id = id,
            toolName = toolName,
            reason = approval.string("reason"),
            arguments = approval["args"]?.toString().orEmpty(),
        ),
    )
}

private fun parseClarificationRequest(event: JsonObject, type: String): ChatStreamEvent {
    val clarification = event["clarification"] as? JsonObject ?: return ChatStreamEvent.Unknown(type)
    val id = clarification.string("id") ?: return ChatStreamEvent.Unknown(type)
    val questions = (clarification["questions"] as? JsonArray).orEmpty().mapNotNull { element ->
        val question = element as? JsonObject ?: return@mapNotNull null
        val questionId = question.string("id") ?: return@mapNotNull null
        val text = question.string("question") ?: return@mapNotNull null
        val questionType = question.string("type") ?: return@mapNotNull null
        ClarificationQuestion(
            id = questionId,
            question = text,
            type = questionType,
            options = (question["options"] as? JsonArray).orEmpty().mapNotNull { optionElement ->
                val option = optionElement as? JsonObject ?: return@mapNotNull null
                ClarificationOption(
                    id = option.string("id") ?: return@mapNotNull null,
                    label = option.string("label") ?: return@mapNotNull null,
                    recommended = option["recommended"]?.jsonPrimitive?.booleanOrNull ?: false,
                )
            },
            optional = question["optional"]?.jsonPrimitive?.booleanOrNull ?: false,
            placeholder = question.string("placeholder"),
        )
    }
    if (questions.isEmpty()) return ChatStreamEvent.Unknown(type)
    return ChatStreamEvent.ClarificationRequested(
        Clarification(id = id, title = clarification.string("title"), questions = questions),
    )
}

private fun parseResolvedId(event: JsonObject, key: String): String? =
    (event[key] as? JsonObject)?.string("id")

private fun turnMessageId(event: JsonObject): String? =
    event.int("turn")?.let { "turn-$it" }

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
                input = part.jsonValue("input"),
                output = part.jsonValue("output"),
                errorMessage = part.errorMessage(),
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

private fun JsonObject.jsonValue(key: String): String? {
    val value = this[key] ?: return null
    if (value is JsonNull) return null
    val primitive = runCatching { value.jsonPrimitive }.getOrNull()
    return primitive?.content ?: value.toString()
}

private fun JsonObject.errorMessage(): String? {
    val error = this["error"] ?: return null
    if (error is JsonObject) return error.string("message")
    return runCatching { error.jsonPrimitive.content }.getOrNull()
}
