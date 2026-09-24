package co.ratmo.anreal.feature.workspace.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Error

data class WorkspacePage<T>(
    val items: List<T>,
    val nextCursor: String?,
)

enum class WorkspaceProjectSort(val wire: String) {
    UpdatedAt("updatedAt"),
    LastOpenedAt("lastOpenedAt"),
    Name("name"),
}

data class Project(
    val id: String,
    val name: String,
    val description: String?,
    val documentCount: Int,
    val chatCount: Int,
    val lastOpenedAt: String?,
    val createdAt: String,
    val updatedAt: String,
)

data class WorkspaceDocument(
    val id: String,
    val filename: String,
    val summary: String,
    val sizeBytes: Long,
    val mimeType: String,
    val pageCount: Int,
    val createdAt: String,
    val originSessionId: String,
    val projectId: String?,
    val projectName: String?,
    val kind: String = "source",
)

data class WorkspaceImage(
    val id: String,
    val projectId: String?,
    val sessionId: String,
    val mediaType: String,
    val width: Int,
    val height: Int,
    val modelId: String,
    val prompt: String,
    val nOfTotal: String?,
    val createdAt: String,
    val caption: String = "",
)

data class DocumentPageImage(
    val id: String,
    val mediaType: String,
    val bytes: ByteArray? = null,
)

data class DocumentPreviewPage(
    val pageIndex: Int,
    val summary: String,
    val markdown: String,
    val images: List<DocumentPageImage>,
)

enum class ScopeSiteStatus { Queued, Running, Ready, Failed }

data class ScopeSiteEntry(
    val siteId: String,
    val sessionId: String = "",
    val version: Int,
    val stableVersion: Int?,
    val status: ScopeSiteStatus,
    val previewUrl: String?,
    val downloadUrl: String,
    val updatedAt: String,
)

enum class TaskStatus { Inbox, Doing, Done }

data class TaskSubtask(
    val id: String,
    val title: String,
    val done: Boolean,
)

data class WorkspaceTask(
    val id: String,
    val title: String,
    val status: TaskStatus = TaskStatus.Inbox,
    val description: String? = null,
    val subtasks: List<TaskSubtask> = emptyList(),
    val sourceSessionId: String? = null,
    val dueAt: String? = null,
)

enum class ScheduleFreq { Once, Daily, Weekly }

data class WorkspaceSchedule(
    val id: String,
    val title: String,
    val prompt: String,
    val freq: ScheduleFreq = ScheduleFreq.Once,
    val nextRunAt: String? = null,
    val status: String = "active",
)

enum class ArtifactType { Document, Image, WebBundle, Site, Task, Schedule, Session, Unknown }

data class ArtifactItem(
    val id: String,
    val type: ArtifactType = ArtifactType.Unknown,
    val title: String? = null,
    val caption: String? = null,
    val prompt: String? = null,
    val status: String? = null,
    val previewUrl: String? = null,
    val downloadUrl: String? = null,
    val version: Int? = null,
)

data class DocumentPreview(
    val id: String,
    val filename: String,
    val mimeType: String,
    val pageCount: Int,
    val sizeBytes: Long,
    val summary: String,
    val pages: List<DocumentPreviewPage>,
)

sealed interface WorkspaceError : Error {
    data class Network(val error: DataError.Network) : WorkspaceError
}
