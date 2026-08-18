package co.ratmo.anreal.feature.workspace.data

import co.ratmo.anreal.core.data.AppConfig
import co.ratmo.anreal.feature.workspace.domain.WorkspaceRepository
import org.koin.dsl.module

val workspaceDataModule = module {
    single<WorkspaceRepository> {
        if (get<AppConfig>().environment.stubApi) StubWorkspaceRepository()
        else KtorWorkspaceRepository(get())
    }
}
