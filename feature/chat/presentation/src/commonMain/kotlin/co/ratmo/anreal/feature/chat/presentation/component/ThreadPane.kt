package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.shape.CircleShape
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Auto_awesome
import com.composables.icons.materialsymbols.rounded.Arrow_downward
import com.composables.icons.materialsymbols.rounded.Expand_more
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealMarkdown
import co.ratmo.anreal.core.designsystem.component.AnrealLoadingIndicator
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
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
import kotlinx.coroutines.launch
import kotlin.time.Duration
import kotlin.time.TimeMark
import kotlin.time.TimeSource

@Composable
internal fun ThreadPane(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier = Modifier,
    topContentPadding: Dp = AnrealSpacing.sm,
    bottomContentPadding: Dp = AnrealSpacing.lg,
    initialScrollReady: Boolean = true,
) {
    when {
        state.historyLoading && state.thread.messages.isEmpty() -> {
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnrealLoadingIndicator()
            }
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
            key(state.selectedSessionId) {
                StreamingThreadList(
                    state = state,
                    onAction = onAction,
                    modifier = modifier,
                    topContentPadding = topContentPadding,
                    bottomContentPadding = bottomContentPadding,
                    initialScrollReady = initialScrollReady,
                )
            }
        }
    }
}

@Composable
private fun StreamingThreadList(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
    modifier: Modifier,
    topContentPadding: Dp,
    bottomContentPadding: Dp,
    initialScrollReady: Boolean,
) {
    val endIndex = state.thread.messages.size
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = endIndex)
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalAnrealReduceMotion.current
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val atBottom by remember(listState) { derivedStateOf { !listState.canScrollForward } }
    var initialScrollSettled by remember { mutableStateOf(false) }
    var followStreaming by remember { mutableStateOf(false) }

    LaunchedEffect(initialScrollReady) {
        if (!initialScrollReady) return@LaunchedEffect
        withFrameNanos { }
        val viewportHeight = listState.layoutInfo.run {
            (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)
        }
        listState.scrollToItem(endIndex, viewportHeight)
        initialScrollSettled = true
        followStreaming = true
    }
    LaunchedEffect(isDragged, atBottom, initialScrollSettled) {
        when {
            isDragged -> followStreaming = false
            initialScrollSettled && atBottom -> followStreaming = true
        }
    }
    LaunchedEffect(
        state.thread.messages.lastOrNull(),
        state.thread.messages.size,
        bottomContentPadding,
        followStreaming,
        isDragged,
    ) {
        if (!initialScrollSettled || !followStreaming || isDragged) return@LaunchedEffect
        // Let text/markdown and composer measurement finish before requesting
        // the absolute end, otherwise the last item's top can become the anchor.
        withFrameNanos { }
        if (followStreaming && !isDragged && listState.canScrollForward) {
            val viewportHeight = listState.layoutInfo.run {
                (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)
            }
            listState.scrollToItem(state.thread.messages.size, viewportHeight)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            contentPadding = PaddingValues(
                start = AnrealSpacing.md,
                end = AnrealSpacing.md,
                top = topContentPadding,
                bottom = bottomContentPadding,
            ),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
        ) {
            val lastMessageId = state.thread.messages.lastOrNull()?.id
            val showThinking = waitingForFirstAssistantToken(state)
            items(state.thread.messages, key = { it.id }) { message ->
                MessageBubble(
                    message = message,
                    busy = state.isSending || state.thread.status == RunStatus.Streaming,
                    onAction = onAction,
                )
                if (showThinking && message.id == lastMessageId) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.STATUS_THINKING),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = "anreal-thread-end") {
                Spacer(modifier = Modifier.height(1.dp))
            }
        }
        if (!atBottom) {
            GlassSurface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = bottomContentPadding + AnrealSpacing.sm),
                shape = CircleShape,
                tone = GlassTone.Thin,
            ) {
                IconButton(
                    onClick = {
                        followStreaming = false
                        scope.launch {
                            if (reduceMotion) {
                                val viewportHeight = listState.layoutInfo.run {
                                    (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)
                                }
                                listState.scrollToItem(endIndex, viewportHeight)
                            } else {
                                listState.animateScrollToItem(endIndex)
                                val viewportHeight = listState.layoutInfo.run {
                                    (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)
                                }
                                listState.scrollToItem(endIndex, viewportHeight)
                            }
                            initialScrollSettled = true
                            followStreaming = true
                        }
                    },
                ) {
                    Icon(
                        imageVector = MaterialSymbols.Rounded.Arrow_downward,
                        contentDescription = AnrealCopy.get(AnrealCopy.CD_SCROLL_TO_BOTTOM),
                    )
                }
            }
        }
    }
}

private fun waitingForFirstAssistantToken(state: ChatState): Boolean {
    if (!(state.isSending || state.thread.status == RunStatus.Streaming)) return false
    val last = state.thread.messages.lastOrNull() ?: return true
    if (last.role == ChatRole.User) return true
    return last.role == ChatRole.Assistant && last.parts.none { part ->
        when (part) {
            is ChatPart.Text -> part.text.isNotBlank()
            is ChatPart.Reasoning -> true
            is ChatPart.Tool -> true
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
        message.parts.forEachIndexed { index, part ->
            key(part.id) {
                when (part) {
                    is ChatPart.Reasoning -> {
                        val hasFollowUp = message.parts.drop(index + 1).any { later ->
                            later is ChatPart.Tool ||
                                (later is ChatPart.Text && later.text.isNotBlank()) ||
                                (later is ChatPart.Reasoning && later.text.isNotBlank())
                        }
                        val isLive = busy && !message.isComplete && !hasFollowUp
                        if (isLive || part.text.isNotBlank()) {
                            ReasoningBlock(text = part.text, isLive = isLive)
                        }
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
private fun ReasoningBlock(
    text: String,
    isLive: Boolean,
) {
    var expanded by remember { mutableStateOf(isLive) }
    var userToggled by remember { mutableStateOf(false) }
    var startedAt by remember { mutableStateOf<TimeMark?>(null) }
    var elapsedLabel by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(isLive) {
        if (isLive) {
            if (startedAt == null) startedAt = TimeSource.Monotonic.markNow()
            elapsedLabel = null
            if (!userToggled) expanded = true
        } else if (startedAt != null && elapsedLabel == null) {
            elapsedLabel = formatThoughtDuration(startedAt!!.elapsedNow())
            if (!userToggled) expanded = false
        }
    }

    val label = when {
        isLive -> AnrealCopy.get(AnrealCopy.LABEL_THINKING_LIVE)
        elapsedLabel != null -> UiText.StringResource(
            AnrealCopy.LABEL_THOUGHT_FOR,
            listOf(elapsedLabel.orEmpty()),
        ).asString()
        else -> AnrealCopy.get(AnrealCopy.LABEL_THOUGHT)
    }
    val summary = text.trim()
    Column(modifier = Modifier.fillMaxWidth()) {
        ActivityToggleRow(
            expanded = expanded,
            running = isLive,
            label = label,
            status = null,
            labelColor = if (isLive) {
                MaterialTheme.colorScheme.onSurface
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            },
            statusColor = MaterialTheme.colorScheme.onSurfaceVariant,
            contentDescription = AnrealCopy.get(AnrealCopy.CD_TOGGLE_REASONING),
            onClick = {
                userToggled = true
                expanded = !expanded
            },
        )
        if (expanded) {
            val lineColor = MaterialTheme.colorScheme.outlineVariant
            Box(
                modifier = Modifier
                    .padding(start = AnrealSpacing.lg, top = AnrealSpacing.xxs, bottom = AnrealSpacing.xs)
                    .drawBehind {
                        drawLine(
                            color = lineColor,
                            start = Offset(0f, 0f),
                            end = Offset(0f, size.height),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                    .padding(start = AnrealSpacing.sm),
            ) {
                if (summary.isNotEmpty()) {
                    AnrealMarkdown(content = summary, compact = true)
                } else {
                    Text(
                        text = AnrealCopy.get(
                            if (isLive) AnrealCopy.LABEL_WAITING_SUMMARY else AnrealCopy.LABEL_NO_SUMMARY,
                        ),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }
    }
}

internal fun formatThoughtDuration(elapsed: Duration): String {
    val millis = elapsed.inWholeMilliseconds.coerceAtLeast(0)
    return when {
        millis < 1_000L -> "${(millis / 100L).coerceAtLeast(1) / 10.0}s"
        millis < 10_000L -> "${((millis / 100L) / 10.0)}s"
        else -> "${(millis / 1_000L)}s"
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
private fun MessageBubbleReasoningLivePreview() {
    AnrealPreview {
        Column(modifier = Modifier.padding(AnrealSpacing.md)) {
            MessageBubble(
                message = ChatMessage(
                    id = "live",
                    role = ChatRole.Assistant,
                    parts = listOf(ChatPart.Reasoning(id = "r", text = "Checking sources…")),
                    isComplete = false,
                ),
                busy = true,
                onAction = {},
            )
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
