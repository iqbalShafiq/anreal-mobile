package co.ratmo.anreal.feature.workspace.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.workspace.domain.TaskStatus
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

    @Test
    fun scope_sites_maps_entries_with_session() = runTest {
        val repository = repository(
            """{"sites":[{"siteId":"s1","sessionId":"abc","version":2,"stableVersion":1,"status":"ready","previewUrl":"/api/sites/s1/v2/preview/index.html","downloadUrl":"/api/sites/s1/v2/download","updatedAt":"t"}]}""",
        )

        val result = repository.listScopeSites("abc")

        val entry = (result as Result.Success).data.single()
        assertThat(entry.siteId).isEqualTo("s1")
        assertThat(entry.sessionId).isEqualTo("abc")
    }

    @Test
    fun scope_sites_400_keeps_message() = runTest {
        val repository = repository(
            body = """{"error":"sessionId is required"}""",
            status = HttpStatusCode.BadRequest,
        )

        val result = repository.listScopeSites("")

        val error = ((result as Result.Error).error as WorkspaceError.Network).error
        assertThat(error.kind).isEqualTo(DataError.Network.Kind.BAD_REQUEST)
        assertThat(error.serverMessage).isEqualTo("sessionId is required")
    }

    @Test
    fun scope_sites_404_maps_not_found() = runTest {
        val repository = repository(
            body = """{"error":"Session not found"}""",
            status = HttpStatusCode.NotFound,
        )

        val result = repository.listScopeSites("ghost")

        val error = ((result as Result.Error).error as WorkspaceError.Network).error
        assertThat(error.kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
    }

    @Test
    fun tasks_list_maps_items() = runTest {
        val repository = repository(
            """{"items":[{"id":"t1","userId":"u1","projectId":null,"title":"Fix login","status":"doing","sourceSessionId":"abc","dueAt":null,"description":"Old bug","subtasks":[{"id":"st1","title":"Repro","done":true}],"createdAt":"t","updatedAt":"t"}]}""",
        )

        val result = repository.listTasks("abc")

        val task = (result as Result.Success).data.single()
        assertThat(task.title).isEqualTo("Fix login")
        assertThat(task.subtasks.single().done).isTrue()
    }

    @Test
    fun tasks_create_maps_result() = runTest {
        val repository = repository("""{"id":"t2","title":"New","status":"inbox"}""")

        val result = repository.createTask("abc", "New", null, emptyList(), null)

        val task = (result as Result.Success).data
        assertThat(task.id).isEqualTo("t2")
        assertThat(task.status).isEqualTo(TaskStatus.Inbox)
    }

    @Test
    fun tasks_update_toggle_maps_subtasks() = runTest {
        val repository = repository(
            """{"id":"t1","title":"Fix login","status":"doing","description":null,"subtasks":[{"id":"st1","title":"Repro","done":false}]}""",
        )

        val result = repository.updateTask("abc", "t1", null, null, null, emptyList(), listOf("st1" to false), emptyList())

        val task = (result as Result.Success).data
        assertThat(task.subtasks.single().done).isFalse()
    }

    @Test
    fun tasks_delete_404_maps_not_found() = runTest {
        val repository = repository(
            body = """{"error":"Task not found","code":"TASK_NOT_FOUND"}""",
            status = HttpStatusCode.NotFound,
        )

        val result = repository.deleteTask("abc", "ghost")

        val error = ((result as Result.Error).error as WorkspaceError.Network).error
        assertThat(error.code).isEqualTo("TASK_NOT_FOUND")
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
