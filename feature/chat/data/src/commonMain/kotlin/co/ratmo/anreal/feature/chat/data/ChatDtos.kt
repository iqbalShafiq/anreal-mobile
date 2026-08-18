package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ActiveRun
import co.ratmo.anreal.feature.chat.domain.ContextUsage
import co.ratmo.anreal.feature.chat.domain.DocumentIngest
import co.ratmo.anreal.feature.chat.domain.DocumentStorage
import co.ratmo.anreal.feature.chat.domain.LibraryDocument
import co.ratmo.anreal.feature.chat.domain.LibraryDocumentPage
import co.ratmo.anreal.feature.chat.domain.ContextSnippet
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionImage
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
data class CreateSessionRequestDto(
    val sessionId: String? = null,
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
    val model: String? = null,
    val reasoningEffort: String? = null,
    val webSearchEnabled: Boolean = false,
    val imageGenerationEnabled: Boolean = false,
)

@Serializable
data class ModelCatalogDto(
    val models: List<ModelInfoDto> = emptyList(),
    val reasoningEfforts: List<ReasoningEffortDto> = emptyList(),
)

@Serializable
data class ModelInfoDto(
    val modelId: String,
    val label: String,
    val reasoningEfforts: List<String> = emptyList(),
    val contextWindowTokens: Int = 0,
    val outputType: String = "text",
)

@Serializable
data class ReasoningEffortDto(
    val key: String,
    val label: String,
    val description: String? = null,
)

@Serializable
data class CapabilitiesDto(
    val webSearchAvailable: Boolean = false,
    val imageGenerationAvailable: Boolean = false,
)

@Serializable
data class ResumeDto(
    val streamId: String,
    val after: Int,
)

@Serializable
data class HistoryMetadataDto(
    val clientMessageId: String? = null,
    val queued: Boolean? = null,
    val memoryPosition: Int? = null,
)

@Serializable
data class HistoryMessageDto(
    val role: String,
    val content: List<HistoryContentDto> = emptyList(),
    val metadata: HistoryMetadataDto? = null,
)

@Serializable
data class SteerMessageDto(
    val clientMessageId: String,
    val text: String,
)

@Serializable
data class SteerRequestDto(
    val sessionId: String,
    val messages: List<SteerMessageDto>,
)

@Serializable
data class SteerResponseDto(
    val ok: Boolean = true,
    val streamId: String? = null,
    val queued: Int = 0,
)

@Serializable
data class QueueSyncRequestDto(
    val sessionId: String,
    val ids: List<String>,
)

@Serializable
data class QueueSyncResponseDto(
    val appliedIds: List<String> = emptyList(),
)

@Serializable
data class HistoryContentDto(
    val type: String,
    val text: String? = null,
    val id: String? = null,
    val toolName: String? = null,
    val toolCallId: String? = null,
    val state: String? = null,
    val output: JsonElement? = null,
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
    val parts = content.mapIndexedNotNull { partIndex, dto ->
        val id = dto.id ?: "history-$index-$partIndex"
        when (dto.type) {
            "text" -> ChatPart.Text(id = id, text = dto.text.orEmpty())
            "reasoning" -> ChatPart.Reasoning(id = id, text = dto.text.orEmpty())
            "tool" -> ChatPart.Tool(
                id = id,
                toolName = dto.toolName.orEmpty(),
                toolCallId = dto.toolCallId.orEmpty(),
                state = dto.state ?: "input-streaming",
                output = dto.output?.toString(),
            )
            else -> null
        }
    }
    return ChatMessage(
        id = "history-$index",
        role = when (role) {
            "user" -> ChatRole.User
            "system" -> ChatRole.System
            "tool" -> ChatRole.Tool
            else -> ChatRole.Assistant
        },
        parts = parts,
        isComplete = true,
        clientMessageId = metadata?.clientMessageId,
        memoryPosition = metadata?.memoryPosition,
    )
}

fun ChatMessage.toHistoryDto(clientMessageId: String? = null): HistoryMessageDto {
    val text = parts.filterIsInstance<ChatPart.Text>().joinToString("") { it.text }
    return HistoryMessageDto(
        role = when (role) {
            ChatRole.User -> "user"
            ChatRole.System -> "system"
            ChatRole.Tool -> "tool"
            ChatRole.Assistant -> "assistant"
        },
        content = listOf(HistoryContentDto(type = "text", text = text)),
        metadata = clientMessageId?.let { HistoryMetadataDto(clientMessageId = it) },
    )
}

fun ModelCatalogDto.toCatalog(): ModelCatalog = ModelCatalog(
    models = models
        .filter { it.outputType != "image" }
        .map { dto ->
            ChatModel(
                id = dto.modelId,
                label = dto.label,
                reasoningEfforts = dto.reasoningEfforts,
                contextWindowTokens = dto.contextWindowTokens,
            )
        },
    efforts = reasoningEfforts.map { dto ->
        ReasoningEffort(key = dto.key, label = dto.label, description = dto.description)
    },
)

fun CapabilitiesDto.toCapabilities(): ChatCapabilities = ChatCapabilities(
    webSearchAvailable = webSearchAvailable,
    imageGenerationAvailable = imageGenerationAvailable,
)

@Serializable
data class SessionDocumentDto(
    val id: String,
    val filename: String,
    val firstPageSummary: String? = null,
)

@Serializable
data class UnlinkDocumentDto(
    val sessionId: String,
    val documentId: String,
)

@Serializable
data class DocumentUploadDto(
    val id: String,
    val filename: String,
    val status: String,
    val sizeBytes: Long,
)

@Serializable
data class DocumentStorageDto(
    val usedBytes: Long,
    val maxBytes: Long,
    val remainingBytes: Long,
)

@Serializable
data class LibraryDocumentPageDto(
    val items: List<LibraryDocumentDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class LibraryDocumentDto(
    val id: String,
    val filename: String,
    val firstPageSummary: String = "",
    val sizeBytes: Long = 0,
    val pageCount: Int = 0,
)

@Serializable
data class LinkDocumentsDto(val sessionId: String, val documentIds: List<String>)

@Serializable
data class LinkedDocumentsDto(val linked: List<SessionDocumentDto> = emptyList())

@Serializable
data class DocumentStatusDto(
    val id: String,
    val filename: String,
    val status: String,
    val pageCount: Int = 0,
    val errorMessage: String? = null,
    val firstPageSummary: String? = null,
    val sizeBytes: Long = 0,
)

@Serializable
data class ImageUploadDto(val image: ImageMetadataDto)

@Serializable
data class ImageMetadataDto(
    val id: String,
    val mediaType: String = "image/png",
    val width: Int = 0,
    val height: Int = 0,
    val modelId: String = "",
    val prompt: String = "",
)

@Serializable
data class ImageListDto(val images: List<ImageMetadataDto> = emptyList())

@Serializable
data class ImageContextDto(val sessionId: String, val imageId: String)

@Serializable
data class ActiveRunsDto(val runs: List<ActiveRunDto> = emptyList())

@Serializable
data class ActiveRunDto(
    val sessionId: String,
    val streamId: String,
    val status: String,
    val lastEventId: Int,
)

@Serializable
data class SessionStateDto(val messageCount: Int)

@Serializable
data class ContextUsageDto(
    val modelId: String,
    val modelLabel: String,
    val contextWindowTokens: Int,
    val estimatedTokens: Int,
    val ratio: Double,
    val thresholdRatio: Double,
    val targetRatio: Double,
    val reasoningEffort: String? = null,
)

@Serializable
data class TruncateRequestDto(
    val sessionId: String,
    val mode: String,
    val clientMessageId: String? = null,
    val memoryPosition: Int? = null,
)

@Serializable
data class TruncateResponseDto(
    val ok: Boolean,
    val deleted: Int,
    val keptThrough: Int,
    val resolvedPosition: Int? = null,
)

@Serializable
data class ContextSnippetBodyDto(val text: String, val sourceRole: String)

@Serializable
data class ContextSnippetResponseDto(val snippet: ContextSnippetDto? = null)

@Serializable
data class StoredContextSnippetResponseDto(val snippet: ContextSnippetDto)

@Serializable
data class ContextSnippetDto(
    val id: String,
    val text: String,
    val sourceRole: String,
)

fun ContextSnippetDto.toSnippet(): ContextSnippet = ContextSnippet(id, text, sourceRole)

@Serializable
data class ApprovalDecisionDto(val approved: Boolean)

@Serializable
data class ClarificationResponseDto(
    val answers: Map<String, List<String>>,
    val skipped: List<String> = emptyList(),
)

@Serializable
data class OkResponseDto(val ok: Boolean = true)

@Serializable
data class ProjectListPageDto(
    val items: List<ProjectListItemDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class ProjectListItemDto(
    val id: String,
    val name: String,
)

fun SessionDocumentDto.toDocument(): SessionDocument = SessionDocument(
    id = id,
    filename = filename,
    summary = firstPageSummary.orEmpty(),
)

fun ProjectListItemDto.toProject(): RecentProject = RecentProject(
    id = id,
    name = name,
)

fun DocumentStorageDto.toStorage(): DocumentStorage = DocumentStorage(usedBytes, maxBytes, remainingBytes)

fun LibraryDocumentPageDto.toPage(): LibraryDocumentPage = LibraryDocumentPage(
    items = items.map { it.toDocument() },
    nextCursor = nextCursor,
)

fun LibraryDocumentDto.toDocument(): LibraryDocument = LibraryDocument(
    id = id,
    filename = filename,
    summary = firstPageSummary,
    sizeBytes = sizeBytes,
    pageCount = pageCount,
)

fun DocumentStatusDto.toIngest(): DocumentIngest = DocumentIngest(
    id, filename, status, pageCount, sizeBytes, errorMessage, firstPageSummary,
)

fun DocumentUploadDto.toIngest(): DocumentIngest = DocumentIngest(
    id, filename, status, pageCount = 0, sizeBytes, errorMessage = null, summary = null,
)

fun ImageMetadataDto.toImage(isPinned: Boolean = false): SessionImage = SessionImage(
    id, prompt, mediaType, width, height, modelId, isPinned,
)

fun ActiveRunDto.toRun(): ActiveRun = ActiveRun(sessionId, streamId, status, lastEventId)

fun ContextUsageDto.toUsage(): ContextUsage = ContextUsage(
    modelId,
    modelLabel,
    contextWindowTokens,
    estimatedTokens,
    ratio,
    thresholdRatio,
    targetRatio,
    reasoningEffort,
)
