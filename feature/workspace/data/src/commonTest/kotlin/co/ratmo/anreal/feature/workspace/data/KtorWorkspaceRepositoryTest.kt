package co.ratmo.anreal.feature.workspace.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.workspace.domain.WorkspaceError
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorWorkspaceRepositoryTest {
    @Test
    fun get_project_maps_openapi_project_shape() = runTest {
        val repository = repository(
            """{"id":"p1","name":"Research","description":"Q3","documentCount":2,"chatCount":3,"lastOpenedAt":null,"createdAt":"now","updatedAt":"now"}""",
        )

        val result = repository.getProject("p1")

        val project = (result as Result.Success).data
        assertThat(project.name).isEqualTo("Research")
        assertThat(project.chatCount).isEqualTo(3)
    }

    @Test
    fun list_projects_maps_full_openapi_shape() = runTest {
        val repository = repository(
            """{"items":[{"id":"p1","name":"Research","description":"Q3","documentCount":2,"chatCount":3,"lastOpenedAt":null,"createdAt":"now","updatedAt":"now"}],"nextCursor":null}""",
        )

        val result = repository.listProjects()

        val project = (result as Result.Success).data.items.single()
        assertThat(project.name).isEqualTo("Research")
        assertThat(project.documentCount).isEqualTo(2)
    }

    @Test
    fun server_error_body_survives_workspace_mapping() = runTest {
        val repository = repository(
            body = """{"error":"Cascade delete requires confirm=true","code":"CONFIRM_REQUIRED"}""",
            status = HttpStatusCode.BadRequest,
        )

        val result = repository.listProjects()

        val error = ((result as Result.Error).error as WorkspaceError.Network).error
        assertThat(error.kind).isEqualTo(DataError.Network.Kind.BAD_REQUEST)
        assertThat(error.code).isEqualTo("CONFIRM_REQUIRED")
        assertThat(error.serverMessage).isEqualTo("Cascade delete requires confirm=true")
    }
}

private fun repository(
    body: String,
    status: HttpStatusCode = HttpStatusCode.OK,
): KtorWorkspaceRepository {
    val engine = MockEngine {
        respond(body, status, headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorWorkspaceRepository(
        HttpClientFactory.create(
            engine,
            InMemorySessionTokenStore(),
            "http://127.0.0.1:3001",
        ),
    )
}
