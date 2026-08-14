package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.Serializable
import kotlin.test.Test

class SafeCallTest {

    @Serializable
    data class Envelope(val ok: Boolean)

    @Test
    fun get_maps_success_and_unauthorized() = runTest {
        val engine = MockEngine { request ->
            if (request.url.encodedPath.endsWith("/ok")) {
                respond(
                    content = """{"ok":true}""",
                    status = HttpStatusCode.OK,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            } else {
                respond(
                    content = """{"error":"no"}""",
                    status = HttpStatusCode.Unauthorized,
                    headers = headersOf(HttpHeaders.ContentType, "application/json"),
                )
            }
        }
        val client = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        )

        val success = client.get<Envelope>(route = "/ok")
        assertThat(success).isEqualTo(Result.Success(Envelope(ok = true)))

        val denied = client.get<Envelope>(route = "/nope")
        assertThat(denied).isEqualTo(Result.Error(DataError.Network.UNAUTHORIZED))
    }

    @Test
    fun unauthorized_clears_the_session_token() = runTest {
        val store = InMemorySessionTokenStore()
        store.save("stale.token")
        val engine = MockEngine {
            respond(
                content = """{"error":"no"}""",
                status = HttpStatusCode.Unauthorized,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClientFactory.create(
            engine = engine,
            tokenStore = store,
            baseUrl = "http://127.0.0.1:3001",
        )

        client.get<Envelope>(route = "/nope")
        assertThat(store.token()).isNull()
    }
}
