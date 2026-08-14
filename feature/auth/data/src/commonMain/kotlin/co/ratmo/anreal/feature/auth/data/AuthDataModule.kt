package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import org.koin.dsl.module

val authDataModule = module {
    single<AuthRemoteDataSource> { KtorAuthDataSource(get(), get()) }
}
