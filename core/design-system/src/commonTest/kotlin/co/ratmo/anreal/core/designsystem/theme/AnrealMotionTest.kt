package co.ratmo.anreal.core.designsystem.theme

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
}
