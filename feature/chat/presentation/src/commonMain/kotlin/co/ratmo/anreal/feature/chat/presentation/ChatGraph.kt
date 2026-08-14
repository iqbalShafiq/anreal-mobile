package co.ratmo.anreal.feature.chat.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import kotlinx.serialization.Serializable

@Serializable
data class ChatRoute(val sessionId: String? = null)

fun NavGraphBuilder.chatGraph(
    account: AccountUi = AccountUi(),
    onSignOut: () -> Unit = {},
) {
    composable<ChatRoute> {
        ChatRoot(account = account, onSignOut = onSignOut)
    }
}
