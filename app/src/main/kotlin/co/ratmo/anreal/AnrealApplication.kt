package co.ratmo.anreal

import android.app.Application
import co.ratmo.anreal.core.data.di.coreDataModule
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AnrealApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AnrealApplication)
            modules(coreDataModule)
        }
    }
}
