package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.feature.chat.domain.stream.ImageGenSettings

data class ChatModel(
    val id: String,
    val label: String,
    val reasoningEfforts: List<String> = emptyList(),
    val contextWindowTokens: Int = 0,
)

data class ReasoningEffort(
    val key: String,
    val label: String,
    val description: String? = null,
)

data class ModelCatalog(
    val models: List<ChatModel> = emptyList(),
    val efforts: List<ReasoningEffort> = emptyList(),
)

data class CachedModelCatalog(
    val catalog: ModelCatalog,
    val selectedModelId: String?,
    val selectedReasoningEffort: String?,
    val lastSuccessfulRefreshEpochMillis: Long?,
)

data class CatalogSelectionResolution(
    val selectedModelId: String?,
    val selectedReasoningEffort: String?,
    val modelUnavailable: Boolean,
    val unavailableModelId: String?,
    val unavailableModelLabel: String?,
)

fun reconcileCatalogSelection(
    catalog: ModelCatalog,
    requestedModelId: String?,
    requestedReasoningEffort: String?,
    requestedModelLabel: String? = null,
): CatalogSelectionResolution {
    val selectedModel = if (requestedModelId == null) {
        catalog.models.firstOrNull()
    } else {
        catalog.models.firstOrNull { it.id == requestedModelId }
    }

    if (requestedModelId != null && selectedModel == null) {
        return CatalogSelectionResolution(
            selectedModelId = null,
            selectedReasoningEffort = null,
            modelUnavailable = true,
            unavailableModelId = requestedModelId,
            unavailableModelLabel = requestedModelLabel ?: requestedModelId,
        )
    }

    val selectedReasoningEffort = selectedModel?.reasoningEfforts
        ?.let { supportedEfforts ->
            reconcileReasoningEffort(
                supportedEfforts = supportedEfforts,
                catalogEfforts = catalog.efforts,
                requestedEffort = requestedReasoningEffort,
            )
        }

    return CatalogSelectionResolution(
        selectedModelId = selectedModel?.id,
        selectedReasoningEffort = selectedReasoningEffort,
        modelUnavailable = false,
        unavailableModelId = null,
        unavailableModelLabel = null,
    )
}

private val canonicalEffortRanks = mapOf(
    "none" to 0,
    "minimal" to 1,
    "low" to 2,
    "medium" to 3,
    "high" to 4,
    "xhigh" to 5,
    "max" to 6,
)

private fun reconcileReasoningEffort(
    supportedEfforts: List<String>,
    catalogEfforts: List<ReasoningEffort>,
    requestedEffort: String?,
): String? {
    if (requestedEffort == null || requestedEffort == "none") return null

    val available = supportedEfforts
        .asSequence()
        .filter { it != "none" }
        .distinct()
        .toList()
    if (requestedEffort in available) return requestedEffort

    val requestedRank = canonicalEffortRanks[requestedEffort]
    if (requestedRank != null) {
        return available
            .asSequence()
            .mapNotNull { effort ->
                canonicalEffortRanks[effort]
                    ?.takeIf { rank -> rank < requestedRank }
                    ?.let { rank -> effort to rank }
            }
            .maxByOrNull { (_, rank) -> rank }
            ?.first
    }

    val requestedCatalogPosition = catalogEfforts.indexOfFirst { it.key == requestedEffort }
    if (requestedCatalogPosition < 0) return null

    val lowerCatalogEfforts = available.filter { effort ->
        catalogEfforts.indexOfFirst { it.key == effort } in 0 until requestedCatalogPosition
    }
    val knownLowerEfforts = lowerCatalogEfforts.filter { it in canonicalEffortRanks }
    val candidates = knownLowerEfforts.ifEmpty { lowerCatalogEfforts }
    return candidates.maxByOrNull { effort ->
        catalogEfforts.indexOfFirst { it.key == effort }
    }
}

data class ChatCapabilities(
    val webSearchAvailable: Boolean = false,
    val deepResearchAvailable: Boolean = false,
    val imageGenerationAvailable: Boolean = false,
    val context7Configured: Boolean = false,
)

data class ChatRunOptions(
    val model: String? = null,
    val reasoningEffort: String? = null,
    val webSearchEnabled: Boolean = false,
    val deepResearchEnabled: Boolean = false,
    val imageGenerationEnabled: Boolean = false,
    val imageGenSettings: ImageGenSettings? = null,
    val documentIds: List<String> = emptyList(),
)
