package co.ratmo.anreal.feature.chat.presentation.preview

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ChatModel
import co.ratmo.anreal.feature.chat.domain.ReasoningEffort
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.ChatSessionUi
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.CitedDocumentUi
import co.ratmo.anreal.feature.chat.presentation.ContextUsageUi
import co.ratmo.anreal.feature.chat.presentation.RecentProjectUi
import co.ratmo.anreal.feature.chat.presentation.SessionDocumentUi

internal val previewAccount = AccountUi(
    name = "shafiq",
    email = "shafiq@testing.com",
)

internal val previewUnreadSession = ChatSessionUi(
    id = "s1",
    title = "halo boy!",
    unread = true,
    updatedAt = "2026-08-14T10:00:00Z",
)

internal val previewReadSession = ChatSessionUi(
    id = "s2",
    title = "panduan Anvia",
    unread = false,
    updatedAt = "2026-08-13T18:00:00Z",
)

internal val previewOlderSession = ChatSessionUi(
    id = "s3",
    title = "oi, siapa nama gw?",
    unread = false,
    updatedAt = "2026-08-12T09:00:00Z",
)

internal val previewSessions = listOf(previewUnreadSession, previewReadSession, previewOlderSession)

internal val previewUserMessage = ChatMessage(
    id = "u1",
    role = ChatRole.User,
    parts = listOf(ChatPart.Text(id = "u1t", text = "Summarize the PDF.")),
    isComplete = true,
)

internal val previewAssistantMessage = ChatMessage(
    id = "a1",
    role = ChatRole.Assistant,
    parts = listOf(ChatPart.Text(id = "a1t", text = "Revenue grew 12% year over year.")),
    isComplete = true,
)

internal val previewReasoningAssistant = ChatMessage(
    id = "a2",
    role = ChatRole.Assistant,
    parts = listOf(
        ChatPart.Reasoning(id = "a2r", text = "Checking the revenue table and year-over-year note."),
        ChatPart.Text(id = "a2t", text = "Operating costs rose 4%."),
    ),
    isComplete = true,
)

internal val previewStreamingAssistant = ChatMessage(
    id = "a3",
    role = ChatRole.Assistant,
    parts = listOf(
        ChatPart.Reasoning(id = "a3r", text = "Reading the cost schedule…"),
        ChatPart.Text(id = "a3t", text = "Headcount"),
    ),
    isComplete = false,
)

internal val previewEmptyPartsMessage = ChatMessage(
    id = "empty",
    role = ChatRole.Assistant,
    parts = emptyList(),
    isComplete = false,
)

internal fun chatEmptyPreviewState(): ChatState = ChatState(sessionsLoading = false)

internal fun chatLoadingPreviewState(): ChatState = ChatState(
    sessionsLoading = true,
    historyLoading = true,
)

internal fun chatErrorPreviewState(): ChatState = ChatState(
    sessionsLoading = false,
    sessionsError = UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET),
    historyError = UiText.StringResource(AnrealCopy.ERROR_NO_INTERNET),
)

internal fun chatPopulatedPreviewState(
    draft: String = "What about costs?",
    isSending: Boolean = false,
    status: RunStatus = RunStatus.Idle,
    runActiveConflict: Boolean = false,
    messages: List<ChatMessage> = listOf(previewUserMessage, previewAssistantMessage),
): ChatState = ChatState(
    sessionsLoading = false,
    sessions = previewSessions,
    selectedSessionId = previewUnreadSession.id,
    thread = ChatThreadState(
        status = status,
        messages = messages,
    ),
    draft = draft,
    isSending = isSending,
    runActiveConflict = runActiveConflict,
    contextUsage = ContextUsageUi(
        modelLabel = "GPT Luna 5.6",
        estimatedTokens = 24_000,
        contextWindowTokens = 200_000,
        ratio = 0.12f,
        thresholdRatio = 0.7f,
        targetRatio = 0.55f,
        reasoningEffort = "xhigh",
        nearThreshold = false,
    ),
)

internal fun chatStreamingPreviewState(): ChatState = chatPopulatedPreviewState(
    isSending = true,
    status = RunStatus.Streaming,
    messages = listOf(previewUserMessage, previewStreamingAssistant),
)

internal fun chatConflictPreviewState(): ChatState = chatPopulatedPreviewState(
    runActiveConflict = true,
)

internal fun chatRenamePreviewState(): ChatState = chatPopulatedPreviewState().copy(
    renameSessionId = previewUnreadSession.id,
    renameDraft = previewUnreadSession.title,
)

internal fun chatDeletePreviewState(): ChatState = chatPopulatedPreviewState().copy(
    deleteSessionId = previewUnreadSession.id,
)

internal fun chatQueuedPreviewState(): ChatState = chatStreamingPreviewState().copy(
    draft = "And margins?",
    queue = listOf(QueuedItem(id = "q1", text = "What about costs?")),
)

internal fun chatQueueConflictPreviewState(): ChatState = chatPopulatedPreviewState(
    draft = "Another question",
).copy(
    queue = listOf(QueuedItem(id = "q1", text = "What about costs?")),
    queueConflict = true,
)

internal fun chatWorkspacePreviewState(): ChatState = chatPopulatedPreviewState().copy(
    recentProjects = listOf(
        RecentProjectUi(id = "p1", name = "Agentic Course"),
        RecentProjectUi(id = "p2", name = "Anvia Project"),
    ),
    activeDocuments = listOf(
        SessionDocumentUi(
            id = "d1",
            filename = "Anvia_Framework_Pandua…",
            summary = "# Anvia Framework — Panduan Lengkap dari Dasar sampai…",
        ),
    ),
    citedDocuments = listOf(
        CitedDocumentUi(
            id = "d1",
            filename = "Anvia_Framework_Panduan_Len…",
            citationCount = 9,
        ),
    ),
)

internal fun chatProjectWorkspacePreviewState(): ChatState = chatWorkspacePreviewState().copy(
    activeProjectId = "p1",
    activeProjectName = "Agentic Course",
    sessions = listOf(
        ChatSessionUi(
            id = "p-s1",
            title = "New chat",
            unread = false,
            updatedAt = "2026-08-14T10:00:00Z",
            projectId = "p1",
        ),
        ChatSessionUi(
            id = "p-s2",
            title = "Eval notes",
            unread = false,
            updatedAt = "2026-08-13T18:00:00Z",
            projectId = "p1",
        ),
    ),
    selectedSessionId = "p-s1",
)

internal fun chatDocumentsPreviewState(): ChatState = chatWorkspacePreviewState()

internal fun chatComposerCatalogPreviewState(
    draft: String = "What about costs?",
    selectedReasoning: String? = "xhigh",
    webSearchEnabled: Boolean = true,
    imageGenerationEnabled: Boolean = false,
): ChatState = chatPopulatedPreviewState(draft = draft).copy(
    models = listOf(
        ChatModel(
            id = "luna",
            label = "GPT Luna 5.6",
            reasoningEfforts = listOf("low", "high", "xhigh"),
            contextWindowTokens = 200_000,
        ),
        ChatModel(
            id = "ds",
            label = "DeepSeek",
            reasoningEfforts = listOf("low", "high"),
            contextWindowTokens = 128_000,
        ),
    ),
    selectedModelId = "luna",
    reasoningEfforts = listOf(
        ReasoningEffort(key = "low", label = "Low", description = "Faster"),
        ReasoningEffort(key = "high", label = "High", description = "Deeper"),
        ReasoningEffort(key = "xhigh", label = "Xhigh", description = "Deepest"),
    ),
    selectedReasoning = selectedReasoning,
    webSearchEnabled = webSearchEnabled,
    imageGenerationEnabled = imageGenerationEnabled,
    capabilities = ChatCapabilities(
        webSearchAvailable = true,
        imageGenerationAvailable = true,
    ),
)
