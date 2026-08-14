package co.ratmo.anreal.core.designsystem.theme

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class ThemeSettingsTest {

    @Test
    fun system_mode_follows_platform() {
        val settings = ThemeSettings(mode = ThemeMode.System, dynamicColor = true)

        assertThat(settings.resolveDark(systemDark = true)).isTrue()
        assertThat(settings.resolveDark(systemDark = false)).isFalse()
    }

    @Test
    fun explicit_modes_ignore_system() {
        assertThat(ThemeSettings(ThemeMode.Dark, dynamicColor = false).resolveDark(false)).isTrue()
        assertThat(ThemeSettings(ThemeMode.Light, dynamicColor = true).resolveDark(true)).isFalse()
    }

    @Test
    fun brand_seed_matches_web_accent() {
        assertThat(AnrealBrand.seedArgb).isEqualTo(0xFFE8A317.toInt())
    }
}
