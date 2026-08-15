package co.ratmo.anreal.core.designsystem.theme

import androidx.compose.animation.core.CubicBezierEasing
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class AnrealMotionTest {

    @Test
    fun durations_match_design_tokens() {
        assertThat(AnrealMotion.durationFast.inWholeMilliseconds).isEqualTo(160)
        assertThat(AnrealMotion.durationMed.inWholeMilliseconds).isEqualTo(220)
        assertThat(AnrealMotion.durationDrawer.inWholeMilliseconds).isEqualTo(280)
        assertThat(AnrealMotion.durationPage.inWholeMilliseconds).isEqualTo(420)
        assertThat(AnrealMotion.durationSplash.inWholeMilliseconds).isEqualTo(1100)
        assertThat(AnrealMotion.durationBoardingHold.inWholeMilliseconds).isEqualTo(4500)
        assertThat(AnrealMotion.durationAuroraMin.inWholeSeconds).isEqualTo(16)
        assertThat(AnrealMotion.durationAurora.inWholeSeconds).isEqualTo(22)
        assertThat(AnrealMotion.durationAuroraMax.inWholeSeconds).isEqualTo(32)
    }

    @Test
    fun daily_stream_and_send_do_not_animate() {
        assertThat(AnrealMotion.shouldAnimate(AnrealMotion.Frequency.Continuous)).isFalse()
        assertThat(AnrealMotion.shouldAnimate(AnrealMotion.Frequency.High)).isFalse()
    }

    @Test
    fun occasional_chrome_does_animate() {
        assertThat(AnrealMotion.shouldAnimate(AnrealMotion.Frequency.Occasional)).isTrue()
        assertThat(AnrealMotion.shouldAnimate(AnrealMotion.Frequency.Rare)).isTrue()
    }

    @Test
    fun press_scale_is_subtle() {
        assertThat(AnrealMotion.pressScale).isEqualTo(0.97f)
        assertThat(AnrealMotion.enterScale).isEqualTo(0.96f)
    }

    @Test
    fun easing_matches_design_tokens() {
        assertThat(AnrealMotion.easeOut).isEqualTo(CubicBezierEasing(0.23f, 1f, 0.32f, 1f))
        assertThat(AnrealMotion.easeInOut).isEqualTo(CubicBezierEasing(0.77f, 0f, 0.175f, 1f))
        assertThat(AnrealMotion.easeDrawer).isEqualTo(CubicBezierEasing(0.32f, 0.72f, 0f, 1f))
    }
}
