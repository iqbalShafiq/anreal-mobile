package co.ratmo.anreal.feature.chat.domain.stream

import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SiteVersionEntry
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
        val input: String? = null,
        val output: String? = null,
        val errorMessage: String? = null,
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

enum class InteractionKind {
    ToolApproval,
    ToolQuestion,
}

data class NativeInteraction(
    val id: String,
    val toolName: String,
    val kind: InteractionKind,
    val reason: String? = null,
    val argumentsJson: String = "",
    val questions: List<InteractionQuestion> = emptyList(),
)

data class InteractionQuestion(
    val id: String,
    val text: String,
    val choices: List<InteractionChoice> = emptyList(),
    val allowCustom: Boolean = false,
)

data class InteractionChoice(
    val label: String,
    val value: String,
)

data class QuestionAnswer(
    val questionId: String,
    val value: String,
)

sealed interface InteractionResponse {
    data class ToolApproval(
        val approved: Boolean,
        val reason: String? = null,
    ) : InteractionResponse

    data class ToolQuestion(
        val answers: List<QuestionAnswer>,
    ) : InteractionResponse
}

enum class InteractionAvailability {
    Pending,
    Unavailable,
}

data class ImageOverrideArgs(
    val modelId: String? = null,
    val aspectRatio: String? = null,
    val quality: String? = null,
    val background: String? = null,
    val imageCount: Int? = null,
)

enum class SessionGrant {
    Session,
}

data class ImageGenSettings(
    val modelId: String,
    val aspectRatio: String? = null,
    val quality: String? = null,
    val background: String? = null,
    val imageCount: Int? = null,
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
    val pendingInteractions: List<NativeInteraction> = emptyList(),
    val staleInteractionIds: Set<String> = emptySet(),
    val deepResearch: DeepResearchStatus? = null,
    val siteBuild: SiteBuildState? = null,
    val siteVersions: List<SiteVersionEntry> = emptyList(),
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
    data class InteractionRequested(val interaction: NativeInteraction) : ChatStreamEvent
    data class InteractionResolved(val id: String) : ChatStreamEvent
    data class InteractionMarkedStale(val id: String) : ChatStreamEvent
    data class DeepResearchProgress(val status: DeepResearchStatus) : ChatStreamEvent
    data class Compaction(val phase: String) : ChatStreamEvent

    data class SiteBuildProgress(
        val siteId: String,
        val version: Int,
        val phase: SiteBuildPhase,
        val message: String,
    ) : ChatStreamEvent

    data class SiteBuildReady(
        val siteId: String,
        val version: Int,
        val previewUrl: String?,
        val downloadUrl: String,
    ) : ChatStreamEvent

    data class Unknown(
        val type: String,
    ) : ChatStreamEvent
}

enum class DeepResearchPhase {
    Planning,
    Researching,
    Synthesizing,
    Completed,
    Failed,
}

data class DeepResearchStatus(
    val phase: DeepResearchPhase,
    val message: String = "",
)
