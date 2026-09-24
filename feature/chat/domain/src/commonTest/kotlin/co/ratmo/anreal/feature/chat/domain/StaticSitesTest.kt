package co.ratmo.anreal.feature.chat.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class StaticSitesTest {

    @Test
    fun ready_event_marks_stable_and_sorts() {
        val versions = applySiteVersionEvent(
            current = listOf(SiteVersionEntry("s1", 1, ready = true, failed = false, stable = true, previewUrl = "p1", downloadUrl = "d1")),
            eventSiteId = "s1", eventVersion = 2, phase = SiteBuildPhase.Ready,
            previewUrl = "/api/sites/s1/v2/preview/index.html", downloadUrl = "/api/sites/s1/v2/download",
        )
        assertThat(versions.map { it.version }).isEqualTo(listOf(1, 2))
        assertThat(versions.first { it.version == 2 }.stable).isTrue()
        assertThat(versions.first { it.version == 1 }.stable).isFalse()
    }

    @Test
    fun failed_event_marks_failed_not_stable() {
        val versions = applySiteVersionEvent(
            current = emptyList(), eventSiteId = "s1", eventVersion = 1,
            phase = SiteBuildPhase.Failed, previewUrl = null, downloadUrl = null,
        )
        assertThat(versions.single().failed).isTrue()
        assertThat(versions.single().stable).isFalse()
    }

    @Test
    fun different_site_id_resets_state() {
        val build = applySiteBuildEvent(
            current = SiteBuildState("s1", 1, SiteBuildPhase.Building, "Membangun halaman."),
            eventSiteId = "s2", eventVersion = 1, phase = SiteBuildPhase.Starting, message = "Menunggu antrean build.",
        )
        assertThat(build.siteId).isEqualTo("s2")
        assertThat(build.previewUrl).isNull()
    }

    @Test
    fun selection_intersects_catalog() {
        assertThat(intersectWithCatalog(listOf("a", "b", "ghost"), listOf("a", "b"))).isEqualTo(listOf("a", "b"))
    }

    @Test
    fun stable_urls_shape() {
        val (preview, download) = stableSiteUrls("s1", 2)
        assertThat(preview).isEqualTo("/api/sites/s1/v2/preview/index.html")
        assertThat(download).isEqualTo("/api/sites/s1/v2/download")
    }
}
