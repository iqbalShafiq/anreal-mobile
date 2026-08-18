package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.workspace.domain.Project
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspacePage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository

class StubWorkspaceRepository : WorkspaceRepository {
    private val projects = mutableListOf(
        Project("project-1", "Quarterly review", "Board sources and research", 2, 4, null, "", ""),
    )

    override suspend fun listProjects(
        query: String?,
        cursor: String?,
    ): Result<WorkspacePage<Project>, WorkspaceError> =
        Result.Success(WorkspacePage(projects.filter { query.isNullOrBlank() || it.name.contains(query, true) }, null))

    override suspend fun createProject(name: String, description: String?): Result<Project, WorkspaceError> {
        val project = Project("project-${projects.size + 1}", name, description, 0, 0, null, "", "")
        projects.add(0, project)
        return Result.Success(project)
    }

    override suspend fun getProject(id: String): Result<Project, WorkspaceError> =
        Result.Success(projects.first { it.id == id })

    override suspend fun updateProject(id: String, name: String, description: String?): Result<Project, WorkspaceError> {
        val current = projects.first { it.id == id }
        val updated = current.copy(name = name, description = description)
        projects[projects.indexOf(current)] = updated
        return Result.Success(updated)
    }

    override suspend fun openProject(id: String): Result<Project, WorkspaceError> =
        Result.Success(projects.first { it.id == id })

    override suspend fun deleteProject(id: String): EmptyResult<WorkspaceError> {
        projects.removeAll { it.id == id }
        return Result.Success(Unit)
    }

    override suspend fun listDocuments(
        projectId: String?,
        query: String?,
        cursor: String?,
    ): Result<WorkspacePage<WorkspaceDocument>, WorkspaceError> =
        Result.Success(WorkspacePage(emptyList(), null))

    override suspend fun deleteDocument(id: String): EmptyResult<WorkspaceError> = Result.Success(Unit)

    override suspend fun getDocumentPreview(
        id: String,
        pageIndex: Int,
        pageLimit: Int,
    ): Result<DocumentPreview, WorkspaceError> = Result.Success(
        DocumentPreview(id, "Document.pdf", "application/pdf", 0, 0, "", emptyList()),
    )

    override suspend fun getDocumentPageImage(
        documentId: String,
        pageIndex: Int,
        imageId: String,
    ): Result<ByteArray, WorkspaceError> = Result.Success(ByteArray(0))

    override suspend fun listImages(projectId: String?): Result<List<WorkspaceImage>, WorkspaceError> =
        Result.Success(emptyList())

    override suspend fun getImageBytes(id: String): Result<ByteArray, WorkspaceError> = Result.Success(ByteArray(0))
}
