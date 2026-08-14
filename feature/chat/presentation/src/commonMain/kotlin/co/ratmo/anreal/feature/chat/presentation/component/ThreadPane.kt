package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Expand_more
import com.composables.icons.materialsymbols.rounded.Expand_less
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealMarkdown
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
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
                modifier = modifier.fillMaxSize(),
                message = state.historyError.asString(),
                onRetry = { onAction(ChatAction.OnRetryHistory) },
            )
        }
        state.thread.messages.isEmpty() -> {
            AnrealEmpty(
                modifier = modifier.fillMaxSize(),
                icon = MaterialSymbols.Rounded.Auto_awesome,
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
                contentPadding = PaddingValues(
                    start = AnrealSpacing.md,
                    end = AnrealSpacing.md,
                    top = AnrealSpacing.sm,
                    bottom = AnrealSpacing.lg,
                ),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            ) {
                items(state.thread.messages, key = { it.id }) { message ->
                    MessageBubble(
                        message = message,
                        busy = state.isSending || state.thread.status == RunStatus.Streaming,
                        onAction = onAction,
                    )
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(
    message: ChatMessage,
    busy: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    val isUser = message.role == ChatRole.User
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs),
    ) {
        message.parts.forEach { part ->
            when (part) {
                is ChatPart.Reasoning -> if (part.text.isNotBlank()) {
                    ReasoningBlock(text = part.text)
                }
                is ChatPart.Tool -> ToolActivityCard(part)
                is ChatPart.Text -> if (part.text.isNotBlank()) {
                    if (isUser) {
                        UserBubble(text = part.text)
                    } else {
                        AnrealMarkdown(
                            content = part.text,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
        val showActions = message.role == ChatRole.User ||
            (message.role == ChatRole.Assistant && message.isComplete)
        if (showActions) {
            MessageActionsBar(
                message = message,
                busy = busy,
                onAction = onAction,
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    GlassSurface(
        modifier = Modifier.widthIn(max = 320.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.Pane,
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
            style = MaterialTheme.typography.bodyLarge,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

@Composable
private fun ReasoningBlock(text: String) {
    var expanded by remember { mutableStateOf(false) }
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clickable { expanded = !expanded }
                .padding(vertical = AnrealSpacing.xxs),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xxs),
        ) {
            Icon(
                imageVector = if (expanded) {
                    MaterialSymbols.Rounded.Expand_less
                } else {
                    MaterialSymbols.Rounded.Expand_more
                },
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Text(
                text = AnrealCopy.get(AnrealCopy.LABEL_THOUGHT),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (expanded) {
            Text(
                text = text,
                modifier = Modifier.padding(start = AnrealSpacing.lg, bottom = AnrealSpacing.xs),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
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
        ThreadPane(
            state = chatErrorPreviewState(),
            onAction = {},
            modifier = Modifier.height(480.dp),
        )
    }
}

@AnrealPreviews
@Composable
private fun ThreadPaneEmptyPreview() {
    AnrealPreview {
        ThreadPane(
            state = chatEmptyPreviewState(),
            onAction = {},
            modifier = Modifier.height(480.dp),
        )
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
            MessageBubble(previewUserMessage, busy = false, onAction = {})
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleAssistantPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewAssistantMessage, busy = false, onAction = {})
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleReasoningPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewReasoningAssistant, busy = false, onAction = {})
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleMixedPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(
                message = ChatMessage(
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
                busy = false,
                onAction = {},
            )
        }
    }
}

@AnrealPreviews
@Composable
private fun MessageBubbleEmptyPartsPreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(previewEmptyPartsMessage, busy = false, onAction = {})
        }
    }
}
