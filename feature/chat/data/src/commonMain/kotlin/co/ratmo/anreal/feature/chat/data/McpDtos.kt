package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.core.data.network.delete
import co.ratmo.anreal.core.data.network.get
import co.ratmo.anreal.core.data.network.patch
import co.ratmo.anreal.core.data.network.post
import co.ratmo.anreal.core.data.network.put
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.core.domain.util.asEmptyResult
import co.ratmo.anreal.core.domain.util.map
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpStatus
import co.ratmo.anreal.feature.chat.domain.McpTestResult
import co.ratmo.anreal.feature.chat.domain.McpTool
import io.ktor.client.HttpClient
import kotlinx.serialization.Serializable

@Serializable
data class McpToolDto(val name: String, val description: String = "")

@Serializable
data class McpServerDto(
    val id: String,
    val userId: String = "",
    val name: String,
    val url: String,
    val authType: String = "none",
    val allowedToolsJson: List<String> = emptyList(),
    val toolsJson: List<McpToolDto>? = null,
    val isEnabled: Boolean = true,
    val status: String = "untested",
    val lastError: String? = null,
    val hasCredentials: Boolean = false,
    val hasHeaders: Boolean = false,
)

@Serializable
data class McpTestResultDto(val ok: Boolean, val tools: List<McpToolDto>? = null, val error: String? = null)

fun McpServerDto.toMcpServer(): McpServer = McpServer(
    id = id, name = name, url = url,
    authType = if (authType == "bearer") McpAuthType.Bearer else McpAuthType.None,
    allowedTools = allowedToolsJson,
    tools = toolsJson.orEmpty().map { McpTool(it.name, it.description) },
    isEnabled = isEnabled,
    status = when (status) { "ok" -> McpStatus.Ok; "error" -> McpStatus.Error; else -> McpStatus.Untested },
    lastError = lastError, hasCredentials = hasCredentials, hasHeaders = hasHeaders,
)

@Serializable
data class McpHeaderDto(val name: String, val value: String)

@Serializable
data class McpUpsertDto(
    val name: String, val url: String, val authType: String,
    val token: String? = null, val headers: List<McpHeaderDto>? = null,
)

@Serializable
data class McpTestDto(
    val url: String, val authType: String,
    val token: String? = null, val headers: List<McpHeaderDto>? = null, val serverId: String? = null,
)

@Serializable
data class McpEnabledDto(val isEnabled: Boolean)

class KtorMcpDataSource(private val httpClient: HttpClient) : McpRemoteDataSource {
    override suspend fun listServers(): Result<List<McpServer>, DataError.Network> =
        httpClient.get<List<McpServerDto>>(route = "/api/mcp-servers").map { dtos -> dtos.map { it.toMcpServer() } }

    override suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network> =
        httpClient.post<McpUpsertDto, McpServerDto>(route = "/api/mcp-servers", body = upsert(name, url, authType, token, headers)).map { it.toMcpServer() }

    override suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network> =
        httpClient.put<McpUpsertDto, McpServerDto>(route = "/api/mcp-servers/$id", body = upsert(name, url, authType, token, headers)).map { it.toMcpServer() }

    override suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network> =
        httpClient.patch<McpEnabledDto, McpServerDto>(route = "/api/mcp-servers/$id/enabled", body = McpEnabledDto(isEnabled)).map { it.toMcpServer() }

    override suspend fun deleteServer(id: String): Result<Unit, DataError.Network> =
        httpClient.delete(route = "/api/mcp-servers/$id").asEmptyResult()

    override suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network> =
        httpClient.post<McpTestDto, McpTestResultDto>(
            route = "/api/mcp-servers/test",
            body = McpTestDto(url, authType.name.lowercase(), token?.takeIf { it.isNotEmpty() }, headers.map { McpHeaderDto(it.first, it.second) }.takeIf { it.isNotEmpty() }, serverId),
        ).map { dto -> McpTestResult(dto.ok, dto.tools.orEmpty().map { McpTool(it.name, it.description) }, dto.error) }

    private fun upsert(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?) = McpUpsertDto(
        name = name, url = url, authType = authType.name.lowercase(),
        token = token?.takeIf { it.isNotEmpty() },
        headers = headers?.map { McpHeaderDto(it.first, it.second) },
    )
}
