package co.ratmo.anreal.feature.auth.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.ratmo.anreal.feature.auth.presentation.boarding.BoardingRoot
import co.ratmo.anreal.feature.auth.presentation.login.LoginRoot
import co.ratmo.anreal.feature.auth.presentation.register.RegisterRoot
import kotlinx.serialization.Serializable

@Serializable
data object BoardingRoute

@Serializable
data class LoginRoute(val email: String = "")

@Serializable
data class RegisterRoute(val email: String = "")

fun NavGraphBuilder.authGraph(
    onNavigateHome: () -> Unit,
    onNavigateRegister: (String) -> Unit,
    onNavigateLogin: (String) -> Unit,
    onNavigateBack: () -> Unit,
) {
    composable<BoardingRoute> {
        BoardingRoot(
            onNavigateRegister = onNavigateRegister,
            onNavigateLogin = onNavigateLogin,
        )
    }
    composable<LoginRoute> {
        LoginRoot(
            onNavigateHome = onNavigateHome,
            onNavigateRegister = onNavigateRegister,
            onNavigateBack = onNavigateBack,
        )
    }
    composable<RegisterRoute> {
        RegisterRoot(
            onNavigateHome = onNavigateHome,
            onNavigateLogin = onNavigateLogin,
            onNavigateBack = onNavigateBack,
        )
    }
}
