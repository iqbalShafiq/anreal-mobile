package co.ratmo.anreal.feature.chat.presentation

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.ratmo.anreal.feature.chat.presentation.account.AccountRoot
import co.ratmo.anreal.feature.chat.presentation.component.McpManagementRoot
import co.ratmo.anreal.feature.chat.presentation.component.SkillsManagementRoot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.serialization.Serializable

data class EnterProjectRequest(val projectId: String, val name: String?)

data class ForkSendRequest(val sessionId: String, val firstMessage: String)

data class OpenOriginRequest(val sessionId: String)

data class DraftPrefillRequest(val text: String)

@Serializable
data class ChatRoute(val sessionId: String? = null)

@Serializable
data class SharedChatRoute(val token: String)

@Serializable
data object AccountRoute

@Serializable
data object SkillsRoute

@Serializable
data object McpRoute

fun NavGraphBuilder.chatGraph(
    navController: NavController,
    account: AccountUi = AccountUi(),
    onSignOut: () -> Unit = {},
    onNavigateProjects: () -> Unit = {},
    onNavigateDocuments: () -> Unit = {},
    onNavigateImages: () -> Unit = {},
    onNavigateToLogin: (String) -> Unit = {},
    onNavigateToRegister: (String) -> Unit = {},
    onNavigateToSignIn: (String) -> Unit = {},
    isAuthenticated: Flow<Boolean> = flowOf(false),
    enterProjectRequest: StateFlow<EnterProjectRequest?>,
    onEnterProjectConsumed: () -> Unit,
    forkSendRequest: StateFlow<ForkSendRequest?>,
    onForkSendConsumed: () -> Unit,
    onForkSend: (ForkSendRequest) -> Unit = {},
    onArtifactFocus: (type: String, id: String, sessionId: String) -> Unit = { _, _, _ -> },
    openOriginRequest: StateFlow<OpenOriginRequest?>,
    onOpenOriginConsumed: () -> Unit,
    draftPrefillRequest: StateFlow<DraftPrefillRequest?>,
    onDraftPrefillConsumed: () -> Unit,
    onOriginInvalid: () -> Unit = {},
) {
    composable<ChatRoute> {
        val enterProject by enterProjectRequest.collectAsStateWithLifecycle(
            minActiveState = Lifecycle.State.CREATED,
        )
        val forkSend by forkSendRequest.collectAsStateWithLifecycle(
            minActiveState = Lifecycle.State.CREATED,
        )
        val openOrigin by openOriginRequest.collectAsStateWithLifecycle(
            minActiveState = Lifecycle.State.CREATED,
        )
        val draftPrefill by draftPrefillRequest.collectAsStateWithLifecycle(
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
            forkSend = forkSend,
            onForkSendConsumed = onForkSendConsumed,
            openOrigin = openOrigin,
            onOpenOriginConsumed = onOpenOriginConsumed,
            draftPrefill = draftPrefill,
            onDraftPrefillConsumed = onDraftPrefillConsumed,
            onNavigateSkills = { navController.navigate(SkillsRoute) },
            onNavigateMcp = { navController.navigate(McpRoute) },
            onArtifactFocus = onArtifactFocus,
            onOriginInvalid = onOriginInvalid,
        )
    }
    composable<AccountRoute> {
        AccountRoot(
            account = account,
            onBack = { navController.popBackStack() },
            onSignOut = onSignOut,
        )
    }
    composable<SkillsRoute> {
        SkillsManagementRoot(onDismiss = { navController.popBackStack() })
    }
    composable<McpRoute> {
        McpManagementRoot(onDismiss = { navController.popBackStack() })
    }
    composable<SharedChatRoute> {
        SharedChatRoot(
            onBack = { navController.popBackStack() },
            onNavigateToChat = { sessionId, firstMessage ->
                onForkSend(ForkSendRequest(sessionId, firstMessage))
                navController.navigate(ChatRoute(sessionId = sessionId)) {
                    popUpTo<ChatRoute> { inclusive = true }
                    launchSingleTop = true
                }
            },
            onNavigateToLogin = onNavigateToLogin,
            onNavigateToRegister = onNavigateToRegister,
            isAuthenticated = isAuthenticated,
        )
    }
}
