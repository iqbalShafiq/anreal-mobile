package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.feature.workspace.domain.Project
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
