package co.ratmo.anreal

import android.app.Application
import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.core.data.di.coreDataModule
import co.ratmo.anreal.feature.auth.data.authDataModule
import co.ratmo.anreal.feature.auth.presentation.authPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AnrealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AnrealApplication)
            modules(
                module { single { AppConfig(BuildConfig.BASE_URL) } },
                coreDataModule,
                authDataModule,
                authPresentationModule,
            )
        }
    }
}
