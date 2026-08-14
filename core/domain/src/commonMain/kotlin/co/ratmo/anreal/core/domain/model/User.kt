package co.ratmo.anreal.core.domain.model

data class User(
    val id: String,
    val email: String,
    val name: String,
    val imageUrl: String? = null,
)
