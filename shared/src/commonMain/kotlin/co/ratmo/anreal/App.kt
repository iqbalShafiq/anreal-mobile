package co.ratmo.anreal

import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import co.ratmo.anreal.feature.auth.presentation.AppViewModel
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import co.ratmo.anreal.feature.auth.presentation.authGraph
import co.ratmo.anreal.feature.chat.presentation.AccountUi
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
            SessionStatus.Checking -> AnrealAtmosphere { Box(modifier = Modifier.fillMaxSize()) }
            SessionStatus.SignedIn,
            SessionStatus.SignedOut,
            -> AuthenticatedHost(status = status, viewModel = viewModel)
        }
    }
}

@Composable
private fun AuthenticatedHost(
    status: SessionStatus,
    viewModel: AppViewModel,
) {
    val navController = rememberNavController()
    val user by viewModel.user.collectAsStateWithLifecycle()
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
    val reduceMotion = LocalAnrealReduceMotion.current
    val focusManager = LocalFocusManager.current
    val keyboard = LocalSoftwareKeyboardController.current
    AnrealAtmosphere {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = if (status is SessionStatus.SignedIn) ChatRoute() else LoginRoute,
            enterTransition = { anrealEnter(reduceMotion) },
            exitTransition = { anrealExit(reduceMotion) },
            popEnterTransition = { anrealEnter(reduceMotion) },
            popExitTransition = { anrealExit(reduceMotion) },
            sizeTransform = { SizeTransform(clip = true) { _, _ -> snap() } },
        ) {
            authGraph(
                onNavigateHome = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(ChatRoute()) {
                        popUpTo(LoginRoute) { inclusive = true }
                    }
                },
                onNavigateRegister = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(RegisterRoute)
                },
                onNavigateLogin = {
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.popBackStack()
                },
            )
            chatGraph(
                navController = navController,
                account = AccountUi(name = user?.name.orEmpty(), email = user?.email.orEmpty()),
                onSignOut = viewModel::signOut,
            )
        }
    }
}
