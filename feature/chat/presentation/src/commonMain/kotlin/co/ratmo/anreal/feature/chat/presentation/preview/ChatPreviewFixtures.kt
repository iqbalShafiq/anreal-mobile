package co.ratmo.anreal.feature.chat.presentation.preview

import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.ChatThreadState
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.presentation.ChatSessionUi
import co.ratmo.anreal.feature.chat.presentation.ChatState

internal val previewUnreadSession = ChatSessionUi(
    id = "s1",
    title = "Q3 report",
    unread = true,
)

internal val previewReadSession = ChatSessionUi(
    id = "s2",
    title = "New chat",
    unread = false,
)

internal val previewSessions = listOf(previewUnreadSession, previewReadSession)

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
