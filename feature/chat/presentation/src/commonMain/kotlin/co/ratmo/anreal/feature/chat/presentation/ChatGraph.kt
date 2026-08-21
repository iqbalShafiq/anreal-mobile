package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.ratmo.anreal.feature.chat.presentation.account.AccountRoot
import kotlinx.coroutines.flow.StateFlow
import kotlinx.serialization.Serializable

data class EnterProjectRequest(val projectId: String, val name: String?)

@Serializable
data class ChatRoute(val sessionId: String? = null)

@Serializable
data object AccountRoute

fun NavGraphBuilder.chatGraph(
    navController: NavController,
    account: AccountUi = AccountUi(),
    onSignOut: () -> Unit = {},
    onNavigateProjects: () -> Unit = {},
    onNavigateDocuments: () -> Unit = {},
    onNavigateImages: () -> Unit = {},
    enterProjectRequest: StateFlow<EnterProjectRequest?>,
    onEnterProjectConsumed: () -> Unit,
) {
    composable<ChatRoute> {
        val enterProject by enterProjectRequest.collectAsStateWithLifecycle(
            minActiveState = Lifecycle.State.CREATED,
        )
        ChatRoot(
            account = account,
            onNavigateAccount = { navController.navigate(AccountRoute) },
            onNavigateProjects = onNavigateProjects,
            onNavigateDocuments = onNavigateDocuments,
            onNavigateImages = onNavigateImages,
            enterProjectId = enterProject?.projectId,
            enterProjectName = enterProject?.name,
            onEnterProjectConsumed = onEnterProjectConsumed,
        )
    }
    composable<AccountRoute> {
        AccountRoot(
            account = account,
            onBack = { navController.popBackStack() },
            onSignOut = onSignOut,
        )
    }
}
