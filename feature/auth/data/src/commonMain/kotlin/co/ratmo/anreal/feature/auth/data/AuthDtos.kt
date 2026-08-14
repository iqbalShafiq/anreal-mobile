package co.ratmo.anreal.feature.auth.data

import co.ratmo.anreal.core.domain.model.User
import kotlinx.serialization.Serializable

@Serializable
data class AuthCredentialsDto(
    val email: String,
    val password: String,
    val name: String? = null,
)

@Serializable
data class AuthUserDto(
    val id: String,
    val email: String,
    val name: String = "",
    val image: String? = null,
)

@Serializable
data class AuthSessionResponseDto(
    val user: AuthUserDto? = null,
)

fun AuthUserDto.toUser(): User = User(
    id = id,
    email = email,
    name = name,
    imageUrl = image,
)
