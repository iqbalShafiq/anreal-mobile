package co.ratmo.anreal.core.presentation

import co.ratmo.anreal.core.domain.util.DataError

fun DataError.toUiText(): UiText {
    val key = when (this) {
        DataError.Network.NO_INTERNET -> AnrealCopy.ERROR_NO_INTERNET
        DataError.Network.UNAUTHORIZED -> AnrealCopy.ERROR_UNAUTHORIZED
        DataError.Network.FORBIDDEN -> AnrealCopy.ERROR_FORBIDDEN
        DataError.Network.NOT_FOUND -> AnrealCopy.ERROR_NOT_FOUND
        DataError.Network.REQUEST_TIMEOUT -> AnrealCopy.ERROR_TIMEOUT
        DataError.Network.CONFLICT -> AnrealCopy.ERROR_CONFLICT
        DataError.Network.TOO_MANY_REQUESTS -> AnrealCopy.ERROR_TOO_MANY_REQUESTS
        DataError.Network.SERVER_ERROR,
        DataError.Network.SERVICE_UNAVAILABLE,
        -> AnrealCopy.ERROR_SERVER
        DataError.Network.BAD_REQUEST,
        DataError.Network.PAYLOAD_TOO_LARGE,
        DataError.Network.SERIALIZATION,
        DataError.Network.UNKNOWN,
        -> AnrealCopy.ERROR_UNKNOWN
        DataError.Local.DISK_FULL -> AnrealCopy.ERROR_DISK_FULL
        DataError.Local.NOT_FOUND -> AnrealCopy.ERROR_NOT_FOUND
        DataError.Local.UNKNOWN -> AnrealCopy.ERROR_UNKNOWN
    }
    return UiText.StringResource(key)
}
