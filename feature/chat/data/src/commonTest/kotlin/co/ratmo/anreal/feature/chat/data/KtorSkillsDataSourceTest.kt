package co.ratmo.anreal.feature.chat.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.network.HttpClientFactory
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import io.ktor.client.engine.mock.MockEngine
import io.ktor.client.engine.mock.respond
import io.ktor.http.HttpHeaders
import io.ktor.http.HttpStatusCode
import io.ktor.http.headersOf
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class KtorSkillsDataSourceTest {

    @Test
    fun list_skills_maps_rows() = runTest {
        val source = skillsSource(
            path = "/api/skills",
            body = """[{"id":"s1","userId":"u1","name":"release-notes","description":"Draft notes","bodyMd":"---\nname: release-notes\n---","isEnabled":true,"status":"active","version":1,"createdAt":"t","updatedAt":"t"}]""",
        )
        when (val result = source.listSkills()) {
            is Result.Success -> assertThat(result.data.single().name).isEqualTo("release-notes")
            is Result.Error -> error("expected success")
        }
    }

    @Test
    fun create_skill_400_keeps_server_message() = runTest {
        val source = skillsSource(
            path = "/api/skills",
            status = HttpStatusCode.BadRequest,
            body = """{"error":"Frontmatter name must equal the skill name","issues":[{"path":"bodyMd","message":"Frontmatter name must equal the skill name"}],"code":"INVALID_SKILL"}""",
        )
        val result = source.createSkill("brief", "Write a brief", "body")
        val error = (result as Result.Error).error
        assertThat(error.serverMessage).isEqualTo("Frontmatter name must equal the skill name")
    }

    @Test
    fun delete_foreign_id_maps_not_found() = runTest {
        val source = skillsSource(
            path = "/api/skills/foreign",
            status = HttpStatusCode.NotFound,
            body = """{"error":"Skill not found","code":"SKILL_NOT_FOUND"}""",
        )
        val result = source.deleteSkill("foreign")
        assertThat((result as Result.Error).error.kind).isEqualTo(DataError.Network.Kind.NOT_FOUND)
    }

    @Test
    fun enable_draft_400_readable() = runTest {
        val source = skillsSource(
            path = "/api/skills/s1/enabled",
            status = HttpStatusCode.BadRequest,
            body = """{"error":"Review the skill in the Skills modal before enabling it","code":"INVALID_SKILL"}""",
        )
        val result = source.setSkillEnabled("s1", true)
        assertThat((result as Result.Error).error.serverMessage).isEqualTo("Review the skill in the Skills modal before enabling it")
    }
}

private fun skillsSource(
    path: String,
    status: HttpStatusCode = HttpStatusCode.OK,
    body: String,
): KtorSkillsDataSource {
    val engine = MockEngine { request ->
        check(request.url.encodedPath == path)
        respond(content = body, status = status, headers = headersOf(HttpHeaders.ContentType, "application/json"))
    }
    return KtorSkillsDataSource(
        httpClient = HttpClientFactory.create(
            engine = engine,
            tokenStore = InMemorySessionTokenStore(),
            baseUrl = "http://127.0.0.1:3001",
        ),
    )
}
