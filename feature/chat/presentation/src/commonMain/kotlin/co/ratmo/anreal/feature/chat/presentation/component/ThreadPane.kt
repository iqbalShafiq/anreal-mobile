package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
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
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
import co.ratmo.anreal.core.designsystem.component.rememberFrostedTopBar
import co.ratmo.anreal.core.designsystem.component.glassDrawerBorderColor
import co.ratmo.anreal.core.designsystem.component.glassDrawerFallbackColor
import co.ratmo.anreal.core.designsystem.component.glassBubbleTintColor
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
import co.ratmo.anreal.feature.chat.presentation.preview.chatOlderHistoryPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatLoadingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.chatStreamingPreviewState
import co.ratmo.anreal.feature.chat.presentation.preview.previewAssistantMessage
import co.ratmo.anreal.feature.chat.presentation.preview.previewEmptyPartsMessage
import co.ratmo.anreal.feature.chat.presentation.preview.previewReasoningAssistant
import co.ratmo.anreal.feature.chat.presentation.preview.previewUserMessage
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.distinctUntilChanged
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
    onFrostedTopBarChange: (Boolean) -> Unit = {},
) {
    when {
        state.historyLoading && state.thread.messages.isEmpty() -> {
            SideEffect { onFrostedTopBarChange(false) }
            Box(modifier = modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                AnrealLoadingIndicator()
            }
        }
        state.historyError != null && state.thread.messages.isEmpty() -> {
            SideEffect { onFrostedTopBarChange(false) }
            AnrealError(
                modifier = modifier.fillMaxSize(),
                message = state.historyError.asString(),
                onRetry = { onAction(ChatAction.OnRetryHistory) },
            )
        }
        state.thread.messages.isEmpty() -> {
            SideEffect { onFrostedTopBarChange(false) }
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
                    onFrostedTopBarChange = onFrostedTopBarChange,
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
    onFrostedTopBarChange: (Boolean) -> Unit,
) {
    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = (state.thread.messages.size + 1).coerceAtLeast(0),
    )
    val scope = rememberCoroutineScope()
    val reduceMotion = LocalAnrealReduceMotion.current
    val frostedTopBar = rememberFrostedTopBar(listState)
    val currentOnFrostedTopBarChange by rememberUpdatedState(onFrostedTopBarChange)
    SideEffect { currentOnFrostedTopBarChange(frostedTopBar) }
    val isDragged by listState.interactionSource.collectIsDraggedAsState()
    val atBottom by remember(listState) { derivedStateOf { !listState.canScrollForward } }
    var initialScrollSettled by remember { mutableStateOf(false) }
    var followStreaming by remember { mutableStateOf(false) }

    val lastMessageId = state.thread.messages.lastOrNull()?.id
    LaunchedEffect(initialScrollReady, state.historyLoading, lastMessageId) {
        if (!initialScrollReady || state.thread.messages.isEmpty()) return@LaunchedEffect
        if (initialScrollSettled && !followStreaming) return@LaunchedEffect
        run {
            repeat(8) {
                withFrameNanos { }
                listState.pinToEnd()
                withFrameNanos { }
                if (!listState.canScrollForward) return@run
            }
        }
        initialScrollSettled = true
        followStreaming = true
    }
    LaunchedEffect(isDragged, atBottom, initialScrollSettled) {
        when {
            isDragged -> followStreaming = false
            initialScrollSettled && atBottom -> followStreaming = true
        }
    }
    val currentIsDragged by rememberUpdatedState(isDragged)
    val currentStreamVersion by rememberUpdatedState(state.thread.messages.lastOrNull())
    val currentStreamActive by rememberUpdatedState(
        state.isSending || state.thread.status == RunStatus.Streaming,
    )
    LaunchedEffect(followStreaming) {
        // A keyed-on-message effect is useless here: every delta restarts it and
        // cancels the in-flight scrollToItem before it lands. Keep one stable
        // loop that reacts to the latest message via rememberUpdatedState and
        // scrolls once per content change, never mid-scroll.
        if (!followStreaming) return@LaunchedEffect
        var lastScrolledVersion: ChatMessage? = null
        while (true) {
            val version = currentStreamVersion
            if (!currentIsDragged && version !== lastScrolledVersion) {
                lastScrolledVersion = version
                // The frame callback fires before the layout pass, so the new
                // content is not measured yet on the first frame after a delta.
                withFrameNanos { }
                withFrameNanos { }
                listState.pinToEnd()
            }
            delay(if (currentStreamActive) 16 else 250)
        }
    }

    var followedSendMessageId by remember { mutableStateOf<String?>(null) }
    val lastMessage = state.thread.messages.lastOrNull()
    LaunchedEffect(lastMessage?.id, state.isSending) {
        // A new user message is the optimistic send bubble: snap to the bottom
        // and follow the stream even if the user had scrolled up to read.
        val message = state.thread.messages.lastOrNull() ?: return@LaunchedEffect
        if (message.role != ChatRole.User || message.id == followedSendMessageId) {
            return@LaunchedEffect
        }
        followedSendMessageId = message.id
        withFrameNanos { }
        listState.pinToEnd()
        initialScrollSettled = true
        followStreaming = true
    }
    val streamingActive = state.isSending || state.thread.status == RunStatus.Streaming
    LaunchedEffect(streamingActive, lastMessage?.isComplete, state.thread.messages.size) {
        // When a run settles, markdown reflow and the composer/IME settle can
        // leave the thread a frame or two short of the end. Clamp to the true
        // bottom once the stream is no longer active.
        if (streamingActive) return@LaunchedEffect
        if (!initialScrollSettled || !followStreaming || isDragged) return@LaunchedEffect
        withFrameNanos { }
        withFrameNanos { }
        if (followStreaming && !isDragged) {
            listState.pinToEnd()
        }
    }

    var olderAnchorId by remember { mutableStateOf<String?>(null) }
    var olderAnchorOffset by remember { mutableIntStateOf(0) }
    var wasLoadingOlder by remember { mutableStateOf(false) }
    LaunchedEffect(state.olderHistoryLoading, state.thread.messages.firstOrNull()?.id) {
        if (state.olderHistoryLoading && !wasLoadingOlder) {
            val anchor = listState.layoutInfo.visibleItemsInfo.firstOrNull { info ->
                val key = info.key as? String
                key != null && key != OLDER_HEADER_KEY && key != THREAD_END_KEY
            }
            olderAnchorId = anchor?.key as? String
            olderAnchorOffset = if (anchor != null && listState.firstVisibleItemIndex == anchor.index) {
                listState.firstVisibleItemScrollOffset
            } else {
                0
            }
        }
        if (!state.olderHistoryLoading && wasLoadingOlder) {
            val anchorId = olderAnchorId
            if (anchorId != null) {
                val messageIndex = state.thread.messages.indexOfFirst { it.id == anchorId }
                if (messageIndex >= 0) {
                    listState.scrollToItem(messageIndex + 1, olderAnchorOffset)
                }
            }
            olderAnchorId = null
        }
        wasLoadingOlder = state.olderHistoryLoading
    }
    val canRequestOlder by rememberUpdatedState(
        initialScrollSettled &&
            state.canLoadOlderHistory &&
            !state.olderHistoryLoading &&
            !state.historyLoading,
    )
    LaunchedEffect(listState, initialScrollSettled) {
        if (!initialScrollSettled) return@LaunchedEffect
        snapshotFlow { listState.firstVisibleItemIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (index <= 1 && canRequestOlder) {
                    onAction(ChatAction.OnLoadOlderHistory)
                }
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
        ) {
            item(key = OLDER_HEADER_KEY) {
                if (state.olderHistoryLoading) {
                    val loadingLabel = AnrealCopy.get(AnrealCopy.CD_LOADING_OLDER_MESSAGES)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = AnrealSpacing.sm)
                            .semantics { contentDescription = loadingLabel },
                        contentAlignment = Alignment.Center,
                    ) {
                        AnrealLoadingIndicator(size = 24.dp)
                    }
                } else {
                    Spacer(modifier = Modifier.height(1.dp))
                }
            }
            val lastMessageId = state.thread.messages.lastOrNull()?.id
            val showThinking = waitingForFirstAssistantToken(state)
            itemsIndexed(state.thread.messages, key = { _, message -> message.id }) { index, message ->
                val previousEndsActivity = index > 0 &&
                    state.thread.messages[index - 1].endsWithVisibleActivity()
                val startsActivity = message.startsWithVisibleActivity()
                val topGap = when {
                    index == 0 -> 0.dp
                    previousEndsActivity && startsActivity -> 0.dp
                    else -> AnrealSpacing.md
                }
                MessageBubble(
                    message = message,
                    busy = state.isSending || state.thread.status == RunStatus.Streaming,
                    onAction = onAction,
                    modifier = Modifier.padding(top = topGap),
                )
                if (showThinking && message.id == lastMessageId) {
                    Text(
                        text = AnrealCopy.get(AnrealCopy.STATUS_THINKING),
                        style = MaterialTheme.typography.bodyLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            item(key = THREAD_END_KEY) {
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
                                listState.pinToEnd()
                            } else {
                                val lastIndex = (listState.layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
                                listState.animateScrollToItem(lastIndex)
                                listState.pinToEnd()
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

private const val OLDER_HEADER_KEY = "anreal-older-header"
private const val THREAD_END_KEY = "anreal-thread-end"

private suspend fun LazyListState.pinToEnd() {
    val lastIndex = (layoutInfo.totalItemsCount - 1).coerceAtLeast(0)
    val viewportHeight = layoutInfo.run {
        (viewportEndOffset - viewportStartOffset).coerceAtLeast(0)
    }
    scrollToItem(lastIndex, viewportHeight)
}

private fun ChatMessage.startsWithVisibleActivity(): Boolean =
    parts.firstOrNull { it.isVisiblePart() }?.isActivityPart() == true

private fun ChatMessage.endsWithVisibleActivity(): Boolean =
    parts.lastOrNull { it.isVisiblePart() }?.isActivityPart() == true

private fun ChatPart.isVisiblePart(): Boolean = when (this) {
    is ChatPart.Reasoning -> text.isNotBlank()
    is ChatPart.Tool -> true
    is ChatPart.Text -> text.isNotBlank()
}

private fun ChatPart.isActivityPart(): Boolean = this is ChatPart.Reasoning || this is ChatPart.Tool

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
    modifier: Modifier = Modifier,
) {
    val isUser = message.role == ChatRole.User
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = if (isUser) Alignment.End else Alignment.Start,
    ) {
        var previousIsActivity = false
        var renderedAny = false
        message.parts.forEachIndexed { index, part ->
            val isActivity = part is ChatPart.Reasoning || part is ChatPart.Tool
            val content: @Composable () -> Unit = when (part) {
                is ChatPart.Reasoning -> {
                    val hasFollowUp = message.parts.drop(index + 1).any { later ->
                        later is ChatPart.Tool ||
                            (later is ChatPart.Text && later.text.isNotBlank()) ||
                            (later is ChatPart.Reasoning && later.text.isNotBlank())
                    }
                    val isLive = busy && !message.isComplete && !hasFollowUp
                    if (isLive || part.text.isNotBlank()) {
                        { ReasoningBlock(text = part.text, isLive = isLive) }
                    } else {
                        null
                    }
                }
                is ChatPart.Tool -> { { ToolActivityCard(part) } }
                is ChatPart.Text -> if (part.text.isNotBlank()) {
                    if (isUser) {
                        { UserBubble(text = part.text) }
                    } else if (busy && !message.isComplete) {
                        {
                            Text(
                                text = part.text,
                                modifier = Modifier.fillMaxWidth(),
                                style = MaterialTheme.typography.bodyLarge,
                                color = MaterialTheme.colorScheme.onSurface,
                            )
                        }
                    } else {
                        { AnrealMarkdown(content = part.text, modifier = Modifier.fillMaxWidth()) }
                    }
                } else {
                    null
                }
            } ?: return@forEachIndexed
            val topGap = if (!renderedAny) {
                0.dp
            } else if (previousIsActivity && isActivity) {
                0.dp
            } else {
                AnrealSpacing.xs
            }
            key(part.id) {
                Column(modifier = Modifier.padding(top = topGap)) {
                    content()
                }
            }
            renderedAny = true
            previousIsActivity = isActivity
        }
        val showActions = message.role == ChatRole.User ||
            (message.role == ChatRole.Assistant && message.isComplete)
        if (showActions) {
            MessageActionsBar(
                message = message,
                busy = busy,
                onAction = onAction,
                modifier = Modifier.padding(top = AnrealSpacing.xs),
            )
        }
    }
}

@Composable
private fun UserBubble(text: String) {
    GlassSurface(
        modifier = Modifier.widthIn(max = 320.dp),
        shape = MaterialTheme.shapes.extraLarge,
        tone = GlassTone.UltraThin,
        borderColor = glassDrawerBorderColor(),
        fallbackColor = glassDrawerFallbackColor(),
        tintColor = glassBubbleTintColor(),
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
                    if (isLive) {
                        Text(
                            text = summary,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        AnrealMarkdown(content = summary, compact = true)
                    }
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
private fun ThreadPaneOlderHistoryLoadingPreview() {
    AnrealPreview {
        ThreadPane(
            state = chatOlderHistoryPreviewState(),
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
