package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorAccountSettingsDataSourceTest {
    @Test
    fun health_uses_public_liveness_contract() = runTest {
        val engine = MockEngine { request ->
            assertThat(request.url.encodedPath).isEqualTo("/health")
            respond(
                content = """{"ok":true}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val source = KtorAccountSettingsDataSource(
            HttpClientFactory.create(
                engine = engine,
                tokenStore = InMemorySessionTokenStore(),
                baseUrl = "http://127.0.0.1:3001",
            ),
        )

        assertThat(source.checkHealth()).isEqualTo(Result.Success(true))
    }
}
