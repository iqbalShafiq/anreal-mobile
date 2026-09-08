package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.feature.chat.presentation.account.AccountViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatPresentationModule = module {
    viewModelOf(::ChatViewModel)
    viewModel { (isAuthenticated: Flow<Boolean>) ->
        SharedChatViewModel(get(), get(), isAuthenticated)
    }
    viewModel { (account: AccountUi) -> AccountViewModel(account, get(), get()) }
}
