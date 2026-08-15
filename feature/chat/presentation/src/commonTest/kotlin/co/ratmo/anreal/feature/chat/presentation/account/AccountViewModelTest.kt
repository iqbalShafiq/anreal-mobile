package co.ratmo.anreal.feature.chat.presentation.account

import app.cash.turbine.test
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
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
        val viewModel = AccountViewModel(AccountUi(name = "shafiq", email = "shafiq@testing.com"))
        assertThat(viewModel.state.value.name).isEqualTo("shafiq")
        assertThat(viewModel.state.value.email).isEqualTo("shafiq@testing.com")
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Account)
        assertThat(viewModel.state.value.isSigningOut).isFalse()
    }

    @Test
    fun selecting_section_updates_state() {
        val viewModel = AccountViewModel(AccountUi(name = "shafiq", email = "a@b.com"))
        viewModel.onAction(AccountAction.OnSelectSection(AccountSection.Usage))
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Usage)
        viewModel.onAction(AccountAction.OnSelectSection(AccountSection.Personalization))
        assertThat(viewModel.state.value.section).isEqualTo(AccountSection.Personalization)
    }

    @Test
    fun back_emits_navigate_back() = runTest {
        val viewModel = AccountViewModel(AccountUi(name = "shafiq", email = "a@b.com"))
        viewModel.events.test {
            viewModel.onAction(AccountAction.OnBack)
            assertThat(awaitItem()).isEqualTo(AccountEvent.NavigateBack)
        }
    }

    @Test
    fun sign_out_emits_once_and_marks_in_flight() = runTest {
        val viewModel = AccountViewModel(AccountUi(name = "shafiq", email = "a@b.com"))
        viewModel.events.test {
            viewModel.onAction(AccountAction.OnSignOut)
            assertThat(viewModel.state.value.isSigningOut).isTrue()
            assertThat(awaitItem()).isEqualTo(AccountEvent.SignOut)
            viewModel.onAction(AccountAction.OnSignOut)
            expectNoEvents()
        }
    }
}
