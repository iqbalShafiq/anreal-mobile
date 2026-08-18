package co.ratmo.anreal.feature.chat.presentation.account

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import co.ratmo.anreal.core.designsystem.preview.AnrealPreview
import co.ratmo.anreal.core.designsystem.preview.AnrealPreviews
import co.ratmo.anreal.core.presentation.ObserveAsEvents
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.account.component.AccountSettingsLayout
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf
import kotlinx.coroutines.launch

@Composable
fun AccountRoot(
    account: AccountUi,
    onBack: () -> Unit,
    onSignOut: () -> Unit,
    viewModel: AccountViewModel = koinViewModel { parametersOf(account) },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val snackbarScope = rememberCoroutineScope()
    ObserveAsEvents(viewModel.events) { event ->
        when (event) {
            AccountEvent.NavigateBack -> onBack()
            AccountEvent.SignOut -> onSignOut()
            is AccountEvent.ShowMessage -> snackbarScope.launch {
                snackbarHostState.showSnackbar(event.message.asString())
            }
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
        AccountScreen(state = state, onAction = viewModel::onAction)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.BottomCenter),
        )
    }
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
        onRetryUsage = { onAction(AccountAction.OnRetryUsage) },
        onRetryHealth = { onAction(AccountAction.OnRetryHealth) },
        onRetryPersonalization = { onAction(AccountAction.OnRetryPersonalization) },
        onRequestResetUserProfile = { onAction(AccountAction.OnRequestResetUserProfile) },
        onRequestResetProjectProfile = { id, name ->
            onAction(AccountAction.OnRequestResetProjectProfile(id, name))
        },
        onConfirmResetProfile = { onAction(AccountAction.OnConfirmResetProfile) },
        onDismissResetProfile = { onAction(AccountAction.OnDismissResetProfile) },
        onSignOut = { onAction(AccountAction.OnSignOut) },
        onThemeModeChange = { onAction(AccountAction.OnThemeModeChange(it)) },
        onToggleDynamicColor = { onAction(AccountAction.OnToggleDynamicColor) },
        onToggleReduceMotion = { onAction(AccountAction.OnToggleReduceMotion) },
        onToggleReduceTransparency = { onAction(AccountAction.OnToggleReduceTransparency) },
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
