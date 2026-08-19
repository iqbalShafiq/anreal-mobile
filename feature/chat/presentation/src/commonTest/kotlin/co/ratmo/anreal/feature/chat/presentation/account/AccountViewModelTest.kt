package co.ratmo.anreal.feature.chat.presentation.account

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import co.ratmo.anreal.core.domain.util.EmptyResult
import co.ratmo.anreal.core.domain.util.Result
import co.ratmo.anreal.feature.chat.domain.AccountSettingsDataSource
import co.ratmo.anreal.feature.chat.domain.ChatError
import co.ratmo.anreal.feature.chat.domain.PersonalizationSettings
import co.ratmo.anreal.feature.chat.domain.StorageUsage
import co.ratmo.anreal.feature.chat.domain.TokenComposition
import co.ratmo.anreal.feature.chat.domain.TokenUsage
import co.ratmo.anreal.feature.chat.domain.UsageSummary
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AccountViewModelTest {

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
    fun seeds_account_from_constructor() {
        val viewModel = createViewModel()
        assertThat(viewModel.state.value.name).isEqualTo("shafiq")
        assertThat(viewModel.state.value.email).isEqualTo("shafiq@testing.com")
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Account)
        assertThat(viewModel.state.value.isSigningOut).isFalse()
    }

    @Test
    fun selecting_section_updates_state() {
        val viewModel = createViewModel()
        viewModel.onAction(AccountAction.OnSelectSection(AccountSection.Usage))
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Usage)
        viewModel.onAction(AccountAction.OnSelectSection(AccountSection.Personalization))
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Personalization)
    }

    @Test
    fun back_emits_navigate_back() = runTest {
        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.onAction(AccountAction.OnBack)
            assertThat(awaitItem()).isEqualTo(AccountEvent.NavigateBack)
        }
    }

    @Test
    fun request_sign_out_opens_confirmation_dialog() {
        val viewModel = createViewModel()
        viewModel.onAction(AccountAction.OnRequestSignOut)
        assertThat(viewModel.state.value.showSignOutDialog).isTrue()
        assertThat(viewModel.state.value.isSigningOut).isFalse()
    }

    @Test
    fun dismiss_sign_out_closes_confirmation_dialog() {
        val viewModel = createViewModel()
        viewModel.onAction(AccountAction.OnRequestSignOut)
        viewModel.onAction(AccountAction.OnDismissSignOut)
        assertThat(viewModel.state.value.showSignOutDialog).isFalse()
        assertThat(viewModel.state.value.isSigningOut).isFalse()
    }

    @Test
    fun confirming_sign_out_closes_dialog_emits_once_and_marks_in_flight() = runTest {
        val viewModel = createViewModel()
        viewModel.events.test {
            viewModel.onAction(AccountAction.OnRequestSignOut)
            assertThat(viewModel.state.value.showSignOutDialog).isTrue()
            viewModel.onAction(AccountAction.OnSignOut)
            assertThat(viewModel.state.value.showSignOutDialog).isFalse()
            assertThat(viewModel.state.value.isSigningOut).isTrue()
            assertThat(awaitItem()).isEqualTo(AccountEvent.SignOut)
            viewModel.onAction(AccountAction.OnSignOut)
            expectNoEvents()
        }
    }

    @Test
    fun selecting_usage_loads_and_maps_server_summary() {
        val viewModel = createViewModel()

        viewModel.onAction(AccountAction.OnSelectSection(AccountSection.Usage))

        assertThat(viewModel.state.value.usage?.requestCount).isEqualTo("2")
        assertThat(viewModel.state.value.usage?.totalTokens).isEqualTo("1,200")
        assertThat(viewModel.state.value.isUsageLoading).isFalse()
    }

    @Test
    fun appearance_actions_update_persisted_preferences() {
        val preferences = FakeAppPreferencesRepository()
        val viewModel = createViewModel(preferencesRepository = preferences)

        viewModel.onAction(AccountAction.OnThemeModeChange(AppThemeMode.Dark))
        viewModel.onAction(AccountAction.OnToggleReduceMotion)

        assertThat(viewModel.state.value.themeMode).isEqualTo(AppThemeMode.Dark)
        assertThat(viewModel.state.value.reduceMotion).isTrue()
    }

    private fun createViewModel(
        dataSource: AccountSettingsDataSource = FakeAccountSettingsDataSource(),
        preferencesRepository: AppPreferencesRepository = FakeAppPreferencesRepository(),
    ): AccountViewModel = AccountViewModel(
        account = AccountUi(name = "shafiq", email = "shafiq@testing.com"),
        settingsDataSource = dataSource,
        preferencesRepository = preferencesRepository,
    )
}

private class FakeAppPreferencesRepository : AppPreferencesRepository {
    private val values = MutableStateFlow(AppPreferences())
    override val preferences: Flow<AppPreferences> = values
    override suspend fun setThemeMode(mode: AppThemeMode) { values.value = values.value.copy(themeMode = mode) }
    override suspend fun setDynamicColor(enabled: Boolean) { values.value = values.value.copy(dynamicColor = enabled) }
    override suspend fun setReduceMotion(enabled: Boolean) { values.value = values.value.copy(reduceMotion = enabled) }
    override suspend fun setReduceTransparency(enabled: Boolean) {
        values.value = values.value.copy(reduceTransparency = enabled)
    }
    override suspend fun setChatModel(modelId: String?) { values.value = values.value.copy(chatModelId = modelId) }
    override suspend fun setChatReasoningEffort(effort: String?) {
        values.value = values.value.copy(chatReasoningEffort = effort)
    }
}

private class FakeAccountSettingsDataSource : AccountSettingsDataSource {
    override suspend fun checkHealth(): Result<Boolean, ChatError> = Result.Success(true)

    override suspend fun loadUsage(): Result<UsageSummary, ChatError> = Result.Success(
        UsageSummary(
            storage = StorageUsage(usedBytes = 1_024, maxBytes = 10_240, remainingBytes = 9_216),
            tokens = TokenUsage(
                requestCount = 2,
                inputTokens = 1_000,
                outputTokens = 200,
                totalTokens = 1_200,
                cachedInputTokens = 100,
                cacheCreationInputTokens = 0,
                composition = TokenComposition(900, 100, 200),
            ),
            byModel = emptyList(),
            byReasoningEffort = emptyList(),
        ),
    )

    override suspend fun loadPersonalization(): Result<PersonalizationSettings, ChatError> =
        Result.Success(PersonalizationSettings(user = null, projects = emptyList()))

    override suspend fun resetUserPersonalization(): EmptyResult<ChatError> = Result.Success(Unit)

    override suspend fun resetProjectPersonalization(projectId: String): EmptyResult<ChatError> =
        Result.Success(Unit)
}
