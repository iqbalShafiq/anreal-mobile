package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.feature.chat.domain.stream.InteractionKind
import co.ratmo.anreal.feature.chat.domain.stream.NativeInteraction
import co.ratmo.anreal.feature.chat.domain.stream.QuestionAnswer
import co.ratmo.anreal.feature.chat.presentation.ChatAction
import co.ratmo.anreal.feature.chat.presentation.ChatState

private val sessionGrantableTools = setOf(
    "web_search",
    "web_fetch",
    "deep_research",
    "generate_image",
    "edit_image",
)

private fun isImageTool(toolName: String): Boolean {
    return toolName == "generate_image" || toolName == "edit_image"
}

@Composable
internal fun InteractionDialog(
    interaction: NativeInteraction,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    when (interaction.kind) {
        InteractionKind.ToolApproval -> ApprovalContent(interaction, state, onAction)
        InteractionKind.ToolQuestion -> QuestionContent(interaction, state, onAction)
    }
}

@Composable
private fun ApprovalContent(
    interaction: NativeInteraction,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val busy = state.humanInputBusy
    val showSessionGrant = interaction.toolName in sessionGrantableTools
    val showOverride = isImageTool(interaction.toolName)
    AlertDialog(
        onDismissRequest = {},
        title = { Text(AnrealCopy.get(AnrealCopy.APPROVAL_TITLE)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                Text(interaction.toolName, style = MaterialTheme.typography.titleMedium)
                interaction.reason?.let { Text(it) }
                if (interaction.argumentsJson.isNotBlank()) {
                    Text(
                        interaction.argumentsJson,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                if (showOverride) {
                    ImageOverrideFields(
                        aspectRatio = state.imageAspectRatio,
                        quality = state.imageQuality,
                        enabled = !busy,
                        onAspectRatioChange = { onAction(ChatAction.OnImageAspectRatioChange(it)) },
                        onQualityChange = { onAction(ChatAction.OnImageQualityChange(it)) },
                    )
                }
            }
        },
        confirmButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                TextButton(
                    onClick = { onAction(ChatAction.OnInteractionReject(interaction.id)) },
                    enabled = !busy,
                ) { Text(AnrealCopy.get(AnrealCopy.ACTION_REJECT)) }
                if (showSessionGrant) {
                    TextButton(
                        onClick = { onAction(ChatAction.OnInteractionAllowSession(interaction.id)) },
                        enabled = !busy,
                    ) { Text(AnrealCopy.get(AnrealCopy.ACTION_ALLOW_SESSION)) }
                }
                TextButton(
                    onClick = {
                        if (showOverride) {
                            onAction(
                                ChatAction.OnInteractionImageOverride(
                                    interactionId = interaction.id,
                                    aspectRatio = state.imageAspectRatio,
                                    quality = state.imageQuality,
                                ),
                            )
                        } else {
                            onAction(ChatAction.OnInteractionAllowOnce(interaction.id))
                        }
                    },
                    enabled = !busy,
                ) { Text(AnrealCopy.get(AnrealCopy.ACTION_ALLOW_ONCE)) }
            }
        },
        dismissButton = {},
    )
}

@Composable
private fun ImageOverrideFields(
    aspectRatio: String?,
    quality: String?,
    enabled: Boolean,
    onAspectRatioChange: (String?) -> Unit,
    onQualityChange: (String?) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(
            AnrealCopy.get(AnrealCopy.IMAGE_OVERRIDE_TITLE),
            style = MaterialTheme.typography.titleSmall,
        )
        Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
            listOf("1:1", "16:9", "9:16").forEach { ratio ->
                FilterChip(
                    selected = aspectRatio == ratio,
                    onClick = { onAspectRatioChange(ratio.takeIf { aspectRatio != ratio }) },
                    enabled = enabled,
                    label = { Text(ratio) },
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
            listOf("low", "medium", "high").forEach { level ->
                FilterChip(
                    selected = quality == level,
                    onClick = { onQualityChange(level.takeIf { quality != level }) },
                    enabled = enabled,
                    label = { Text(level) },
                )
            }
        }
    }
}

@Composable
private fun QuestionContent(
    interaction: NativeInteraction,
    state: ChatState,
    onAction: (ChatAction) -> Unit,
) {
    val busy = state.humanInputBusy
    var answers by remember(interaction.id) {
        mutableStateOf<Map<String, String>>(emptyMap())
    }
    val requiredAnswered = interaction.questions.all { question ->
        question.allowCustom || answers[question.id].orEmpty().isNotBlank()
    }
    AlertDialog(
        onDismissRequest = {},
        title = { Text(AnrealCopy.get(AnrealCopy.CLARIFICATION_TITLE)) },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            ) {
                interaction.questions.forEach { question ->
                    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                        Text(question.text, style = MaterialTheme.typography.titleSmall)
                        if (question.choices.isEmpty() || question.allowCustom) {
                            OutlinedTextField(
                                value = answers[question.id].orEmpty(),
                                onValueChange = { answers = answers + (question.id to it) },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !busy,
                            )
                        } else {
                            question.choices.forEach { choice ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Checkbox(
                                        checked = answers[question.id] == choice.value,
                                        onCheckedChange = { checked ->
                                            answers = if (checked) {
                                                answers + (question.id to choice.value)
                                            } else {
                                                answers - question.id
                                            }
                                        },
                                        enabled = !busy,
                                    )
                                    Text(choice.label, modifier = Modifier.fillMaxWidth())
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(
                        ChatAction.OnInteractionQuestionAnswer(
                            interactionId = interaction.id,
                            answers = answers
                                .filterValues { it.isNotBlank() }
                                .map { (questionId, value) -> QuestionAnswer(questionId, value) },
                        ),
                    )
                },
                enabled = !busy && requiredAnswered,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_SUBMIT)) }
        },
        dismissButton = {},
    )
}
