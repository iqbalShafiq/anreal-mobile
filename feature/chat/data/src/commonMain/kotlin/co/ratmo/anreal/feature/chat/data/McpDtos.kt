package co.ratmo.anreal.feature.chat.data

import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpStatus
import co.ratmo.anreal.feature.chat.domain.McpTool
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
