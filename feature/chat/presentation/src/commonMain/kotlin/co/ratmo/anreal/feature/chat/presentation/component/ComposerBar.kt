package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.component.AnrealComposerField
import co.ratmo.anreal.core.designsystem.component.GlassSurface
import co.ratmo.anreal.core.designsystem.component.GlassTone
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.RunStatus
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState
import co.ratmo.anreal.feature.chat.presentation.preview.chatPopulatedPreviewState
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Arrow_upward
import com.composables.icons.materialsymbols.rounded.South_west
import com.composables.icons.materialsymbols.rounded.Stop

@Composable
internal fun ComposerBar(
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val streaming = state.isSending || state.thread.status == RunStatus.Streaming
    val canSubmit = state.draft.isNotBlank()
    GlassSurface(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
            .padding(horizontal = AnrealSpacing.md, vertical = AnrealSpacing.sm),
        tone = GlassTone.Thin,
        emphasized = canSubmit || streaming,
    ) {
        Column(
            modifier = Modifier.padding(
                start = AnrealSpacing.md,
                end = AnrealSpacing.sm,
                top = AnrealSpacing.md,
                bottom = AnrealSpacing.sm,
            ),
            verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            MessageQueueDock(state = state, onAction = onAction)
            AnrealComposerField(
                value = state.draft,
                onValueChange = { onAction(ChatAction.OnDraftChange(it)) },
                placeholder = AnrealCopy.get(AnrealCopy.COMPOSER_PLACEHOLDER),
                onSubmit = { onAction(ChatAction.OnSend) },
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Spacer(modifier = Modifier.weight(1f))
                ComposerSubmitButton(
                    streaming = streaming,
                    canSubmit = canSubmit,
                    onAction = onAction,
                )
            }
        }
    }
}

@Composable
private fun ComposerSubmitButton(
    streaming: Boolean,
    canSubmit: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    val colors = IconButtonDefaults.filledIconButtonColors()
    when {
        streaming && !canSubmit -> {
            FilledIconButton(
                onClick = { onAction(ChatAction.OnStop) },
                modifier = Modifier.size(AnrealSpacing.touch),
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Stop,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_STOP),
                )
            }
        }
        streaming -> {
            FilledIconButton(
                onClick = { onAction(ChatAction.OnSend) },
                modifier = Modifier.size(AnrealSpacing.touch),
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.South_west,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_QUEUE),
                )
            }
        }
        else -> {
            FilledIconButton(
                onClick = { onAction(ChatAction.OnSend) },
                modifier = Modifier.size(AnrealSpacing.touch),
                enabled = canSubmit,
                shape = CircleShape,
                colors = colors,
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Arrow_upward,
                    contentDescription = AnrealCopy.get(AnrealCopy.ACTION_SEND),
                )
            }
        }
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarEmptyPreview() {
    AnrealPreview {
        ComposerBar(state = ChatState(draft = ""), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarFilledPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(draft = "What about costs?"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarStreamingStopPreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(draft = "", isSending = true, status = RunStatus.Streaming),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun ComposerBarStreamingQueuePreview() {
    AnrealPreview {
        ComposerBar(
            state = chatPopulatedPreviewState(
                draft = "What about costs?",
                isSending = true,
                status = RunStatus.Streaming,
            ),
            onAction = {},
        )
    }
}
