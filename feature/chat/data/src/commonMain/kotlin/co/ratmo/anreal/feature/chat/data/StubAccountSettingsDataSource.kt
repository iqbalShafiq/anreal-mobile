package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ModelUsage
import co.ratmo.anreal.feature.chat.domain.PersonalizationSettings
import co.ratmo.anreal.feature.chat.domain.ReasoningUsage
import co.ratmo.anreal.feature.chat.domain.StorageUsage
import co.ratmo.anreal.feature.chat.domain.TokenComposition
import co.ratmo.anreal.feature.chat.domain.TokenUsage
import co.ratmo.anreal.feature.chat.domain.UsageSummary

class StubAccountSettingsDataSource : AccountSettingsDataSource {
    override suspend fun checkHealth(): Result<Boolean, ChatError> = Result.Success(true)

    override suspend fun loadUsage(): Result<UsageSummary, ChatError> = Result.Success(
        UsageSummary(
            storage = StorageUsage(usedBytes = 12_582_912, maxBytes = 209_715_200, remainingBytes = 197_132_288),
            tokens = TokenUsage(
                requestCount = 42,
                inputTokens = 180_000,
                outputTokens = 60_000,
                totalTokens = 240_000,
                cachedInputTokens = 20_000,
                cacheCreationInputTokens = 5_000,
                composition = TokenComposition(160_000, 20_000, 60_000),
            ),
            byModel = listOf(ModelUsage("openai/gpt-5.6-luna", 40, 230_000)),
            byReasoningEffort = listOf(ReasoningUsage("high", 10, 80_000)),
        ),
    )

    override suspend fun loadPersonalization(): Result<PersonalizationSettings, ChatError> =
        Result.Success(PersonalizationSettings(user = null, projects = emptyList()))

    override suspend fun resetUserPersonalization(): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun resetProjectPersonalization(projectId: String): EmptyResult<ChatError> =
        Result.Success(Unit)
}
