package co.ratmo.anreal.feature.chat.presentation

import co.ratmo.anreal.feature.chat.presentation.account.AccountViewModel
import co.ratmo.anreal.feature.chat.presentation.component.FileKitSiteZipSaver
import co.ratmo.anreal.feature.chat.presentation.component.McpViewModel
import co.ratmo.anreal.feature.chat.presentation.component.SiteZipSaver
import co.ratmo.anreal.feature.chat.presentation.component.SkillsViewModel
import kotlinx.coroutines.flow.Flow
import org.koin.core.module.dsl.viewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatPresentationModule = module {
    single<SiteZipSaver> { FileKitSiteZipSaver() }
    viewModelOf(::ChatViewModel)
    viewModelOf(::SkillsViewModel)
    viewModelOf(::McpViewModel)
    viewModel { (isAuthenticated: Flow<Boolean>) ->
        SharedChatViewModel(get(), get(), isAuthenticated)
    }
    viewModel { (account: AccountUi) -> AccountViewModel(account, get(), get()) }
}
