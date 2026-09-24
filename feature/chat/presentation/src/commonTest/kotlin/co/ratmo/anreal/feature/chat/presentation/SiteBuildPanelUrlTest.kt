package co.ratmo.anreal.feature.chat.presentation

import assertk.assertThat
import assertk.assertions.isEqualTo
import co.ratmo.anreal.feature.chat.presentation.component.resolveSiteUrl
import kotlin.test.Test

class SiteBuildPanelUrlTest {

    @Test
    fun resolve_joins_relative_preview_route_against_base_url() {
        assertThat(
            resolveSiteUrl("http://127.0.0.1:3001", "/api/sites/s1/v3/preview/index.html"),
        ).isEqualTo("http://127.0.0.1:3001/api/sites/s1/v3/preview/index.html")
    }

    @Test
    fun resolve_trims_trailing_slash_from_base_url() {
        assertThat(
            resolveSiteUrl("http://127.0.0.1:3001/", "api/sites/s1/v3/download"),
        ).isEqualTo("http://127.0.0.1:3001/api/sites/s1/v3/download")
    }

    @Test
    fun resolve_keeps_absolute_preview_url_untouched() {
        val absolute = "https://cdn.example.com/sites/s1/v3/preview/index.html"
        assertThat(resolveSiteUrl("http://127.0.0.1:3001", absolute)).isEqualTo(absolute)
    }

    @Test
    fun resolve_empty_base_returns_route_unchanged() {
        assertThat(resolveSiteUrl("", "/api/sites/s1/v3/download"))
            .isEqualTo("/api/sites/s1/v3/download")
    }
}
