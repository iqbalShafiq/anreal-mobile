package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.SessionSiteEntry
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import co.ratmo.anreal.feature.chat.domain.SiteBuildState
import co.ratmo.anreal.feature.chat.domain.SitesRemoteDataSource

class StubSitesDataSource : SitesRemoteDataSource {
    override suspend fun sitesBySession(sessionId: String): Result<List<SessionSiteEntry>, DataError.Network> =
        Result.Success(emptyList())

    override suspend fun downloadSite(siteId: String, version: Int): Result<ByteArray, DataError.Network> =
        Result.Success(ByteArray(0))

    override suspend fun retrySite(siteId: String): Result<SiteBuildState, DataError.Network> =
        Result.Success(SiteBuildState("", 0, SiteBuildPhase.Starting, ""))

    override suspend fun rollbackSite(siteId: String, version: Int): Result<Int, DataError.Network> =
        Result.Success(version)
}
