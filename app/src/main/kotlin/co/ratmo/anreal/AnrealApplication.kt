package co.ratmo.anreal

import android.app.Application
import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.core.data.AppEnvironment
import co.ratmo.anreal.core.data.di.coreDataModule
import co.ratmo.anreal.core.database.databaseModule
import co.ratmo.anreal.feature.auth.data.authDataModule
import co.ratmo.anreal.feature.auth.presentation.authPresentationModule
import co.ratmo.anreal.feature.chat.data.chatDataModule
import co.ratmo.anreal.feature.chat.presentation.chatPresentationModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import org.koin.dsl.module

class AnrealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AnrealApplication)
            modules(
                module {
                    single {
                        AppConfig(
                            environment = AppEnvironment.parse(BuildConfig.ENVIRONMENT),
                            baseUrl = BuildConfig.BASE_URL,
                        )
                    }
                },
                coreDataModule,
                databaseModule,
                authDataModule,
                authPresentationModule,
                chatDataModule,
                chatPresentationModule,
            )
        }
    }
}
