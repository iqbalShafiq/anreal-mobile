package co.ratmo.anreal.feature.workspace.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.Project
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteEntry
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteStatus
import co.ratmo.anreal.feature.workspace.domain.ArtifactItem
import co.ratmo.anreal.feature.workspace.domain.ArtifactType
import co.ratmo.anreal.feature.workspace.domain.ScheduleFreq
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import co.ratmo.anreal.feature.workspace.domain.TaskSubtask
import co.ratmo.anreal.feature.workspace.domain.WorkspaceSchedule
import co.ratmo.anreal.feature.workspace.domain.WorkspaceTask
import co.ratmo.anreal.feature.workspace.domain.WorkspaceProjectSort
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspacePage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class WorkspaceViewModelTest {
    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initial_section_loads_and_maps_projects() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Projects, repository)

        assertThat(repository.projectLoads).isEqualTo(1)
        assertThat(viewModel.state.value.projects).hasSize(1)
        assertThat(viewModel.state.value.projects.single().name).isEqualTo("Research")
        assertThat(viewModel.state.value.loadedSections).isEqualTo(setOf(WorkspaceSection.Projects))
        assertThat(viewModel.state.value.isLoading).isFalse()
    }

    @Test
    fun selecting_an_already_loaded_section_does_not_fetch_twice() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Projects, repository)

        viewModel.onAction(WorkspaceAction.SelectSection(WorkspaceSection.Projects))

        assertThat(repository.projectLoads).isEqualTo(1)
    }

    @Test
    fun creating_project_validates_then_prepends_server_result() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Projects, repository)

        viewModel.onAction(WorkspaceAction.ShowCreateProject)
        viewModel.onAction(WorkspaceAction.CreateProject)
        assertThat(viewModel.state.value.mutationError).isNotNull()
        assertThat(repository.createdNames).hasSize(0)

        viewModel.onAction(WorkspaceAction.ChangeProjectName("Mobile"))
        viewModel.onAction(WorkspaceAction.CreateProject)

        assertThat(repository.createdNames).isEqualTo(listOf("Mobile"))
        assertThat(viewModel.state.value.projects.first().name).isEqualTo("Mobile")
        assertThat(viewModel.state.value.showCreateProject).isFalse()
    }

    @Test
    fun deleting_document_removes_it_after_server_success() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Documents, repository)

        val document = viewModel.state.value.documents.single()
        viewModel.onAction(WorkspaceAction.RequestDeleteDocument(document.id, document.filename))
        viewModel.onAction(WorkspaceAction.ConfirmDelete)

        assertThat(repository.deletedDocumentIds).isEqualTo(listOf(document.id))
        assertThat(viewModel.state.value.documents).hasSize(0)
        assertThat(viewModel.state.value.deleteTarget == null).isTrue()
    }

    @Test
    fun opening_project_emits_navigation_only_after_server_success() = runTest {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Projects, repository)

        viewModel.events.test {
            viewModel.onAction(WorkspaceAction.OpenProject("p1"))
            assertThat(awaitItem()).isEqualTo(WorkspaceEvent.OpenProject("p1", "Research"))
        }
    }

    @Test
    fun images_become_visible_when_their_binary_payload_finishes_loading() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Images, repository)

        assertThat(viewModel.state.value.loadedSections).isEqualTo(setOf(WorkspaceSection.Images))
        assertThat(viewModel.state.value.images.single().bytes?.toList())
            .isEqualTo(byteArrayOf(1, 2, 3).toList())
        assertThat(viewModel.state.value.images.single().loading).isFalse()
    }
    @Test
    fun sites_section_loads_scope_entries() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Sites, repository, scopeSessionId = "abc")

        assertThat(repository.scopeLoads).isEqualTo(listOf("abc"))
        assertThat(viewModel.state.value.sites).hasSize(1)
        assertThat(viewModel.state.value.sites.single().siteId).isEqualTo("s1")
        assertThat(viewModel.state.value.loadedSections).isEqualTo(setOf(WorkspaceSection.Sites))
    }

    @Test
    fun scope_unknown_session_shows_retry() {
        val repository = FakeWorkspaceRepository().apply { scopeError = true }

        val viewModel = WorkspaceViewModel(WorkspaceSection.Sites, repository, scopeSessionId = "ghost")

        assertThat(viewModel.state.value.sites).hasSize(0)
        assertThat(viewModel.state.value.error).isNotNull()
        val calls = repository.scopeLoads.size
        viewModel.onAction(WorkspaceAction.Retry)
        assertThat(repository.scopeLoads.size).isEqualTo(calls + 1)
    }

    @Test
    fun sites_without_scope_session_loads_nothing() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Sites, repository)

        assertThat(repository.scopeLoads).hasSize(0)
        assertThat(viewModel.state.value.sites).hasSize(0)
    }

    @Test
    fun tasks_section_loads_items() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Tasks, repository, scopeSessionId = "abc")

        assertThat(repository.taskScopes).isEqualTo(listOf("abc"))
        assertThat(viewModel.state.value.tasks).hasSize(1)
        assertThat(viewModel.state.value.tasks.single().title).isEqualTo("Fix login")
    }

    @Test
    fun task_save_with_no_changes_makes_no_network_call() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Tasks, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnEditTask("t1"))
        viewModel.onAction(WorkspaceAction.OnTaskSave)

        assertThat(repository.taskUpdates).hasSize(0)
        assertThat(viewModel.state.value.taskEditor).isNotNull()
    }

    @Test
    fun task_create_validates_title_then_saves() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Tasks, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnNewTask)
        viewModel.onAction(WorkspaceAction.OnTaskSave)
        assertThat(repository.taskCreates).hasSize(0)

        viewModel.onAction(WorkspaceAction.OnTaskTitleChange("Ship it"))
        viewModel.onAction(WorkspaceAction.OnTaskSave)
        assertThat(repository.taskCreates).isEqualTo(listOf("Ship it"))
        assertThat(viewModel.state.value.taskEditor).isNull()
        assertThat(viewModel.state.value.tasks.first().title).isEqualTo("Ship it")
    }

    @Test
    fun task_subtask_toggle_persists_and_updates_list() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Tasks, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnEditTask("t1"))
        viewModel.onAction(WorkspaceAction.OnTaskToggleSubtask("st1", true))
        viewModel.onAction(WorkspaceAction.OnTaskSave)

        assertThat(repository.taskUpdates).hasSize(1)
        assertThat(viewModel.state.value.tasks.single().doneCount).isEqualTo(1)
    }

    @Test
    fun schedules_section_loads_items() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Schedules, repository, scopeSessionId = "abc")

        assertThat(repository.scheduleScopes).isEqualTo(listOf("abc"))
        assertThat(viewModel.state.value.schedules).hasSize(1)
        assertThat(viewModel.state.value.schedules.single().title).isEqualTo("Morning brief")
    }

    @Test
    fun schedule_create_validates_then_saves() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Schedules, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnNewSchedule)
        viewModel.onAction(WorkspaceAction.OnScheduleSave)
        assertThat(repository.scheduleCreates).hasSize(0)

        viewModel.onAction(WorkspaceAction.OnScheduleTitleChange("Weekly"))
        viewModel.onAction(WorkspaceAction.OnSchedulePromptChange("Summarize"))
        viewModel.onAction(WorkspaceAction.OnScheduleSave)
        assertThat(repository.scheduleCreates).isEqualTo(listOf("Weekly"))
        assertThat(viewModel.state.value.scheduleEditor).isNull()
    }

    @Test
    fun schedule_cancel_removes_from_list() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Schedules, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnRequestCancelSchedule("sc1", "Morning brief"))
        viewModel.onAction(WorkspaceAction.ConfirmDelete)

        assertThat(repository.scheduleCancels).isEqualTo(listOf("sc1"))
        assertThat(viewModel.state.value.schedules).hasSize(0)
    }

    @Test
    fun artifacts_section_loads_items() {
        val repository = FakeWorkspaceRepository()

        val viewModel = WorkspaceViewModel(WorkspaceSection.Artifacts, repository, scopeSessionId = "abc")

        assertThat(repository.artifactScopes).isEqualTo(listOf("abc"))
        assertThat(viewModel.state.value.artifacts).hasSize(2)
    }

    @Test
    fun artifacts_type_filter_reloads() {
        val repository = FakeWorkspaceRepository()
        val viewModel = WorkspaceViewModel(WorkspaceSection.Artifacts, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnArtifactTypeFilter(ArtifactType.Image))

        assertThat(repository.artifactTypes).isEqualTo(listOf(null, ArtifactType.Image))
        assertThat(viewModel.state.value.artifactTypeFilter).isEqualTo(ArtifactType.Image)
    }

    @Test
    fun caption_404_keeps_editor() {
        val repository = FakeWorkspaceRepository().apply { captionError = true }
        val viewModel = WorkspaceViewModel(WorkspaceSection.Artifacts, repository, scopeSessionId = "abc")

        viewModel.onAction(WorkspaceAction.OnArtifactOpen("i1"))
        viewModel.onAction(WorkspaceAction.OnCaptionChange("Chart typed"))
        viewModel.onAction(WorkspaceAction.OnCaptionSave)

        assertThat(viewModel.state.value.artifactDetail).isNotNull()
        assertThat(viewModel.state.value.captionDraft).isEqualTo("Chart typed")
        assertThat(viewModel.state.value.captionError).isNotNull()
    }
}

private class FakeWorkspaceRepository : WorkspaceRepository {
    var projectLoads = 0
    val createdNames = mutableListOf<String>()
    val deletedDocumentIds = mutableListOf<String>()
    val scopeLoads = mutableListOf<String>()
    var scopeError = false

    private val project = Project("p1", "Research", "Notes", 1, 2, null, "now", "now")
    private val document = WorkspaceDocument(
        id = "d1",
        filename = "brief.pdf",
        summary = "Brief",
        sizeBytes = 1_024,
        mimeType = "application/pdf",
        pageCount = 2,
        createdAt = "now",
        originSessionId = "s1",
        projectId = null,
        projectName = null,
    )

    override suspend fun listProjects(
        query: String?,
        cursor: String?,
        sort: WorkspaceProjectSort,
    ): Result<WorkspacePage<Project>, WorkspaceError> {
        projectLoads += 1
        return Result.Success(WorkspacePage(listOf(project), null))
    }

    override suspend fun createProject(
        name: String,
        description: String?,
    ): Result<Project, WorkspaceError> {
        createdNames += name
        return Result.Success(project.copy(id = "p2", name = name, description = description))
    }

    override suspend fun getProject(id: String): Result<Project, WorkspaceError> =
        Result.Success(project.copy(id = id))

    override suspend fun updateProject(
        id: String,
        name: String,
        description: String?,
    ): Result<Project, WorkspaceError> = Result.Success(project.copy(id = id, name = name, description = description))

    override suspend fun openProject(id: String): Result<Project, WorkspaceError> = Result.Success(project.copy(id = id))

    override suspend fun deleteProject(id: String): EmptyResult<WorkspaceError> = Result.Success(Unit)

    override suspend fun listDocuments(
        projectId: String?,
        query: String?,
        cursor: String?,
    ): Result<WorkspacePage<WorkspaceDocument>, WorkspaceError> =
        Result.Success(WorkspacePage(listOf(document), null))

    override suspend fun deleteDocument(id: String): EmptyResult<WorkspaceError> {
        deletedDocumentIds += id
        return Result.Success(Unit)
    }

    override suspend fun getDocumentPreview(
        id: String,
        pageIndex: Int,
        pageLimit: Int,
    ): Result<DocumentPreview, WorkspaceError> = error("not needed")

    override suspend fun getDocumentPageImage(
        documentId: String,
        pageIndex: Int,
        imageId: String,
    ): Result<ByteArray, WorkspaceError> = error("not needed")

    override suspend fun listImages(projectId: String?): Result<List<WorkspaceImage>, WorkspaceError> =
        Result.Success(
            listOf(
                WorkspaceImage(
                    id = "i1",
                    projectId = null,
                    sessionId = "s1",
                    mediaType = "image/png",
                    width = 1024,
                    height = 768,
                    modelId = "image-model",
                    prompt = "A research chart",
                    nOfTotal = null,
                    createdAt = "now",
                ),
            ),
        )

    override suspend fun getImageBytes(id: String): Result<ByteArray, WorkspaceError> =
        Result.Success(byteArrayOf(1, 2, 3))

    override suspend fun listScopeSites(sessionId: String): Result<List<ScopeSiteEntry>, WorkspaceError> {
        scopeLoads += sessionId
        if (scopeError) {
            return Result.Error(
                WorkspaceError.Network(
                    DataError.Network(DataError.Network.Kind.NOT_FOUND, 404, "Session not found", "SESSION_NOT_FOUND", emptyMap()),
                ),
            )
        }
        return Result.Success(
            listOf(
                ScopeSiteEntry(
                    siteId = "s1", sessionId = sessionId, version = 2, stableVersion = 1,
                    status = ScopeSiteStatus.Ready,
                    previewUrl = "/api/sites/s1/v2/preview/index.html",
                    downloadUrl = "/api/sites/s1/v2/download", updatedAt = "t",
                ),
            ),
        )
    }

    private val task = WorkspaceTask(
        id = "t1", title = "Fix login", status = TaskStatus.Doing, description = "Old bug",
        subtasks = listOf(TaskSubtask("st1", "Repro", false)),
        sourceSessionId = "abc", dueAt = null,
    )
    val taskScopes = mutableListOf<String>()
    val taskCreates = mutableListOf<String>()
    val taskUpdates = mutableListOf<String>()

    override suspend fun listTasks(sessionId: String): Result<List<WorkspaceTask>, WorkspaceError> {
        taskScopes += sessionId
        return Result.Success(listOf(task))
    }

    override suspend fun createTask(
        sessionId: String,
        title: String,
        description: String?,
        subtasks: List<String>,
        dueAt: String?,
    ): Result<WorkspaceTask, WorkspaceError> {
        taskCreates += title
        return Result.Success(task.copy(id = "t2", title = title, status = TaskStatus.Inbox))
    }

    override suspend fun updateTask(
        sessionId: String,
        id: String,
        status: TaskStatus?,
        title: String?,
        description: String?,
        addSubtasks: List<String>,
        toggleSubtasks: List<Pair<String, Boolean>>,
        removeSubtasks: List<String>,
    ): Result<WorkspaceTask, WorkspaceError> {
        taskUpdates += id
        var updated = task.copy(id = id)
        if (status != null) updated = updated.copy(status = status)
        if (title != null) updated = updated.copy(title = title)
        if (description != null) updated = updated.copy(description = description)
        val toggled = toggleSubtasks.toMap()
        updated = updated.copy(
            subtasks = updated.subtasks.map { it.copy(done = toggled[it.id] ?: it.done) },
        )
        return Result.Success(updated)
    }

    override suspend fun deleteTask(sessionId: String, id: String): EmptyResult<WorkspaceError> =
        Result.Success(Unit)

    private val schedule = WorkspaceSchedule(
        id = "sc1", title = "Morning brief", prompt = "Summarize",
        freq = ScheduleFreq.Daily, nextRunAt = "2026-09-25T07:00:00+07:00", status = "active",
    )
    val scheduleScopes = mutableListOf<String>()
    val scheduleCreates = mutableListOf<String>()
    val scheduleCancels = mutableListOf<String>()

    override suspend fun listSchedules(sessionId: String): Result<List<WorkspaceSchedule>, WorkspaceError> {
        scheduleScopes += sessionId
        return Result.Success(listOf(schedule))
    }

    override suspend fun createSchedule(
        sessionId: String,
        title: String,
        prompt: String,
        freq: ScheduleFreq,
        runAt: String?,
    ): Result<WorkspaceSchedule, WorkspaceError> {
        scheduleCreates += title
        return Result.Success(schedule.copy(id = "sc2", title = title))
    }

    override suspend fun cancelSchedule(sessionId: String, id: String): EmptyResult<WorkspaceError> {
        scheduleCancels += id
        return Result.Success(Unit)
    }

    private val artifactImage = ArtifactItem(
        id = "i1", type = ArtifactType.Image, title = null,
        caption = "Chart", prompt = "Draw", status = null,
        previewUrl = null, downloadUrl = null, version = null,
    )
    private val artifactSite = ArtifactItem(
        id = "s1", type = ArtifactType.Site, title = null, caption = null,
        prompt = null, status = "ready",
        previewUrl = "/api/sites/s1/v1/preview/index.html",
        downloadUrl = "/api/sites/s1/v1/download", version = 1,
    )
    val artifactScopes = mutableListOf<String>()
    val artifactTypes = mutableListOf<ArtifactType?>()
    var captionError = false

    override suspend fun listArtifacts(
        sessionId: String,
        type: ArtifactType?,
        query: String?,
    ): Result<List<ArtifactItem>, WorkspaceError> {
        artifactScopes += sessionId
        artifactTypes += type
        return Result.Success(listOf(artifactImage, artifactSite))
    }

    override suspend fun getArtifact(
        sessionId: String,
        type: ArtifactType,
        id: String,
    ): Result<ArtifactItem, WorkspaceError> = Result.Success(artifactImage.copy(id = id))

    override suspend fun updateImageCaption(
        sessionId: String,
        imageId: String,
        caption: String,
    ): Result<ArtifactItem, WorkspaceError> {
        if (captionError) {
            return Result.Error(
                WorkspaceError.Network(
                    DataError.Network(DataError.Network.Kind.NOT_FOUND, 404, "Image not found", "IMAGE_NOT_FOUND", emptyMap()),
                ),
            )
        }
        return Result.Success(artifactImage.copy(caption = caption))
    }
}
