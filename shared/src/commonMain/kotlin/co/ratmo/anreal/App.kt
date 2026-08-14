package co.ratmo.anreal

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import co.ratmo.anreal.feature.auth.presentation.authGraph
import co.ratmo.anreal.feature.chat.presentation.ChatRoute
import co.ratmo.anreal.feature.chat.presentation.chatGraph

@Composable
fun App() {
    AnrealTheme {
        val navController = rememberNavController()
        NavHost(
            navController = navController,
            startDestination = LoginRoute,
        ) {
            authGraph(
                onNavigateHome = {
                    navController.navigate(ChatRoute()) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateRegister = { navController.navigate(RegisterRoute) },
                onNavigateLogin = { navController.popBackStack() },
            )
            chatGraph()
        }
    }
}
