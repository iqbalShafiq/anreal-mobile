package co.ratmo.anreal.feature.workspace.presentation

import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

val workspacePresentationModule = module {
    viewModel { (section: WorkspaceSection) -> WorkspaceViewModel(section, get()) }
}
