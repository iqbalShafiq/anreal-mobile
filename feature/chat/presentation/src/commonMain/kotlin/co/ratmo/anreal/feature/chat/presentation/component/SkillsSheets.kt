package co.ratmo.anreal.feature.chat.presentation.component

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.snap
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.designsystem.component.AnrealBottomSheet
import co.ratmo.anreal.core.designsystem.component.AnrealEmpty
import co.ratmo.anreal.core.designsystem.component.AnrealError
import co.ratmo.anreal.core.designsystem.component.AnrealPrimaryButton
import co.ratmo.anreal.core.designsystem.component.AnrealSheetTitle
import co.ratmo.anreal.core.designsystem.component.AnrealSkeletonList
import co.ratmo.anreal.core.designsystem.component.AnrealTextField
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealSpacing
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillStatus
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.validateSkillInput
import com.composables.icons.materialsymbols.MaterialSymbols
import com.composables.icons.materialsymbols.rounded.Add
import com.composables.icons.materialsymbols.rounded.Delete
import com.composables.icons.materialsymbols.rounded.Edit
import com.composables.icons.materialsymbols.rounded.Upload
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.dialogs.FileKitType
import io.github.vinceglb.filekit.dialogs.openFilePicker
import io.github.vinceglb.filekit.readBytes
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import org.koin.compose.viewmodel.koinViewModel

private const val EDITOR_NAME_KEY = "skillsEditorName"
private const val EDITOR_DESCRIPTION_KEY = "skillsEditorDescription"
private const val EDITOR_BODY_KEY = "skillsEditorBody"

internal data class SkillEditorState(
    val isNew: Boolean = true,
    val sourceId: String? = null,
    val name: String = "",
    val description: String = "",
    val bodyMd: String = "",
    val fieldErrors: Map<String, String> = emptyMap(),
    val serverError: String? = null,
    val saving: Boolean = false,
)

internal data class SkillsState(
    val skills: List<Skill> = emptyList(),
    val loading: Boolean = true,
    val error: UiText? = null,
    val editor: SkillEditorState? = null,
    val rowBusyId: String? = null,
    val deleteTargetId: String? = null,
    val deleteBusy: Boolean = false,
    val deleteError: UiText? = null,
)

internal sealed interface SkillsAction {
    data object OnRetry : SkillsAction
    data object OnNewSkill : SkillsAction
    data class OnEditSkill(val id: String) : SkillsAction
    data object OnCloseEditor : SkillsAction
    data class OnEditorNameChange(val name: String) : SkillsAction
    data class OnEditorDescriptionChange(val description: String) : SkillsAction
    data class OnEditorBodyChange(val bodyMd: String) : SkillsAction
    data class OnEditorUploadMd(val content: String) : SkillsAction
    data object OnSaveEditor : SkillsAction
    data class OnToggleEnabled(val id: String, val enabled: Boolean) : SkillsAction
    data class OnRequestDelete(val id: String) : SkillsAction
    data object OnCancelDelete : SkillsAction
    data object OnConfirmDelete : SkillsAction
}

internal sealed interface SkillsEvent {
    data class ShowMessage(val message: UiText) : SkillsEvent
}

internal class SkillsViewModel(
    private val savedStateHandle: SavedStateHandle,
    private val skillsSource: SkillsRemoteDataSource,
) : ViewModel() {

    private val _state = MutableStateFlow(SkillsState())
    val state = _state.asStateFlow()

    private val _events = Channel<SkillsEvent>()
    val events = _events.receiveAsFlow()

    init {
        viewModelScope.launch { loadSkills() }
    }

    fun onAction(action: SkillsAction) {
        when (action) {
            SkillsAction.OnRetry -> viewModelScope.launch { loadSkills() }
            SkillsAction.OnNewSkill -> openNewEditor()
            is SkillsAction.OnEditSkill -> openEditEditor(action.id)
            SkillsAction.OnCloseEditor -> _state.update { it.copy(editor = null) }
            is SkillsAction.OnEditorNameChange -> updateEditor { editor ->
                editor.copy(name = action.name, fieldErrors = editor.fieldErrors - "name")
                    .also { savedStateHandle[EDITOR_NAME_KEY] = action.name }
            }
            is SkillsAction.OnEditorDescriptionChange -> updateEditor { editor ->
                editor.copy(description = action.description, fieldErrors = editor.fieldErrors - "description")
                    .also { savedStateHandle[EDITOR_DESCRIPTION_KEY] = action.description }
            }
            is SkillsAction.OnEditorBodyChange -> updateEditor { editor ->
                editor.copy(bodyMd = action.bodyMd, fieldErrors = editor.fieldErrors - "bodyMd")
                    .also { savedStateHandle[EDITOR_BODY_KEY] = action.bodyMd }
            }
            is SkillsAction.OnEditorUploadMd -> applyUploadedMd(action.content)
            SkillsAction.OnSaveEditor -> viewModelScope.launch { saveEditor() }
            is SkillsAction.OnToggleEnabled -> viewModelScope.launch {
                toggleEnabled(action.id, action.enabled)
            }
            is SkillsAction.OnRequestDelete -> _state.update {
                it.copy(deleteTargetId = action.id, deleteError = null)
            }
            SkillsAction.OnCancelDelete -> if (!_state.value.deleteBusy) {
                _state.update { it.copy(deleteTargetId = null, deleteError = null) }
            }
            SkillsAction.OnConfirmDelete -> viewModelScope.launch { confirmDelete() }
        }
    }

    private suspend fun loadSkills() {
        _state.update { it.copy(loading = true, error = null) }
        when (val result = skillsSource.listSkills()) {
            is co.ratmo.anreal.core.domain.util.Result.Success -> {
                _state.update { it.copy(loading = false, skills = result.data) }
            }
            is co.ratmo.anreal.core.domain.util.Result.Error -> {
                _state.update {
                    it.copy(
                        loading = false,
                        error = result.error.serverMessage?.let(UiText::DynamicString)
                            ?: result.error.toUiText(),
                    )
                }
            }
        }
    }

    private fun openNewEditor() {
        _state.update {
            it.copy(
                editor = SkillEditorState(
                    isNew = true,
                    name = savedStateHandle.get<String>(EDITOR_NAME_KEY).orEmpty(),
                    description = savedStateHandle.get<String>(EDITOR_DESCRIPTION_KEY).orEmpty(),
                    bodyMd = savedStateHandle.get<String>(EDITOR_BODY_KEY)
                        .takeIf { body -> !body.isNullOrBlank() }
                        ?: SKILL_BODY_TEMPLATE,
                ),
            )
        }
    }

    private fun openEditEditor(id: String) {
        val skill = _state.value.skills.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                editor = SkillEditorState(
                    isNew = false,
                    sourceId = skill.id,
                    name = skill.name,
                    description = skill.description,
                    bodyMd = skill.bodyMd,
                ),
            )
        }
    }

    private fun updateEditor(transform: (SkillEditorState) -> SkillEditorState) {
        _state.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(editor = transform(editor).copy(serverError = null))
        }
    }

    private fun applyUploadedMd(content: String) {
        val frontmatter = parseFrontmatterFields(content)
        _state.update { current ->
            val editor = current.editor ?: return@update current
            current.copy(
                editor = editor.copy(
                    bodyMd = content,
                    name = editor.name.ifBlank { frontmatter?.first.orEmpty() },
                    description = editor.description.ifBlank { frontmatter?.second.orEmpty() },
                    fieldErrors = editor.fieldErrors - "bodyMd",
                    serverError = null,
                ),
            )
        }
    }

    private suspend fun saveEditor() {
        val editor = _state.value.editor ?: return
        val errors = validateSkillInput(editor.name, editor.description, editor.bodyMd)
        if (errors.isNotEmpty()) {
            _state.update { it.copy(editor = editor.copy(fieldErrors = errors)) }
            return
        }
        _state.update { it.copy(editor = editor.copy(saving = true, serverError = null)) }
        val name = editor.name.trim()
        val description = editor.description.trim()
        val result = if (editor.isNew) {
            skillsSource.createSkill(name, description, editor.bodyMd)
        } else {
            val existing = _state.value.skills.firstOrNull { it.id == editor.sourceId }
            skillsSource.updateSkill(
                id = editor.sourceId.orEmpty(),
                name = name,
                description = description,
                bodyMd = editor.bodyMd,
                markReviewed = existing != null && existing.status != SkillStatus.Active,
            )
        }
        when (result) {
            is co.ratmo.anreal.core.domain.util.Result.Success -> {
                savedStateHandle[EDITOR_NAME_KEY] = ""
                savedStateHandle[EDITOR_DESCRIPTION_KEY] = ""
                savedStateHandle[EDITOR_BODY_KEY] = ""
                _state.update { current ->
                    val others = current.skills.filterNot { it.id == result.data.id }
                    current.copy(
                        skills = (others + result.data).sortedBy { it.name },
                        editor = null,
                    )
                }
            }
            is co.ratmo.anreal.core.domain.util.Result.Error -> {
                _state.update { current ->
                    current.copy(
                        editor = current.editor?.copy(
                            saving = false,
                            serverError = result.error.serverMessage
                                ?: result.error.toUiText().asString(),
                        ),
                    )
                }
            }
        }
    }

    private suspend fun toggleEnabled(id: String, enabled: Boolean) {
        if (_state.value.rowBusyId != null) return
        _state.update { it.copy(rowBusyId = id) }
        when (val result = skillsSource.setSkillEnabled(id, enabled)) {
            is co.ratmo.anreal.core.domain.util.Result.Success -> {
                _state.update { current ->
                    current.copy(
                        rowBusyId = null,
                        skills = current.skills.map { skill ->
                            if (skill.id == id) result.data else skill
                        },
                    )
                }
            }
            is co.ratmo.anreal.core.domain.util.Result.Error -> {
                _state.update { it.copy(rowBusyId = null) }
                _events.send(SkillsEvent.ShowMessage(result.error.toUiText()))
            }
        }
    }

    private suspend fun confirmDelete() {
        val id = _state.value.deleteTargetId ?: return
        _state.update { it.copy(deleteBusy = true, deleteError = null) }
        when (val result = skillsSource.deleteSkill(id)) {
            is co.ratmo.anreal.core.domain.util.Result.Success -> {
                _state.update { current ->
                    current.copy(
                        deleteBusy = false,
                        deleteTargetId = null,
                        skills = current.skills.filterNot { it.id == id },
                    )
                }
            }
            is co.ratmo.anreal.core.domain.util.Result.Error -> {
                _state.update {
                    it.copy(deleteBusy = false, deleteError = result.error.toUiText())
                }
            }
        }
    }

    private companion object {
        const val SKILL_BODY_TEMPLATE = "---\nname: \ndescription: \n---\n"
    }
}

private fun parseFrontmatterFields(body: String): Pair<String, String>? {
    val lines = body.lines()
    if (lines.firstOrNull()?.trim() != "---") return null
    val end = lines.drop(1).indexOfFirst { it.trim() == "---" }
    if (end < 0) return null
    var name = ""
    var description = ""
    lines.drop(1).take(end).forEach { line ->
        val key = line.substringBefore(":").trim()
        val value = line.substringAfter(":", "").trim()
            .removeSurrounding("\"").removeSurrounding("'")
        if (key == "name") name = value
        if (key == "description") description = value
    }
    return name to description
}

@Composable
internal fun SkillsManagementRoot(
    onDismiss: () -> Unit,
    viewModel: SkillsViewModel = koinViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            is SkillsEvent.ShowMessage -> snackbarScope.launch {
                snackbarHostState.showSnackbar(event.message.asString())
            }
        }
    }
    Box(modifier = Modifier.fillMaxWidth()) {
        SkillsManagementScreen(state = state, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
    // onDismiss is consumed by the hosting AnrealBottomSheet.
    @Suppress("UNUSED_EXPRESSION")
    onDismiss
}

@Composable
internal fun SkillsManagementScreen(
    state: SkillsState,
    onAction: (SkillsAction) -> Unit,
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        AnrealSheetTitle(AnrealCopy.get(AnrealCopy.LABEL_SKILLS))
        val reduceMotion = LocalAnrealReduceMotion.current
        val slideSpec: FiniteAnimationSpec<IntOffset> = if (reduceMotion) {
            snap()
        } else {
            tween(
                durationMillis = AnrealMotion.durationFast.inWholeMilliseconds.toInt(),
                easing = AnrealMotion.easeOut,
            )
        }
        AnimatedContent(
            targetState = state.editor != null,
            transitionSpec = {
                if (targetState) {
                    (slideInHorizontally(slideSpec) { it } + fadeIn()) togetherWith
                        (slideOutHorizontally(slideSpec) { -it } + fadeOut())
                } else {
                    (slideInHorizontally(slideSpec) { -it } + fadeIn()) togetherWith
                        (slideOutHorizontally(slideSpec) { it } + fadeOut())
                }.using(SizeTransform(clip = false))
            },
            label = "skillsDrillDown",
        ) { editing ->
            if (editing) {
                SkillEditor(
                    editor = state.editor ?: SkillEditorState(),
                    onAction = onAction,
                )
            } else {
                SkillsList(state = state, onAction = onAction)
            }
        }
    }
    val deleteTarget = state.skills.firstOrNull { it.id == state.deleteTargetId }
    if (deleteTarget != null) {
        AlertDialog(
            onDismissRequest = { if (!state.deleteBusy) onAction(SkillsAction.OnCancelDelete) },
            icon = {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Delete,
                    contentDescription = null,
                )
            },
            title = { Text(AnrealCopy.get(AnrealCopy.DIALOG_DELETE_SKILL_TITLE)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.xs)) {
                    Text(
                        text = UiText.StringResource(
                            AnrealCopy.DIALOG_DELETE_SKILL_BODY,
                            listOf(deleteTarget.name),
                        ).asString(),
                        style = MaterialTheme.typography.bodyMedium,
                    )
                    state.deleteError?.let { error ->
                        Text(
                            text = error.asString(),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = { onAction(SkillsAction.OnConfirmDelete) },
                    enabled = !state.deleteBusy,
                ) {
                    Text(
                        text = if (state.deleteBusy) {
                            AnrealCopy.get(AnrealCopy.ACTION_DELETING)
                        } else {
                            AnrealCopy.get(AnrealCopy.ACTION_DELETE)
                        },
                        color = MaterialTheme.colorScheme.error,
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { onAction(SkillsAction.OnCancelDelete) },
                    enabled = !state.deleteBusy,
                ) {
                    Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
                }
            },
        )
    }
}

@Composable
private fun SkillsList(
    state: SkillsState,
    onAction: (SkillsAction) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = AnrealSpacing.lg),
    ) {
        when {
            state.loading -> {
                Column(
                    modifier = Modifier.padding(horizontal = AnrealSpacing.md),
                    verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
                ) {
                    AnrealSkeletonList(count = 2, itemHeight = 64.dp)
                }
            }
            state.error != null && state.skills.isEmpty() -> {
                AnrealError(
                    message = state.error.asString(),
                    retryLabel = AnrealCopy.get(AnrealCopy.ACTION_RETRY),
                    onRetry = { onAction(SkillsAction.OnRetry) },
                )
            }
            state.skills.isEmpty() -> {
                AnrealEmpty(
                    title = AnrealCopy.get(AnrealCopy.SKILLS_LIST_EMPTY_TITLE),
                    body = AnrealCopy.get(AnrealCopy.SKILLS_LIST_EMPTY_BODY),
                    actionLabel = AnrealCopy.get(AnrealCopy.ACTION_NEW),
                    onAction = { onAction(SkillsAction.OnNewSkill) },
                )
            }
            else -> {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                ) {
                    TextButton(
                        onClick = { onAction(SkillsAction.OnNewSkill) },
                        modifier = Modifier
                            .heightIn(min = AnrealSpacing.touch)
                            .padding(end = AnrealSpacing.sm),
                    ) {
                        Icon(
                            imageVector = MaterialSymbols.Rounded.Add,
                            contentDescription = null,
                        )
                        Text(
                            text = AnrealCopy.get(AnrealCopy.ACTION_NEW),
                            modifier = Modifier.padding(start = AnrealSpacing.xs),
                        )
                    }
                }
                Column(verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm)) {
                    state.skills.forEach { skill ->
                        SkillRow(
                            skill = skill,
                            busy = state.rowBusyId == skill.id,
                            onAction = onAction,
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SkillRow(
    skill: Skill,
    busy: Boolean,
    onAction: (SkillsAction) -> Unit,
) {
    val toggleEnabled = !busy && skill.status != SkillStatus.Draft
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = AnrealSpacing.md),
        shape = MaterialTheme.shapes.extraLarge,
        color = MaterialTheme.colorScheme.surfaceContainer,
        contentColor = MaterialTheme.colorScheme.onSurface,
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = AnrealSpacing.md, end = AnrealSpacing.xs)
                .heightIn(min = AnrealSpacing.menuRow),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = skill.name,
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurface,
                )
                val subtitle = when (skill.status) {
                    SkillStatus.Draft -> AnrealCopy.get(AnrealCopy.SKILL_STATUS_DRAFT)
                    SkillStatus.Invalid -> AnrealCopy.get(AnrealCopy.SKILL_STATUS_INVALID)
                    SkillStatus.Active -> skill.description
                }
                if (subtitle.isNotBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (skill.status == SkillStatus.Active) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                    )
                }
            }
            Switch(
                checked = skill.isEnabled,
                onCheckedChange = { onAction(SkillsAction.OnToggleEnabled(skill.id, it)) },
                enabled = toggleEnabled,
            )
            IconButton(
                onClick = { onAction(SkillsAction.OnEditSkill(skill.id)) },
                enabled = !busy,
                modifier = Modifier.size(AnrealSpacing.touch),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Edit,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_EDIT_SKILL),
                )
            }
            IconButton(
                onClick = { onAction(SkillsAction.OnRequestDelete(skill.id)) },
                enabled = !busy,
                modifier = Modifier.size(AnrealSpacing.touch),
            ) {
                Icon(
                    imageVector = MaterialSymbols.Rounded.Delete,
                    contentDescription = AnrealCopy.get(AnrealCopy.CD_DELETE_SKILL),
                )
            }
        }
    }
}

@Composable
private fun SkillEditor(
    editor: SkillEditorState,
    onAction: (SkillsAction) -> Unit,
) {
    val pickerScope = rememberCoroutineScope()
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AnrealSpacing.md)
            .padding(bottom = AnrealSpacing.lg),
        verticalArrangement = Arrangement.spacedBy(AnrealSpacing.sm),
    ) {
        Text(
            text = AnrealCopy.get(
                if (editor.isNew) {
                    AnrealCopy.SKILL_EDITOR_NEW_TITLE
                } else {
                    AnrealCopy.SKILL_EDITOR_EDIT_TITLE
                },
            ),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        OutlinedButton(
            onClick = {
                pickerScope.launch {
                    try {
                        FileKit.openFilePicker(type = FileKitType.File(listOf("md")))?.let { file ->
                            file.readBytes().decodeToString().let { content ->
                                onAction(SkillsAction.OnEditorUploadMd(content))
                            }
                        }
                    } catch (cancelled: CancellationException) {
                        throw cancelled
                    } catch (_: Exception) {
                        // Picker failures stay silent; the editor remains usable.
                    }
                }
            },
            modifier = Modifier.heightIn(min = AnrealSpacing.touch),
            enabled = !editor.saving,
        ) {
            Icon(
                imageVector = MaterialSymbols.Rounded.Upload,
                contentDescription = null,
            )
            Text(
                text = AnrealCopy.get(AnrealCopy.ACTION_UPLOAD_MD),
                modifier = Modifier.padding(start = AnrealSpacing.xs),
            )
        }
        AnrealTextField(
            value = editor.name,
            onValueChange = { onAction(SkillsAction.OnEditorNameChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_NAME),
            placeholder = AnrealCopy.get(AnrealCopy.SKILL_NAME_PLACEHOLDER),
            error = editor.fieldErrors["name"],
            enabled = !editor.saving,
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Ascii,
                imeAction = ImeAction.Next,
            ),
        )
        AnrealTextField(
            value = editor.description,
            onValueChange = { onAction(SkillsAction.OnEditorDescriptionChange(it)) },
            label = AnrealCopy.get(AnrealCopy.LABEL_DESCRIPTION),
            placeholder = AnrealCopy.get(AnrealCopy.SKILL_DESCRIPTION_PLACEHOLDER),
            error = editor.fieldErrors["description"],
            enabled = !editor.saving,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
        )
        Text(
            text = AnrealCopy.get(AnrealCopy.SKILL_BODY_LABEL).uppercase(),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = editor.bodyMd,
            onValueChange = { onAction(SkillsAction.OnEditorBodyChange(it)) },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 192.dp),
            enabled = !editor.saving,
            placeholder = { Text(AnrealCopy.get(AnrealCopy.SKILL_BODY_PLACEHOLDER)) },
            isError = editor.fieldErrors.containsKey("bodyMd"),
            supportingText = editor.fieldErrors["bodyMd"]?.let { message ->
                { Text(text = message, color = MaterialTheme.colorScheme.error) }
            },
            textStyle = MaterialTheme.typography.bodyMedium.copy(fontFamily = FontFamily.Monospace),
            shape = MaterialTheme.shapes.extraLarge,
        )
        editor.serverError?.let { message ->
            Text(
                text = message,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.error,
            )
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AnrealSpacing.sm, Alignment.End),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(
                onClick = { onAction(SkillsAction.OnCloseEditor) },
                modifier = Modifier.heightIn(min = AnrealSpacing.touch),
                enabled = !editor.saving,
            ) {
                Text(AnrealCopy.get(AnrealCopy.ACTION_CANCEL))
            }
            AnrealPrimaryButton(
                label = AnrealCopy.get(
                    if (editor.isNew) AnrealCopy.ACTION_CREATE else AnrealCopy.ACTION_SAVE,
                ),
                loadingLabel = AnrealCopy.get(AnrealCopy.ACTION_SAVING),
                loading = editor.saving,
                onClick = { onAction(SkillsAction.OnSaveEditor) },
            )
        }
    }
}

internal fun skillsLoadingState(): SkillsState = SkillsState(loading = true)

internal fun skillsEmptyState(): SkillsState = SkillsState(loading = false)

internal fun skillsErrorState(): SkillsState = SkillsState(
    loading = false,
    error = UiText.DynamicString(AnrealCopy.get(AnrealCopy.SKILLS_LOAD_ERROR)),
)

internal fun skillsPopulatedState(): SkillsState = SkillsState(
    loading = false,
    skills = listOf(
        Skill(
            id = "s1",
            name = "release-notes",
            description = "Draft release notes from merged work.",
            isEnabled = true,
            status = SkillStatus.Active,
        ),
        Skill(
            id = "s2",
            name = "plan-review",
            description = "Review plans before execution.",
            isEnabled = false,
            status = SkillStatus.Draft,
        ),
        Skill(
            id = "s3",
            name = "bad-frontmatter",
            description = "Needs a frontmatter fix.",
            isEnabled = false,
            status = SkillStatus.Invalid,
        ),
    ),
)

internal fun skillEditorNewState(): SkillsState = skillsPopulatedState().copy(
    editor = SkillEditorState(
        isNew = true,
        name = "",
        description = "",
        bodyMd = "---\nname: \ndescription: \n---\n",
    ),
)

internal fun skillEditorEditState(): SkillsState = skillsPopulatedState().copy(
    editor = SkillEditorState(
        isNew = false,
        sourceId = "s1",
        name = "release-notes",
        description = "Draft release notes from merged work.",
        bodyMd = "---\nname: release-notes\ndescription: Draft release notes from merged work.\n---\n# body",
    ),
)

internal fun skillEditorSavingState(): SkillsState = skillEditorEditState().copy(
    editor = skillEditorEditState().editor?.copy(saving = true),
)

@AnrealPreviews
@Composable
private fun SkillsListLoadingPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillsLoadingState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsListEmptyPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillsEmptyState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsListErrorPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillsErrorState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsListPopulatedPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillsPopulatedState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsEditorNewPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillEditorNewState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsEditorEditPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillEditorEditState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsEditorSavingPreview() {
    AnrealPreview {
        SkillsManagementScreen(state = skillEditorSavingState(), onAction = {})
    }
}

@AnrealPreviews
@Composable
private fun SkillsDeleteConfirmPreview() {
    AnrealPreview {
        AnrealBottomSheet(onDismiss = {}) {
            SkillsManagementScreen(
                state = skillsPopulatedState().copy(deleteTargetId = "s1"),
                onAction = {},
            )
        }
    }
}
