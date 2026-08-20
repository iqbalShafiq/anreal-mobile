package co.ratmo.anreal.core.designsystem.component

import assertk.assertThat
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class FrostedTopBarTest {

    @Test
    fun start_of_list_is_not_frosted() {
        assertThat(isScrolledFromStart(0, 0)).isFalse()
        assertThat(isScrolledFromStart(0, FrostedTopBarSlopPx)).isFalse()
    }

    @Test
    fun leftover_fling_offset_within_slop_is_not_frosted() {
        assertThat(isScrolledFromStart(0, 1)).isFalse()
        assertThat(isScrolledFromStart(0, FrostedTopBarSlopPx - 1)).isFalse()
    }

    @Test
    fun scrolled_content_is_frosted() {
        assertThat(isScrolledFromStart(0, FrostedTopBarSlopPx + 1)).isTrue()
        assertThat(isScrolledFromStart(1, 0)).isTrue()
    }
}
