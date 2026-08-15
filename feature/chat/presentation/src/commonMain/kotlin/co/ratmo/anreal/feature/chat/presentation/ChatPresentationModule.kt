package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.feature.chat.presentation.account.AccountViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatPresentationModule = module {
    viewModelOf(::ChatViewModel)
    viewModel { (account: AccountUi) -> AccountViewModel(account) }
}
