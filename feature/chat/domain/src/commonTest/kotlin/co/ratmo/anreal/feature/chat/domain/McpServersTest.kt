package co.ratmo.anreal.feature.chat.domain

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class McpServersTest {

    @Test
    fun validate_rejects_authorization_header_and_http_url() {
        val errors = validateMcpInput(
            name = "docs", url = "http://mcp.example.com/mcp", authType = "bearer",
            headers = listOf("Authorization" to "x"),
        )
        assertThat(errors.containsKey("url")).isTrue()
        assertThat(errors.containsKey("headers")).isTrue()
    }

    @Test
    fun validate_accepts_clean_bearer_input() {
        val errors = validateMcpInput(
            name = "docs", url = "https://mcp.example.com/mcp", authType = "bearer",
            headers = listOf("X-Api-Key" to "k"),
        )
        assertThat(errors.isEmpty()).isTrue()
    }

    @Test
    fun validate_rejects_private_host() {
        val errors = validateMcpInput(name = "docs", url = "https://192.168.1.9/mcp", authType = "none", headers = emptyList())
        assertThat(errors.containsKey("url")).isTrue()
    }

    @Test
    fun mcp_test_result_shapes() {
        val ok = McpTestResult(ok = true, tools = listOf(McpTool("search_docs", "Search")))
        assertThat(ok.tools.single().name == "search_docs").isTrue()
        assertThat(McpTestResult(ok = false, error = "boom").error == "boom").isTrue()
        assertThat(ok.ok).isTrue()
        assertThat(McpTestResult(ok = false).ok).isFalse()
    }
}
