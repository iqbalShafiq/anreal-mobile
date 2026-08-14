package co.ratmo.anreal.feature.chat.domain

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

data class ChatCapabilities(
    val webSearchAvailable: Boolean = false,
    val imageGenerationAvailable: Boolean = false,
)

data class ChatRunOptions(
    val model: String? = null,
    val reasoningEffort: String? = null,
    val webSearchEnabled: Boolean = false,
    val imageGenerationEnabled: Boolean = false,
)
