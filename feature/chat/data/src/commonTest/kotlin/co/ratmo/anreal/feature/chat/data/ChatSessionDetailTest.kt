package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import kotlin.test.Test

class ChatSessionDetailTest {

    private val json = Json { ignoreUnknownKeys = true }

    @Test
    fun session_entry_old_payload_defaults_session_id() {
        val dto = json.decodeFromString<SessionSiteEntryDto>(
            """{"siteId":"s1","version":1,"stableVersion":1,"status":"ready","downloadUrl":"/api/sites/s1/v1/download","updatedAt":"t"}""",
        )
        assertThat(dto.toEntry().sessionId).isEqualTo("")
    }

    @Test
    fun get_session_maps_detail() = runTest {
        val source = sessionSource(
            path = "/api/chat/sessions/abc",
            body = """{"sessionId":"abc","projectId":"p1","title":"Docs","updatedAt":"t"}""",
        )
        val detail = (source.getSession("abc") as Result.Success).data
        assertThat(detail.projectId).isEqualTo("p1")
        assertThat(detail.title).isEqualTo("Docs")
    }

    @Test
    fun get_session_404_maps_not_found() = runTest {
        val source = sessionSource(
            path = "/api/chat/sessions/ghost",
            status = HttpStatusCode.NotFound,
            body = """{"error":"Chat session not found","code":"CHAT_SESSION_NOT_FOUND"}""",
        )
        when (val result = source.getSession("ghost")) {
            is Result.Success -> error("expected error")
            is Result.Error -> {
                val network = result.error as ChatError.Network
                assertThat(network.error.kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
            }
        }
    }
}

private fun sessionSource(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorChatRemoteDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorChatRemoteDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
}
