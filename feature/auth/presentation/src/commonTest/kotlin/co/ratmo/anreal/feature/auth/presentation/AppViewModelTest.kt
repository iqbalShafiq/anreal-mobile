package co.ratmo.anreal.feature.auth.presentation

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
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
        val viewModel = AppViewModel(session)
        viewModel.status.test {
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedIn)
            session.emit(SessionStatus.SignedOut)
            assertThat(awaitItem()).isEqualTo(SessionStatus.SignedOut)
        }
    }
}

private class FakeAuthSession(
    initial: SessionStatus,
) : AuthSession {
    private val values = MutableStateFlow(initial)
    override val status: Flow<SessionStatus> = values
    fun emit(status: SessionStatus) {
        values.value = status
    }
}
