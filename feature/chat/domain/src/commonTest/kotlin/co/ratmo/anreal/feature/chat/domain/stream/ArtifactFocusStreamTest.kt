package co.ratmo.anreal.feature.chat.domain.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import assertk.assertions.isTrue
import kotlin.test.Test

class ArtifactFocusStreamTest {

    @Test
    fun focus_valid_maps() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":1,"event":{"type":"data","name":"artifactFocus","data":{"artifactId":"t1","artifactType":"task","label":"Fix login"}}}""",
        )
        val focus = (event as StreamEnvelope.Event).event as ChatStreamEvent.ArtifactFocus
        assertThat(focus.artifactType).isEqualTo("task")
        assertThat(focus.label).isEqualTo("Fix login")
    }

    @Test
    fun focus_without_label_maps() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":2,"event":{"type":"data","name":"artifactFocus","data":{"artifactId":"s1","artifactType":"site"}}}""",
        )
        val focus = (event as StreamEnvelope.Event).event as ChatStreamEvent.ArtifactFocus
        assertThat(focus.label).isNull()
    }

    @Test
    fun focus_unknown_type_maps_unknown() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":3,"event":{"type":"data","name":"artifactFocus","data":{"artifactId":"x","artifactType":"teleporter"}}}""",
        )
        assertThat((event as StreamEnvelope.Event).event is ChatStreamEvent.Unknown).isTrue()
    }

    @Test
    fun focus_extra_key_maps_unknown() {
        val event = parseStreamLine(
            """{"type":"stream_event","streamId":"stream-1","eventId":4,"event":{"type":"data","name":"artifactFocus","data":{"artifactId":"x","artifactType":"task","hax":1}}}""",
        )
        assertThat((event as StreamEnvelope.Event).event is ChatStreamEvent.Unknown).isTrue()
    }

    @Test
    fun reducer_sets_pending_focus() {
        val state = ChatThreadState().reduce(
            StreamEnvelope.Event("stream-1", 5, ChatStreamEvent.ArtifactFocus("t1", "task", null)),
        )
        assertThat(state.pendingArtifactFocus?.artifactId).isEqualTo("t1")
    }
}
