package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import co.ratmo.anreal.feature.chat.domain.McpStatus
import kotlinx.serialization.json.Json
import kotlin.test.Test

class McpDtosTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun mcp_mapper_reads_allowed_tools_json_and_flags() {
        val dto = json.decodeFromString<McpServerDto>(
            """{"id":"m1","userId":"u1","name":"docs","url":"https://mcp.example.com/mcp","authType":"bearer","allowedToolsJson":["search_docs"],"toolsJson":[{"name":"search_docs","description":"Search"}],"isEnabled":true,"status":"untested","hasCredentials":true,"hasHeaders":false}""",
        )
        val server = dto.toMcpServer()
        assertThat(server.allowedTools).isEqualTo(listOf("search_docs"))
        assertThat(server.hasCredentials).isTrue()
        assertThat(server.status).isEqualTo(McpStatus.Untested)
    }

    @Test
    fun mcp_mapper_unknown_status_defaults_untested() {
        val dto = json.decodeFromString<McpServerDto>(
            """{"id":"m1","name":"docs","url":"https://x.example/mcp","status":"weird"}""",
        )
        assertThat(dto.toMcpServer().status).isEqualTo(McpStatus.Untested)
    }
}
