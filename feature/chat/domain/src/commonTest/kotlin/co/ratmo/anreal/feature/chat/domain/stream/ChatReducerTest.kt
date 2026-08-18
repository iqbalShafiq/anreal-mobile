package co.ratmo.anreal.feature.chat.domain.stream

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class ChatReducerTest {

    @Test
    fun text_deltas_append_to_the_matching_part() {
        val started = ChatThreadState().reduce(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 1,
                event = ChatStreamEvent.MessageStart(
                    message = ChatMessage(
                        id = "m1",
                        role = ChatRole.Assistant,
                        parts = listOf(ChatPart.Text(id = "p1", text = "")),
                    ),
                ),
            ),
        )

        val first = started.reduce(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 2,
                event = ChatStreamEvent.TextDelta(messageId = "m1", partId = "p1", delta = "Hel"),
            ),
        )
        val second = first.reduce(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 3,
                event = ChatStreamEvent.TextDelta(messageId = "m1", partId = "p1", delta = "lo"),
            ),
        )

        assertThat(second.streamId).isEqualTo("s1")
        assertThat(second.lastEventId).isEqualTo(3)
        assertThat(second.status).isEqualTo(RunStatus.Streaming)
        assertThat(second.messages.single().parts.single()).isEqualTo(
            ChatPart.Text(id = "p1", text = "Hello"),
        )
    }

    @Test
    fun reasoning_delta_creates_a_part_when_missing() {
        val state = ChatThreadState()
            .reduce(StreamEnvelope.Start(streamId = "s1"))
            .reduce(
                StreamEnvelope.Event(
                    streamId = "s1",
                    eventId = 1,
                    event = ChatStreamEvent.MessageStart(
                        message = ChatMessage(id = "m1", role = ChatRole.Assistant),
                    ),
                ),
            )
            .reduce(
                StreamEnvelope.Event(
                    streamId = "s1",
                    eventId = 2,
                    event = ChatStreamEvent.ReasoningDelta(
                        messageId = "m1",
                        partId = "r1",
                        delta = "plan",
                    ),
                ),
            )

        assertThat(state.messages.single().parts.single()).isEqualTo(
            ChatPart.Reasoning(id = "r1", text = "plan"),
        )
    }

    @Test
    fun tool_update_replaces_the_part() {
        val state = ChatThreadState().reduce(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 1,
                event = ChatStreamEvent.ToolUpdate(
                    messageId = "m1",
                    part = ChatPart.Tool(
                        id = "t1",
                        toolName = "search",
                        toolCallId = "c1",
                        state = "output-available",
                    ),
                ),
            ),
        )

        assertThat(state.messages.single().parts.single()).isEqualTo(
            ChatPart.Tool(
                id = "t1",
                toolName = "search",
                toolCallId = "c1",
                state = "output-available",
            ),
        )
    }

    @Test
    fun error_and_stream_end_set_terminal_status() {
        val failed = ChatThreadState()
            .reduce(StreamEnvelope.Start(streamId = "s1"))
            .reduce(
                StreamEnvelope.Event(
                    streamId = "s1",
                    eventId = 4,
                    event = ChatStreamEvent.Error(message = "Provider timed out"),
                ),
            )
            .reduce(
                StreamEnvelope.End(
                    streamId = "s1",
                    eventId = 5,
                    status = StreamEndStatus.Error,
                ),
            )

        assertThat(failed.status).isEqualTo(RunStatus.Failed)
        assertThat(failed.error).isEqualTo("Provider timed out")
        assertThat(failed.lastEventId).isEqualTo(5)
    }

    @Test
    fun completed_stream_keeps_last_event_id_for_resume() {
        val state = ChatThreadState()
            .reduce(StreamEnvelope.Start(streamId = "s1"))
            .reduce(
                StreamEnvelope.Event(
                    streamId = "s1",
                    eventId = 1,
                    event = ChatStreamEvent.MessageStart(
                        message = ChatMessage(id = "m1", role = ChatRole.Assistant),
                    ),
                ),
            )
            .reduce(
                StreamEnvelope.Event(
                    streamId = "s1",
                    eventId = 2,
                    event = ChatStreamEvent.MessageEnd(messageId = "m1"),
                ),
            )
            .reduce(
                StreamEnvelope.End(
                    streamId = "s1",
                    eventId = 3,
                    status = StreamEndStatus.Completed,
                ),
            )

        assertThat(state.status).isEqualTo(RunStatus.Completed)
        assertThat(state.lastEventId).isEqualTo(3)
        assertThat(state.messages.single().isComplete).isEqualTo(true)
        assertThat(state.error).isNull()
    }

    @Test
    fun unknown_events_only_advance_the_resume_cursor() {
        val state = ChatThreadState().reduce(
            StreamEnvelope.Event(
                streamId = "s1",
                eventId = 9,
                event = ChatStreamEvent.Unknown(type = "compaction"),
            ),
        )

        assertThat(state.lastEventId).isEqualTo(9)
        assertThat(state.messages).isEqualTo(emptyList())
    }

    @Test
    fun approval_request_and_result_update_pending_input() {
        val approval = ToolApproval("a1", "web_search", null, "{}")
        val pending = ChatThreadState().reduce(
            StreamEnvelope.Event("s1", 1, ChatStreamEvent.ApprovalRequested(approval)),
        )
        val resolved = pending.reduce(
            StreamEnvelope.Event("s1", 2, ChatStreamEvent.ApprovalResolved("a1")),
        )

        assertThat(pending.pendingApprovals).isEqualTo(listOf(approval))
        assertThat(resolved.pendingApprovals).isEqualTo(emptyList())
    }
}
