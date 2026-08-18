package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.core.domain.util.mapError
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.PersonalizationSettings
import co.ratmo.anreal.feature.chat.domain.UsageSummary
import io.ktor.client.HttpClient

class KtorAccountSettingsDataSource(
    private val httpClient: HttpClient,
) : AccountSettingsDataSource {
    override suspend fun checkHealth(): Result<Boolean, ChatError> =
        httpClient.get<HealthDto>(route = "/health")
            .map(HealthDto::ok)
            .mapError(ChatError::Network)

    override suspend fun loadUsage(): Result<UsageSummary, ChatError> =
        httpClient.get<UsageSummaryDto>(route = "/api/usage/summary")
            .map(UsageSummaryDto::toUsageSummary)
            .mapError(ChatError::Network)

    override suspend fun loadPersonalization(): Result<PersonalizationSettings, ChatError> =
        httpClient.get<ProfilingSettingsDto>(route = "/api/profiling")
            .map(ProfilingSettingsDto::toPersonalizationSettings)
            .mapError(ChatError::Network)

    override suspend fun resetUserPersonalization(): EmptyResult<ChatError> =
        httpClient.delete(
            route = "/api/profiling",
            queryParameters = mapOf("scope" to "user"),
        ).mapError(ChatError::Network).asEmptyResult()

    override suspend fun resetProjectPersonalization(projectId: String): EmptyResult<ChatError> =
        httpClient.delete(route = "/api/profiling/projects/$projectId")
            .mapError(ChatError::Network)
            .asEmptyResult()
}
