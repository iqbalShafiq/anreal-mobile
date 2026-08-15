package co.ratmo.anreal

import androidx.compose.animation.AnimatedContentTransitionScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.ui.unit.IntOffset
import androidx.navigation.NavBackStackEntry
import androidx.navigation.NavDestination
import androidx.navigation.NavDestination.Companion.hasRoute
import co.ratmo.anreal.core.designsystem.theme.AnrealMotion
import co.ratmo.anreal.feature.auth.presentation.BoardingRoute
import co.ratmo.anreal.feature.auth.presentation.LoginRoute
import co.ratmo.anreal.feature.auth.presentation.RegisterRoute
import co.ratmo.anreal.feature.chat.presentation.AccountRoute
import co.ratmo.anreal.feature.chat.presentation.ChatRoute

enum class AnrealRouteKind {
    Boarding,
    Login,
    Register,
    Chat,
    Account,
    Other,
}

enum class AnrealNavMotion {
    VerticalUp,
    VerticalDown,
    HorizontalForward,
    HorizontalBack,
    Fade,
}

fun classifyNavMotion(from: AnrealRouteKind, to: AnrealRouteKind): AnrealNavMotion {
    val fromAuth = from.isAuth
    val toAuth = to.isAuth
    val fromApp = from.isApp
    val toApp = to.isApp
    val fromForm = from == AnrealRouteKind.Login || from == AnrealRouteKind.Register
    val toForm = to == AnrealRouteKind.Login || to == AnrealRouteKind.Register
    return when {
        from == AnrealRouteKind.Boarding && toForm -> AnrealNavMotion.VerticalUp
        fromForm && to == AnrealRouteKind.Boarding -> AnrealNavMotion.VerticalDown
        from == AnrealRouteKind.Login && to == AnrealRouteKind.Register -> AnrealNavMotion.VerticalUp
        from == AnrealRouteKind.Register && to == AnrealRouteKind.Login -> AnrealNavMotion.VerticalDown
        fromAuth && toApp -> AnrealNavMotion.HorizontalForward
        fromApp && toAuth -> AnrealNavMotion.HorizontalBack
        from == AnrealRouteKind.Chat && to == AnrealRouteKind.Account -> AnrealNavMotion.HorizontalForward
        from == AnrealRouteKind.Account && to == AnrealRouteKind.Chat -> AnrealNavMotion.HorizontalBack
        else -> AnrealNavMotion.Fade
    }
}

private val AnrealRouteKind.isAuth: Boolean
    get() = this == AnrealRouteKind.Boarding ||
        this == AnrealRouteKind.Login ||
        this == AnrealRouteKind.Register

private val AnrealRouteKind.isApp: Boolean
    get() = this == AnrealRouteKind.Chat || this == AnrealRouteKind.Account

fun NavDestination.toRouteKind(): AnrealRouteKind {
    return when {
        hasRoute<BoardingRoute>() -> AnrealRouteKind.Boarding
        hasRoute<LoginRoute>() -> AnrealRouteKind.Login
        hasRoute<RegisterRoute>() -> AnrealRouteKind.Register
        hasRoute<ChatRoute>() -> AnrealRouteKind.Chat
        hasRoute<AccountRoute>() -> AnrealRouteKind.Account
        else -> AnrealRouteKind.Other
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.anrealEnter(
    reduceMotion: Boolean,
): EnterTransition {
    val motion = classifyNavMotion(
        initialState.destination.toRouteKind(),
        targetState.destination.toRouteKind(),
    )
    if (reduceMotion) return fadeIn(animationSpec = AnrealMotion.fadeSpec())
    val page: FiniteAnimationSpec<IntOffset> = AnrealMotion.pageSpec()
    val push: FiniteAnimationSpec<IntOffset> = AnrealMotion.drawerSpec()
    return when (motion) {
        AnrealNavMotion.VerticalUp -> slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = page,
        )
        AnrealNavMotion.VerticalDown -> slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = page,
        )
        AnrealNavMotion.HorizontalForward -> slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = push,
        )
        AnrealNavMotion.HorizontalBack -> slideIntoContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = push,
        )
        AnrealNavMotion.Fade -> fadeIn(animationSpec = AnrealMotion.fadeSpec())
    }
}

fun AnimatedContentTransitionScope<NavBackStackEntry>.anrealExit(
    reduceMotion: Boolean,
): ExitTransition {
    val motion = classifyNavMotion(
        initialState.destination.toRouteKind(),
        targetState.destination.toRouteKind(),
    )
    if (reduceMotion) return fadeOut(animationSpec = AnrealMotion.fadeSpec())
    val page: FiniteAnimationSpec<IntOffset> = AnrealMotion.pageSpec()
    val push: FiniteAnimationSpec<IntOffset> = AnrealMotion.drawerSpec()
    return when (motion) {
        AnrealNavMotion.VerticalUp -> slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Up,
            animationSpec = page,
        )
        AnrealNavMotion.VerticalDown -> slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Down,
            animationSpec = page,
        )
        AnrealNavMotion.HorizontalForward -> slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Left,
            animationSpec = push,
        )
        AnrealNavMotion.HorizontalBack -> slideOutOfContainer(
            towards = AnimatedContentTransitionScope.SlideDirection.Right,
            animationSpec = push,
        )
        AnrealNavMotion.Fade -> fadeOut(animationSpec = AnrealMotion.fadeSpec())
    }
}
