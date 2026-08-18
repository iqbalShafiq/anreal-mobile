package co.ratmo.anreal.feature.auth.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.core.domain.model.AppPreferences
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import co.ratmo.anreal.core.domain.model.AppThemeMode
import co.ratmo.anreal.core.domain.model.User
import co.ratmo.anreal.feature.auth.domain.AuthSession
import co.ratmo.anreal.feature.auth.domain.SessionStatus
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
class AppViewModelTest {

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
    fun starts_checking_then_follows_session() = runTest {
        val session = FakeAuthSession(SessionStatus.SignedIn)
        val viewModel = AppViewModel(session, FakeAppPreferencesRepository())
        viewModel.status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedIn)
            session.emit(SessionStatus.SignedOut)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedOut)
        }
    }
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
}

private class FakeAuthSession(
    initial: SessionStatus,
) : AuthSession {
    private val values = MutableStateFlow(initial)
    private val users = MutableStateFlow<User?>(null)
    override val status: Flow<SessionStatus> = values
    override val user: Flow<User?> = users
    override suspend fun signOut() {
        users.value = null
        values.value = SessionStatus.SignedOut
    }
    fun emit(status: SessionStatus) {
        values.value = status
    }
}
