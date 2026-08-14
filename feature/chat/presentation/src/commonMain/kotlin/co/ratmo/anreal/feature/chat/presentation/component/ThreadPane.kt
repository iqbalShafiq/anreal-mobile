package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealMarkdown
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatEmptyPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatErrorPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatLoadingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatStreamingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewAssistantMessage
import co.ratmo.anreal.feature.chat.presentation.preview.previewEmptyPartsMessage
import co.ratmo.anreal.feature.chat.presentation.preview.previewReasoningAssistant
import co.ratmo.anreal.feature.chat.presentation.preview.previewUserMessage

@Composable
internal fun ThreadPane(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    when {
        state.historyLoading && state.thread.messages.isEmpty() -> {
            AnrealSkeletonList(
                modifier = modifier.padding(AnrealSpacing.md),
                count = 5,
                itemHeight = 64.dp,
            )
        }
        state.historyError != null && state.thread.messages.isEmpty() -> {
            AnrealError(
                modifier = modifier,
                message = state.historyError.asString(),
                onRetry = { onAction(ChatAction.OnRetryHistory) },
            )
        }
        state.thread.messages.isEmpty() -> {
            AnrealEmpty(
                modifier = modifier,
                title = AnrealCopy.get(AnrealCopy.CHAT_EMPTY_TITLE),
                body = AnrealCopy.get(AnrealCopy.CHAT_EMPTY_BODY),
            )
        }
        else -> {
            val listState = rememberLazyListState()
            LaunchedEffect(state.thread.messages.size) {
                listState.animateScrollToItem(state.thread.messages.lastIndex.coerceAtLeast(0))
            }
            LazyColumn(
                modifier = modifier.fillMaxSize(),
                state = listState,
                contentPadding = PaddingValues(AnrealSpacing.md),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(state.thread.messages, key = { it.id }) { message ->
                    MessageBubble(message)
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: ChatMessage) {
    val isUser = message.role == ChatRole.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        message.parts.forEach { part ->
            when (part) {
                is ChatPart.Reasoning -> if (part.text.isNotBlank()) {
                    Text(
                        text = part.text,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                is ChatPart.Tool -> ToolActivityCard(part)
                is ChatPart.Text -> if (part.text.isNotBlank()) {
                    if (isUser) {
                        Text(
                            text = part.text,
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    } else {
                        AnrealMarkdown(content = part.text)
                    }
                }
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun ThreadPaneLoadingPreview() {
    AnrealPreview {
        ThreadPane(state = chatLoadingPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ThreadPaneErrorPreview() {
    AnrealPreview {
        ThreadPane(state = chatErrorPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ThreadPaneEmptyPreview() {
    AnrealPreview {
        ThreadPane(state = chatEmptyPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ThreadPanePopulatedPreview() {
    AnrealPreview {
        ThreadPane(
            state = chatPopulatedPreviewState(
                messages = listOf(previewUserMessage, previewAssistantMessage, previewReasoningAssistant),
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ThreadPaneStreamingPreview() {
    AnrealPreview {
        ThreadPane(state = chatStreamingPreviewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleUserPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewUserMessage)
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleAssistantPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewAssistantMessage)
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleReasoningPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewReasoningAssistant)
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleMixedPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(
                ChatMessage(
                    id = "mix",
                    role = ChatRole.Assistant,
                    parts = listOf(
                        ChatPart.Reasoning(id = "r", text = "Looking up the table."),
                        ChatPart.Tool(
                            id = "t",
                            toolName = "find_documents",
                            toolCallId = "c",
                            state = "output-available",
                        ),
                        ChatPart.Text(id = "p", text = "## Result\nRevenue grew **12%**."),
                    ),
                    isComplete = true,
                ),
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleEmptyPartsPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewEmptyPartsMessage)
        }
    }
}
