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
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspacePage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import io.ktor.client.HttpClient

class KtorWorkspaceRepository(private val httpClient: HttpClient) : WorkspaceRepository {
    override suspend fun listProjects(
        query: String?,
        cursor: String?,
    ): Result<WorkspacePage<Project>, WorkspaceError> =
        httpClient.get<ProjectPageDto>(
            route = "/api/projects",
            queryParameters = mapOf(
                "q" to query,
                "cursor" to cursor,
                "limit" to 50,
                "sort" to "updatedAt",
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
}

private fun <T> Result<T, DataError.Network>.mapWorkspaceError(): Result<T, WorkspaceError> =
    mapError(WorkspaceError::Network)
