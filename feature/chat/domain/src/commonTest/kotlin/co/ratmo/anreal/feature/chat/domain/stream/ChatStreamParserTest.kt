package co.ratmo.anreal.feature.chat.domain.stream

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ChatStreamParserTest {

    @Test
    fun blank_and_invalid_lines_are_ignored() {
        assertThat(parseStreamLine("")).isNull()
        assertThat(parseStreamLine("   ")).isNull()
        assertThat(parseStreamLine("{not-json")).isNull()
        assertThat(parseStreamLine("""{"type":"unknown"}""")).isNull()
    }

    @Test
    fun parses_stream_start() {
        val envelope = parseStreamLine(
            """{"type":"stream_start","streamId":"s1","eventId":0}""",
        )

        assertThat(envelope).isEqualTo(StreamEnvelope.Start(streamId = "s1"))
    }

    @Test
    fun parses_text_delta_inside_stream_event() {
        val envelope = parseStreamLine(
            """
            {"type":"stream_event","streamId":"s1","eventId":2,
             "event":{"type":"text_delta","messageId":"m1","partId":"p1","delta":"Hello"}}
            """.trimIndent().replace("\n", ""),
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 2,
                event = ChatStreamEvent.TextDelta(
                    messageId = "m1",
                    partId = "p1",
                    delta = "Hello",
                ),
            ),
        )
    }

    @Test
    fun parses_reasoning_error_and_completion() {
        val lines = """
            {"type":"stream_event","streamId":"s1","eventId":3,"event":{"type":"reasoning_delta","messageId":"m1","partId":"r1","delta":"think"}}
            {"type":"stream_event","streamId":"s1","eventId":4,"event":{"type":"error","error":{"message":"Provider timed out"}}}
            {"type":"stream_end","streamId":"s1","eventId":5,"status":"completed"}
        """.trimIndent()

        assertThat(parseStreamLines(lines)).containsExactly(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 3,
                event = ChatStreamEvent.ReasoningDelta(
                    messageId = "m1",
                    partId = "r1",
                    delta = "think",
                ),
            ),
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 4,
                event = ChatStreamEvent.Error(message = "Provider timed out"),
            ),
            StreamEnvelope.End(
                streamId = "s1",
                eventId = 5,
                status = StreamEndStatus.Completed,
            ),
        )
    }

    @Test
    fun unknown_inner_event_is_kept_so_resume_cursor_still_advances() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":9,"event":{"type":"compaction","phase":"start"}}""",
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 9,
                event = ChatStreamEvent.Unknown(type = "compaction"),
            ),
        )
    }
}
