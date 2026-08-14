package co.ratmo.anreal.feature.chat.domain

data class SessionDocument(
    val id: String,
    val filename: String,
    val summary: String = "",
)

data class CitedDocument(
    val id: String,
    val filename: String,
    val citationCount: Int,
)

data class RecentProject(
    val id: String,
    val name: String,
)
