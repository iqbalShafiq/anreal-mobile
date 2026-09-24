package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.getBytes
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.postForResponse
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.feature.chat.domain.SessionSiteEntry
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SiteStatus
import co.ratmo.anreal.feature.chat.domain.SitesRemoteDataSource
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

@Serializable
data class SessionSiteEntryDto(
    val siteId: String, val version: Int, val stableVersion: Int? = null,
    val status: String = "queued", val previewUrl: String? = null,
    val downloadUrl: String = "", val updatedAt: String = "",
    val sessionId: String = "",
)

@Serializable
data class SessionSitesDto(val sites: List<SessionSiteEntryDto> = emptyList())

@Serializable
data class SiteRetryDto(val siteId: String, val version: Int, val status: String = "queued")

@Serializable
data class SiteRollbackRequestDto(val version: Int)

@Serializable
data class SiteManifestDto(val siteId: String, val stableVersion: Int? = null, val version: Int = 0)

fun SessionSiteEntryDto.toEntry(): SessionSiteEntry = SessionSiteEntry(
    siteId = siteId, version = version, stableVersion = stableVersion,
    status = when (status) { "running" -> SiteStatus.Running; "ready" -> SiteStatus.Ready; "failed" -> SiteStatus.Failed; else -> SiteStatus.Queued },
    previewUrl = previewUrl, downloadUrl = downloadUrl, updatedAt = updatedAt, sessionId = sessionId,
)

class KtorSitesDataSource(private val httpClient: HttpClient) : SitesRemoteDataSource {
    override suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network> =
        httpClient.get<SessionSitesDto>(route = "/api/sites/by-session/$sessionId")
            .map { dto -> dto.sites.map { it.toEntry() } }

    override suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network> =
        httpClient.getBytes(route = "/api/sites/$siteId/v$version/download")

    override suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network> =
        httpClient.postForResponse<SiteRetryDto>(route = "/api/sites/$siteId/retry")
            .map { dto -> SiteBuildState(dto.siteId, dto.version, SiteBuildPhase.Starting, "Mengulang build.") }

    override suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network> =
        httpClient.post<SiteRollbackRequestDto, SiteManifestDto>(route = "/api/sites/$siteId/rollback", body = SiteRollbackRequestDto(version))
            .map { dto -> dto.stableVersion ?: version }
}
