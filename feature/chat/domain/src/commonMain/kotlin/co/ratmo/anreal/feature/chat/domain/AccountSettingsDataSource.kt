package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result

data class StorageUsage(
    val usedBytes: Long,
    val maxBytes: Long,
    val remainingBytes: Long,
)

data class TokenComposition(
    val inputUncached: Long,
    val cacheRead: Long,
    val output: Long,
)

data class TokenUsage(
    val requestCount: Long,
    val inputTokens: Long,
    val outputTokens: Long,
    val totalTokens: Long,
    val cachedInputTokens: Long,
    val cacheCreationInputTokens: Long,
    val composition: TokenComposition,
)

data class ModelUsage(
    val model: String,
    val requestCount: Long,
    val totalTokens: Long,
)

data class ReasoningUsage(
    val reasoningEffort: String,
    val requestCount: Long,
    val totalTokens: Long,
)

data class UsageSummary(
    val storage: StorageUsage,
    val tokens: TokenUsage,
    val byModel: List<ModelUsage>,
    val byReasoningEffort: List<ReasoningUsage>,
)

data class ProfileBullet(
    val text: String,
    val sourceSessionIds: List<String>,
)

data class ExplicitProfileFact(
    val section: String?,
    val fact: String,
    val createdAt: String,
)

data class PersonalizationProfile(
    val sections: Map<String, List<ProfileBullet>>,
    val explicitFacts: List<ExplicitProfileFact>,
    val updatedAt: String,
) {
    val isEmpty: Boolean
        get() = sections.values.all(List<ProfileBullet>::isEmpty) && explicitFacts.isEmpty()
}

data class ProjectPersonalization(
    val id: String,
    val name: String,
    val profile: PersonalizationProfile?,
)

data class PersonalizationSettings(
    val user: PersonalizationProfile?,
    val projects: List<ProjectPersonalization>,
)

interface AccountSettingsDataSource {
    suspend fun checkHealth(): Result<Boolean, ChatError>
    suspend fun loadUsage(): Result<UsageSummary, ChatError>
    suspend fun loadPersonalization(): Result<PersonalizationSettings, ChatError>
    suspend fun resetUserPersonalization(): EmptyResult<ChatError>
    suspend fun resetProjectPersonalization(projectId: String): EmptyResult<ChatError>
}
