package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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
import co.ratmo.anreal.feature.chat.domain.stream.Clarification
import co.ratmo.anreal.feature.chat.domain.stream.ClarificationQuestion
import co.ratmo.anreal.feature.chat.domain.stream.ToolApproval
import co.ratmo.anreal.feature.chat.presentation.ChatAction

@Composable
internal fun ApprovalDialog(
    approval: ToolApproval,
    busy: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = {},
        title = { Text(AnrealCopy.get(AnrealCopy.APPROVAL_TITLE)) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                Text(approval.toolName, style = MaterialTheme.typography.titleMedium)
                approval.reason?.let { Text(it) }
                if (approval.arguments.isNotBlank()) {
                    Text(
                        approval.arguments,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnApprovalDecision(approval.id, true)) },
                enabled = !busy,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_ALLOW_ONCE)) }
        },
        dismissButton = {
            TextButton(
                onClick = { onAction(ChatAction.OnApprovalDecision(approval.id, false)) },
                enabled = !busy,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_REJECT)) }
        },
    )
}

@Composable
internal fun ClarificationDialog(
    clarification: Clarification,
    busy: Boolean,
    onAction: (ChatAction) -> Unit,
) {
    var answers by remember(clarification.id) {
        mutableStateOf(
            clarification.questions.associate { question ->
                question.id to question.options.filter { it.recommended }.map { it.id }
            }.filterValues { it.isNotEmpty() },
        )
    }
    val requiredAnswered = clarification.questions.all { question ->
        question.optional || answers[question.id].orEmpty().any(String::isNotBlank)
    }
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(clarification.title ?: AnrealCopy.get(AnrealCopy.CLARIFICATION_TITLE))
        },
        text = {
            LazyColumn(
                modifier = Modifier.heightIn(max = 400.dp),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.md),
            ) {
                items(clarification.questions, key = ClarificationQuestion::id) { question ->
                    ClarificationQuestionField(
                        question = question,
                        values = answers[question.id].orEmpty(),
                        onChange = { values -> answers = answers + (question.id to values) },
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onAction(
                        ChatAction.OnClarificationResponse(
                            clarificationId = clarification.id,
                            answers = answers.filterValues { values -> values.any(String::isNotBlank) },
                            skipped = clarification.questions
                                .filter { answers[it.id].orEmpty().none(String::isNotBlank) }
                                .map(ClarificationQuestion::id),
                        ),
                    )
                },
                enabled = !busy && requiredAnswered,
            ) { Text(AnrealCopy.get(AnrealCopy.ACTION_SUBMIT)) }
        },
        dismissButton = {},
    )
}

@Composable
private fun ClarificationQuestionField(
    question: ClarificationQuestion,
    values: List<String>,
    onChange: (List<String>) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
        Text(question.question, style = MaterialTheme.typography.titleSmall)
        when (question.type) {
            "free_text" -> OutlinedTextField(
                value = values.firstOrNull().orEmpty(),
                onValueChange = { onChange(listOf(it)) },
                modifier = Modifier.fillMaxWidth(),
                placeholder = question.placeholder?.let { placeholder -> { Text(placeholder) } },
            )
            "multiple_choice" -> question.options.forEach { option ->
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Checkbox(
                        checked = option.id in values,
                        onCheckedChange = { checked ->
                            onChange(if (checked) values + option.id else values - option.id)
                        },
                    )
                    Text(option.label, modifier = Modifier.padding(start = AnrealSpacing.xs))
                }
            }
            else -> Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                question.options.forEach { option ->
                    FilterChip(
                        selected = option.id in values,
                        onClick = { onChange(listOf(option.id)) },
                        label = { Text(option.label) },
                    )
                }
            }
        }
    }
}
