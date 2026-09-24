package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonCard
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.workspace.domain.ScheduleFreq
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Schedule

@Composable
fun SchedulesPanel(
    schedules: List<ScheduleUi>,
    isLoading: Boolean,
    loaded: Boolean,
    hasScope: Boolean,
    error: UiText?,
    editor: ScheduleEditorState?,
    isMutating: Boolean,
    mutationError: UiText?,
    onAction: (WorkspaceAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = AnrealSpacing.screenCompact),
            horizontalArrangement = Arrangement.End,
        ) {
            TextButton(onClick = { onAction(WorkspaceAction.OnNewSchedule) }) {
                Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                Text(AnrealCopy.get(AnrealCopy.ACTION_NEW))
            }
        }
        when {
            !hasScope -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_SCHEDULES),
                body = AnrealCopy.get(AnrealCopy.SCHEDULES_NO_SCOPE),
                icon = MaterialSymbols.Rounded.Schedule,
                modifier = Modifier.fillMaxSize(),
            )
            isLoading && !loaded -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WorkspaceListPadding,
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(4) { AnrealSkeletonCard() }
            }
            error != null && !loaded -> AnrealError(
                message = error.asString(),
                retryLabel = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
                onRetry = { onAction(WorkspaceAction.Retry) },
                modifier = Modifier.fillMaxSize(),
            )
            schedules.isEmpty() -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_SCHEDULES),
                body = AnrealCopy.get(AnrealCopy.SCHEDULES_EMPTY),
                icon = MaterialSymbols.Rounded.Schedule,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WorkspaceListPadding,
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(schedules, key = ScheduleUi::id) { schedule ->
                    ScheduleCard(
                        schedule = schedule,
                        onCancel = { onAction(WorkspaceAction.OnRequestCancelSchedule(schedule.id, schedule.title)) },
                    )
                }
            }
        }
    }
    if (editor != null) {
        ScheduleEditorDialog(
            editor = editor,
            isMutating = isMutating,
            mutationError = mutationError,
            onAction = onAction,
        )
    }
}

@Composable
private fun ScheduleCard(
    schedule: ScheduleUi,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(AnrealSpacing.md),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = schedule.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val detail = buildString {
                    append(
                        when (schedule.freq) {
                            ScheduleFreq.Once -> "once"
                            ScheduleFreq.Daily -> "daily"
                            ScheduleFreq.Weekly -> "weekly"
                        },
                    )
                    if (!schedule.nextRunAt.isNullOrBlank()) append(" · ${schedule.nextRunAt}")
                    append(" · ${schedule.status}")
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onCancel) {
                Icon(
                    MaterialSymbols.Rounded.Delete,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.error,
                )
            }
        }
    }
}

@Composable
private fun ScheduleEditorDialog(
    editor: ScheduleEditorState,
    isMutating: Boolean,
    mutationError: UiText?,
    onAction: (WorkspaceAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.OnCloseScheduleEditor) },
        title = { Text(AnrealCopy.get(AnrealCopy.SCHEDULES_NEW_TITLE)) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                OutlinedTextField(
                    value = editor.title,
                    onValueChange = { onAction(WorkspaceAction.OnScheduleTitleChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_TITLE)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.prompt,
                    onValueChange = { onAction(WorkspaceAction.OnSchedulePromptChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_PROMPT)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                    ScheduleFreq.entries.forEach { freq ->
                        FilterChip(
                            selected = editor.freq == freq,
                            onClick = { onAction(WorkspaceAction.OnScheduleFreqChange(freq)) },
                            label = {
                                Text(
                                    when (freq) {
                                        ScheduleFreq.Once -> "once"
                                        ScheduleFreq.Daily -> "daily"
                                        ScheduleFreq.Weekly -> "weekly"
                                    },
                                )
                            },
                        )
                    }
                }
                OutlinedTextField(
                    value = editor.runAt,
                    onValueChange = { onAction(WorkspaceAction.OnScheduleRunAtChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_RUN_AT)) },
                    placeholder = { Text("2026-09-30T10:00:00+07:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                val message = editor.fieldError?.asString() ?: mutationError?.asString()
                if (message != null) {
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(WorkspaceAction.OnScheduleSave) },
                enabled = !isMutating && !editor.saving,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(WorkspaceAction.OnCloseScheduleEditor) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun SchedulesPopulatedPreview() {
    AnrealPreview {
        SchedulesPanel(
            schedules = listOf(
                ScheduleUi("sc1", "Morning brief", "Summarize", ScheduleFreq.Daily, "2026-09-25T07:00:00+07:00", "active"),
                ScheduleUi("sc2", "Weekly", "Report", ScheduleFreq.Weekly, null, "active"),
            ),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            editor = null,
            isMutating = false,
            mutationError = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SchedulesEditorPreview() {
    AnrealPreview {
        SchedulesPanel(
            schedules = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            editor = ScheduleEditorState(title = "Weekly", prompt = "Report", freq = ScheduleFreq.Weekly),
            isMutating = false,
            mutationError = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun SchedulesEmptyPreview() {
    AnrealPreview {
        SchedulesPanel(
            schedules = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            editor = null,
            isMutating = false,
            mutationError = null,
            onAction = {},
        )
    }
}
