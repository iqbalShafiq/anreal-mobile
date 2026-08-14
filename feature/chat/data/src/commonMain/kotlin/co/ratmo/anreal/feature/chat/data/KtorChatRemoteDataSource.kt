package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.postJsonl
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.core.domain.util.mapError
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import io.ktor.client.HttpClient

class KtorChatRemoteDataSource(
    private val httpClient: HttpClient,
) {
    suspend fun listSessions(): Result<SessionPage, ChatError> {
        return httpClient.get<SessionListPageDto>(
            route = "/api/chat/sessions",
            queryParameters = mapOf("limit" to 50),
        ).map { page ->
            SessionPage(items = page.items.map { it.toSession() }, nextCursor = page.nextCursor)
        }.mapNetwork()
    }

    suspend fun openDraft(): Result<ChatSession, ChatError> {
        return httpClient.post<DraftRequestDto, SessionMutationDto>(
            route = "/api/chat/sessions/draft",
            body = DraftRequestDto(),
        ).map { it.toSession() }.mapNetwork()
    }

    suspend fun renameSession(sessionId: String, title: String): Result<ChatSession, ChatError> {
        return httpClient.patch<SessionTitleDto, SessionMutationDto>(
            route = "/api/chat/sessions/$sessionId",
            body = SessionTitleDto(title = title),
        ).map { it.toSession() }.mapNetwork()
    }

    suspend fun deleteSession(sessionId: String): EmptyResult<ChatError> {
        return httpClient.delete(
            route = "/api/chat/sessions/$sessionId",
            queryParameters = mapOf("confirm" to true),
        ).mapNetwork().asEmptyResult()
    }

    suspend fun markRead(sessionId: String): EmptyResult<ChatError> {
        return httpClient.post<MarkReadDto, Unit>(
            route = "/api/chat/sessions/mark-read",
            body = MarkReadDto(sessionId = sessionId),
        ).mapNetwork().asEmptyResult()
    }

    suspend fun loadHistory(sessionId: String): Result<List<ChatMessage>, ChatError> {
        return httpClient.get<List<HistoryMessageDto>>(
            route = "/api/chat",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { messages -> messages.mapIndexed { index, dto -> dto.toMessage(index) } }
            .mapNetwork()
    }

    suspend fun send(
        sessionId: String,
        messages: List<ChatMessage>,
        resume: ResumeDto? = null,
        clientMessageId: String? = null,
        options: ChatRunOptions = ChatRunOptions(),
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        return httpClient.postJsonl(
            route = "/api/chat",
            body = ChatRequestDto(
                sessionId = sessionId,
                messages = messages.map { it.toHistoryDto(clientMessageId) },
                resume = resume,
                model = options.model,
                reasoningEffort = options.reasoningEffort,
                webSearchEnabled = options.webSearchEnabled,
                imageGenerationEnabled = options.imageGenerationEnabled,
            ),
            onLine = onLine,
        ).toChatResult()
    }

    suspend fun loadCatalog(): Result<ModelCatalog, ChatError> {
        return httpClient.get<ModelCatalogDto>(route = "/api/models")
            .map { it.toCatalog() }
            .mapNetwork()
    }

    suspend fun loadCapabilities(): Result<ChatCapabilities, ChatError> {
        return httpClient.get<CapabilitiesDto>(route = "/api/chat/capabilities")
            .map { it.toCapabilities() }
            .mapNetwork()
    }

    suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError> {
        return httpClient.post<SteerRequestDto, SteerResponseDto>(
            route = "/api/chat/steer",
            body = SteerRequestDto(
                sessionId = sessionId,
                messages = items.map { item ->
                    SteerMessageDto(clientMessageId = item.id, text = item.text)
                },
            ),
        ).mapError { error ->
            if (error == DataError.Network.CONFLICT) ChatError.NoActiveRun
            else ChatError.Network(error)
        }.asEmptyResult()
    }

    suspend fun syncQueue(sessionId: String, ids: List<String>): Result<List<String>, ChatError> {
        return httpClient.post<QueueSyncRequestDto, QueueSyncResponseDto>(
            route = "/api/chat/queue/sync",
            body = QueueSyncRequestDto(sessionId = sessionId, ids = ids),
        ).map { it.appliedIds }.mapNetwork()
    }

    suspend fun stop(streamId: String): EmptyResult<ChatError> {
        return httpClient.post<StopRunDto, Unit>(
            route = "/api/chat/stop",
            body = StopRunDto(streamId = streamId),
        ).mapNetwork().asEmptyResult()
    }

    suspend fun runStatus(sessionId: String): Result<RunStatusSnapshot, ChatError> {
        return httpClient.get<RunStatusDto>(
            route = "/api/chat/run-status",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { dto ->
            RunStatusSnapshot(
                streamId = dto.streamId,
                status = dto.status,
                lastEventId = dto.lastEventId,
            )
        }.mapNetwork()
    }

    suspend fun listSessionDocuments(sessionId: String): Result<List<SessionDocument>, ChatError> {
        return httpClient.get<List<SessionDocumentDto>>(
            route = "/api/documents",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { items -> items.map { it.toDocument() } }.mapNetwork()
    }

    suspend fun unlinkSessionDocument(
        sessionId: String,
        documentId: String,
    ): EmptyResult<ChatError> {
        return httpClient.delete(
            route = "/api/documents/links",
            body = UnlinkDocumentDto(sessionId = sessionId, documentId = documentId),
        ).mapNetwork().asEmptyResult()
    }

    suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError> {
        return httpClient.get<ProjectListPageDto>(
            route = "/api/projects",
            queryParameters = mapOf("limit" to 5, "sort" to "lastOpenedAt"),
        ).map { page -> page.items.map { it.toProject() } }.mapNetwork()
    }
}

private fun <T> Result<T, DataError.Network>.mapNetwork(): Result<T, ChatError> {
    return mapError { it.toChatError() }
}

private fun Result<Unit, DataError.Network>.toChatResult(): EmptyResult<ChatError> {
    return mapError { it.toChatError() }
}

private fun DataError.Network.toChatError(): ChatError {
    return if (this == DataError.Network.CONFLICT) ChatError.RunActive
    else ChatError.Network(this)
}
