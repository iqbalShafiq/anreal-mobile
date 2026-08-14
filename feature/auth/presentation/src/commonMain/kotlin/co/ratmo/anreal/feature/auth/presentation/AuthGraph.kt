package co.ratmo.anreal.feature.auth.presentation

import androidx.navigation.NavGraphBuilder
import androidx.navigation.compose.composable
import co.ratmo.anreal.feature.auth.presentation.login.LoginRoot
import co.ratmo.anreal.feature.auth.presentation.register.RegisterRoot
import kotlinx.serialization.Serializable

@Serializable
data object LoginRoute

@Serializable
data object RegisterRoute

fun NavGraphBuilder.authGraph(
    onNavigateHome: () -> Unit,
    onNavigateRegister: () -> Unit,
    onNavigateLogin: () -> Unit,
) {
    composable<LoginRoute> {
        LoginRoot(
            onNavigateHome = onNavigateHome,
            onNavigateRegister = onNavigateRegister,
        )
    }
    composable<RegisterRoute> {
        RegisterRoot(
            onNavigateHome = onNavigateHome,
            onNavigateLogin = onNavigateLogin,
        )
    }
}
