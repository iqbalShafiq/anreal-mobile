package co.ratmo.anreal.core.designsystem.component

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class ImeFocusShiftTest {

    @Test
    fun no_focus_or_closed_keyboard_does_not_shift() {
        assertThat(
            imeFocusShiftPx(
                focusedBottomInWindow = null,
                appliedShiftPx = 0,
                imeBottomPx = 800,
                windowHeightPx = 2000,
                extraGapPx = 12,
            ),
        ).isEqualTo(0)
        assertThat(
            imeFocusShiftPx(
                focusedBottomInWindow = 1900f,
                appliedShiftPx = 0,
                imeBottomPx = 0,
                windowHeightPx = 2000,
                extraGapPx = 12,
            ),
        ).isEqualTo(0)
    }

    @Test
    fun overlapping_field_shifts_just_enough() {
        assertThat(
            imeFocusShiftPx(
                focusedBottomInWindow = 1700f,
                appliedShiftPx = 0,
                imeBottomPx = 800,
                windowHeightPx = 2000,
                extraGapPx = 12,
            ),
        ).isEqualTo(512)
    }

    @Test
    fun already_shifted_field_stays_stable() {
        assertThat(
            imeFocusShiftPx(
                focusedBottomInWindow = 1188f,
                appliedShiftPx = 512,
                imeBottomPx = 800,
                windowHeightPx = 2000,
                extraGapPx = 12,
            ),
        ).isEqualTo(512)
    }

    @Test
    fun field_already_above_keyboard_does_not_shift() {
        assertThat(
            imeFocusShiftPx(
                focusedBottomInWindow = 900f,
                appliedShiftPx = 0,
                imeBottomPx = 800,
                windowHeightPx = 2000,
                extraGapPx = 12,
            ),
        ).isEqualTo(0)
    }
}
