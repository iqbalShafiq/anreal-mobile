package co.ratmo.anreal.feature.chat.presentation.account

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.account.component.AccountSettingsLayout
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

@Composable
fun AccountRoot(
    account: AccountUi,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = koinViewModel { parametersOf(account) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AccountEvent.NavigateBack -> onBack()
            AccountEvent.SignOut -> onSignOut()
        }
    }
    AccountScreen(state = state, onAction = viewModel::onAction)
}

@Composable
fun AccountScreen(
    state: AccountState,
    onAction: (AccountAction) -> Unit,
) {
    AccountSettingsLayout(
        state = state,
        onBack = { onAction(AccountAction.OnBack) },
        onSelectSection = { onAction(AccountAction.OnSelectSection(it)) },
        onSignOut = { onAction(AccountAction.OnSignOut) },
    )
}

@AnrealPreviews
@Composable
private fun AccountPopulatedPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(name = "shafiq", email = "shafiq@testing.com"),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountSigningOutPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                isSigningOut = true,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountUsageEmptyPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                section = AccountSection.Usage,
            ),
            onAction = {},
        )
    }
}

@AnrealPreviews
@Composable
private fun AccountPersonalizationEmptyPreview() {
    AnrealPreview {
        AccountScreen(
            state = AccountState(
                name = "shafiq",
                email = "shafiq@testing.com",
                section = AccountSection.Personalization,
            ),
            onAction = {},
        )
    }
}
