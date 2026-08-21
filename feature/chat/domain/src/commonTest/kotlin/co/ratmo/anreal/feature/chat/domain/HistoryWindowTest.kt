package co.ratmo.anreal.feature.chat.domain

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage
import co.ratmo.anreal.feature.chat.domain.stream.ChatPart
import co.ratmo.anreal.feature.chat.domain.stream.ChatRole
import kotlin.test.Test

class HistoryWindowTest {

    @Test
    fun empty_list_has_no_older_pages() {
        val window = emptyList<ChatMessage>().toLatestHistoryWindow()

        assertThat(window.messages).isEqualTo(emptyList())
        assertThat(window.oldestPosition).isEqualTo(0)
        assertThat(window.totalCount).isEqualTo(0)
        assertThat(window.canLoadOlder).isFalse()
    }

    @Test
    fun short_history_returns_every_message() {
        val messages = historyMessages(3)

        val window = messages.toLatestHistoryWindow()

        assertThat(window.messages).isEqualTo(messages)
        assertThat(window.oldestPosition).isEqualTo(0)
        assertThat(window.canLoadOlder).isFalse()
    }

    @Test
    fun long_history_keeps_only_the_latest_page() {
        val messages = historyMessages(HISTORY_PAGE_SIZE + 5)

        val window = messages.toLatestHistoryWindow()

        assertThat(window.messages).isEqualTo(messages.takeLast(HISTORY_PAGE_SIZE))
        assertThat(window.oldestPosition).isEqualTo(5)
        assertThat(window.totalCount).isEqualTo(HISTORY_PAGE_SIZE + 5)
        assertThat(window.canLoadOlder).isTrue()
    }

    private fun historyMessages(count: Int): List<ChatMessage> = List(count) { index ->
        ChatMessage(
            id = "history-$index",
            role = if (index % 2 == 0) ChatRole.User else ChatRole.Assistant,
            parts = listOf(ChatPart.Text(id = "t$index", text = "Message $index")),
            isComplete = true,
        )
    }
}
