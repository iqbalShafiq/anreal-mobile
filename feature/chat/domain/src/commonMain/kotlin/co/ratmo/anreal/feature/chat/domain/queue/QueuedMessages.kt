package co.ratmo.anreal.feature.chat.domain.queue

enum class QueueStatus {
    Pending,
    Inflight,
    Editing,
}

data class QueuedItem(
    val id: String,
    val text: String,
    val status: QueueStatus = QueueStatus.Pending,
)

fun addItem(items: List<QueuedItem>, item: QueuedItem): List<QueuedItem> = items + item

fun removeItem(items: List<QueuedItem>, id: String): List<QueuedItem> = items.filterNot { it.id == id }

fun reorder(items: List<QueuedItem>, from: Int, to: Int): List<QueuedItem> {
    if (from !in items.indices || to !in items.indices || from == to) return items
    return items.toMutableList().also { list ->
        val item = list.removeAt(from)
        list.add(to, item)
    }
}

fun markInflight(items: List<QueuedItem>, ids: List<String>): List<QueuedItem> {
    val idSet = ids.toSet()
    return items.map { item ->
        if (item.id in idSet && item.status == QueueStatus.Pending) {
            item.copy(status = QueueStatus.Inflight)
        } else {
            item
        }
    }
}

fun revertInflight(items: List<QueuedItem>): List<QueuedItem> {
    return items.map { item ->
        if (item.status == QueueStatus.Inflight) item.copy(status = QueueStatus.Pending) else item
    }
}

fun applyAck(items: List<QueuedItem>, id: String): List<QueuedItem> = removeItem(items, id)

fun startEdit(items: List<QueuedItem>, id: String): List<QueuedItem> {
    return items.map { item ->
        when {
            item.id == id -> item.copy(status = QueueStatus.Editing)
            item.status == QueueStatus.Editing -> item.copy(status = QueueStatus.Pending)
            else -> item
        }
    }
}

fun finishEdit(items: List<QueuedItem>, id: String, text: String): List<QueuedItem> {
    return items.map { item ->
        if (item.id == id) item.copy(text = text, status = QueueStatus.Pending) else item
    }
}

fun cancelEdit(items: List<QueuedItem>, id: String): List<QueuedItem> {
    return items.map { item ->
        if (item.id == id && item.status == QueueStatus.Editing) {
            item.copy(status = QueueStatus.Pending)
        } else {
            item
        }
    }
}

fun nextFlushable(items: List<QueuedItem>): QueuedItem? {
    for (item in items) {
        when (item.status) {
            QueueStatus.Editing -> return null
            QueueStatus.Pending -> return item
            QueueStatus.Inflight -> Unit
        }
    }
    return null
}

fun restoreQueue(items: List<QueuedItem>): List<QueuedItem> {
    return items.map { item ->
        if (item.status == QueueStatus.Pending) item else item.copy(status = QueueStatus.Pending)
    }
}
