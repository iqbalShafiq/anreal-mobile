package co.ratmo.anreal.core.presentation

import co.ratmo.anreal.core.domain.util.DataError

fun DataError.toUiText(): UiText {
    if (this is DataError.Network && shouldShowServerMessage()) {
        return UiText.DynamicString(requireNotNull(serverMessage))
    }
    val key = when (this) {
        is DataError.Network -> when (kind) {
            DataError.Network.Kind.NO_INTERNET -> AnrealCopy.ERROR_NO_INTERNET
            DataError.Network.Kind.UNAUTHORIZED -> AnrealCopy.ERROR_UNAUTHORIZED
            DataError.Network.Kind.FORBIDDEN -> AnrealCopy.ERROR_FORBIDDEN
            DataError.Network.Kind.NOT_FOUND -> AnrealCopy.ERROR_NOT_FOUND
            DataError.Network.Kind.REQUEST_TIMEOUT -> AnrealCopy.ERROR_TIMEOUT
            DataError.Network.Kind.CONFLICT -> AnrealCopy.ERROR_CONFLICT
            DataError.Network.Kind.TOO_MANY_REQUESTS -> AnrealCopy.ERROR_TOO_MANY_REQUESTS
            DataError.Network.Kind.SERVER_ERROR,
            DataError.Network.Kind.SERVICE_UNAVAILABLE,
            -> AnrealCopy.ERROR_SERVER
            DataError.Network.Kind.BAD_REQUEST,
            DataError.Network.Kind.UNPROCESSABLE_ENTITY,
            DataError.Network.Kind.PAYLOAD_TOO_LARGE,
            DataError.Network.Kind.SERIALIZATION,
            DataError.Network.Kind.UNKNOWN,
            -> AnrealCopy.ERROR_UNKNOWN
        }
        DataError.Local.DISK_FULL -> AnrealCopy.ERROR_DISK_FULL
        DataError.Local.NOT_FOUND -> AnrealCopy.ERROR_NOT_FOUND
        DataError.Local.UNKNOWN -> AnrealCopy.ERROR_UNKNOWN
    }
    return UiText.StringResource(key)
}

private fun DataError.Network.shouldShowServerMessage(): Boolean {
    if (serverMessage.isNullOrBlank()) return false
    return kind != DataError.Network.Kind.UNAUTHORIZED &&
        kind != DataError.Network.Kind.SERVER_ERROR &&
        kind != DataError.Network.Kind.SERVICE_UNAVAILABLE
}
