package co.ratmo.anreal.feature.chat.presentation.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class AccountSection {
    Account,
    Usage,
    Personalization,
}

data class AccountState(
    val name: String = "",
    val email: String = "",
    val section: AccountSection = AccountSection.Account,
    val isSigningOut: Boolean = false,
)

sealed interface AccountAction {
    data class OnSelectSection(val section: AccountSection) : AccountAction
    data object OnBack : AccountAction
    data object OnSignOut : AccountAction
}

sealed interface AccountEvent {
    data object NavigateBack : AccountEvent
    data object SignOut : AccountEvent
}

class AccountViewModel(
    account: AccountUi,
) : ViewModel() {

    private val _state = MutableStateFlow(
        AccountState(name = account.name, email = account.email),
    )
    val state = _state.asStateFlow()

    private val _events = Channel<AccountEvent>()
    val events = _events.receiveAsFlow()

    fun onAction(action: AccountAction) {
        when (action) {
            is AccountAction.OnSelectSection -> _state.update { it.copy(section = action.section) }
            AccountAction.OnBack -> viewModelScope.launch {
                _events.send(AccountEvent.NavigateBack)
            }
            AccountAction.OnSignOut -> {
                if (_state.value.isSigningOut) return
                _state.update { it.copy(isSigningOut = true) }
                viewModelScope.launch { _events.send(AccountEvent.SignOut) }
            }
        }
    }
}
