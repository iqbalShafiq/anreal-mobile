package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorChatRemoteDataSourceTest {

    @Test
    fun listSessions_maps_page() = runTest {
        val source = source(
            path = "/api/chat/sessions",
            body = """{"items":[{"sessionId":"s1","title":"Docs","updatedAt":"2026-08-14T00:00:00Z","unread":true}],"nextCursor":null}""",
        )

        when (val result = source.listSessions()) {
            is Result.Success -> {
                assertThat(result.data.items.single().id).isEqualTo("s1")
                assertThat(result.data.items.single().unread).isEqualTo(true)
            }
            is Result.Error -> error("expected success")
        }
    }

    @Test
    fun send_maps_409_to_run_active() = runTest {
        val source = source(
            path = "/api/chat",
            status = HttpStatusCode.Conflict,
            body = """{"code":"RUN_ACTIVE"}""",
        )

        val result = source.send("s1", emptyList()) {}

        assertThat(result).isEqualTo(Result.Error(ChatError.RunActive))
    }
}

private fun source(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorChatRemoteDataSource {
    val engine = MockEngine {
        respond(
            content = body,
            status = status,
            headers = headersOf(HttpHeaders.ContentType, "application/json"),
        )
    }
    return KtorChatRemoteDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
}
