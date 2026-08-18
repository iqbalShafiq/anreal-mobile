package co.ratmo.anreal.feature.workspace.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Error

data class WorkspacePage<T>(
    val items: List<T>,
    val nextCursor: String?,
)

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
