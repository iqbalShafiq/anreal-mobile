package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.getBytes
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.postForResponse
import co.ratmo.anreal.core.data.network.postJsonl
import co.ratmo.anreal.core.data.network.postMultipart
import co.ratmo.anreal.core.data.network.put
import co.ratmo.anreal.core.data.network.MultipartFile
import co.ratmo.anreal.core.domain.model.ChatSession
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.core.domain.util.mapError
import co.ratmo.anreal.feature.chat.domain.ChatCapabilities
import co.ratmo.anreal.feature.chat.domain.ActiveRun
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ChatSessionDetail
import co.ratmo.anreal.feature.chat.domain.ChatShareDeactivation
import co.ratmo.anreal.feature.chat.domain.ChatShareLink
import co.ratmo.anreal.feature.chat.domain.ChatShareStatus
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.ForkResult
import co.ratmo.anreal.feature.chat.domain.ForkSeed
import co.ratmo.anreal.feature.chat.domain.PublicShareSnapshot
import co.ratmo.anreal.feature.chat.domain.ContextSnippet
import co.ratmo.anreal.feature.chat.domain.ContextUsage
import co.ratmo.anreal.feature.chat.domain.DocumentIngest
import co.ratmo.anreal.feature.chat.domain.DocumentStorage
import co.ratmo.anreal.feature.chat.domain.LibraryDocumentPage
import co.ratmo.anreal.feature.chat.domain.ModelCatalog
import co.ratmo.anreal.feature.chat.domain.RecentProject
import co.ratmo.anreal.feature.chat.domain.RunStatusSnapshot
import co.ratmo.anreal.feature.chat.domain.SessionDocument
import co.ratmo.anreal.feature.chat.domain.SessionImage
import co.ratmo.anreal.feature.chat.domain.SessionPage
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ImageOverrideArgs
import co.ratmo.anreal.feature.chat.domain.stream.InteractionAvailability
import co.ratmo.anreal.feature.chat.domain.stream.InteractionResponse
import co.ratmo.anreal.feature.chat.domain.stream.SessionGrant
import io.ktor.client.HttpClient

class KtorChatRemoteDataSource(
    private val httpClient: HttpClient,
) {
    suspend fun listSessions(
        cursor: String? = null,
        projectId: String? = null,
    ): Result<SessionPage, ChatError> {
        return httpClient.get<SessionListPageDto>(
            route = "/api/chat/sessions",
            queryParameters = mapOf(
                "limit" to 50,
                "cursor" to cursor,
                "projectId" to projectId,
            ),
        ).map { page ->
            SessionPage(items = page.items.map { it.toSession() }, nextCursor = page.nextCursor)
        }.mapNetwork()
    }

    suspend fun createSession(
        sessionId: String? = null,
        projectId: String? = null,
    ): Result<ChatSession, ChatError> = httpClient.post<CreateSessionRequestDto, SessionMutationDto>(
        route = "/api/chat/sessions",
        body = CreateSessionRequestDto(sessionId = sessionId, projectId = projectId),
    ).map { it.toSession() }.mapNetwork()

    suspend fun openDraft(projectId: String? = null): Result<ChatSession, ChatError> {
        return httpClient.post<DraftRequestDto, SessionMutationDto>(
            route = "/api/chat/sessions/draft",
            body = DraftRequestDto(projectId = projectId),
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
        ).map { messages ->
            messages.mapIndexed { index, dto -> dto.toMessage(index) }.mergeToolResultMessages()
        }.mapNetwork()
    }

    suspend fun send(
        sessionId: String,
        messages: List<ChatMessage>,
        resume: ResumeDto? = null,
        clientMessageId: String? = null,
        options: ChatRunOptions = ChatRunOptions(),
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        val modelId = options.model
        if (modelId.isNullOrBlank()) {
            return Result.Error(ChatError.Network(DataError.Network.BAD_REQUEST))
        }
        return httpClient.postJsonl(
            route = "/api/chat",
            body = SendMessagesRequestDto(
                type = "messages",
                metadata = options.toMetadataDto(sessionId),
                messages = messages.map { it.toHistoryDto(clientMessageId) },
                resume = resume,
            ),
            onLine = onLine,
        ).toChatResult()
    }

    suspend fun answerInteraction(
        interactionId: String,
        response: InteractionResponse,
        options: ChatRunOptions,
        sessionId: String,
        onLine: suspend (String) -> Unit,
    ): EmptyResult<ChatError> {
        return httpClient.postJsonl(
            route = "/api/chat",
            body = InteractionResponseRequestDto(
                type = "interaction_response",
                interactionId = interactionId,
                response = response.toJson(),
                metadata = options.toMetadataDto(sessionId),
            ),
            onLine = onLine,
        ).toChatResult()
    }

    suspend fun loadCatalog(outputType: String? = null): Result<ModelCatalog, ChatError> {
        return httpClient.get<ModelCatalogDto>(
            route = "/api/models",
            queryParameters = mapOf("outputType" to outputType),
        ).map { it.toCatalog() }
            .mapNetwork()
    }

    suspend fun loadCapabilities(sessionId: String? = null): Result<ChatCapabilities, ChatError> {
        return httpClient.get<CapabilitiesDto>(
            route = "/api/chat/capabilities",
            queryParameters = mapOf("sessionId" to sessionId),
        )
            .map { it.toCapabilities() }
            .mapNetwork()
    }

    suspend fun getSession(id: String): Result<ChatSessionDetail, ChatError> {
        return httpClient.get<ChatSessionDetailDto>(
            route = "/api/chat/sessions/$id",
        )
            .map { it.toDetail() }
            .mapNetwork()
    }

    suspend fun steer(sessionId: String, items: List<QueuedItem>): EmptyResult<ChatError> {
        return httpClient.post<SteerRequestDto, SteerResponseDto>(
            route = "/api/chat/steer",
            body = SteerRequestDto(
                sessionId = sessionId,
                messages = items.map { item ->
                    SteerMessageDto(
                        clientMessageId = item.id,
                        text = item.text,
                        attachments = item.attachments.map { it.toDto() },
                        contextSnippet = item.contextSnippet?.toDto(),
                    )
                },
            ),
        ).mapError { error ->
            if (
                error.kind == DataError.Network.Kind.CONFLICT &&
                error.code == "NO_ACTIVE_RUN"
            ) ChatError.NoActiveRun
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

    suspend fun listActiveRuns(): Result<List<ActiveRun>, ChatError> =
        httpClient.get<ActiveRunsDto>(route = "/api/chat/runs")
            .map { response -> response.runs.map { it.toRun() } }
            .mapNetwork()

    suspend fun getSessionMessageCount(sessionId: String): Result<Int, ChatError> =
        httpClient.get<SessionStateDto>(
            route = "/api/chat/session-state",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { it.messageCount }.mapNetwork()

    suspend fun getContextUsage(
        sessionId: String,
        model: String?,
        reasoningEffort: String?,
    ): Result<ContextUsage, ChatError> = httpClient.get<ContextUsageDto>(
        route = "/api/chat/context-usage",
        queryParameters = mapOf(
            "sessionId" to sessionId,
            "model" to model,
            "reasoningEffort" to reasoningEffort,
        ),
    ).map { it.toUsage() }.mapNetwork()

    suspend fun truncateSession(
        sessionId: String,
        mode: String,
        clientMessageId: String?,
        memoryPosition: Int?,
    ): EmptyResult<ChatError> = httpClient.post<TruncateRequestDto, TruncateResponseDto>(
        route = "/api/chat/truncate",
        body = TruncateRequestDto(sessionId, mode, clientMessageId, memoryPosition),
    ).mapNetwork().asEmptyResult()

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

    suspend fun getDocumentStorage(): Result<DocumentStorage, ChatError> =
        httpClient.get<DocumentStorageDto>(route = "/api/documents/storage")
            .map { it.toStorage() }
            .mapNetwork()

    suspend fun listLibraryDocuments(
        query: String?,
        cursor: String?,
        projectId: String?,
    ): Result<LibraryDocumentPage, ChatError> = httpClient.get<LibraryDocumentPageDto>(
        route = "/api/documents/library",
        queryParameters = mapOf(
            "scope" to "attach",
            "q" to query,
            "cursor" to cursor,
            "projectId" to projectId,
            "limit" to 50,
        ),
    ).map { it.toPage() }.mapNetwork()

    suspend fun linkDocuments(
        sessionId: String,
        documentIds: List<String>,
    ): Result<List<SessionDocument>, ChatError> = httpClient.post<LinkDocumentsDto, LinkedDocumentsDto>(
        route = "/api/documents/links",
        body = LinkDocumentsDto(sessionId, documentIds),
    ).map { response -> response.linked.map { it.toDocument() } }.mapNetwork()

    suspend fun uploadDocument(sessionId: String, file: ChatUpload): Result<DocumentIngest, ChatError> {
        return httpClient.postMultipart<DocumentUploadDto>(
            route = "/api/documents",
            fields = mapOf("sessionId" to sessionId),
            file = file.toMultipartFile(),
        ).map { it.toIngest() }.mapNetwork()
    }

    suspend fun getDocumentStatus(
        sessionId: String,
        documentId: String,
    ): Result<DocumentIngest, ChatError> = httpClient.get<DocumentStatusDto>(
        route = "/api/documents/$documentId",
        queryParameters = mapOf("sessionId" to sessionId),
    ).map { it.toIngest() }.mapNetwork()

    suspend fun uploadImage(sessionId: String, file: ChatUpload): Result<SessionImage, ChatError> {
        return httpClient.postMultipart<ImageUploadDto>(
            route = "/api/images",
            fields = mapOf("sessionId" to sessionId),
            file = file.toMultipartFile(),
        ).map { it.image.toImage() }.mapNetwork()
    }

    suspend fun listSessionImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        httpClient.get<ImageListDto>(
            route = "/api/images",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { response -> response.images.map { it.toImage() } }.mapNetwork()

    suspend fun listPinnedImages(sessionId: String): Result<List<SessionImage>, ChatError> =
        httpClient.get<ImageListDto>(
            route = "/api/images/context",
            queryParameters = mapOf("sessionId" to sessionId),
        ).map { response -> response.images.map { it.toImage(isPinned = true) } }.mapNetwork()

    suspend fun pinImage(sessionId: String, imageId: String): EmptyResult<ChatError> =
        httpClient.post<ImageContextDto, OkResponseDto>(
            route = "/api/images/context",
            body = ImageContextDto(sessionId, imageId),
        ).mapNetwork().asEmptyResult()

    suspend fun unpinImage(sessionId: String, imageId: String): EmptyResult<ChatError> =
        httpClient.delete(
            route = "/api/images/context/$imageId",
            queryParameters = mapOf("sessionId" to sessionId),
        ).mapNetwork().asEmptyResult()

    suspend fun loadImageBytes(imageId: String): Result<ByteArray, ChatError> =
        httpClient.getBytes(route = "/api/images/$imageId").mapNetwork()

    suspend fun loadContextSnippet(sessionId: String): Result<ContextSnippet?, ChatError> =
        httpClient.get<ContextSnippetResponseDto>(route = "/api/chat/$sessionId/context-snippet")
            .map { response -> response.snippet?.toSnippet() }
            .mapNetwork()

    suspend fun saveContextSnippet(
        sessionId: String,
        text: String,
        sourceRole: String,
    ): Result<ContextSnippet, ChatError> = httpClient.put<ContextSnippetBodyDto, StoredContextSnippetResponseDto>(
        route = "/api/chat/$sessionId/context-snippet",
        body = ContextSnippetBodyDto(text, sourceRole),
    ).map { response -> response.snippet.toSnippet() }.mapNetwork()

    suspend fun clearContextSnippet(sessionId: String, snippetId: String): EmptyResult<ChatError> =
        httpClient.delete(
            route = "/api/chat/context-snippet/$snippetId",
            queryParameters = mapOf("sessionId" to sessionId),
        ).mapNetwork().asEmptyResult()

    suspend fun getInteractionStatus(interactionId: String): Result<InteractionAvailability, ChatError> {
        return httpClient.get<InteractionStatusDto>(
            route = "/api/chat/interactions/$interactionId",
        ).map { it.toAvailability() }.mapNetwork()
    }

    suspend fun stageInteractionPolicy(
        interactionId: String,
        response: InteractionResponse,
        grantScope: SessionGrant? = null,
        overrideArgs: ImageOverrideArgs? = null,
    ): EmptyResult<ChatError> {
        return httpClient.post<StageInteractionRequestDto, OkResponseDto>(
            route = "/api/chat/interactions/$interactionId/stage",
            body = StageInteractionRequestDto(
                response = response.toJson(),
                grantScope = grantScope?.toWire(),
                overrideArgs = overrideArgs?.toJson(),
            ),
        ).mapError { it.toInteractionError() }.asEmptyResult()
    }

    suspend fun listRecentProjects(): Result<List<RecentProject>, ChatError> {
        return httpClient.get<ProjectListPageDto>(
            route = "/api/projects",
            queryParameters = mapOf("limit" to 5, "sort" to "lastOpenedAt"),
        ).map { page -> page.items.map { it.toProject() } }.mapNetwork()
    }

    suspend fun openProject(id: String): Result<RecentProject, ChatError> {
        return httpClient.postForResponse<ProjectListItemDto>(
            route = "/api/projects/$id/open",
        ).map { it.toProject() }.mapNetwork()
    }

    suspend fun createShare(sessionId: String): Result<ChatShareLink, ChatError> {
        return httpClient.postForResponse<ShareLinkDto>(
            route = "/api/chat/sessions/$sessionId/shares",
        ).map { it.toShareLink() }.mapNetwork()
    }

    suspend fun shareStatus(sessionId: String): Result<ChatShareStatus, ChatError> {
        return httpClient.get<ShareStatusDto>(
            route = "/api/chat/sessions/$sessionId/shares/status",
        ).map { it.toShareStatus() }.mapNetwork()
    }

    suspend fun latestShare(sessionId: String): Result<ChatShareLink, ChatError> {
        return httpClient.get<ShareLinkDto>(
            route = "/api/chat/sessions/$sessionId/shares/latest",
        ).map { it.toShareLink() }.mapError { it.toShareError() }
    }

    suspend fun deactivateShares(sessionId: String): Result<ChatShareDeactivation, ChatError> {
        return httpClient.postForResponse<DeactivateSharesDto>(
            route = "/api/chat/sessions/$sessionId/shares/deactivate",
        ).map { it.toDeactivation() }.mapNetwork()
    }

    suspend fun forkSharedChat(seed: ForkSeed): Result<ForkResult, ChatError> {
        return httpClient.post<ForkRequestDto, ForkResponseDto>(
            route = "/api/chat/fork",
            body = seed.toRequestDto(),
        ).map { it.toForkResult() }.mapError { it.toForkError() }
    }

    suspend fun publicShare(token: String): Result<PublicShareSnapshot, ChatError> {
        return httpClient.get<PublicShareDto>(
            route = "/api/shares/$token",
            skipAuth = true,
        ).map { it.toSnapshot() }.mapError { it.toShareError() }
    }
}

private fun ChatUpload.toMultipartFile(): MultipartFile = MultipartFile(bytes, filename, mediaType)

private fun <T> Result<T, DataError.Network>.mapNetwork(): Result<T, ChatError> {
    return mapError { it.toChatError() }
}

private fun Result<Unit, DataError.Network>.toChatResult(): EmptyResult<ChatError> {
    return mapError { it.toChatError() }
}

private fun DataError.Network.toChatError(): ChatError {
    return if (kind == DataError.Network.Kind.CONFLICT && code == "RUN_ACTIVE") ChatError.RunActive
    else ChatError.Network(this)
}

private fun DataError.Network.toInteractionError(): ChatError {
    return when (code) {
        "INTERACTION_NOT_FOUND" -> ChatError.InteractionNotFound
        "INTERACTION_EXPIRED" -> ChatError.InteractionExpired
        "INTERACTION_STATE_CONFLICT",
        "INTERACTION_POLICY_CONFLICT",
        "INTERACTION_REPLAYED",
        "INTERACTION_CLAIMED",
        -> ChatError.InteractionHandled
        "INTERACTION_POLICY_UNAVAILABLE" -> ChatError.InteractionPolicyUnavailable
        "INTERACTION_STAGE_INVALID" -> ChatError.InteractionStageInvalid
        else -> ChatError.Network(this)
    }
}

private fun DataError.Network.toShareError(): ChatError {
    if (kind == DataError.Network.Kind.NOT_FOUND) {
        return when (code) {
            "CHAT_SHARE_NOT_FOUND" -> ChatError.ShareNotFound
            else -> ChatError.NoActiveShare
        }
    }
    return when (code) {
        "CHAT_SHARE_NOT_FOUND" -> ChatError.ShareNotFound
        else -> mapShareUnavailable()
    }
}

private fun DataError.Network.toForkError(): ChatError {
    return when {
        kind == DataError.Network.Kind.NOT_FOUND -> ChatError.ForkTargetNotFound
        kind == DataError.Network.Kind.BAD_REQUEST ||
            kind == DataError.Network.Kind.UNPROCESSABLE_ENTITY -> ChatError.InvalidForkRequest
        else -> mapShareUnavailable()
    }
}

private fun DataError.Network.mapShareUnavailable(): ChatError {
    return if (
        kind == DataError.Network.Kind.SERVER_ERROR ||
        kind == DataError.Network.Kind.SERVICE_UNAVAILABLE
    ) {
        ChatError.ShareUnavailable
    } else {
        ChatError.Network(this)
    }
}

private fun ChatRunOptions.toMetadataDto(sessionId: String): ChatRequestMetadataDto {
    val imageSettings = imageGenSettings
    return ChatRequestMetadataDto(
        sessionId = sessionId,
        documentIds = documentIds.distinct(),
        modelId = model.orEmpty(),
        reasoningEffort = reasoningEffort,
        webSearchEnabled = webSearchEnabled,
        imageGenerationEnabled = imageGenerationEnabled,
        deepResearchEnabled = deepResearchEnabled,
        imageGenSettings = imageSettings?.takeIf { imageGenerationEnabled }?.toDto(),
        skillIds = skillIds.distinct().take(20),
        mcpServerIds = mcpServerIds.distinct().take(5),
    )
}
