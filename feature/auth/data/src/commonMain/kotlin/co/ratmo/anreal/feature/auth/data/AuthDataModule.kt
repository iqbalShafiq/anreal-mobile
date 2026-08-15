package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.auth.domain.AuthRemoteDataSource
import co.ratmo.anreal.feature.auth.domain.AuthSession
import org.koin.dsl.module

val authDataModule = module {
    single<AuthRemoteDataSource> {
        if (get<AppConfig>().environment.stubApi) {
            StubAuthRemoteDataSource(get())
        } else {
            KtorAuthDataSource(get(), get())
        }
    }
    single<AuthSession> { StoredAuthSession(get(), get()) }
}
