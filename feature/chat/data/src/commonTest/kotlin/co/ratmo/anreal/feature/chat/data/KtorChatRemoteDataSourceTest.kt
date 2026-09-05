package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.ChatRunOptions
import co.ratmo.anreal.feature.chat.domain.ChatUpload
import co.ratmo.anreal.feature.chat.domain.queue.QueuedItem
import co.ratmo.anreal.feature.chat.domain.queue.SteerAttachment
import co.ratmo.anreal.feature.chat.domain.queue.SteerSnippet
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import co.ratmo.anreal.feature.chat.domain.stream.InteractionAvailability
import co.ratmo.anreal.feature.chat.domain.stream.InteractionResponse
import co.ratmo.anreal.feature.chat.domain.stream.SessionGrant
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.client.engine.mock.toByteArray
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

        val result = source.send(
            "s1",
            emptyList(),
            options = ChatRunOptions(model = "m1"),
        ) {}

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
    fun send_posts_canonical_messages_request() = runTest {
        var captured: String? = null
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/chat")
            captured = request.body.toByteArray().decodeToString()
            respond(
                content = """{"type":"stream_end","streamId":"s","eventId":0,"status":"completed"}""" + "\n",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }
        val source = KtorChatRemoteDataSource(
            httpClient = HttpClientFactory.create(
                engine = engine,
                tokenStore = InMemorySessionTokenStore(),
                baseUrl = "http://127.0.0.1:3001",
            ),
        )

        val result = source.send(
            sessionId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
            messages = emptyList(),
            options = ChatRunOptions(
                model = "deepseek/deepseek-v4-flash-0731",
                reasoningEffort = "max",
                webSearchEnabled = true,
                deepResearchEnabled = true,
            ),
        ) {}

        assertThat(result).isEqualTo(Result.Success(Unit))
        val body = requireNotNull(captured)
        assertThat(body.contains("\"type\":\"messages\"")).isEqualTo(true)
        assertThat(body.contains("\"modelId\":\"deepseek/deepseek-v4-flash-0731\"")).isEqualTo(true)
        assertThat(body.contains("\"deepResearchEnabled\":true")).isEqualTo(true)
        assertThat(body.contains("\"model\":")).isEqualTo(false)
    }

    @Test
    fun answer_interaction_posts_interaction_response() = runTest {
        var captured: String? = null
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/chat")
            captured = request.body.toByteArray().decodeToString()
            respond(
                content = """{"type":"stream_end","streamId":"s","eventId":0,"status":"completed"}""" + "\n",
                status = HttpStatusCode.OK,
                headers = headersOf(HttpHeaders.ContentType, "application/x-ndjson"),
            )
        }
        val source = KtorChatRemoteDataSource(
            httpClient = HttpClientFactory.create(
                engine = engine,
                tokenStore = InMemorySessionTokenStore(),
                baseUrl = "http://127.0.0.1:3001",
            ),
        )

        val result = source.answerInteraction(
            interactionId = "i1",
            response = InteractionResponse.ToolApproval(approved = true),
            options = ChatRunOptions(model = "m1"),
            sessionId = "3fa85f64-5717-4562-b3fc-2c963f66afa6",
        ) {}

        assertThat(result).isEqualTo(Result.Success(Unit))
        val body = requireNotNull(captured)
        assertThat(body.contains("\"type\":\"interaction_response\"")).isEqualTo(true)
        assertThat(body.contains("\"interactionId\":\"i1\"")).isEqualTo(true)
    }

    @Test
    fun interaction_status_maps_pending_and_unavailable() = runTest {
        val pending = source(
            path = "/api/chat/interactions/i1",
            body = """{"status":"pending"}""",
        ).getInteractionStatus("i1")
        assertThat(pending).isEqualTo(Result.Success(InteractionAvailability.Pending))

        val gone = source(
            path = "/api/chat/interactions/i1",
            body = """{"status":"unavailable"}""",
        ).getInteractionStatus("i1")
        assertThat(gone).isEqualTo(Result.Success(InteractionAvailability.Unavailable))
    }

    @Test
    fun stage_allow_session_posts_grant_scope() = runTest {
        var captured: String? = null
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/chat/interactions/i1/stage")
            captured = request.body.toByteArray().decodeToString()
            respond(
                content = """{"ok":true}""",
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

        val result = source.stageInteractionPolicy(
            interactionId = "i1",
            response = InteractionResponse.ToolApproval(approved = true),
            grantScope = SessionGrant.Session,
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
        assertThat(requireNotNull(captured).contains("\"grantScope\":\"session\"")).isEqualTo(true)
    }

    @Test
    fun stage_maps_interaction_error_codes() = runTest {
        val codes = mapOf(
            "INTERACTION_NOT_FOUND" to ChatError.InteractionNotFound,
            "INTERACTION_EXPIRED" to ChatError.InteractionExpired,
            "INTERACTION_STATE_CONFLICT" to ChatError.InteractionHandled,
            "INTERACTION_POLICY_CONFLICT" to ChatError.InteractionHandled,
            "INTERACTION_REPLAYED" to ChatError.InteractionHandled,
            "INTERACTION_CLAIMED" to ChatError.InteractionHandled,
            "INTERACTION_POLICY_UNAVAILABLE" to ChatError.InteractionPolicyUnavailable,
            "INTERACTION_STAGE_INVALID" to ChatError.InteractionStageInvalid,
        )
        codes.forEach { (code, expected) ->
            val status = if (code == "INTERACTION_POLICY_UNAVAILABLE") {
                HttpStatusCode.ServiceUnavailable
            } else if (code == "INTERACTION_NOT_FOUND") {
                HttpStatusCode.NotFound
            } else if (code == "INTERACTION_STAGE_INVALID") {
                HttpStatusCode.BadRequest
            } else {
                HttpStatusCode.Conflict
            }
            val result = source(
                path = "/api/chat/interactions/i1/stage",
                status = status,
                body = """{"code":"$code"}""",
            ).stageInteractionPolicy(
                interactionId = "i1",
                response = InteractionResponse.ToolApproval(approved = true),
            )
            assertThat(result).isEqualTo(Result.Error(expected))
        }
    }

    @Test
    fun capabilities_maps_four_flags() = runTest {
        val source = source(
            path = "/api/chat/capabilities",
            body = """{"webSearchAvailable":true,"deepResearchAvailable":true,"imageGenerationAvailable":false,"context7Configured":true}""",
        )

        when (val result = source.loadCapabilities("s1")) {
            is Result.Success -> {
                assertThat(result.data.webSearchAvailable).isEqualTo(true)
                assertThat(result.data.deepResearchAvailable).isEqualTo(true)
                assertThat(result.data.imageGenerationAvailable).isEqualTo(false)
                assertThat(result.data.context7Configured).isEqualTo(true)
            }
            is Result.Error -> error("expected success")
        }
    }

    @Test
    fun catalog_sends_output_type_query() = runTest {
        var captured: String? = "unset"
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/models")
            captured = request.url.parameters["outputType"]
            respond(
                content = """{"models":[],"reasoningEfforts":[]}""",
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

        source.loadCatalog(outputType = "text")
        assertThat(captured).isEqualTo("text")

        source.loadCatalog()
        assertThat(captured).isEqualTo(null)
    }

    @Test
    fun steer_posts_attachments_and_snippet() = runTest {
        var captured: String? = null
        val engine = MockEngine { request ->
            check(request.url.encodedPath == "/api/chat/steer")
            captured = request.body.toByteArray().decodeToString()
            respond(
                content = """{"ok":true,"streamId":"s","queued":1}""",
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

        val result = source.steer(
            "s1",
            listOf(
                QueuedItem(
                    id = "q1",
                    text = "Follow up",
                    attachments = listOf(SteerAttachment("image/png", "abc")),
                    contextSnippet = SteerSnippet("Keep this", "user"),
                ),
            ),
        )

        assertThat(result).isEqualTo(Result.Success(Unit))
        val body = requireNotNull(captured)
        assertThat(body.contains("attachments")).isEqualTo(true)
        assertThat(body.contains("contextSnippet")).isEqualTo(true)
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
