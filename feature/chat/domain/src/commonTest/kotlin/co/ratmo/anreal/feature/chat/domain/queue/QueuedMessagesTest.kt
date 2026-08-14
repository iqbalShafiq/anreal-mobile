package co.ratmo.anreal.feature.chat.domain.queue

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isNull
import kotlin.test.Test

class QueuedMessagesTest {

    @Test
    fun add_item_appends_pending_and_keeps_order() {
        val first = QueuedItem(id = "a", text = "one")
        val second = QueuedItem(id = "b", text = "two")
        val items = addItem(addItem(emptyList(), first), second)
        assertThat(items).containsExactly(first, second)
    }

    @Test
    fun next_flushable_is_null_if_head_is_editing() {
        val items = listOf(
            QueuedItem(id = "a", text = "one", status = QueueStatus.Editing),
            QueuedItem(id = "b", text = "two"),
        )
        assertThat(nextFlushable(items)).isNull()
    }

    @Test
    fun next_flushable_skips_inflight_and_stops_before_editing() {
        val pending = QueuedItem(id = "b", text = "two")
        val items = listOf(
            QueuedItem(id = "a", text = "one", status = QueueStatus.Inflight),
            pending,
            QueuedItem(id = "c", text = "three", status = QueueStatus.Editing),
        )
        assertThat(nextFlushable(items)).isEqualTo(pending)
    }

    @Test
    fun apply_ack_removes_and_unknown_id_is_noop() {
        val items = listOf(QueuedItem(id = "a", text = "one"), QueuedItem(id = "b", text = "two"))
        assertThat(applyAck(items, "a").map { it.id }).containsExactly("b")
        assertThat(applyAck(items, "missing")).isEqualTo(items)
    }

    @Test
    fun revert_inflight_returns_to_pending() {
        val items = markInflight(
            listOf(QueuedItem(id = "a", text = "one")),
            listOf("a"),
        )
        assertThat(revertInflight(items).single().status).isEqualTo(QueueStatus.Pending)
    }

    @Test
    fun restore_normalizes_editing_and_inflight() {
        val items = restoreQueue(
            listOf(
                QueuedItem(id = "a", text = "one", status = QueueStatus.Editing),
                QueuedItem(id = "b", text = "two", status = QueueStatus.Inflight),
            ),
        )
        assertThat(items.map { it.status }).containsExactly(QueueStatus.Pending, QueueStatus.Pending)
    }
}
