package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpTestResult

class StubMcpDataSource : McpRemoteDataSource {
    override suspend fun listServers(): Result<List<McpServer>, DataError.Network> = Result.Success(emptyList())

    override suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun deleteServer(id: String): Result<Unit, DataError.Network> =
        Result.Error(DataError.Network(DataError.Network.Kind.UNKNOWN, 0, null, null, emptyMap()))

    override suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network> =
        Result.Success(McpTestResult(ok = false, error = "Unavailable in stubs"))
}
