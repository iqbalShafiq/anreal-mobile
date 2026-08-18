package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEmpty
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class HttpClientFactoryTest {
    @Test
    fun network_logging_is_disabled_by_default() = runTest {
        val messages = mutableListOf<String>()
        val client = createClient(messages = messages)

        client.get("/health")

        assertThat(messages).isEmpty()
        client.close()
    }

    @Test
    fun network_logging_records_request_and_response_when_enabled() = runTest {
        val messages = mutableListOf<String>()
        val client = createClient(
            enableNetworkLogging = true,
            messages = messages,
        )

        client.get("/health")

        val output = messages.joinToString(separator = "\n")
        assertThat(output).contains("http://127.0.0.1:3001/health")
        assertThat(output).contains("200 OK")
        client.close()
    }

    private fun createClient(
        enableNetworkLogging: Boolean = false,
        messages: MutableList<String>,
    ) = HttpClientFactory.create(
        engine = MockEngine {
            respond(
                content = "{}",
                status = HttpStatusCode.OK,
            )
        },
        tokenStore = InMemorySessionTokenStore(),
        baseUrl = "http://127.0.0.1:3001",
        enableNetworkLogging = enableNetworkLogging,
        networkLogger = object : Logger {
            override fun log(message: String) {
                messages += message
            }
        },
    )
}
