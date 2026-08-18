package co.ratmo.anreal.feature.chat.domain.stream

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

enum class ChatRole {
    User,
    Assistant,
    System,
    Tool,
}

@Serializable
sealed interface ChatPart {
    val id: String

    @Serializable
    @SerialName("text")
    data class Text(
        override val id: String,
        val text: String,
    ) : ChatPart

    @Serializable
    @SerialName("reasoning")
    data class Reasoning(
        override val id: String,
        val text: String,
    ) : ChatPart

    @Serializable
    @SerialName("tool")
    data class Tool(
        override val id: String,
        val toolName: String,
        val toolCallId: String,
        val state: String,
        val output: String? = null,
    ) : ChatPart
}

data class ChatMessage(
    val id: String,
    val role: ChatRole,
    val parts: List<ChatPart> = emptyList(),
    val isComplete: Boolean = false,
    val clientMessageId: String? = null,
    val memoryPosition: Int? = null,
)

enum class RunStatus {
    Idle,
    Streaming,
    Completed,
    Failed,
}

enum class StreamEndStatus {
    Completed,
    Error,
    Running,
    Missing,
}

data class ChatThreadState(
    val streamId: String? = null,
    val lastEventId: Int = 0,
    val status: RunStatus = RunStatus.Idle,
    val messages: List<ChatMessage> = emptyList(),
    val error: String? = null,
    val pendingApprovals: List<ToolApproval> = emptyList(),
    val pendingClarifications: List<Clarification> = emptyList(),
)

data class ToolApproval(
    val id: String,
    val toolName: String,
    val reason: String?,
    val arguments: String,
)

data class ClarificationOption(
    val id: String,
    val label: String,
    val recommended: Boolean,
)

data class ClarificationQuestion(
    val id: String,
    val question: String,
    val type: String,
    val options: List<ClarificationOption>,
    val optional: Boolean,
    val placeholder: String?,
)

data class Clarification(
    val id: String,
    val title: String?,
    val questions: List<ClarificationQuestion>,
)

sealed interface StreamEnvelope {
    data class Start(
        val streamId: String,
    ) : StreamEnvelope

    data class Event(
        val streamId: String,
        val eventId: Int,
        val event: ChatStreamEvent,
    ) : StreamEnvelope

    data class End(
        val streamId: String,
        val eventId: Int,
        val status: StreamEndStatus,
    ) : StreamEnvelope
}

sealed interface ChatStreamEvent {
    data class MessageStart(
        val message: ChatMessage,
    ) : ChatStreamEvent

    data class TextDelta(
        val messageId: String,
        val partId: String,
        val delta: String,
    ) : ChatStreamEvent

    data class ReasoningDelta(
        val messageId: String,
        val partId: String,
        val delta: String,
    ) : ChatStreamEvent

    data class ToolUpdate(
        val messageId: String,
        val part: ChatPart.Tool,
    ) : ChatStreamEvent

    data class MessageEnd(
        val messageId: String,
    ) : ChatStreamEvent

    data class Error(
        val message: String,
    ) : ChatStreamEvent

    data class QueuedMessageApplied(
        val clientMessageId: String,
        val text: String,
    ) : ChatStreamEvent

    data class ApprovalRequested(val approval: ToolApproval) : ChatStreamEvent
    data class ApprovalResolved(val id: String) : ChatStreamEvent
    data class ClarificationRequested(val clarification: Clarification) : ChatStreamEvent
    data class ClarificationResolved(val id: String) : ChatStreamEvent
    data class Compaction(val phase: String) : ChatStreamEvent

    data class Unknown(
        val type: String,
    ) : ChatStreamEvent
}
