package co.ratmo.anreal.feature.workspace.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.hasSize
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.Project
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
            assertThat(awaitItem()).isEqualTo(WorkspaceEvent.OpenProject("p1"))
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
}

private class FakeWorkspaceRepository : WorkspaceRepository {
    var projectLoads = 0
    val createdNames = mutableListOf<String>()
    val deletedDocumentIds = mutableListOf<String>()

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
}
