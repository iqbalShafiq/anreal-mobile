package co.ratmo.anreal.feature.chat.presentation

import androidx.lifecycle.SavedStateHandle
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotNull
import assertk.assertions.isNull
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.McpAuthType
import co.ratmo.anreal.feature.chat.domain.McpRemoteDataSource
import co.ratmo.anreal.feature.chat.domain.McpServer
import co.ratmo.anreal.feature.chat.domain.McpStatus
import co.ratmo.anreal.feature.chat.domain.McpTestResult
import co.ratmo.anreal.feature.chat.domain.McpTool
import co.ratmo.anreal.feature.chat.domain.Skill
import co.ratmo.anreal.feature.chat.domain.SkillStatus
import co.ratmo.anreal.feature.chat.domain.SkillsRemoteDataSource
import co.ratmo.anreal.feature.chat.presentation.component.McpAction
import co.ratmo.anreal.feature.chat.presentation.component.McpTestState
import co.ratmo.anreal.feature.chat.presentation.component.McpViewModel
import co.ratmo.anreal.feature.chat.presentation.component.SkillsAction
import co.ratmo.anreal.feature.chat.presentation.component.SkillsViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SkillsMcpViewModelTest {

    private val dispatcher = UnconfinedTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun skills_save_blocked_when_frontmatter_mismatched() = runTest {
        val source = FakeSkillsSource()
        val viewModel = SkillsViewModel(SavedStateHandle(), source)
        runCurrent()
        viewModel.onAction(SkillsAction.OnNewSkill)
        viewModel.onAction(SkillsAction.OnEditorNameChange("release-notes"))
        viewModel.onAction(SkillsAction.OnEditorDescriptionChange("Draft notes"))
        viewModel.onAction(
            SkillsAction.OnEditorBodyChange("---\nname: other\ndescription: Draft notes\n---\nbody"),
        )
        viewModel.onAction(SkillsAction.OnSaveEditor)
        runCurrent()

        assertThat(source.created).isEqualTo(0)
        assertThat(viewModel.state.value.editor?.fieldErrors?.get("bodyMd")).isNotNull()
    }

    @Test
    fun skills_save_creates_when_valid() = runTest {
        val source = FakeSkillsSource()
        val viewModel = SkillsViewModel(SavedStateHandle(), source)
        runCurrent()
        viewModel.onAction(SkillsAction.OnNewSkill)
        viewModel.onAction(SkillsAction.OnEditorNameChange("release-notes"))
        viewModel.onAction(SkillsAction.OnEditorDescriptionChange("Draft notes"))
        viewModel.onAction(
            SkillsAction.OnEditorBodyChange(
                "---\nname: release-notes\ndescription: Draft notes\n---\n# body",
            ),
        )
        viewModel.onAction(SkillsAction.OnSaveEditor)
        runCurrent()

        assertThat(source.created).isEqualTo(1)
        assertThat(viewModel.state.value.editor).isNull()
        assertThat(viewModel.state.value.skills.single().name).isEqualTo("release-notes")
    }

    @Test
    fun skills_list_error_surfaces() = runTest {
        val source = FakeSkillsSource(
            listError = DataError.Network(
                kind = DataError.Network.Kind.SERVER_ERROR,
                statusCode = 500,
            ),
        )
        val viewModel = SkillsViewModel(SavedStateHandle(), source)
        runCurrent()

        assertThat(viewModel.state.value.loading).isFalse()
        assertThat(viewModel.state.value.error).isNotNull()
    }

    @Test
    fun mcp_save_blocked_without_fresh_test() = runTest {
        val source = FakeMcpSource()
        val viewModel = McpViewModel(SavedStateHandle(), source)
        runCurrent()
        viewModel.onAction(McpAction.OnNewServer)
        viewModel.onAction(McpAction.OnEditorNameChange("docs"))
        viewModel.onAction(McpAction.OnEditorUrlChange("https://mcp.example.com/mcp"))
        viewModel.onAction(McpAction.OnSaveEditor)
        runCurrent()

        assertThat(source.created).isEqualTo(0)
        assertThat(viewModel.state.value.editor?.editorError).isNotNull()
    }

    @Test
    fun mcp_save_blocked_when_url_changed_after_test() = runTest {
        val source = FakeMcpSource()
        val viewModel = McpViewModel(SavedStateHandle(), source)
        runCurrent()
        viewModel.onAction(McpAction.OnNewServer)
        viewModel.onAction(McpAction.OnEditorNameChange("docs"))
        viewModel.onAction(McpAction.OnEditorUrlChange("https://mcp.example.com/mcp"))
        viewModel.onAction(McpAction.OnTestConnection)
        runCurrent()
        viewModel.onAction(McpAction.OnEditorUrlChange("https://other.example.com/mcp"))
        viewModel.onAction(McpAction.OnSaveEditor)
        runCurrent()

        assertThat(source.created).isEqualTo(0)
        assertThat(viewModel.state.value.editor?.testState is McpTestState.Tested).isTrue()
    }

    @Test
    fun mcp_save_succeeds_after_fresh_test_with_tool() = runTest {
        val source = FakeMcpSource()
        val viewModel = McpViewModel(SavedStateHandle(), source)
        runCurrent()
        viewModel.onAction(McpAction.OnNewServer)
        viewModel.onAction(McpAction.OnEditorNameChange("docs"))
        viewModel.onAction(McpAction.OnEditorUrlChange("https://mcp.example.com/mcp"))
        viewModel.onAction(McpAction.OnTestConnection)
        runCurrent()
        viewModel.onAction(McpAction.OnSaveEditor)
        runCurrent()

        assertThat(source.created).isEqualTo(1)
        assertThat(viewModel.state.value.editor).isNull()
    }

    private class FakeSkillsSource(
        private val listError: DataError.Network? = null,
    ) : SkillsRemoteDataSource {
        var created: Int = 0
        private val stored = mutableListOf<Skill>()

        override suspend fun listSkills(): Result<List<Skill>, DataError.Network> =
            if (listError != null) {
                Result.Error(listError)
            } else {
                Result.Success(stored.toList())
            }

        override suspend fun getSkill(id: String): Result<Skill, DataError.Network> {
            val skill = stored.firstOrNull { it.id == id }
            return if (skill != null) {
                Result.Success(skill)
            } else {
                Result.Error(DataError.Network(kind = DataError.Network.Kind.NOT_FOUND, statusCode = 404))
            }
        }

        override suspend fun createSkill(
            name: String,
            description: String,
            bodyMd: String,
        ): Result<Skill, DataError.Network> {
            created += 1
            val skill = Skill(id = "s$created", name = name, description = description, bodyMd = bodyMd)
            stored += skill
            return Result.Success(skill)
        }

        override suspend fun updateSkill(
            id: String,
            name: String,
            description: String,
            bodyMd: String,
            markReviewed: Boolean,
        ): Result<Skill, DataError.Network> {
            val skill = Skill(id = id, name = name, description = description, bodyMd = bodyMd)
            stored.removeAll { it.id == id }
            stored += skill
            return Result.Success(skill)
        }

        override suspend fun setSkillEnabled(id: String, isEnabled: Boolean): Result<Skill, DataError.Network> {
            val skill = stored.first { it.id == id }.copy(isEnabled = isEnabled)
            stored.removeAll { it.id == id }
            stored += skill
            return Result.Success(skill)
        }

        override suspend fun deleteSkill(id: String): Result<Unit, DataError.Network> {
            stored.removeAll { it.id == id }
            return Result.Success(Unit)
        }
    }

    private class FakeMcpSource : McpRemoteDataSource {
        var created: Int = 0
        private val stored = mutableListOf<McpServer>()

        override suspend fun listServers(): Result<List<McpServer>, DataError.Network> =
            Result.Success(stored.toList())

        override suspend fun createServer(
            name: String,
            url: String,
            authType: McpAuthType,
            token: String?,
            headers: List<Pair<String, String>>,
        ): Result<McpServer, DataError.Network> {
            created += 1
            val server = McpServer(id = "m$created", name = name, url = url, authType = authType)
            stored += server
            return Result.Success(server)
        }

        override suspend fun updateServer(
            id: String,
            name: String,
            url: String,
            authType: McpAuthType,
            token: String?,
            headers: List<Pair<String, String>>?,
        ): Result<McpServer, DataError.Network> {
            val server = McpServer(id = id, name = name, url = url, authType = authType)
            stored.removeAll { it.id == id }
            stored += server
            return Result.Success(server)
        }

        override suspend fun setServerEnabled(
            id: String,
            isEnabled: Boolean,
        ): Result<McpServer, DataError.Network> {
            val server = stored.first { it.id == id }.copy(isEnabled = isEnabled)
            stored.removeAll { it.id == id }
            stored += server
            return Result.Success(server)
        }

        override suspend fun deleteServer(id: String): Result<Unit, DataError.Network> {
            stored.removeAll { it.id == id }
            return Result.Success(Unit)
        }

        override suspend fun testConnection(
            url: String,
            authType: McpAuthType,
            token: String?,
            headers: List<Pair<String, String>>,
            serverId: String?,
        ): Result<McpTestResult, DataError.Network> = Result.Success(
            McpTestResult(
                ok = true,
                tools = listOf(McpTool("search_docs", "Search the docs")),
            ),
        )
    }
}
