package co.ratmo.anreal.feature.chat.domain.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import co.ratmo.anreal.feature.chat.domain.SiteBuildPhase
import kotlin.test.Test

class SiteBuildStreamTest {

    @Test
    fun parser_maps_site_build_progress() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"data","name":"siteBuildProgress","data":{"siteId":"s1","version":1,"phase":"building","message":"Membangun hero."}}}""",
        )
        val progress = (event as StreamEnvelope.Event).event as ChatStreamEvent.SiteBuildProgress
        assertThat(progress.phase).isEqualTo(SiteBuildPhase.Building)
    }

    @Test
    fun parser_maps_site_build_ready() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":2,"event":{"type":"data","name":"siteBuildReady","data":{"siteId":"s1","version":1,"previewUrl":"/api/sites/s1/v1/preview/index.html","downloadUrl":"/api/sites/s1/v1/download"}}}""",
        )
        val ready = (event as StreamEnvelope.Event).event as ChatStreamEvent.SiteBuildReady
        assertThat(ready.downloadUrl).isEqualTo("/api/sites/s1/v1/download")
    }

    @Test
    fun reducer_applies_progress_then_ready() {
        val afterProgress = ChatThreadState().reduce(
            StreamEnvelope.Event("stream-1", 1, ChatStreamEvent.SiteBuildProgress("s1", 1, SiteBuildPhase.Building, "Membangun hero.")),
        )
        assertThat(afterProgress.siteBuild?.phase).isEqualTo(SiteBuildPhase.Building)
        val afterReady = afterProgress.reduce(
            StreamEnvelope.Event("stream-1", 2, ChatStreamEvent.SiteBuildReady("s1", 1, "/api/sites/s1/v1/preview/index.html", "/api/sites/s1/v1/download")),
        )
        assertThat(afterReady.siteBuild?.phase).isEqualTo(SiteBuildPhase.Ready)
        assertThat(afterReady.siteVersions.single().stable).isTrue()
    }

    @Test
    fun unknown_phase_maps_unknown() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":3,"event":{"type":"data","name":"siteBuildProgress","data":{"siteId":"s1","version":1,"phase":"teleporting","message":"x"}}}""",
        )
        assertThat((event as StreamEnvelope.Event).event is ChatStreamEvent.Unknown).isTrue()
    }
}
