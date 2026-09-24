package co.ratmo.anreal.feature.chat.domain.stream

import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.applySiteBuildEvent
import co.ratmo.anreal.feature.chat.domain.applySiteVersionEvent

fun ChatThreadState.reduce(envelope: StreamEnvelope): ChatThreadState {
    return when (envelope) {
        is StreamEnvelope.Start -> copy(
            streamId = envelope.streamId,
            lastEventId = 0,
            status = RunStatus.Streaming,
            error = null,
        )
        is StreamEnvelope.Event -> applyEvent(envelope)
        is StreamEnvelope.End -> copy(
            streamId = envelope.streamId,
            lastEventId = envelope.eventId,
            status = when (envelope.status) {
                StreamEndStatus.Error -> RunStatus.Failed
                StreamEndStatus.Completed -> RunStatus.Completed
                StreamEndStatus.Running -> RunStatus.Streaming
                StreamEndStatus.Missing -> status
            },
        )
    }
}

private fun ChatThreadState.applyEvent(envelope: StreamEnvelope.Event): ChatThreadState {
    val advanced = copy(
        streamId = envelope.streamId,
        lastEventId = envelope.eventId,
        status = if (status == RunStatus.Idle) RunStatus.Streaming else status,
    )
    return when (val event = envelope.event) {
        is ChatStreamEvent.MessageStart -> advanced.upsertMessage(event.message)
        is ChatStreamEvent.TextDelta -> advanced.appendText(
            messageId = event.messageId,
            partId = event.partId,
            delta = event.delta,
        )
        is ChatStreamEvent.ReasoningDelta -> advanced.appendReasoning(
            messageId = event.messageId,
            partId = event.partId,
            delta = event.delta,
        )
        is ChatStreamEvent.ToolUpdate -> advanced.upsertTool(event.messageId, event.part)
        is ChatStreamEvent.MessageEnd -> advanced.updateMessage(event.messageId) { it.copy(isComplete = true) }
        is ChatStreamEvent.Error -> advanced.copy(
            error = event.message,
            status = RunStatus.Failed,
        )
        is ChatStreamEvent.QueuedMessageApplied -> advanced
        is ChatStreamEvent.InteractionRequested -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions
                .filterNot { it.id == event.interaction.id } + event.interaction,
            staleInteractionIds = advanced.staleInteractionIds - event.interaction.id,
        )
        is ChatStreamEvent.InteractionResolved -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions.filterNot { it.id == event.id },
            staleInteractionIds = advanced.staleInteractionIds - event.id,
        )
        is ChatStreamEvent.InteractionMarkedStale -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions.filterNot { it.id == event.id },
            staleInteractionIds = advanced.staleInteractionIds + event.id,
        )
        is ChatStreamEvent.DeepResearchProgress -> advanced.copy(deepResearch = event.status)
        is ChatStreamEvent.ApprovalRequested -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions + NativeInteraction(
                id = event.approval.id,
                toolName = event.approval.toolName,
                kind = InteractionKind.ToolApproval,
                reason = event.approval.reason,
                argumentsJson = event.approval.arguments,
            ),
        )
        is ChatStreamEvent.ApprovalResolved -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions.filterNot { it.id == event.id },
            staleInteractionIds = advanced.staleInteractionIds - event.id,
        )
        is ChatStreamEvent.ClarificationRequested -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions + NativeInteraction(
                id = event.clarification.id,
                toolName = "clarification",
                kind = InteractionKind.ToolQuestion,
                questions = event.clarification.questions.map { question ->
                    InteractionQuestion(
                        id = question.id,
                        text = question.question,
                        choices = question.options.map { option ->
                            InteractionChoice(label = option.label, value = option.id)
                        },
                        allowCustom = question.optional,
                    )
                },
            ),
        )
        is ChatStreamEvent.ClarificationResolved -> advanced.copy(
            pendingInteractions = advanced.pendingInteractions.filterNot { it.id == event.id },
            staleInteractionIds = advanced.staleInteractionIds - event.id,
        )
        is ChatStreamEvent.Compaction -> advanced
        is ChatStreamEvent.SiteBuildProgress -> advanced.copy(
            siteBuild = applySiteBuildEvent(advanced.siteBuild, event.siteId, event.version, event.phase, event.message),
            siteVersions = applySiteVersionEvent(advanced.siteVersions, event.siteId, event.version, event.phase, previewUrl = null, downloadUrl = null),
        )
        is ChatStreamEvent.SiteBuildReady -> advanced.copy(
            siteBuild = applySiteBuildEvent(advanced.siteBuild, event.siteId, event.version, SiteBuildPhase.Ready, "", event.previewUrl, event.downloadUrl),
            siteVersions = applySiteVersionEvent(advanced.siteVersions, event.siteId, event.version, SiteBuildPhase.Ready, event.previewUrl, event.downloadUrl),
        )
        is ChatStreamEvent.ArtifactFocus -> advanced.copy(
            pendingArtifactFocus = ArtifactFocusState(
                artifactId = event.artifactId,
                artifactType = event.artifactType,
                label = event.label,
            ),
        )
        is ChatStreamEvent.Unknown -> advanced
    }
}

private fun ChatThreadState.upsertMessage(message: ChatMessage): ChatThreadState {
    val index = messages.indexOfFirst { it.id == message.id }
    if (index < 0) return copy(messages = messages + message)
    return copy(messages = messages.toMutableList().also { it[index] = message })
}

private fun ChatThreadState.updateMessage(
    messageId: String,
    transform: (ChatMessage) -> ChatMessage,
): ChatThreadState {
    val index = messages.indexOfFirst { it.id == messageId }
    if (index < 0) {
        return copy(messages = messages + transform(ChatMessage(id = messageId, role = ChatRole.Assistant)))
    }
    return copy(messages = messages.toMutableList().also { it[index] = transform(it[index]) })
}

private fun ChatThreadState.appendText(
    messageId: String,
    partId: String,
    delta: String,
): ChatThreadState {
    return updateMessage(messageId) { message ->
        message.copy(parts = message.parts.appendOrCreate(partId) { existing ->
            when (existing) {
                is ChatPart.Text -> existing.copy(text = existing.text + delta)
                null -> ChatPart.Text(id = partId, text = delta)
                else -> existing
            }
        })
    }
}

private fun ChatThreadState.appendReasoning(
    messageId: String,
    partId: String,
    delta: String,
): ChatThreadState {
    return updateMessage(messageId) { message ->
        message.copy(parts = message.parts.appendOrCreate(partId) { existing ->
            when (existing) {
                is ChatPart.Reasoning -> existing.copy(text = existing.text + delta)
                null -> ChatPart.Reasoning(id = partId, text = delta)
                else -> existing
            }
        })
    }
}

private fun ChatThreadState.upsertTool(
    messageId: String,
    part: ChatPart.Tool,
): ChatThreadState {
    return updateMessage(messageId) { message ->
        message.copy(parts = message.parts.appendOrCreate(part.id) { part })
    }
}

private fun List<ChatPart>.appendOrCreate(
    partId: String,
    transform: (ChatPart?) -> ChatPart,
): List<ChatPart> {
    val index = indexOfFirst { it.id == partId }
    if (index < 0) return this + transform(null)
    return toMutableList().also { it[index] = transform(it[index]) }
}
