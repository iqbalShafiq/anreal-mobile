package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.feature.workspace.domain.Project
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteEntry
import co.ratmo.anreal.feature.workspace.domain.ScopeSiteStatus
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
import co.ratmo.anreal.feature.workspace.domain.TaskSubtask
import co.ratmo.anreal.feature.workspace.domain.WorkspaceTask
import co.ratmo.anreal.feature.workspace.domain.DocumentPageImage
import co.ratmo.anreal.feature.workspace.domain.DocumentPreview
import co.ratmo.anreal.feature.workspace.domain.DocumentPreviewPage
import co.ratmo.anreal.feature.workspace.domain.WorkspaceDocument
import co.ratmo.anreal.feature.workspace.domain.WorkspaceImage
import co.ratmo.anreal.feature.workspace.domain.WorkspacePage
import kotlinx.serialization.Serializable

@Serializable
data class ProjectPageDto(val items: List<ProjectDto> = emptyList(), val nextCursor: String? = null)

@Serializable
data class ProjectDto(
    val id: String,
    val name: String,
    val description: String? = null,
    val documentCount: Int = 0,
    val chatCount: Int = 0,
    val lastOpenedAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ProjectMutationDto(val name: String, val description: String? = null)

@Serializable
data class DocumentPageDto(
    val items: List<DocumentDto> = emptyList(),
    val nextCursor: String? = null,
)

@Serializable
data class DocumentDto(
    val id: String,
    val filename: String,
    val firstPageSummary: String = "",
    val sizeBytes: Long = 0,
    val mimeType: String = "application/octet-stream",
    val pageCount: Int = 0,
    val createdAt: String = "",
    val originSessionId: String = "",
    val projectId: String? = null,
    val projectName: String? = null,
)

@Serializable
data class ImagePageDto(val images: List<ImageDto> = emptyList())

@Serializable
data class ImageDto(
    val id: String,
    val projectId: String? = null,
    val sessionId: String,
    val mediaType: String,
    val width: Int = 0,
    val height: Int = 0,
    val modelId: String = "",
    val prompt: String = "",
    val nOfTotal: String? = null,
    val createdAt: String = "",
)

@Serializable
data class DocumentPreviewDto(
    val id: String,
    val filename: String,
    val mimeType: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val firstPageSummary: String = "",
    val summary: String? = null,
    val pages: List<DocumentPreviewPageDto> = emptyList(),
)

@Serializable
data class DocumentPreviewPageDto(
    val pageIndex: Int,
    val summary: String = "",
    val rawMarkdown: String = "",
    val images: List<DocumentPageImageDto> = emptyList(),
)

@Serializable
data class DocumentPageImageDto(val id: String, val mediaType: String)

@Serializable
data class ScopeSiteEntryDto(
    val siteId: String,
    val sessionId: String = "",
    val version: Int,
    val stableVersion: Int? = null,
    val status: String = "queued",
    val previewUrl: String? = null,
    val downloadUrl: String = "",
    val updatedAt: String = "",
)

@Serializable
data class ScopeSitesDto(val sites: List<ScopeSiteEntryDto> = emptyList())

@Serializable
data class TaskSubtaskDto(val id: String, val title: String, val done: Boolean = false)

@Serializable
data class WorkspaceTaskDto(
    val id: String,
    val title: String,
    val status: String = "inbox",
    val description: String? = null,
    val subtasks: List<TaskSubtaskDto> = emptyList(),
    val sourceSessionId: String? = null,
    val dueAt: String? = null,
    val createdAt: String = "",
    val updatedAt: String = "",
)

@Serializable
data class TaskListDto(val items: List<WorkspaceTaskDto> = emptyList())

@Serializable
data class TaskCreateDto(
    val sessionId: String,
    val title: String,
    val description: String? = null,
    val addSubtasks: List<String>? = null,
    val dueAt: String? = null,
)

@Serializable
data class TaskToggleDto(val id: String, val done: Boolean)

@Serializable
data class TaskUpdateDto(
    val sessionId: String,
    val status: String? = null,
    val title: String? = null,
    val description: String? = null,
    val addSubtasks: List<String>? = null,
    val toggleSubtasks: List<TaskToggleDto>? = null,
    val removeSubtasks: List<String>? = null,
)

fun ProjectPageDto.toPage(): WorkspacePage<Project> = WorkspacePage(items.map(ProjectDto::toProject), nextCursor)

fun ProjectDto.toProject(): Project = Project(
    id, name, description, documentCount, chatCount, lastOpenedAt, createdAt, updatedAt,
)

fun DocumentPageDto.toPage(): WorkspacePage<WorkspaceDocument> = WorkspacePage(
    items = items.map { item ->
        WorkspaceDocument(
            item.id,
            item.filename,
            item.firstPageSummary,
            item.sizeBytes,
            item.mimeType,
            item.pageCount,
            item.createdAt,
            item.originSessionId,
            item.projectId,
            item.projectName,
        )
    },
    nextCursor = nextCursor,
)

fun ImageDto.toImage(): WorkspaceImage = WorkspaceImage(
    id, projectId, sessionId, mediaType, width, height, modelId, prompt, nOfTotal, createdAt,
)

fun ScopeSiteEntryDto.toScopeEntry(): ScopeSiteEntry = ScopeSiteEntry(
    siteId = siteId,
    sessionId = sessionId,
    version = version,
    stableVersion = stableVersion,
    status = when (status) {
        "running" -> ScopeSiteStatus.Running
        "ready" -> ScopeSiteStatus.Ready
        "failed" -> ScopeSiteStatus.Failed
        else -> ScopeSiteStatus.Queued
    },
    previewUrl = previewUrl,
    downloadUrl = downloadUrl,
    updatedAt = updatedAt,
)

fun TaskStatus.toWire(): String = when (this) {
    TaskStatus.Inbox -> "inbox"
    TaskStatus.Doing -> "doing"
    TaskStatus.Done -> "done"
}

fun String.toTaskStatus(): TaskStatus = when (this) {
    "doing" -> TaskStatus.Doing
    "done" -> TaskStatus.Done
    else -> TaskStatus.Inbox
}

fun TaskSubtaskDto.toSubtask(): TaskSubtask = TaskSubtask(id = id, title = title, done = done)

fun WorkspaceTaskDto.toTask(): WorkspaceTask = WorkspaceTask(
    id = id,
    title = title,
    status = status.toTaskStatus(),
    description = description,
    subtasks = subtasks.map { it.toSubtask() },
    sourceSessionId = sourceSessionId,
    dueAt = dueAt,
)

fun DocumentPreviewDto.toPreview(): DocumentPreview = DocumentPreview(
    id = id,
    filename = filename,
    mimeType = mimeType,
    pageCount = pageCount,
    sizeBytes = sizeBytes,
    summary = summary ?: firstPageSummary,
    pages = pages.map { page ->
        DocumentPreviewPage(
            pageIndex = page.pageIndex,
            summary = page.summary,
            markdown = page.rawMarkdown,
            images = page.images.map { image -> DocumentPageImage(image.id, image.mediaType) },
        )
    },
)
