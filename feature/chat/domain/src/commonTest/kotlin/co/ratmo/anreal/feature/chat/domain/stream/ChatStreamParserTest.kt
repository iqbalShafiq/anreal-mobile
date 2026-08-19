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
    fun compaction_event_is_typed_so_resume_cursor_still_advances() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":9,"event":{"type":"compaction","phase":"start"}}""",
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 9,
                event = ChatStreamEvent.Compaction(phase = "start"),
            ),
        )
    }

    @Test
    fun parses_approval_and_clarification_requests() {
        val lines = """
            {"type":"stream_event","streamId":"s1","eventId":10,"event":{"type":"tool_approval_request","approval":{"id":"a1","toolName":"generate_image","args":{"prompt":"cat"},"reason":"Creates an image"}}}
            {"type":"stream_event","streamId":"s1","eventId":11,"event":{"type":"clarification_request","clarification":{"id":"c1","questions":[{"id":"q1","question":"Which style?","type":"single_choice","options":[{"id":"clean","label":"Clean","recommended":true}]}]}}}
        """.trimIndent()

        val events = parseStreamLines(lines).map { (it as StreamEnvelope.Event).event }

        assertThat(events[0]).isEqualTo(
            ChatStreamEvent.ApprovalRequested(
                ToolApproval("a1", "generate_image", "Creates an image", "{\"prompt\":\"cat\"}"),
            ),
        )
        assertThat(events[1]).isEqualTo(
            ChatStreamEvent.ClarificationRequested(
                Clarification(
                    id = "c1",
                    title = null,
                    questions = listOf(
                        ClarificationQuestion(
                            id = "q1",
                            question = "Which style?",
                            type = "single_choice",
                            options = listOf(ClarificationOption("clean", "Clean", true)),
                            optional = false,
                            placeholder = null,
                        ),
                    ),
                ),
            ),
        )
    }

    @Test
    fun parses_tool_update_input_and_output() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":8,"event":{"type":"tool_update","messageId":"m1","part":{"id":"t1","type":"tool","toolName":"web_search","toolCallId":"c1","state":"output-available","input":{"query":"kotlin"},"output":{"results":[]}}}}""",
        )
        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 8,
                event = ChatStreamEvent.ToolUpdate(
                    messageId = "m1",
                    part = ChatPart.Tool(
                        id = "t1",
                        toolName = "web_search",
                        toolCallId = "c1",
                        state = "output-available",
                        input = """{"query":"kotlin"}""",
                        output = """{"results":[]}""",
                    ),
                ),
            ),
        )
    }

    @Test
    fun parses_queued_message_applied() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":6,"event":{"type":"queued_message_applied","clientMessageId":"q1","text":"Follow up"}}""",
        )
        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 6,
                event = ChatStreamEvent.QueuedMessageApplied(
                    clientMessageId = "q1",
                    text = "Follow up",
                ),
            ),
        )
    }

    @Test
    fun parses_anthropic_style_text_delta_with_turn() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":31,"event":{"type":"text_delta","turn":1,"delta":"Halo"}}""",
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 31,
                event = ChatStreamEvent.TextDelta(
                    messageId = "turn-1",
                    partId = "turn-1-text",
                    delta = "Halo",
                ),
            ),
        )
    }

    @Test
    fun parses_anthropic_style_reasoning_delta_with_turn_and_id() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":3,"event":{"type":"reasoning_delta","turn":1,"delta":"The user","id":"rs_tmp_abc","contentType":"text"}}""",
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 3,
                event = ChatStreamEvent.ReasoningDelta(
                    messageId = "turn-1",
                    partId = "rs_tmp_abc",
                    delta = "The user",
                ),
            ),
        )
    }

    @Test
    fun turn_end_maps_to_message_end_for_the_turn() {
        val envelope = parseStreamLine(
            """{"type":"stream_event","streamId":"s1","eventId":111,"event":{"type":"turn_end","turn":1,"response":{"choice":[]}}}""",
        )

        assertThat(envelope).isEqualTo(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 111,
                event = ChatStreamEvent.MessageEnd(messageId = "turn-1"),
            ),
        )
    }

    @Test
    fun turn_start_generation_start_and_final_are_ignored() {
        val lines = listOf(
            """{"type":"stream_event","streamId":"s1","eventId":1,"event":{"type":"turn_start","turn":1,"prompt":{}}}""",
            """{"type":"stream_event","streamId":"s1","eventId":2,"event":{"type":"generation_start","turn":1,"request":{}}}""",
            """{"type":"stream_event","streamId":"s1","eventId":112,"event":{"type":"final","runId":"r1","output":"done"}}""",
        )

        val events = lines.map { (parseStreamLine(it) as StreamEnvelope.Event).event }

        assertThat(events[0]).isEqualTo(ChatStreamEvent.Unknown("turn_start"))
        assertThat(events[1]).isEqualTo(ChatStreamEvent.Unknown("generation_start"))
        assertThat(events[2]).isEqualTo(ChatStreamEvent.Unknown("final"))
    }
}
