package co.ratmo.anreal.core.data.preferences

import co.ratmo.anreal.core.data.auth.createSessionDataStore
import co.ratmo.anreal.core.domain.model.AppPreferencesRepository
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val appPreferencesModule: Module = module {
    single<AppPreferencesRepository> {
        val path = androidContext().applicationContext.filesDir
            .resolve("anreal_preferences.preferences_pb")
            .absolutePath
        DataStoreAppPreferencesRepository(createSessionDataStore { path })
    }
}
