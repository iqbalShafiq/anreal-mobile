package co.ratmo.anreal.feature.workspace.domain

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result

fun interface WorkspaceBaseUrlProvider {
    fun baseUrl(): String
}

interface WorkspaceRepository {
    suspend fun listProjects(
        query: String? = null,
        cursor: String? = null,
        sort: WorkspaceProjectSort = WorkspaceProjectSort.UpdatedAt,
    ): Result<WorkspacePage<Project>, WorkspaceError>
    suspend fun createProject(name: String, description: String?): Result<Project, WorkspaceError>
    suspend fun getProject(id: String): Result<Project, WorkspaceError>
    suspend fun updateProject(id: String, name: String, description: String?): Result<Project, WorkspaceError>
    suspend fun openProject(id: String): Result<Project, WorkspaceError>
    suspend fun deleteProject(id: String): EmptyResult<WorkspaceError>
    suspend fun listDocuments(
        projectId: String? = null,
        query: String? = null,
        cursor: String? = null,
    ): Result<WorkspacePage<WorkspaceDocument>, WorkspaceError>
    suspend fun deleteDocument(id: String): EmptyResult<WorkspaceError>
    suspend fun getDocumentPreview(
        id: String,
        pageIndex: Int = 0,
        pageLimit: Int = 5,
    ): Result<DocumentPreview, WorkspaceError>
    suspend fun getDocumentPageImage(
        documentId: String,
        pageIndex: Int,
        imageId: String,
    ): Result<ByteArray, WorkspaceError>
    suspend fun listImages(projectId: String? = null): Result<List<WorkspaceImage>, WorkspaceError>
    suspend fun getImageBytes(id: String): Result<ByteArray, WorkspaceError>
    suspend fun listScopeSites(sessionId: String): Result<List<ScopeSiteEntry>, WorkspaceError>
    suspend fun listTasks(sessionId: String): Result<List<WorkspaceTask>, WorkspaceError>
    suspend fun createTask(
        sessionId: String,
        title: String,
        description: String?,
        subtasks: List<String>,
        dueAt: String?,
    ): Result<WorkspaceTask, WorkspaceError>
    suspend fun updateTask(
        sessionId: String,
        id: String,
        status: TaskStatus?,
        title: String?,
        description: String?,
        addSubtasks: List<String>,
        toggleSubtasks: List<Pair<String, Boolean>>,
        removeSubtasks: List<String>,
    ): Result<WorkspaceTask, WorkspaceError>
    suspend fun deleteTask(sessionId: String, id: String): EmptyResult<WorkspaceError>
}
