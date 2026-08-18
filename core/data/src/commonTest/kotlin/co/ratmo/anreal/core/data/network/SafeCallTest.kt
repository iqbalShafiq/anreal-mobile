package co.ratmo.anreal.core.data.network

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.network.sockets.SocketTimeoutException
import io.ktor.client.plugins.HttpRequestTimeoutException
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
        when (denied) {
            is Result.Success -> error("expected error")
            is Result.Error -> {
                assertThat(denied.error.kind).isEqualTo(DataError.Network.Kind.UNAUTHORIZED)
                assertThat(denied.error.serverMessage).isEqualTo("no")
                assertThat(denied.error.statusCode).isEqualTo(401)
            }
        }
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

    @Test
    fun stored_token_is_sent_as_bearer_authorization() = runTest {
        val store = InMemorySessionTokenStore().apply { save("native-token") }
        var authorization: String? = null
        val engine = MockEngine { request ->
            authorization = request.headers[HttpHeaders.Authorization]
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val client = HttpClientFactory.create(
            engine = engine,
            tokenStore = store,
            baseUrl = "http://127.0.0.1:3001",
        )

        client.get<Envelope>(route = "/ok")

        assertThat(authorization).isEqualTo("Bearer native-token")
    }

    @Test
    fun safe_call_maps_all_ktor_timeout_types() = runTest {
        val requestTimeout = safeCall<Envelope> {
            throw HttpRequestTimeoutException("http://127.0.0.1:3001", 1_000L)
        }
        val connectTimeout = safeCall<Envelope> {
            throw ConnectTimeoutException("connect timeout")
        }
        val socketTimeout = safeCall<Envelope> {
            throw SocketTimeoutException("socket timeout")
        }

        assertThat(requestTimeout).isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
        assertThat(connectTimeout).isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
        assertThat(socketTimeout).isEqualTo(Result.Error(DataError.Network.REQUEST_TIMEOUT))
    }
}
