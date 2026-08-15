package co.ratmo.anreal.core.designsystem.component

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.math.abs
import kotlin.test.Test

class AnrealAuroraTest {

    @Test
    fun wave_starts_at_rest() {
        assertThat(auroraWave(seconds = 0f, periodSeconds = 16f)).isEqualTo(0f)
    }

    @Test
    fun wave_peaks_at_quarter_period() {
        assertClose(auroraWave(seconds = 4f, periodSeconds = 16f), 1f)
    }

    @Test
    fun wave_troughs_at_three_quarters() {
        assertClose(auroraWave(seconds = 12f, periodSeconds = 16f), -1f)
    }

    @Test
    fun pulse_is_mid_at_rest_and_peaks_later() {
        assertClose(auroraPulse(seconds = 0f, periodSeconds = 16f), 0.81f)
        assertClose(auroraPulse(seconds = 4f, periodSeconds = 16f), 1f)
        assertClose(auroraPulse(seconds = 12f, periodSeconds = 16f), 0.62f)
    }

    @Test
    fun invalid_period_stays_at_rest() {
        assertThat(auroraWave(seconds = 4f, periodSeconds = 0f)).isEqualTo(0f)
    }
}

private fun assertClose(actual: Float, expected: Float) {
    val delta = abs(actual - expected)
    assertThat(delta < 0.001f, name = "|$actual - $expected| < 0.001").isEqualTo(true)
}
