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
import androidx.compose.material3.Checkbox
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
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Checklist
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Edit

@Composable
fun TasksPanel(
    tasks: List<TaskUi>,
    isLoading: Boolean,
    loaded: Boolean,
    hasScope: Boolean,
    error: UiText?,
    editor: TaskEditorState?,
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
            TextButton(onClick = { onAction(WorkspaceAction.OnNewTask) }) {
                Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                Text(AnrealCopy.get(AnrealCopy.ACTION_NEW))
            }
        }
        when {
            !hasScope -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_TASKS),
                body = AnrealCopy.get(AnrealCopy.TASKS_NO_SCOPE),
                icon = MaterialSymbols.Rounded.Checklist,
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
            tasks.isEmpty() -> AnrealEmpty(
                title = AnrealCopy.get(AnrealCopy.LABEL_TASKS),
                body = AnrealCopy.get(AnrealCopy.TASKS_EMPTY),
                icon = MaterialSymbols.Rounded.Checklist,
                modifier = Modifier.fillMaxSize(),
            )
            else -> LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = WorkspaceListPadding,
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                items(tasks, key = TaskUi::id) { task ->
                    TaskCard(
                        task = task,
                        onEdit = { onAction(WorkspaceAction.OnEditTask(task.id)) },
                        onDelete = { onAction(WorkspaceAction.OnRequestDeleteTask(task.id, task.title)) },
                    )
                }
            }
        }
    }
    if (editor != null) {
        TaskEditorDialog(
            editor = editor,
            isMutating = isMutating,
            mutationError = mutationError,
            onAction = onAction,
        )
    }
}

@Composable
private fun TaskCard(
    task: TaskUi,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
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
                    text = task.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val detail = buildString {
                    append(
                        when (task.status) {
                            TaskStatus.Inbox -> "inbox"
                            TaskStatus.Doing -> "doing"
                            TaskStatus.Done -> "done"
                        },
                    )
                    if (task.totalCount > 0) append(" · ${task.doneCount}/${task.totalCount}")
                    if (!task.dueAt.isNullOrBlank()) append(" · ${task.dueAt}")
                }
                Text(
                    text = detail,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            IconButton(onClick = onEdit) {
                Icon(MaterialSymbols.Rounded.Edit, contentDescription = null)
            }
            IconButton(onClick = onDelete) {
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
private fun TaskEditorDialog(
    editor: TaskEditorState,
    isMutating: Boolean,
    mutationError: UiText?,
    onAction: (WorkspaceAction) -> Unit,
) {
    AlertDialog(
        onDismissRequest = { onAction(WorkspaceAction.OnCloseTaskEditor) },
        title = {
            Text(
                if (editor.isNew) AnrealCopy.get(AnrealCopy.TASKS_NEW_TITLE)
                else AnrealCopy.get(AnrealCopy.TASKS_EDIT_TITLE),
            )
        },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
            ) {
                OutlinedTextField(
                    value = editor.title,
                    onValueChange = { onAction(WorkspaceAction.OnTaskTitleChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_TITLE)) },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(
                        capitalization = KeyboardCapitalization.Sentences,
                        imeAction = ImeAction.Next,
                    ),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.description,
                    onValueChange = { onAction(WorkspaceAction.OnTaskDescriptionChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_DESCRIPTION)) },
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                    modifier = Modifier.fillMaxWidth(),
                )
                OutlinedTextField(
                    value = editor.dueAt,
                    onValueChange = { onAction(WorkspaceAction.OnTaskDueChange(it)) },
                    label = { Text(AnrealCopy.get(AnrealCopy.LABEL_DUE_DATE)) },
                    placeholder = { Text("2026-09-30T10:00:00+07:00") },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                    TaskStatus.entries.forEach { status ->
                        FilterChip(
                            selected = editor.status == status,
                            onClick = { onAction(WorkspaceAction.OnTaskStatusChange(status)) },
                            label = {
                                Text(
                                    when (status) {
                                        TaskStatus.Inbox -> "inbox"
                                        TaskStatus.Doing -> "doing"
                                        TaskStatus.Done -> "done"
                                    },
                                )
                            },
                        )
                    }
                }
                Text(
                    text = AnrealCopy.get(AnrealCopy.TASKS_SUBTASKS_TITLE),
                    style = MaterialTheme.typography.titleSmall,
                )
                editor.subtasks.forEach { subtask ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Checkbox(
                            checked = subtask.done,
                            onCheckedChange = { onAction(WorkspaceAction.OnTaskToggleSubtask(subtask.id, it)) },
                        )
                        Text(
                            text = subtask.title,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodyMedium,
                        )
                        IconButton(onClick = { onAction(WorkspaceAction.OnTaskRemoveSubtask(subtask.id)) }) {
                            Icon(MaterialSymbols.Rounded.Delete, contentDescription = null)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = editor.newSubtaskTitle,
                        onValueChange = { onAction(WorkspaceAction.OnTaskNewSubtaskChange(it)) },
                        label = { Text(AnrealCopy.get(AnrealCopy.TASKS_ADD_SUBTASK)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                        modifier = Modifier.weight(1f),
                    )
                    IconButton(onClick = { onAction(WorkspaceAction.OnTaskAddSubtask) }) {
                        Icon(MaterialSymbols.Rounded.Add, contentDescription = null)
                    }
                }
                val message = editor.fieldError?.asString() ?: mutationError?.asString()
                if (message != null) {
                    Text(text = message, color = MaterialTheme.colorScheme.error)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { onAction(WorkspaceAction.OnTaskSave) },
                enabled = !isMutating && !editor.saving,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_SAVE))
            }
        },
        dismissButton = {
            TextButton(onClick = { onAction(WorkspaceAction.OnCloseTaskEditor) }) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
        },
    )
}

@AnrealPreviews
@Composable
private fun TasksPopulatedPreview() {
    AnrealPreview {
        TasksPanel(
            tasks = listOf(
                TaskUi("t1", "Fix login", TaskStatus.Doing, "Old bug", listOf(SubtaskUi("st1", "Repro", true), SubtaskUi("st2", "Fix", false)), null),
                TaskUi("t2", "Ship it", TaskStatus.Inbox, null, emptyList(), "2026-09-30T10:00:00+07:00"),
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
private fun TasksEditorPreview() {
    AnrealPreview {
        TasksPanel(
            tasks = emptyList(),
            isLoading = false,
            loaded = true,
            hasScope = true,
            error = null,
            editor = TaskEditorState(
                isNew = false, sourceId = "t1", title = "Fix login", description = "Old bug",
                status = TaskStatus.Doing,
                subtasks = listOf(SubtaskUi("st1", "Repro", true)),
            ),
            isMutating = false,
            mutationError = null,
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun TasksEmptyPreview() {
    AnrealPreview {
        TasksPanel(
            tasks = emptyList(),
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
