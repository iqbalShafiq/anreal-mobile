package co.ratmo.anreal.core.database

import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.dsl.module

actual val databaseModule: Module = module {
    single { getRoomDatabase(getDatabaseBuilder(androidContext())) }
    single { get<AnrealDatabase>().sessionDao() }
    single { get<AnrealDatabase>().messageDao() }
    single { get<AnrealDatabase>().queuedItemDao() }
}
