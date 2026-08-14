package co.ratmo.anreal.core.data.di

import co.ratmo.anreal.core.data.auth.InMemorySessionTokenStore
import co.ratmo.anreal.core.data.auth.SessionTokenStore
import org.koin.dsl.module

val coreDataModule = module {
    single<SessionTokenStore> { InMemorySessionTokenStore() }
}
