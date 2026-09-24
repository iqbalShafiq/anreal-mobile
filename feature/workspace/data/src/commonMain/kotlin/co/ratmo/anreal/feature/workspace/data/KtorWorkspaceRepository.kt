package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.getBytes
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.postForResponse
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.core.domain.util.mapError
import co.ratmo.anreal.feature.workspace.domain.Project
import co.ratmo.anreal.feature.workspace.domain.ArtifactItem
import co.ratmo.anreal.feature.workspace.domain.ArtifactType
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteEntry
import co.ratmo.anreal.feature.workspace.domain.ScheduleFreq
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import co.ratmo.anreal.feature.workspace.domain.WorkspaceSchedule
import co.ratmo.anreal.feature.workspace.domain.WorkspaceTask
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspacePage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceProjectSort
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import io.ktor.client.HttpClient

class KtorWorkspaceRepository(private val httpClient: HttpClient) : WorkspaceRepository {
    override suspend fun listProjects(
        query: String?,
        cursor: String?,
        sort: WorkspaceProjectSort,
    ): Result<WorkspacePage<Project>, WorkspaceError> =
        httpClient.get<ProjectPageDto>(
            route = "/api/projects",
            queryParameters = mapOf(
                "q" to query,
                "cursor" to cursor,
                "limit" to 50,
                "sort" to sort.wire,
            ),
        ).map(ProjectPageDto::toPage).mapWorkspaceError()

    override suspend fun createProject(
        name: String,
        description: String?,
    ): Result<Project, WorkspaceError> = httpClient.post<ProjectMutationDto, ProjectDto>(
        route = "/api/projects",
        body = ProjectMutationDto(name, description),
    ).map(ProjectDto::toProject).mapWorkspaceError()

    override suspend fun getProject(id: String): Result<Project, WorkspaceError> =
        httpClient.get<ProjectDto>(route = "/api/projects/$id")
            .map(ProjectDto::toProject)
            .mapWorkspaceError()

    override suspend fun updateProject(
        id: String,
        name: String,
        description: String?,
    ): Result<Project, WorkspaceError> = httpClient.patch<ProjectMutationDto, ProjectDto>(
        route = "/api/projects/$id",
        body = ProjectMutationDto(name, description),
    ).map(ProjectDto::toProject).mapWorkspaceError()

    override suspend fun openProject(id: String): Result<Project, WorkspaceError> =
        httpClient.postForResponse<ProjectDto>(route = "/api/projects/$id/open")
            .map(ProjectDto::toProject)
            .mapWorkspaceError()

    override suspend fun deleteProject(id: String): EmptyResult<WorkspaceError> =
        httpClient.delete(route = "/api/projects/$id", queryParameters = mapOf("confirm" to true))
            .mapWorkspaceError()
            .asEmptyResult()

    override suspend fun listDocuments(
        projectId: String?,
        query: String?,
        cursor: String?,
    ): Result<WorkspacePage<WorkspaceDocument>, WorkspaceError> =
        httpClient.get<DocumentPageDto>(
            route = "/api/documents/library",
            queryParameters = mapOf(
                "scope" to "browser",
                "projectId" to projectId,
                "q" to query,
                "cursor" to cursor,
                "limit" to 50,
            ),
        ).map(DocumentPageDto::toPage).mapWorkspaceError()

    override suspend fun deleteDocument(id: String): EmptyResult<WorkspaceError> =
        httpClient.delete(route = "/api/documents/$id", queryParameters = mapOf("confirm" to true))
            .mapWorkspaceError()
            .asEmptyResult()

    override suspend fun getDocumentPreview(
        id: String,
        pageIndex: Int,
        pageLimit: Int,
    ): Result<DocumentPreview, WorkspaceError> = httpClient.get<DocumentPreviewDto>(
        route = "/api/documents/$id/preview",
        queryParameters = mapOf("pageIndex" to pageIndex, "pageLimit" to pageLimit),
    ).map(DocumentPreviewDto::toPreview).mapWorkspaceError()

    override suspend fun getDocumentPageImage(
        documentId: String,
        pageIndex: Int,
        imageId: String,
    ): Result<ByteArray, WorkspaceError> = httpClient.getBytes(
        route = "/api/documents/$documentId/pages/$pageIndex/images/$imageId",
    ).mapWorkspaceError()

    override suspend fun listImages(projectId: String?): Result<List<WorkspaceImage>, WorkspaceError> =
        httpClient.get<ImagePageDto>(
            route = "/api/images",
            queryParameters = if (projectId == null) mapOf("scope" to "user") else mapOf("projectId" to projectId),
        ).map { page -> page.images.map(ImageDto::toImage) }.mapWorkspaceError()

    override suspend fun getImageBytes(id: String): Result<ByteArray, WorkspaceError> =
        httpClient.getBytes(route = "/api/images/$id").mapWorkspaceError()

    override suspend fun listScopeSites(sessionId: String): Result<List<ScopeSiteEntry>, WorkspaceError> =
        httpClient.get<ScopeSitesDto>(
            route = "/api/sites",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { dto -> dto.sites.map { it.toScopeEntry() } }.mapWorkspaceError()

    override suspend fun listTasks(sessionId: String): Result<List<WorkspaceTask>, WorkspaceError> =
        httpClient.get<TaskListDto>(
            route = "/api/tasks",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { dto -> dto.items.map { it.toTask() } }.mapWorkspaceError()

    override suspend fun createTask(
        sessionId: String,
        title: String,
        description: String?,
        subtasks: List<String>,
        dueAt: String?,
    ): Result<WorkspaceTask, WorkspaceError> = httpClient.post<TaskCreateDto, WorkspaceTaskDto>(
        route = "/api/tasks",
        body = TaskCreateDto(
            sessionId = sessionId,
            title = title,
            description = description,
            addSubtasks = subtasks.ifEmpty { null },
            dueAt = dueAt,
        ),
    ).map { it.toTask() }.mapWorkspaceError()

    override suspend fun updateTask(
        sessionId: String,
        id: String,
        status: TaskStatus?,
        title: String?,
        description: String?,
        addSubtasks: List<String>,
        toggleSubtasks: List<Pair<String, Boolean>>,
        removeSubtasks: List<String>,
    ): Result<WorkspaceTask, WorkspaceError> = httpClient.patch<TaskUpdateDto, WorkspaceTaskDto>(
        route = "/api/tasks/$id",
        body = TaskUpdateDto(
            sessionId = sessionId,
            status = status?.toWire(),
            title = title,
            description = description,
            addSubtasks = addSubtasks.ifEmpty { null },
            toggleSubtasks = toggleSubtasks.map { TaskToggleDto(it.first, it.second) }.ifEmpty { null },
            removeSubtasks = removeSubtasks.ifEmpty { null },
        ),
    ).map { it.toTask() }.mapWorkspaceError()

    override suspend fun deleteTask(sessionId: String, id: String): EmptyResult<WorkspaceError> =
        httpClient.delete(route = "/api/tasks/$id", queryParameters = mapOf("sessionId" to sessionId))
            .mapWorkspaceError()
            .asEmptyResult()

    override suspend fun listSchedules(sessionId: String): Result<List<WorkspaceSchedule>, WorkspaceError> =
        httpClient.get<ScheduleListDto>(
            route = "/api/schedules",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { dto -> dto.items.map { it.toSchedule() } }.mapWorkspaceError()

    override suspend fun createSchedule(
        sessionId: String,
        title: String,
        prompt: String,
        freq: ScheduleFreq,
        runAt: String?,
    ): Result<WorkspaceSchedule, WorkspaceError> = httpClient.post<ScheduleCreateDto, WorkspaceScheduleDto>(
        route = "/api/schedules",
        body = ScheduleCreateDto(sessionId = sessionId, title = title, prompt = prompt, freq = freq.toWire(), runAt = runAt),
    ).map { it.toSchedule() }.mapWorkspaceError()

    override suspend fun cancelSchedule(sessionId: String, id: String): EmptyResult<WorkspaceError> =
        httpClient.delete(route = "/api/schedules/$id", queryParameters = mapOf("sessionId" to sessionId))
            .mapWorkspaceError()
            .asEmptyResult()

    override suspend fun listArtifacts(
        sessionId: String,
        type: ArtifactType?,
        query: String?,
    ): Result<List<ArtifactItem>, WorkspaceError> =
        httpClient.get<ArtifactListDto>(
            route = "/api/artifacts",
            queryParameters = mapOf(
                "sessionId" to sessionId,
                "type" to type?.toWire(),
                "q" to query?.ifBlank { null },
            ),
        ).map { dto -> dto.items.map { it.toArtifact() } }.mapWorkspaceError()

    override suspend fun getArtifact(
        sessionId: String,
        type: ArtifactType,
        id: String,
    ): Result<ArtifactItem, WorkspaceError> =
        httpClient.get<ArtifactDetailDto>(
            route = "/api/artifacts/$id",
            queryParameters = mapOf(
                "sessionId" to sessionId,
                "type" to (type.toWire() ?: return Result.Error(
                    WorkspaceError.Network(DataError.Network(DataError.Network.Kind.BAD_REQUEST, 400, "Unknown artifact type", null, emptyMap())),
                )),
            ),
        ).map { it.artifact.toArtifact() }.mapWorkspaceError()

    override suspend fun updateImageCaption(
        sessionId: String,
        imageId: String,
        caption: String,
    ): Result<ArtifactItem, WorkspaceError> = httpClient.patch<ImageCaptionDto, ArtifactItemDto>(
        route = "/api/artifacts/images/$imageId",
        body = ImageCaptionDto(sessionId = sessionId, caption = caption),
    ).map { it.toArtifact() }.mapWorkspaceError()
}

private fun <T> Result<T, DataError.Network>.mapWorkspaceError(): Result<T, WorkspaceError> =
    mapError(WorkspaceError::Network)
