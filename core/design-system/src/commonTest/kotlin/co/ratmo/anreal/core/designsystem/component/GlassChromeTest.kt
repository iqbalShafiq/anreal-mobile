package co.ratmo.anreal.core.designsystem.component

import assertk.assertThat
import assertk.assertions.isTrue
import kotlin.test.Test

class GlassChromeTest {

    @Test
    fun settledSurfaceUsesStrongerChromeThanAurora() {
        val aurora = glassChromeTargets(GlassChromeMode.Aurora, emphasized = false)
        val surface = glassChromeTargets(GlassChromeMode.Surface, emphasized = false)

        assertThat(surface.effectAlpha > aurora.effectAlpha).isTrue()
        assertThat(surface.opaqueTintAlpha > aurora.opaqueTintAlpha).isTrue()
        assertThat(surface.fallbackAlpha > aurora.fallbackAlpha).isTrue()
    }

    @Test
    fun frostedAuroraRaisesChromeWithoutBecomingSettledSurface() {
        val clearAurora = glassChromeTargets(GlassChromeMode.Aurora, emphasized = false)
        val frostedAurora = glassChromeTargets(GlassChromeMode.Aurora, emphasized = true)
        val surface = glassChromeTargets(GlassChromeMode.Surface, emphasized = false)

        assertThat(frostedAurora.effectAlpha > clearAurora.effectAlpha).isTrue()
        assertThat(frostedAurora.opaqueTintAlpha < surface.opaqueTintAlpha).isTrue()
    }
}
