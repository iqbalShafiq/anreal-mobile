package co.ratmo.anreal.feature.chat.presentation

import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val chatPresentationModule = module {
    viewModelOf(::ChatViewModel)
}
