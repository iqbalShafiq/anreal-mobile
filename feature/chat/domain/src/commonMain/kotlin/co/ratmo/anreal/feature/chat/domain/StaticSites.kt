package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import kotlinx.coroutines.flow.Flow

enum class SiteStatus { Queued, Running, Ready, Failed }
enum class SiteBuildPhase { Starting, Planning, Building, Bundling, Preview, Ready, Failed }

data class SessionSiteEntry(
    val siteId: String, val version: Int, val stableVersion: Int?,
    val status: SiteStatus, val previewUrl: String?, val downloadUrl: String, val updatedAt: String,
    val sessionId: String = "",
)

data class SiteVersionEntry(
    val siteId: String, val version: Int,
    val ready: Boolean, val failed: Boolean, val stable: Boolean,
    val previewUrl: String?, val downloadUrl: String,
)

data class SiteBuildState(
    val siteId: String, val version: Int, val phase: SiteBuildPhase,
    val message: String, val previewUrl: String? = null, val downloadUrl: String? = null,
)

interface SitesRemoteDataSource {
    suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network>
    suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network>
    suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network>
    suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network>
}

interface EnhancementSelectionStore {
    fun observeSkillIds(): Flow<List<String>>
    fun observeMcpServerIds(): Flow<List<String>>
    suspend fun saveSkillIds(ids: List<String>)
    suspend fun saveMcpServerIds(ids: List<String>)
}

fun interface SiteBaseUrlProvider {
    fun baseUrl(): String
}

fun intersectWithCatalog(selected: List<String>, catalogIds: List<String>): List<String> {
    val catalog = catalogIds.toSet()
    return selected.filter { it in catalog }.distinct()
}

fun applySiteBuildEvent(
    current: SiteBuildState?, eventSiteId: String, eventVersion: Int,
    phase: SiteBuildPhase, message: String, previewUrl: String? = null, downloadUrl: String? = null,
): SiteBuildState {
    if (current == null || current.siteId != eventSiteId) {
        return SiteBuildState(eventSiteId, eventVersion, phase, message, previewUrl, downloadUrl)
    }
    if (phase == SiteBuildPhase.Ready && message.isBlank()) {
        return current.copy(version = eventVersion, phase = phase, message = "Situs siap diunduh.", previewUrl = previewUrl ?: current.previewUrl, downloadUrl = downloadUrl ?: current.downloadUrl)
    }
    return current.copy(version = eventVersion, phase = phase, message = message, previewUrl = previewUrl ?: current.previewUrl, downloadUrl = downloadUrl ?: current.downloadUrl)
}

fun applySiteVersionEvent(
    current: List<SiteVersionEntry>, eventSiteId: String, eventVersion: Int,
    phase: SiteBuildPhase, previewUrl: String?, downloadUrl: String?,
): List<SiteVersionEntry> {
    val base = if (current.any { it.siteId != eventSiteId }) emptyList() else current
    val existing = base.firstOrNull { it.version == eventVersion }
    val updated = if (existing == null) {
        SiteVersionEntry(eventSiteId, eventVersion, ready = phase == SiteBuildPhase.Ready, failed = phase == SiteBuildPhase.Failed, stable = phase == SiteBuildPhase.Ready, previewUrl = previewUrl, downloadUrl = downloadUrl ?: "")
    } else {
        existing.copy(ready = phase == SiteBuildPhase.Ready, failed = phase == SiteBuildPhase.Failed, stable = if (phase == SiteBuildPhase.Ready) true else existing.stable, previewUrl = previewUrl ?: existing.previewUrl, downloadUrl = downloadUrl ?: existing.downloadUrl)
    }
    val merged = (base.filterNot { it.version == eventVersion } + updated).sortedBy { it.version }
    return if (updated.stable) merged.map { it.copy(stable = it.version == eventVersion) } else merged
}

fun stableSiteUrls(siteId: String, version: Int): Pair<String, String> =
    "/api/sites/$siteId/v$version/preview/index.html" to "/api/sites/$siteId/v$version/download"
