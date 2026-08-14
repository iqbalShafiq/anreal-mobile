package co.ratmo.anreal

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import co.ratmo.anreal.feature.auth.presentation.AppViewModel
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import co.ratmo.anreal.feature.auth.presentation.authGraph
import co.ratmo.anreal.feature.chat.presentation.ChatRoute
import co.ratmo.anreal.feature.chat.presentation.chatGraph
import org.koin.compose.viewmodel.koinViewModel

@Composable
fun App(
    viewModel: AppViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    AnrealTheme {
        when (status) {
            SessionStatus.Checking -> Box(modifier = Modifier.fillMaxSize())
            SessionStatus.SignedIn,
            SessionStatus.SignedOut,
            -> AuthenticatedHost(status = status)
        }
    }
}

@Composable
private fun AuthenticatedHost(status: SessionStatus) {
    val navController = rememberNavController()
    var hadSession by remember { mutableStateOf(status is SessionStatus.SignedIn) }
    LaunchedEffect(status) {
        when (status) {
            SessionStatus.SignedIn -> hadSession = true
            SessionStatus.SignedOut -> if (hadSession) {
                navController.navigate(LoginRoute) {
                    popUpTo(0) { inclusive = true }
                }
            }
            SessionStatus.Checking -> Unit
        }
    }
    NavHost(
        navController = navController,
        startDestination = if (status is SessionStatus.SignedIn) ChatRoute() else LoginRoute,
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
