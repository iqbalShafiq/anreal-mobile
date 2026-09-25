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
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import co.ratmo.anreal.core.designsystem.component.AnrealAtmosphere
import co.ratmo.anreal.core.designsystem.component.AnrealSplash
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.core.designsystem.theme.AnrealTheme
import co.ratmo.anreal.core.designsystem.theme.ThemeMode
import co.ratmo.anreal.core.designsystem.theme.ThemeSettings
import co.ratmo.anreal.core.domain.model.AppThemeMode
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
import co.ratmo.anreal.feature.chat.presentation.DraftPrefillRequest
import co.ratmo.anreal.feature.chat.presentation.EnterProjectRequest
import co.ratmo.anreal.feature.chat.presentation.ForkSendRequest
import co.ratmo.anreal.feature.chat.presentation.OpenOriginRequest
import co.ratmo.anreal.feature.chat.presentation.SharedChatRoute
import co.ratmo.anreal.feature.chat.presentation.chatGraph
import kotlinx.coroutines.flow.MutableStateFlow
import co.ratmo.anreal.feature.workspace.presentation.WorkspaceRoute
import co.ratmo.anreal.feature.workspace.presentation.WorkspaceSection
import co.ratmo.anreal.feature.workspace.presentation.workspaceGraph
import kotlinx.coroutines.delay
import org.koin.compose.viewmodel.koinViewModel
import kotlin.time.TimeSource

@Composable
fun App(
    buildInfo: AppBuildInfo = AppBuildInfo(versionName = "1.0"),
    sharedToken: String? = null,
    viewModel: AppViewModel = koinViewModel(),
) {
    val status by viewModel.status.collectAsStateWithLifecycle()
    val preferences by viewModel.preferences.collectAsStateWithLifecycle()
    AnrealTheme(
        settings = ThemeSettings(
            mode = when (preferences.themeMode) {
                AppThemeMode.System -> ThemeMode.System
                AppThemeMode.Light -> ThemeMode.Light
                AppThemeMode.Dark -> ThemeMode.Dark
            },
            dynamicColor = preferences.dynamicColor,
        ),
        reduceMotion = preferences.reduceMotion,
        reduceTransparency = preferences.reduceTransparency,
    ) {
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
                    AuthenticatedHost(status = status, viewModel = viewModel, sharedToken = sharedToken)
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
    sharedToken: String? = null,
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
    val enterProjectRequest = remember { MutableStateFlow<EnterProjectRequest?>(null) }
    val forkSendRequest = remember { MutableStateFlow<ForkSendRequest?>(null) }
    val openOriginRequest = remember { MutableStateFlow<OpenOriginRequest?>(null) }
    val draftPrefillRequest = remember { MutableStateFlow<DraftPrefillRequest?>(null) }
    var pendingSharedToken by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(sharedToken) {
        if (!sharedToken.isNullOrBlank()) {
            navController.navigate(SharedChatRoute(sharedToken))
        }
    }
    LaunchedEffect(status) {
        val token = pendingSharedToken
        if (status is SessionStatus.SignedIn && token != null) {
            pendingSharedToken = null
            navController.navigate(SharedChatRoute(token)) {
                popUpTo(0) { inclusive = true }
            }
        }
    }
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
                    navController.replaceCurrentWith(ChatRoute())
                },
                onNavigateRegister = { email ->
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.replaceCurrentWith(RegisterRoute(email))
                },
                onNavigateLogin = { email ->
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.replaceCurrentWith(LoginRoute(email))
                },
            )
            chatGraph(
                navController = navController,
                account = AccountUi(name = user?.name.orEmpty(), email = user?.email.orEmpty()),
                onSignOut = viewModel::signOut,
                onNavigateProjects = { navController.navigate(WorkspaceRoute(WorkspaceSection.Projects)) },
                onNavigateDocuments = { navController.navigate(WorkspaceRoute(WorkspaceSection.Documents)) },
                onNavigateImages = { navController.navigate(WorkspaceRoute(WorkspaceSection.Images)) },
                onNavigateToLogin = { token ->
                    pendingSharedToken = token
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(LoginRoute(""))
                },
                onNavigateToRegister = { token ->
                    pendingSharedToken = token
                    keyboard?.hide()
                    focusManager.clearFocus()
                    navController.navigate(RegisterRoute(""))
                },
                onNavigateToSignIn = { navController.navigate(LoginRoute("")) },
                isAuthenticated = viewModel.isSignedIn,
                enterProjectRequest = enterProjectRequest,
                onEnterProjectConsumed = { enterProjectRequest.value = null },
                forkSendRequest = forkSendRequest,
                onForkSendConsumed = { forkSendRequest.value = null },
                onForkSend = { forkSendRequest.value = it },
                onArtifactFocus = { type, id, sessionId ->
                    when (type) {
                        "site" -> navController.navigate(WorkspaceRoute(WorkspaceSection.Sites, scopeSessionId = sessionId))
                        "task" -> navController.navigate(WorkspaceRoute(WorkspaceSection.Tasks, scopeSessionId = sessionId))
                        "schedule" -> navController.navigate(WorkspaceRoute(WorkspaceSection.Schedules, scopeSessionId = sessionId))
                        "session" -> navController.navigate(ChatRoute(sessionId = id))
                        else -> navController.navigate(WorkspaceRoute(WorkspaceSection.Artifacts, scopeSessionId = sessionId))
                    }
                },
                openOriginRequest = openOriginRequest,
                onOpenOriginConsumed = { openOriginRequest.value = null },
                draftPrefillRequest = draftPrefillRequest,
                onDraftPrefillConsumed = { draftPrefillRequest.value = null },
                onOriginInvalid = { navController.popBackStack() },
            )
            workspaceGraph(
                navController = navController,
                onOpenProject = { projectId, name ->
                    enterProjectRequest.value = EnterProjectRequest(projectId, name)
                    navController.popBackStack()
                },
                onOpenSiteOrigin = { _, sessionId ->
                    navController.navigate(ChatRoute(sessionId = sessionId)) {
                        launchSingleTop = true
                    }
                    openOriginRequest.value = OpenOriginRequest(sessionId)
                },
                onContinueSite = { siteId, _ ->
                    navController.navigate(ChatRoute()) {
                        launchSingleTop = true
                    }
                    draftPrefillRequest.value = DraftPrefillRequest(
                        AnrealCopy.get(AnrealCopy.SITE_CONTINUE_DRAFT).replace("{0}", siteId.take(8)),
                    )
                },
            )
        }
    }
}

internal fun NavController.replaceCurrentWith(route: Any) {
    val currentDestinationId = currentDestination?.id
    navigate(route) {
        launchSingleTop = true
        if (currentDestinationId != null) {
            popUpTo(currentDestinationId) { inclusive = true }
        }
    }
}
