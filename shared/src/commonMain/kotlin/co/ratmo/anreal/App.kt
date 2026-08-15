package co.ratmo.anreal

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.core.snap
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import co.ratmo.anreal.core.designsystem.component.AnrealSplash
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.core.designsystem.theme.LocalAnrealReduceMotion
import co.ratmo.anreal.core.presentation.AnrealCopy
import co.ratmo.anreal.core.presentation.UiText
import co.ratmo.anreal.core.presentation.asString
import co.ratmo.anreal.feature.auth.domain.SessionStatus
import co.ratmo.anreal.feature.auth.presentation.AppViewModel
import co.ratmo.anreal.feature.auth.presentation.BoardingRoute
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import co.ratmo.anreal.feature.auth.presentation.authGraph
import co.ratmo.anreal.feature.chat.presentation.AccountUi
import co.ratmo.anreal.feature.chat.presentation.ChatRoute
import co.ratmo.anreal.feature.chat.presentation.chatGraph
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.TimeSource

@Composable
fun App(
    buildInfo: AppBuildInfo = AppBuildInfo(versionName = "1.0"),
    viewModel: AppViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    AnrealTheme {
        AnrealAtmosphere {
            val showSplash = rememberSplashVisible(status)
            AnimatedContent(
                targetState = showSplash,
                transitionSpec = {
                    fadeIn(animationSpec = AnrealMotion.fadeSpec()) togetherWith
                        fadeOut(animationSpec = AnrealMotion.fadeSpec())
                },
                label = "splash",
            ) { splash ->
                if (splash) {
                    AnrealSplash(
                        versionLabel = UiText.StringResource(
                            AnrealCopy.SPLASH_VERSION,
                            listOf(buildInfo.versionName),
                        ).asString(),
                        credit = AnrealCopy.get(AnrealCopy.SPLASH_CREDIT),
                        markDescription = AnrealCopy.get(AnrealCopy.CD_APP_MARK),
                    )
                } else {
                    AuthenticatedHost(status = status, viewModel = viewModel)
                }
            }
        }
    }
}

@Composable
private fun rememberSplashVisible(status: SessionStatus): Boolean {
    val reduceMotion = LocalAnrealReduceMotion.current
    val startedAt = remember { TimeSource.Monotonic.markNow() }
    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(status, reduceMotion) {
        if (status is SessionStatus.Checking) {
            visible = true
            return@LaunchedEffect
        }
        val minHold = if (reduceMotion) {
            AnrealMotion.durationFast
        } else {
            AnrealMotion.durationSplash
        }
        val remaining = minHold - startedAt.elapsedNow()
        if (remaining.isPositive()) delay(remaining)
        visible = false
    }
    return visible
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
                navController.navigate(BoardingRoute) {
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
            startDestination = if (status is SessionStatus.SignedIn) ChatRoute() else BoardingRoute,
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
                        popUpTo(BoardingRoute) { inclusive = true }
                    }
                },
                onNavigateRegister = { email ->
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(RegisterRoute(email)) {
                        launchSingleTop = true
                        popUpTo(BoardingRoute) { inclusive = false }
                    }
                },
                onNavigateLogin = { email ->
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(LoginRoute(email)) {
                        launchSingleTop = true
                        popUpTo(BoardingRoute) { inclusive = false }
                    }
                },
                onNavigateBack = {
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
