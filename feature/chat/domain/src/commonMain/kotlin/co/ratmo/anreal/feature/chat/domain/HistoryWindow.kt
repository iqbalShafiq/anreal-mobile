package co.ratmo.anreal.feature.chat.domain

import co.ratmo.anreal.feature.chat.domain.stream.ChatMessage

const val HISTORY_PAGE_SIZE = 40

data class HistoryWindow(
    val messages: List<ChatMessage>,
    val oldestPosition: Int,
    val totalCount: Int,
) {
    val canLoadOlder: Boolean get() = oldestPosition > 0
}

fun List<ChatMessage>.toLatestHistoryWindow(
    pageSize: Int = HISTORY_PAGE_SIZE,
): HistoryWindow {
    if (isEmpty()) return HistoryWindow(emptyList(), oldestPosition = 0, totalCount = 0)
    val start = (size - pageSize).coerceAtLeast(0)
    return HistoryWindow(
        messages = drop(start),
        oldestPosition = start,
        totalCount = size,
    )
}
