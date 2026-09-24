package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result

enum class McpAuthType { None, Bearer }
enum class McpStatus { Ok, Untested, Error }

data class McpTool(val name: String, val description: String)

data class McpServer(
    val id: String,
    val name: String,
    val url: String,
    val authType: McpAuthType = McpAuthType.None,
    val allowedTools: List<String> = emptyList(),
    val tools: List<McpTool> = emptyList(),
    val isEnabled: Boolean = true,
    val status: McpStatus = McpStatus.Untested,
    val lastError: String? = null,
    val hasCredentials: Boolean = false,
    val hasHeaders: Boolean = false,
)

data class McpTestResult(val ok: Boolean, val tools: List<McpTool> = emptyList(), val error: String? = null)

interface McpRemoteDataSource {
    suspend fun listServers(): Result<List<McpServer>, DataError.Network>
    suspend fun createServer(name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>): Result<McpServer, DataError.Network>
    suspend fun updateServer(id: String, name: String, url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>?): Result<McpServer, DataError.Network>
    suspend fun setServerEnabled(id: String, isEnabled: Boolean): Result<McpServer, DataError.Network>
    suspend fun deleteServer(id: String): Result<Unit, DataError.Network>
    suspend fun testConnection(url: String, authType: McpAuthType, token: String?, headers: List<Pair<String, String>>, serverId: String?): Result<McpTestResult, DataError.Network>
}

fun validateMcpInput(name: String, url: String, authType: String, headers: List<Pair<String, String>>): Map<String, String> {
    val errors = mutableMapOf<String, String>()
    if (authType != "none" && authType != "bearer") errors["authType"] = "Choose None or Bearer token."
    if (name.trim().isEmpty() || name.trim().length > 64) errors["name"] = "Short label used for tool prefixes."
    if (!isPublicHttpsShallow(url)) errors["url"] = "Public https Streamable HTTP endpoint."
    val seen = mutableSetOf<String>()
    headers.forEach { (headerName, value) ->
        if (headerName.equals("authorization", ignoreCase = true)) errors["headers"] = "Use the auth type, not an authorization header."
        else if (!seen.add(headerName.lowercase())) errors["headers"] = "Duplicate header."
        else if (value.isEmpty()) errors["headers"] = "Header value required."
    }
    if (headers.size > 16) errors["headers"] = "At most 16 custom headers."
    return errors
}

fun isPublicHttpsShallow(rawUrl: String): Boolean {
    val trimmed = rawUrl.trim()
    if (!trimmed.startsWith("https://")) return false
    val host = trimmed.removePrefix("https://").substringBefore("/").substringBefore(":").lowercase()
    if (host.isEmpty()) return false
    if (host == "localhost" || host.endsWith(".localhost") || host.endsWith(".local") || host.endsWith(".internal")) return false
    if (host == "0.0.0.0" || host == "::1") return false
    if (host.startsWith("10.") || host.startsWith("192.168.") || host.startsWith("127.") || host.startsWith("172.")) return false
    return true
}
