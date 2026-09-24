package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.SiteStatus
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorSitesDataSourceTest {

    @Test
    fun by_session_maps_entries() = runTest {
        val source = sitesSource(
            path = "/api/sites/by-session/abc",
            body = """{"sites":[{"siteId":"s1","version":2,"stableVersion":1,"status":"ready","previewUrl":"/api/sites/s1/v2/preview/index.html","downloadUrl":"/api/sites/s1/v2/download","updatedAt":"t"}]}""",
        )
        val entry = (source.sitesBySession("abc") as Result.Success).data.single()
        assertThat(entry.version).isEqualTo(2)
        assertThat(entry.stableVersion).isEqualTo(1)
        assertThat(entry.status).isEqualTo(SiteStatus.Ready)
    }

    @Test
    fun retry_non_failed_maps_conflict() = runTest {
        val source = sitesSource(
            path = "/api/sites/s1/retry",
            status = HttpStatusCode.Conflict,
            body = """{"error":"only failed builds can be retried"}""",
        )
        val result = source.retrySite("s1")
        assertThat((result as Result.Error).error.kind).isEqualTo(DataError.Network.Kind.CONFLICT)
    }

    @Test
    fun rollback_success_returns_stable_version() = runTest {
        val source = sitesSource(
            path = "/api/sites/s1/rollback",
            body = """{"siteId":"s1","stableVersion":1,"version":2}""",
        )
        assertThat((source.rollbackSite("s1", 1) as Result.Success).data).isEqualTo(1)
    }

    @Test
    fun download_404_maps_not_found() = runTest {
        val source = sitesSource(
            path = "/api/sites/s1/v9/download",
            status = HttpStatusCode.NotFound,
            body = """{"error":"Build not found"}""",
        )
        val result = source.downloadSite("s1", 9)
        assertThat((result as Result.Error).error.kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
    }
}

private fun sitesSource(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorSitesDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorSitesDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
}
