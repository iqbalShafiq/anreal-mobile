package co.ratmo.anreal.core.domain.model

data class ChatSession(
    val id: String,
    val title: String,
    val updatedAt: String,
    val projectId: String? = null,
    val unread: Boolean = false,
    val streamId: String? = null,
    val lastEventId: Int = 0,
)
