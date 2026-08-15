package co.ratmo.anreal.feature.chat.presentation

import androidx.navigation.NavController
import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.ratmo.anreal.feature.chat.presentation.account.AccountRoot
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoute(val sessionId: String? = null)

@Serializable
data object AccountRoute

fun NavGraphBuilder.chatGraph(
    navController: NavController,
    account: AccountUi = AccountUi(),
    onSignOut: () -> Unit = {},
) {
    composable<ChatRoute> {
        ChatRoot(
            account = account,
            onNavigateAccount = { navController.navigate(AccountRoute) },
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
