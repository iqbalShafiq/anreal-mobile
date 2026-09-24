package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorMcpDataSourceTest {

    @Test
    fun test_ok_true_maps_tools() = runTest {
        val source = mcpSource(
            path = "/api/mcp-servers/test",
            body = """{"ok":true,"tools":[{"name":"search_docs","description":"Search the docs"}]}""",
        )
        val result = source.testConnection("https://mcp.example.com/mcp", McpAuthType.None, null, emptyList(), null)
        val data = (result as Result.Success).data
        assertThat(data.ok).isTrue()
        assertThat(data.tools.single().name).isEqualTo("search_docs")
    }

    @Test
    fun test_ok_false_is_success_result_with_error() = runTest {
        val source = mcpSource(
            path = "/api/mcp-servers/test",
            body = """{"ok":false,"error":"MCP URL must use public https (http and private hosts are blocked)"}""",
        )
        val result = source.testConnection("http://mcp.example.com/mcp", McpAuthType.None, null, emptyList(), null)
        val data = (result as Result.Success).data
        assertThat(data.ok).isFalse()
    }

    @Test
    fun create_400_duplicate_name_keeps_message() = runTest {
        val source = mcpSource(
            path = "/api/mcp-servers",
            status = HttpStatusCode.BadRequest,
            body = """{"error":"An MCP server with this name already exists","code":"INVALID_MCP_SERVER"}""",
        )
        val result = source.createServer("docs", "https://mcp.example.com/mcp", McpAuthType.None, null, emptyList())
        assertThat((result as Result.Error).error.serverMessage).isEqualTo("An MCP server with this name already exists")
    }

    @Test
    fun list_servers_maps_allowed_tools() = runTest {
        val source = mcpSource(
            path = "/api/mcp-servers",
            body = """[{"id":"m1","name":"docs","url":"https://mcp.example.com/mcp","authType":"none","allowedToolsJson":["a"],"isEnabled":true,"status":"ok","hasCredentials":false,"hasHeaders":false}]""",
        )
        val data = (source.listServers() as Result.Success).data
        assertThat(data.single().allowedTools).isEqualTo(listOf("a"))
    }
}

private fun mcpSource(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorMcpDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorMcpDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
}
