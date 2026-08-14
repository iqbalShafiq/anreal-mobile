package co.ratmo.anreal.core.data.auth

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val sessionTokenModule: Module = module {
    single<TokenCipher> { TinkTokenCipher(androidContext()) }
    single<SessionTokenStore> {
        val path = androidContext().applicationContext.filesDir
            .resolve("anreal_session.preferences_pb")
            .absolutePath
        DataStoreSessionTokenStore(
            dataStore = createSessionDataStore { path },
            cipher = get(),
        )
    }
}
