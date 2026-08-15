package co.ratmo.anreal.core.data

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class AppEnvironmentTest {

    @Test
    fun development_stubs_api() {
        assertThat(AppEnvironment.parse("development").stubApi).isTrue()
        assertThat(AppEnvironment.parse("dev").stubApi).isTrue()
    }

    @Test
    fun staging_and_production_hit_real_api() {
        assertThat(AppEnvironment.parse("staging").stubApi).isFalse()
        assertThat(AppEnvironment.parse("production").stubApi).isFalse()
        assertThat(AppEnvironment.parse("prod")).isEqualTo(AppEnvironment.Production)
    }
}
