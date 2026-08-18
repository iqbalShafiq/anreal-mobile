package co.ratmo.anreal

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.Test

class AnrealNavTransitionsTest {

    @Test
    fun login_to_register_slides_up() {
        assertThat(classifyNavMotion(AnrealRouteKind.Login, AnrealRouteKind.Register))
            .isEqualTo(AnrealNavMotion.VerticalUp)
    }

    @Test
    fun register_to_login_slides_down() {
        assertThat(classifyNavMotion(AnrealRouteKind.Register, AnrealRouteKind.Login))
            .isEqualTo(AnrealNavMotion.VerticalDown)
    }

    @Test
    fun boarding_to_forms_slides_up() {
        assertThat(classifyNavMotion(AnrealRouteKind.Boarding, AnrealRouteKind.Register))
            .isEqualTo(AnrealNavMotion.VerticalUp)
        assertThat(classifyNavMotion(AnrealRouteKind.Boarding, AnrealRouteKind.Login))
            .isEqualTo(AnrealNavMotion.VerticalUp)
    }

    @Test
    fun auth_to_chat_slides_forward() {
        assertThat(classifyNavMotion(AnrealRouteKind.Login, AnrealRouteKind.Chat))
            .isEqualTo(AnrealNavMotion.HorizontalForward)
        assertThat(classifyNavMotion(AnrealRouteKind.Register, AnrealRouteKind.Chat))
            .isEqualTo(AnrealNavMotion.HorizontalForward)
        assertThat(classifyNavMotion(AnrealRouteKind.Boarding, AnrealRouteKind.Chat))
            .isEqualTo(AnrealNavMotion.HorizontalForward)
    }

    @Test
    fun logout_slides_back() {
        assertThat(classifyNavMotion(AnrealRouteKind.Chat, AnrealRouteKind.Boarding))
            .isEqualTo(AnrealNavMotion.HorizontalBack)
        assertThat(classifyNavMotion(AnrealRouteKind.Account, AnrealRouteKind.Boarding))
            .isEqualTo(AnrealNavMotion.HorizontalBack)
        assertThat(classifyNavMotion(AnrealRouteKind.Chat, AnrealRouteKind.Login))
            .isEqualTo(AnrealNavMotion.HorizontalBack)
        assertThat(classifyNavMotion(AnrealRouteKind.Account, AnrealRouteKind.Login))
            .isEqualTo(AnrealNavMotion.HorizontalBack)
    }

    @Test
    fun chat_to_account_slides_forward() {
        assertThat(classifyNavMotion(AnrealRouteKind.Chat, AnrealRouteKind.Account))
            .isEqualTo(AnrealNavMotion.HorizontalForward)
        assertThat(classifyNavMotion(AnrealRouteKind.Account, AnrealRouteKind.Chat))
            .isEqualTo(AnrealNavMotion.HorizontalBack)
    }
}
