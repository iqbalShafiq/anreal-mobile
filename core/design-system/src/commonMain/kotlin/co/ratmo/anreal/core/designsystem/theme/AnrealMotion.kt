package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.tween
import androidx.compose.runtime.staticCompositionLocalOf
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

val LocalAnrealReduceMotion = staticCompositionLocalOf { false }

object AnrealMotion {
    val durationFast: Duration = 160.milliseconds
    val durationMed: Duration = 220.milliseconds
    val durationDrawer: Duration = 280.milliseconds
    val durationPage: Duration = 420.milliseconds

    val easeOut: Easing = CubicBezierEasing(0.23f, 1f, 0.32f, 1f)
    val easeInOut: Easing = CubicBezierEasing(0.77f, 0f, 0.175f, 1f)
    val easeDrawer: Easing = CubicBezierEasing(0.32f, 0.72f, 0f, 1f)

    const val pressScale: Float = 0.97f
    const val enterScale: Float = 0.96f

    fun <T> drawerSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = durationDrawer.inWholeMilliseconds.toInt(),
        easing = easeDrawer,
    )

    fun <T> pageSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = durationPage.inWholeMilliseconds.toInt(),
        easing = easeDrawer,
    )

    fun <T> fadeSpec(): FiniteAnimationSpec<T> = tween(
        durationMillis = durationFast.inWholeMilliseconds.toInt(),
        easing = easeOut,
    )

    enum class Frequency {
        Continuous,
        High,
        Occasional,
        Rare,
    }

    fun shouldAnimate(frequency: Frequency): Boolean {
        return when (frequency) {
            Frequency.Continuous,
            Frequency.High,
            -> false
            Frequency.Occasional,
            Frequency.Rare,
            -> true
        }
    }
}
