package co.ratmo.anreal.core.designsystem.theme

import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds

object AnrealMotion {
    val durationFast: Duration = 160.milliseconds
    val durationMed: Duration = 220.milliseconds
    val durationDrawer: Duration = 280.milliseconds

    const val pressScale: Float = 0.97f
    const val enterScale: Float = 0.96f

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
