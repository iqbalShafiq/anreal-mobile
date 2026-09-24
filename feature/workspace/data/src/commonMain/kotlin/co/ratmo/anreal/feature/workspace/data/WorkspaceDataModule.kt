package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.workspace.domain.WorkspaceBaseUrlProvider
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import org.koin.dsl.module

val workspaceDataModule = module {
    single<WorkspaceRepository> {
        if (get<AppConfig>().environment.stubApi) StubWorkspaceRepository()
        else KtorWorkspaceRepository(get())
    }
    single<WorkspaceBaseUrlProvider> { AppConfigWorkspaceBaseUrlProvider(get()) }
}

private class AppConfigWorkspaceBaseUrlProvider(
    private val appConfig: AppConfig,
) : WorkspaceBaseUrlProvider {
    override fun baseUrl(): String = appConfig.baseUrl
}
