package co.ratmo.anreal.feature.workspace.presentation

import androidx.compose.runtime.Stable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.onFailure
import co.ratmo.anreal.core.domain.util.onSuccess
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.toUiText
import co.ratmo.anreal.feature.workspace.domain.Project
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteEntry
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteStatus
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import co.ratmo.anreal.feature.workspace.domain.WorkspaceBaseUrlProvider
import co.ratmo.anreal.feature.workspace.domain.WorkspaceTask
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceProjectSort
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable

private const val SEARCH_DEBOUNCE_MS = 300L
private const val LOAD_WAIT_INTERVAL_MS = 25L

@Serializable
enum class WorkspaceSection { Projects, Documents, Images, Sites, Tasks }

enum class WorkspaceViewMode { List, Grid }

data class ProjectUi(
    val id: String,
    val name: String,
    val description: String,
    val documentCount: Int,
    val chatCount: Int,
)

data class DocumentUi(
    val id: String,
    val filename: String,
    val summary: String,
    val detail: String,
    val projectName: String?,
)

data class ImageUi(
    val id: String,
    val prompt: String,
    val detail: String,
    val bytes: ByteArray? = null,
    val loading: Boolean = false,
)

data class SiteUi(
    val siteId: String,
    val sessionId: String,
    val version: Int,
    val stableVersion: Int?,
    val statusLabel: String,
    val previewUrl: String?,
    val downloadUrl: String,
)

data class SubtaskUi(
    val id: String,
    val title: String,
    val done: Boolean,
)

data class TaskUi(
    val id: String,
    val title: String,
    val status: TaskStatus,
    val description: String?,
    val subtasks: List<SubtaskUi>,
    val dueAt: String?,
) {
    val doneCount: Int get() = subtasks.count { it.done }
    val totalCount: Int get() = subtasks.size
}

data class TaskEditorState(
    val isNew: Boolean = true,
    val sourceId: String? = null,
    val title: String = "",
    val description: String = "",
    val dueAt: String = "",
    val status: TaskStatus = TaskStatus.Inbox,
    val subtasks: List<SubtaskUi> = emptyList(),
    val newSubtaskTitle: String = "",
    val fieldError: UiText? = null,
    val saving: Boolean = false,
)

data class DocumentPreviewUi(
    val id: String,
    val filename: String,
    val summary: String,
    val pageIndex: Int,
    val pageCount: Int,
    val markdown: String,
    val images: List<ByteArray>,
)

sealed interface WorkspaceDeleteTarget {
    val id: String
    val label: String

    data class Project(override val id: String, override val label: String) : WorkspaceDeleteTarget
    data class Document(override val id: String, override val label: String) : WorkspaceDeleteTarget
    data class Task(override val id: String, override val label: String) : WorkspaceDeleteTarget
}

sealed interface WorkspaceCardSheetTarget {
    val id: String
    val label: String

    data class Project(
        override val id: String,
        override val label: String,
        val description: String = "",
        val documentCount: Int = 0,
    ) : WorkspaceCardSheetTarget
    data class Document(override val id: String, override val label: String) : WorkspaceCardSheetTarget
}

@Stable
data class WorkspaceState(
    val section: WorkspaceSection = WorkspaceSection.Projects,
    val viewMode: WorkspaceViewMode = WorkspaceViewMode.List,
    val projectSort: WorkspaceProjectSort = WorkspaceProjectSort.UpdatedAt,
    val projects: List<ProjectUi> = emptyList(),
    val documents: List<DocumentUi> = emptyList(),
    val images: List<ImageUi> = emptyList(),
    val sites: List<SiteUi> = emptyList(),
    val scopeSessionId: String? = null,
    val tasks: List<TaskUi> = emptyList(),
    val taskEditor: TaskEditorState? = null,
    val siteBaseUrl: String = "",
    val previewSiteId: String? = null,
    val query: String = "",
    val nextCursors: Map<WorkspaceSection, String?> = emptyMap(),
    val isLoadingMore: Boolean = false,
    val loadedSections: Set<WorkspaceSection> = emptySet(),
    val isLoading: Boolean = false,
    val error: UiText? = null,
    val showCreateProject: Boolean = false,
    val editProjectId: String? = null,
    val projectName: String = "",
    val projectDescription: String = "",
    val mutationError: UiText? = null,
    val isMutating: Boolean = false,
    val deleteTarget: WorkspaceDeleteTarget? = null,
    val cardSheetTarget: WorkspaceCardSheetTarget? = null,
    val previewDocumentId: String? = null,
    val preview: DocumentPreviewUi? = null,
    val previewLoading: Boolean = false,
    val previewError: UiText? = null,
)

sealed interface WorkspaceAction {
    data class SelectSection(val section: WorkspaceSection) : WorkspaceAction
    data class SetViewMode(val mode: WorkspaceViewMode) : WorkspaceAction
    data class SetProjectSort(val sort: WorkspaceProjectSort) : WorkspaceAction
    data object Retry : WorkspaceAction
    data class ChangeQuery(val value: String) : WorkspaceAction
    data object LoadMore : WorkspaceAction
    data class ShowCardSheet(val target: WorkspaceCardSheetTarget) : WorkspaceAction
    data object DismissCardSheet : WorkspaceAction
    data object ShowCreateProject : WorkspaceAction
    data object DismissCreateProject : WorkspaceAction
    data class ChangeProjectName(val value: String) : WorkspaceAction
    data class ChangeProjectDescription(val value: String) : WorkspaceAction
    data object CreateProject : WorkspaceAction
    data class ShowEditProject(val id: String) : WorkspaceAction
    data class OpenProject(val id: String) : WorkspaceAction
    data class RequestDeleteProject(val id: String, val name: String) : WorkspaceAction
    data class RequestDeleteDocument(val id: String, val name: String) : WorkspaceAction
    data class OpenDocument(val id: String) : WorkspaceAction
    data object CloseDocument : WorkspaceAction
    data object RetryDocumentPreview : WorkspaceAction
    data class OpenDocumentPage(val pageIndex: Int) : WorkspaceAction
    data object DismissDelete : WorkspaceAction
    data object ConfirmDelete : WorkspaceAction
    data object Back : WorkspaceAction
    data class OpenSitePreview(val siteId: String) : WorkspaceAction
    data object CloseSitePreview : WorkspaceAction
    data object OnNewTask : WorkspaceAction
    data class OnEditTask(val id: String) : WorkspaceAction
    data object OnCloseTaskEditor : WorkspaceAction
    data class OnTaskTitleChange(val value: String) : WorkspaceAction
    data class OnTaskDescriptionChange(val value: String) : WorkspaceAction
    data class OnTaskDueChange(val value: String) : WorkspaceAction
    data class OnTaskStatusChange(val status: TaskStatus) : WorkspaceAction
    data class OnTaskNewSubtaskChange(val value: String) : WorkspaceAction
    data object OnTaskAddSubtask : WorkspaceAction
    data class OnTaskToggleSubtask(val id: String, val done: Boolean) : WorkspaceAction
    data class OnTaskRemoveSubtask(val id: String) : WorkspaceAction
    data object OnTaskSave : WorkspaceAction
    data class OnRequestDeleteTask(val id: String, val title: String) : WorkspaceAction
}

sealed interface WorkspaceEvent {
    data object NavigateBack : WorkspaceEvent
    data class OpenProject(val projectId: String, val name: String) : WorkspaceEvent
}

class WorkspaceViewModel(
    initialSection: WorkspaceSection,
    private val repository: WorkspaceRepository,
    scopeSessionId: String? = null,
    baseUrlProvider: WorkspaceBaseUrlProvider? = null,
) : ViewModel() {
    private val _state = MutableStateFlow(
        WorkspaceState(
            section = initialSection,
            scopeSessionId = scopeSessionId,
            siteBaseUrl = baseUrlProvider?.baseUrl().orEmpty(),
        ),
    )
    val state = _state.asStateFlow()
    private val _events = Channel<WorkspaceEvent>()
    val events = _events.receiveAsFlow()
    private var searchJob: Job? = null

    init { viewModelScope.launch { load(initialSection) } }

    fun onAction(action: WorkspaceAction) {
        when (action) {
            is WorkspaceAction.SelectSection -> {
                val defaultMode = when (action.section) {
                    WorkspaceSection.Images -> WorkspaceViewMode.Grid
                    else -> WorkspaceViewMode.List
                }
                _state.update { it.copy(section = action.section, viewMode = defaultMode, error = null) }
                if (action.section !in _state.value.loadedSections) {
                    viewModelScope.launch { awaitThenLoad(action.section) }
                }
            }
            is WorkspaceAction.SetViewMode -> _state.update { it.copy(viewMode = action.mode) }
            is WorkspaceAction.SetProjectSort -> {
                _state.update { it.copy(projectSort = action.sort) }
                if (_state.value.section == WorkspaceSection.Projects) {
                    viewModelScope.launch { awaitThenLoad(WorkspaceSection.Projects) }
                }
            }
            WorkspaceAction.Retry -> viewModelScope.launch { awaitThenLoad(_state.value.section) }
            is WorkspaceAction.ChangeQuery -> {
                _state.update { it.copy(query = action.value) }
                searchJob?.cancel()
                searchJob = viewModelScope.launch {
                    delay(SEARCH_DEBOUNCE_MS)
                    awaitThenLoad(_state.value.section)
                }
            }
            WorkspaceAction.LoadMore -> viewModelScope.launch { loadMore() }
            is WorkspaceAction.ShowCardSheet -> _state.update { it.copy(cardSheetTarget = action.target) }
            WorkspaceAction.DismissCardSheet -> _state.update { it.copy(cardSheetTarget = null) }
            WorkspaceAction.ShowCreateProject -> _state.update {
                it.copy(
                    showCreateProject = true,
                    editProjectId = null,
                    projectName = "",
                    projectDescription = "",
                    mutationError = null,
                )
            }
            WorkspaceAction.DismissCreateProject -> if (!_state.value.isMutating) {
                _state.update { it.copy(showCreateProject = false, editProjectId = null, mutationError = null) }
            }
            is WorkspaceAction.ChangeProjectName -> _state.update {
                it.copy(projectName = action.value, mutationError = null)
            }
            is WorkspaceAction.ChangeProjectDescription -> _state.update {
                it.copy(projectDescription = action.value, mutationError = null)
            }
            WorkspaceAction.CreateProject -> viewModelScope.launch { createProject() }
            is WorkspaceAction.ShowEditProject -> viewModelScope.launch { showEditProject(action.id) }
            is WorkspaceAction.OpenProject -> viewModelScope.launch { openProject(action.id) }
            is WorkspaceAction.RequestDeleteProject -> _state.update {
                it.copy(deleteTarget = WorkspaceDeleteTarget.Project(action.id, action.name), mutationError = null)
            }
            is WorkspaceAction.RequestDeleteDocument -> _state.update {
                it.copy(deleteTarget = WorkspaceDeleteTarget.Document(action.id, action.name), mutationError = null)
            }
            is WorkspaceAction.OpenDocument -> viewModelScope.launch { openDocument(action.id, 0) }
            WorkspaceAction.CloseDocument -> _state.update {
                it.copy(
                    previewDocumentId = null,
                    preview = null,
                    previewError = null,
                    previewLoading = false,
                )
            }
            WorkspaceAction.RetryDocumentPreview -> viewModelScope.launch {
                _state.value.previewDocumentId?.let { id ->
                    openDocument(id, _state.value.preview?.pageIndex ?: 0)
                }
            }
            is WorkspaceAction.OpenDocumentPage -> viewModelScope.launch {
                _state.value.previewDocumentId?.let { openDocument(it, action.pageIndex) }
            }
            WorkspaceAction.DismissDelete -> if (!_state.value.isMutating) {
                _state.update { it.copy(deleteTarget = null, mutationError = null) }
            }
            WorkspaceAction.ConfirmDelete -> viewModelScope.launch { deleteSelected() }
            WorkspaceAction.Back -> viewModelScope.launch { _events.send(WorkspaceEvent.NavigateBack) }
            is WorkspaceAction.OpenSitePreview -> _state.update { it.copy(previewSiteId = action.siteId) }
            WorkspaceAction.CloseSitePreview -> _state.update { it.copy(previewSiteId = null) }
            WorkspaceAction.OnNewTask -> _state.update { it.copy(taskEditor = TaskEditorState()) }
            is WorkspaceAction.OnEditTask -> openTaskEditor(action.id)
            WorkspaceAction.OnCloseTaskEditor -> if (_state.value.taskEditor?.saving != true) {
                _state.update { it.copy(taskEditor = null, mutationError = null) }
            }
            is WorkspaceAction.OnTaskTitleChange -> updateTaskEditor { it.copy(title = action.value, fieldError = null) }
            is WorkspaceAction.OnTaskDescriptionChange -> updateTaskEditor { it.copy(description = action.value, fieldError = null) }
            is WorkspaceAction.OnTaskDueChange -> updateTaskEditor { it.copy(dueAt = action.value, fieldError = null) }
            is WorkspaceAction.OnTaskStatusChange -> updateTaskEditor { it.copy(status = action.status, fieldError = null) }
            is WorkspaceAction.OnTaskNewSubtaskChange -> updateTaskEditor { it.copy(newSubtaskTitle = action.value) }
            WorkspaceAction.OnTaskAddSubtask -> addTaskSubtask()
            is WorkspaceAction.OnTaskToggleSubtask -> updateTaskEditor { editor ->
                editor.copy(subtasks = editor.subtasks.map { if (it.id == action.id) it.copy(done = action.done) else it })
            }
            is WorkspaceAction.OnTaskRemoveSubtask -> updateTaskEditor { editor ->
                editor.copy(subtasks = editor.subtasks.filterNot { it.id == action.id })
            }
            WorkspaceAction.OnTaskSave -> viewModelScope.launch { saveTaskEditor() }
            is WorkspaceAction.OnRequestDeleteTask -> _state.update {
                it.copy(deleteTarget = WorkspaceDeleteTarget.Task(action.id, action.title), mutationError = null)
            }
        }
    }

    private suspend fun awaitThenLoad(section: WorkspaceSection) {
        while (_state.value.isLoading) delay(LOAD_WAIT_INTERVAL_MS)
        if (_state.value.section == section) load(section)
    }

    private suspend fun load(section: WorkspaceSection) {
        if (_state.value.isLoading) return
        _state.update { it.copy(isLoading = true, error = null) }
        when (section) {
            WorkspaceSection.Projects -> applyResult(
                section,
                repository.listProjects(_state.value.query, sort = _state.value.projectSort),
            ) { page ->
                _state.update { state ->
                    state.copy(
                        projects = page.items.map(Project::toUi),
                        nextCursors = state.nextCursors + (section to page.nextCursor),
                    )
                }
            }
            WorkspaceSection.Documents -> applyResult(
                section,
                repository.listDocuments(query = _state.value.query),
            ) { page ->
                _state.update { state ->
                    state.copy(
                        documents = page.items.map(WorkspaceDocument::toUi),
                        nextCursors = state.nextCursors + (section to page.nextCursor),
                    )
                }
            }
            WorkspaceSection.Images -> applyResult(section, repository.listImages()) { images ->
                _state.update { state -> state.copy(images = images.map(WorkspaceImage::toUi)) }
                loadImageBytes(images)
            }
            WorkspaceSection.Sites -> {
                val scopeId = _state.value.scopeSessionId
                if (scopeId.isNullOrBlank()) {
                    _state.update {
                        it.copy(sites = emptyList(), isLoading = false, loadedSections = it.loadedSections + section)
                    }
                } else {
                    applyResult(section, repository.listScopeSites(scopeId)) { entries ->
                        _state.update { state -> state.copy(sites = entries.map(ScopeSiteEntry::toUi)) }
                    }
                }
            }
            WorkspaceSection.Tasks -> {
                val scopeId = _state.value.scopeSessionId
                if (scopeId.isNullOrBlank()) {
                    _state.update {
                        it.copy(tasks = emptyList(), isLoading = false, loadedSections = it.loadedSections + section)
                    }
                } else {
                    applyResult(section, repository.listTasks(scopeId)) { tasks ->
                        _state.update { state -> state.copy(tasks = tasks.map(WorkspaceTask::toUi)) }
                    }
                }
            }
        }
    }

    private suspend fun loadMore() {
        val section = _state.value.section
        val cursor = _state.value.nextCursors[section] ?: return
        if (_state.value.isLoadingMore) return
        _state.update { it.copy(isLoadingMore = true) }
        when (section) {
            WorkspaceSection.Projects -> repository.listProjects(
                _state.value.query,
                cursor,
                _state.value.projectSort,
            )
                .onSuccess { page ->
                    _state.update {
                        it.copy(
                            projects = (it.projects + page.items.map(Project::toUi)).distinctBy(ProjectUi::id),
                            nextCursors = it.nextCursors + (section to page.nextCursor),
                            isLoadingMore = false,
                        )
                    }
                }.onFailure { finishLoadMore(it) }
            WorkspaceSection.Documents -> repository.listDocuments(
                query = _state.value.query,
                cursor = cursor,
            ).onSuccess { page ->
                _state.update {
                    it.copy(
                        documents = (it.documents + page.items.map(WorkspaceDocument::toUi))
                            .distinctBy(DocumentUi::id),
                        nextCursors = it.nextCursors + (section to page.nextCursor),
                        isLoadingMore = false,
                    )
                }
            }.onFailure { finishLoadMore(it) }
            WorkspaceSection.Images -> _state.update { it.copy(isLoadingMore = false) }
            WorkspaceSection.Sites -> _state.update { it.copy(isLoadingMore = false) }
            WorkspaceSection.Tasks -> _state.update { it.copy(isLoadingMore = false) }
        }
    }

    private fun finishLoadMore(error: WorkspaceError) {
        _state.update { it.copy(isLoadingMore = false, error = error.toUiText()) }
    }

    private suspend fun <T> applyResult(
        section: WorkspaceSection,
        result: Result<T, WorkspaceError>,
        onSuccess: suspend (T) -> Unit,
    ) {
        when (result) {
            is Result.Success -> {
                onSuccess(result.data)
                _state.update {
                    it.copy(isLoading = false, loadedSections = it.loadedSections + section)
                }
            }
            is Result.Error -> _state.update {
                it.copy(isLoading = false, error = result.error.toUiText())
            }
        }
    }

    private suspend fun createProject() {
        val name = _state.value.projectName.trim()
        if (name.isBlank()) {
            _state.update { it.copy(mutationError = UiText.StringResource(AnrealCopy.ERROR_TITLE_REQUIRED)) }
            return
        }
        _state.update { it.copy(isMutating = true, mutationError = null) }
        val description = _state.value.projectDescription.trim().ifBlank { null }
        val editId = _state.value.editProjectId
        val result = if (editId == null) {
            repository.createProject(name, description)
        } else {
            repository.updateProject(editId, name, description)
        }
        when (result) {
            is Result.Success -> _state.update {
                it.copy(
                    projects = if (editId == null) {
                        listOf(result.data.toUi()) + it.projects
                    } else {
                        it.projects.map { project ->
                            if (project.id == editId) result.data.toUi() else project
                        }
                    },
                    showCreateProject = false,
                    editProjectId = null,
                    isMutating = false,
                )
            }
            is Result.Error -> _state.update {
                it.copy(isMutating = false, mutationError = result.error.toUiText())
            }
        }
    }

    private suspend fun showEditProject(id: String) {
        _state.update { it.copy(isMutating = true, mutationError = null) }
        when (val result = repository.getProject(id)) {
            is Result.Success -> _state.update {
                it.copy(
                    showCreateProject = true,
                    editProjectId = id,
                    projectName = result.data.name,
                    projectDescription = result.data.description.orEmpty(),
                    isMutating = false,
                )
            }
            is Result.Error -> _state.update {
                it.copy(isMutating = false, mutationError = result.error.toUiText())
            }
        }
    }

    private suspend fun openDocument(id: String, pageIndex: Int) {
        _state.update {
            it.copy(previewDocumentId = id, previewLoading = true, previewError = null)
        }
        when (val result = repository.getDocumentPreview(id, pageIndex.coerceAtLeast(0))) {
            is Result.Success -> {
                val preview = result.data
                val page = preview.pages.firstOrNull()
                val images = if (page == null) {
                    emptyList()
                } else {
                    page.images.mapNotNull { image ->
                        when (val bytes = repository.getDocumentPageImage(id, page.pageIndex, image.id)) {
                            is Result.Success -> bytes.data
                            is Result.Error -> null
                        }
                    }
                }
                _state.update { it.copy(preview = preview.toUi(images), previewLoading = false) }
            }
            is Result.Error -> _state.update {
                it.copy(previewLoading = false, previewError = result.error.toUiText())
            }
        }
    }

    private fun loadImageBytes(images: List<WorkspaceImage>) {
        images.forEach { image ->
            _state.update { state ->
                state.copy(
                    images = state.images.map { item ->
                        if (item.id == image.id) item.copy(loading = true) else item
                    },
                )
            }
            viewModelScope.launch {
                repository.getImageBytes(image.id).onSuccess { bytes ->
                    _state.update { state ->
                        state.copy(
                            images = state.images.map { item ->
                                if (item.id == image.id) item.copy(bytes = bytes, loading = false) else item
                            },
                        )
                    }
                }.onFailure {
                    _state.update { state ->
                        state.copy(
                            images = state.images.map { item ->
                                if (item.id == image.id) item.copy(loading = false) else item
                            },
                        )
                    }
                }
            }
        }
    }

    private suspend fun openProject(id: String) {
        _state.update { it.copy(isLoading = true, error = null) }
        when (val result = repository.openProject(id)) {
            is Result.Success -> {
                _state.update { it.copy(isLoading = false) }
                _events.send(WorkspaceEvent.OpenProject(result.data.id, result.data.name))
            }
            is Result.Error -> _state.update {
                it.copy(isLoading = false, error = result.error.toUiText())
            }
        }
    }

    private fun openTaskEditor(id: String) {
        val source = _state.value.tasks.firstOrNull { it.id == id } ?: return
        _state.update {
            it.copy(
                taskEditor = TaskEditorState(
                    isNew = false,
                    sourceId = source.id,
                    title = source.title,
                    description = source.description.orEmpty(),
                    dueAt = source.dueAt.orEmpty(),
                    status = source.status,
                    subtasks = source.subtasks,
                ),
                mutationError = null,
            )
        }
    }

    private inline fun updateTaskEditor(transform: (TaskEditorState) -> TaskEditorState) {
        _state.update { it.copy(taskEditor = it.taskEditor?.let(transform)) }
    }

    private fun addTaskSubtask() {
        val title = _state.value.taskEditor?.newSubtaskTitle?.trim().orEmpty()
        if (title.isEmpty() || title.length > 200) return
        val editor = _state.value.taskEditor ?: return
        _state.update {
            it.copy(
                taskEditor = editor.copy(
                    subtasks = editor.subtasks + SubtaskUi(id = "local-${editor.subtasks.size}", title = title, done = false),
                    newSubtaskTitle = "",
                ),
            )
        }
    }

    private suspend fun saveTaskEditor() {
        val editor = _state.value.taskEditor ?: return
        val scopeId = _state.value.scopeSessionId
        if (scopeId.isNullOrBlank()) {
            _state.update { it.copy(taskEditor = editor.copy(saving = false)) }
            return
        }
        val title = editor.title.trim()
        if (title.isEmpty() || title.length > 200) {
            updateTaskEditor { it.copy(fieldError = UiText.StringResource(AnrealCopy.ERROR_TITLE_REQUIRED)) }
            return
        }
        val dueAt = editor.dueAt.trim().ifEmpty { null }
        if (dueAt != null && !isValidDueAt(dueAt)) {
            updateTaskEditor { it.copy(fieldError = UiText.StringResource(AnrealCopy.ERROR_DUE_DATE_INVALID)) }
            return
        }
        _state.update { it.copy(taskEditor = editor.copy(saving = true, fieldError = null)) }
        if (editor.isNew) {
            when (val result = repository.createTask(scopeId, title, editor.description.trim().ifEmpty { null }, editor.subtasks.map { it.title }, dueAt)) {
                is Result.Success -> _state.update {
                    it.copy(
                        tasks = listOf(result.data.toUi()) + it.tasks,
                        loadedSections = it.loadedSections + WorkspaceSection.Tasks,
                        taskEditor = null,
                    )
                }
                is Result.Error -> _state.update {
                    it.copy(taskEditor = it.taskEditor?.copy(saving = false, fieldError = result.error.toUiText()))
                }
            }
            return
        }
        val sourceId = editor.sourceId ?: return
        val source = _state.value.tasks.firstOrNull { it.id == sourceId }
        val description = editor.description.trim().ifEmpty { null }
        val added = editor.subtasks.filter { it.id.startsWith("local-") }.map { it.title }
        val keptIds = editor.subtasks.map { it.id }.toSet()
        val removed = (source?.subtasks.orEmpty().map { it.id } - keptIds).toList()
        val toggled = editor.subtasks.mapNotNull { ui ->
            val before = source?.subtasks?.firstOrNull { it.id == ui.id }
            if (before != null && before.done != ui.done) ui.id to ui.done else null
        }
        val statusChanged = if (source?.status != editor.status) editor.status else null
        val titleChanged = if (source?.title != title) title else null
        val descriptionChanged = if (source?.description != description) description else null
        if (statusChanged == null && titleChanged == null && descriptionChanged == null &&
            added.isEmpty() && toggled.isEmpty() && removed.isEmpty()
        ) {
            updateTaskEditor { it.copy(saving = false, fieldError = UiText.StringResource(AnrealCopy.ERROR_NOTHING_TO_UPDATE)) }
            return
        }
        when (
            val result = repository.updateTask(
                sessionId = scopeId, id = sourceId, status = statusChanged, title = titleChanged,
                description = descriptionChanged, addSubtasks = added,
                toggleSubtasks = toggled, removeSubtasks = removed,
            )
        ) {
            is Result.Success -> _state.update {
                it.copy(
                    tasks = it.tasks.map { item -> if (item.id == sourceId) result.data.toUi() else item },
                    taskEditor = null,
                )
            }
            is Result.Error -> _state.update {
                it.copy(taskEditor = it.taskEditor?.copy(saving = false, fieldError = result.error.toUiText()))
            }
        }
    }

    private val dueAtRegex = Regex("""^\d{4}-\d{2}-\d{2}T\d{2}:\d{2}(:\d{2}(\.\d+)?)?(Z|[+-]\d{2}:?\d{2})$""")

    private fun isValidDueAt(value: String): Boolean = dueAtRegex.matches(value.trim())

    private suspend fun deleteSelected() {
        val target = _state.value.deleteTarget ?: return
        _state.update { it.copy(isMutating = true, mutationError = null) }
        val result = when (target) {
            is WorkspaceDeleteTarget.Project -> repository.deleteProject(target.id)
            is WorkspaceDeleteTarget.Document -> repository.deleteDocument(target.id)
            is WorkspaceDeleteTarget.Task -> repository.deleteTask(_state.value.scopeSessionId.orEmpty(), target.id)
        }
        when (result) {
            is Result.Success -> _state.update {
                it.copy(
                    projects = it.projects.filterNot { project -> project.id == target.id },
                    documents = it.documents.filterNot { document -> document.id == target.id },
                    tasks = it.tasks.filterNot { task -> task.id == target.id },
                    deleteTarget = null,
                    isMutating = false,
                )
            }
            is Result.Error -> _state.update {
                it.copy(isMutating = false, mutationError = result.error.toUiText())
            }
        }
    }
}

private fun WorkspaceError.toUiText(): UiText = when (this) {
    is WorkspaceError.Network -> error.toUiText()
}

private fun Project.toUi(): ProjectUi = ProjectUi(id, name, description.orEmpty(), documentCount, chatCount)

private fun WorkspaceTask.toUi(): TaskUi = TaskUi(
    id = id,
    title = title,
    status = status,
    description = description,
    subtasks = subtasks.map { SubtaskUi(it.id, it.title, it.done) },
    dueAt = dueAt,
)

private fun ScopeSiteEntry.toUi(): SiteUi = SiteUi(
    siteId = siteId,
    sessionId = sessionId,
    version = version,
    stableVersion = stableVersion,
    statusLabel = when (status) {
        ScopeSiteStatus.Queued -> "queued"
        ScopeSiteStatus.Running -> "running"
        ScopeSiteStatus.Ready -> "ready"
        ScopeSiteStatus.Failed -> "failed"
    },
    previewUrl = previewUrl,
    downloadUrl = downloadUrl,
)

private fun WorkspaceDocument.toUi(): DocumentUi = DocumentUi(
    id = id,
    filename = filename,
    summary = summary,
    detail = "$pageCount pages",
    projectName = projectName,
)

private fun WorkspaceImage.toUi(): ImageUi = ImageUi(
    id = id,
    prompt = prompt,
    detail = listOf(modelId, "${width}×$height").filter(String::isNotBlank).joinToString(" · "),
)

private fun DocumentPreview.toUi(images: List<ByteArray>): DocumentPreviewUi {
    val page = pages.firstOrNull()
    return DocumentPreviewUi(
        id = id,
        filename = filename,
        summary = summary,
        pageIndex = page?.pageIndex ?: 0,
        pageCount = pageCount,
        markdown = page?.markdown.orEmpty(),
        images = images,
    )
}
