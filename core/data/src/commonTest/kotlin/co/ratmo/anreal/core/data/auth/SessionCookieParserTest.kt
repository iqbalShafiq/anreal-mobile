package co.ratmo.anreal.core.data.auth

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class SessionCookieParserTest {

    @Test
    fun extracts_better_auth_session_token() {
        val headers = listOf(
            "other=1; Path=/",
            "better-auth.session_token=abc.123; Path=/; HttpOnly; SameSite=Lax",
        )

        assertThat(parseSessionToken(headers)).isEqualTo("abc.123")
    }

    @Test
    fun returns_null_when_session_cookie_missing() {
        assertThat(parseSessionToken(listOf("foo=bar; Path=/"))).isNull()
        assertThat(parseSessionToken(emptyList())).isNull()
    }
}
