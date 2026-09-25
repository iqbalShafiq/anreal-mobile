package co.ratmo.anreal.feature.workspace.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import androidx.navigation.toRoute
import kotlinx.serialization.Serializable

@Serializable
data class WorkspaceRoute(val section: WorkspaceSection, val scopeSessionId: String? = null)

fun NavGraphBuilder.workspaceGraph(
    navController: NavController,
    onOpenProject: (projectId: String, name: String) -> Unit,
    onOpenSiteOrigin: (siteId: String, sessionId: String) -> Unit = { _, _ -> },
    onContinueSite: (siteId: String, sessionId: String) -> Unit = { _, _ -> },
) {
    composable<WorkspaceRoute> { backStackEntry ->
        val route: WorkspaceRoute = backStackEntry.toRoute()
        WorkspaceRoot(
            initialSection = route.section,
            onBack = { navController.popBackStack() },
            onOpenProject = onOpenProject,
            scopeSessionId = route.scopeSessionId,
            onOpenSiteOrigin = onOpenSiteOrigin,
            onContinueSite = onContinueSite,
        )
    }
}
