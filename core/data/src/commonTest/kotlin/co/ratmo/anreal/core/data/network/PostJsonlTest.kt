package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

class PostJsonlTest {

    @Serializable
    private data class Request(val message: String)

    @Test
    fun emits_each_jsonl_record_and_uses_unbuffered_stream_headers() = runTest {
        var accept: String? = null
        var encoding: String? = null
        val engine = MockEngine { request ->
            accept = request.headers[HttpHeaders.Accept]
            encoding = request.headers[HttpHeaders.AcceptEncoding]
            respond(
                content = """
                    {"type":"stream_start","streamId":"s1","eventId":0}
                    {"type":"stream_end","streamId":"s1","eventId":1,"status":"completed"}
                """.trimIndent(),
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }
        val client = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        )
        val lines = mutableListOf<String>()

        val result = client.postJsonl(
            route = "/api/chat",
            body = Request("hello"),
            onLine = lines::add,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(lines).containsExactly(
            """{"type":"stream_start","streamId":"s1","eventId":0}""",
            """{"type":"stream_end","streamId":"s1","eventId":1,"status":"completed"}""",
        )
        assertThat(accept).isEqualTo("application/x-ndjson")
        assertThat(encoding).isEqualTo("identity")
        client.close()
    }
}
