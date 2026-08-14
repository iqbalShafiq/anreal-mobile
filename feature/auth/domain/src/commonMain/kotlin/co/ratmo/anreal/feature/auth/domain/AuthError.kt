package co.ratmo.anreal.feature.auth.domain

import co.ratmo.anreal.core.domain.util.DataError
import co.ratmo.anreal.core.domain.util.Error

sealed interface AuthError : Error {
    data object InvalidCredentials : AuthError
    data object EmailTaken : AuthError
    data class Network(val error: DataError.Network) : AuthError
}
