package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorChatRemoteDataSourceTest {

    @Test
    fun create_session_maps_openapi_session_shape() = runTest {
        val source = source(
            path = "/api/chat/sessions",
            status = HttpStatusCode.Created,
            body = """{"sessionId":"s1","title":"New chat","projectId":"p1","createdAt":"now","updatedAt":"now"}""",
        )

        val result = source.createSession(sessionId = "s1", projectId = "p1")

        val session = (result as Result.Success).data
        assertThat(session.id).isEqualTo("s1")
        assertThat(session.projectId).isEqualTo("p1")
    }

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
    fun listSessions_sends_project_id_query() = runTest {
        var captured: String? = "unset"
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/chat/sessions")
            captured = request.url.parameters["projectId"]
            respond(
                content = """{"items":[],"nextCursor":null}""",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/json"),
            )
        }
        val source = KtorChatRemoteDataSource(
            httpClient = HttpClientFactory.create(
                engine = engine,
                tokenStore = InMemorySessionTokenStore(),
                baseUrl = "http://127.0.0.1:3001",
            ),
        )

        source.listSessions(projectId = "p1")
        assertThat(captured).isEqualTo("p1")

        source.listSessions()
        assertThat(captured).isEqualTo(null)
    }

    @Test
    fun openProject_maps_name() = runTest {
        val source = source(
            path = "/api/projects/p1/open",
            body = """{"id":"p1","name":"Research","description":"Notes","documentCount":1,"chatCount":2}""",
        )

        val project = (source.openProject("p1") as Result.Success).data
        assertThat(project.id).isEqualTo("p1")
        assertThat(project.name).isEqualTo("Research")
    }

    @Test
    fun loadHistory_accepts_mixed_anvia_content_shapes() = runTest {
        val source = source(
            path = "/api/chat",
            body = """
                [
                  {"role":"user","content":[{"type":"text","text":"Hi"}],"metadata":{"clientMessageId":"c1"}},
                  {"role":"system","content":"Earlier turns were summarized.","metadata":{"kind":"summary"}},
                  {"role":"assistant","content":[{"type":"text","text":"Hello there"}]}
                ]
            """.trimIndent(),
        )

        when (val result = source.loadHistory("s1")) {
            is Result.Success -> {
                assertThat(result.data.size).isEqualTo(3)
                assertThat((result.data[0].parts.single() as ChatPart.Text).text).isEqualTo("Hi")
                assertThat(result.data[1].role).isEqualTo(ChatRole.System)
                assertThat((result.data[2].parts.single() as ChatPart.Text).text).isEqualTo("Hello there")
            }
            is Result.Error -> error("expected success, got ${result.error}")
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

    @Test
    fun listSessionDocuments_maps_array() = runTest {
        val source = source(
            path = "/api/documents",
            body = """[{"id":"d1","filename":"Anvia.pdf","firstPageSummary":"Framework notes"}]""",
        )
        when (val result = source.listSessionDocuments("s1")) {
            is Result.Success -> {
                assertThat(result.data.single().id).isEqualTo("d1")
                assertThat(result.data.single().filename).isEqualTo("Anvia.pdf")
            }
            is Result.Error -> error("expected success")
        }
    }

    @Test
    fun steer_maps_409_to_no_active_run() = runTest {
        val source = source(
            path = "/api/chat/steer",
            status = HttpStatusCode.Conflict,
            body = """{"code":"NO_ACTIVE_RUN"}""",
        )
        val result = source.steer(
            "s1",
            listOf(QueuedItem(id = "q1", text = "Follow up")),
        )
        assertThat(result).isEqualTo(Result.Error(ChatError.NoActiveRun))
    }

    @Test
    fun document_upload_uses_multipart_contract() = runTest {
        val source = source(
            path = "/api/documents",
            status = HttpStatusCode.Accepted,
            body = """{"id":"d1","filename":"brief.pdf","status":"queued","sizeBytes":3}""",
        )

        val result = source.uploadDocument(
            "s1",
            ChatUpload("brief.pdf", "application/pdf", byteArrayOf(1, 2, 3)),
        )

        when (result) {
            is Result.Success -> {
                assertThat(result.data.id).isEqualTo("d1")
                assertThat(result.data.status).isEqualTo("queued")
                assertThat(result.data.sizeBytes).isEqualTo(3L)
            }
            is Result.Error -> error("expected success")
        }
    }

    @Test
    fun context_snippet_maps_server_wrapper() = runTest {
        val source = source(
            path = "/api/chat/s1/context-snippet",
            body = """{"snippet":{"id":"n1","text":"Keep this","sourceRole":"assistant","createdAt":"now"}}""",
        )

        val result = source.loadContextSnippet("s1")

        assertThat((result as Result.Success).data?.id).isEqualTo("n1")
    }
}

private fun source(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorChatRemoteDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
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
