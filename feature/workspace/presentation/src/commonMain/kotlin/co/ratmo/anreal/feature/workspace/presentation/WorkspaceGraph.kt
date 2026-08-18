package co.ratmo.anreal.feature.workspace.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceRoute(val section: WorkspaceSection)

fun NavGraphBuilder.workspaceGraph(
    navController: NavController,
    onOpenProject: (String) -> Unit,
) {
    composable<WorkspaceRoute> { backStackEntry ->
        val route = backStackEntry.toRoute<WorkspaceRoute>()
        WorkspaceRoot(
            initialSection = route.section,
            onBack = { navController.popBackStack() },
            onOpenProject = onOpenProject,
        )
    }
}
