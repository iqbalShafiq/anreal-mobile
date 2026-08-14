package co.ratmo.anreal.feature.auth.presentation

import co.ratmo.anreal.feature.auth.presentation.login.LoginViewModel
import co.ratmo.anreal.feature.auth.presentation.register.RegisterViewModel
import org.koin.core.module.dsl.viewModelOf
import org.koin.dsl.module

val authPresentationModule = module {
    viewModelOf(::LoginViewModel)
    viewModelOf(::RegisterViewModel)
}
